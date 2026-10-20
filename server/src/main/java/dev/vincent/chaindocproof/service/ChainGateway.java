package dev.vincent.chaindocproof.service;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import dev.vincent.chaindocproof.config.ChainProperties;
import dev.vincent.chaindocproof.config.ContractProperties;
import dev.vincent.chaindocproof.support.BizException;

/**
 * 链交互网关：封装 web3j 对 DocToken / DocProof 合约的读写
 */
@Service
public class ChainGateway {

    /** ERC20 Transfer 事件 topic0 */
    public static final String TRANSFER_TOPIC = Numeric.toHexString(
            Hash.sha3("Transfer(address,address,uint256)".getBytes()));

    private static final BigInteger GWEI = BigInteger.valueOf(1_000_000_000L);

    /** 交易回执轮询：间隔 2 秒，最多 30 次（约 1 分钟） */
    private static final long RECEIPT_POLL_MS = 2_000L;
    private static final int RECEIPT_POLL_MAX = 30;

    /**
     * 购买验证轮询：间隔 2 秒，最多 10 次（约 20 秒，需小于前端 60 秒请求超时）。
     * 前端在钱包自己的 RPC 上确认交易，后端用配置的公共 RPC 查回执，
     * 两个节点对刚出块的交易可能存在数秒索引延迟，单次查询会误判为"无转账"
     */
    private static final long HAS_TRANSFER_POLL_MS = 2_000L;
    private static final int HAS_TRANSFER_POLL_MAX = 10;

    private final Web3j web3j;
    private final ChainProperties chain;
    private final ContractProperties contract;
    private final long hasTransferPollMs;
    private final int hasTransferPollMax;

    /** 多构造器时必须显式标注，否则 Spring 会回退找无参构造器导致启动失败 */
    @Autowired
    public ChainGateway(Web3j web3j, ChainProperties chain, ContractProperties contract) {
        this(web3j, chain, contract, HAS_TRANSFER_POLL_MS, HAS_TRANSFER_POLL_MAX);
    }

    /** 单测用：注入购买验证轮询间隔与次数，避免真实等待 */
    ChainGateway(Web3j web3j, ChainProperties chain, ContractProperties contract,
                 long hasTransferPollMs, int hasTransferPollMax) {
        this.web3j = web3j;
        this.chain = chain;
        this.contract = contract;
        this.hasTransferPollMs = hasTransferPollMs;
        this.hasTransferPollMax = hasTransferPollMax;
    }

    private Credentials credentials() {
        return Credentials.create(chain.getAdminPrivateKey());
    }

    /**
     * 以后端钱包签名发送合约交易，轮询回执确认执行结果，返回交易 hash
     *
     * @param to   合约地址
     * @param data 编码后的函数调用数据
     */
    public String sendTx(String to, String data) {
        try {
            RawTransactionManager tm = new RawTransactionManager(web3j, credentials());
            EthSendTransaction sent = tm.sendTransaction(
                    BigInteger.valueOf(chain.getGasPriceGwei()).multiply(GWEI),
                    BigInteger.valueOf(chain.getGasLimit()),
                    to, data, BigInteger.ZERO, false);
            if (sent.hasError()) {
                throw new BizException(500, "链上交易发送失败: " + sent.getError().getMessage());
            }
            String txHash = sent.getTransactionHash();
            TransactionReceipt receipt = waitReceipt(txHash);
            if (receipt == null) {
                throw new BizException(500, "等待链上交易回执超时: " + txHash);
            }
            if (!receipt.isStatusOK()) {
                throw new BizException(500, "链上交易执行失败（revert）: " + txHash);
            }
            return txHash;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "链上交易失败: " + e.getMessage());
        }
    }

    /** 轮询等待交易回执，超时返回 null */
    private TransactionReceipt waitReceipt(String txHash) throws Exception {
        for (int i = 0; i < RECEIPT_POLL_MAX; i++) {
            Optional<TransactionReceipt> opt =
                    web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            if (opt.isPresent()) {
                return opt.get();
            }
            Thread.sleep(RECEIPT_POLL_MS);
        }
        return null;
    }

    /** 合约只读调用并解码返回值 */
    public List<Type> call(String to, String data, List<TypeReference<Type>> outputs) {
        try {
            Transaction tx = Transaction.createEthCallTransaction(null, to, data, BigInteger.ZERO);
            EthCall result = web3j.ethCall(tx, DefaultBlockParameter.valueOf("latest")).send();
            if (result.hasError()) {
                throw new BizException(500, "链上查询失败: " + result.getError().getMessage());
            }
            String value = result.getValue();
            if (value == null || value.equals("0x")) {
                throw new BizException(500, "链上查询无返回值（请检查合约地址配置）");
            }
            return FunctionReturnDecoder.decode(value, outputs);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "链上查询失败: " + e.getMessage());
        }
    }

    /**
     * 存证上链：调用 DocProof.storeProof(hash, timestamp, user)，只写入链上记录
     * （DocToken 在购买次数时已由用户 burn 销毁，此处不再消耗代币）
     *
     * @param hash      文件 SHA-256 字节
     * @param timestamp 文件上传时间戳（秒）
     * @param user      存证用户钱包地址（本合约 msg.sender 恒为后端钱包，用户地址由后端显式传入）
     * @return 交易 hash
     */
    public String storeProof(byte[] hash, long timestamp, String user) {
        if (hash == null || hash.length != 32) {
            throw new BizException(400, "存证 hash 必须为 32 字节（SHA-256）");
        }
        // 合约参数类型为 bytes32，必须用静态 Bytes32 编码（不能用动态 bytes）
        Function function = new Function("storeProof",
                List.of(new Bytes32(hash), new Uint256(BigInteger.valueOf(timestamp)), new Address(user)), List.of());
        return sendTx(contract.getDocProof(), FunctionEncoder.encode(function));
    }

    /**
     * 管理端发放 DocToken：调用 DocToken.mint(to, amount)
     *
     * @return 交易 hash
     */
    public String mint(String to, BigInteger amount) {
        Function function = new Function("mint",
                List.of(new Address(to), new Uint256(amount)), List.of());
        return sendTx(contract.getDocToken(), FunctionEncoder.encode(function));
    }

    /**
     * 查询 DocToken 合约的当前汇率（1 枚 DocToken 对应的 ETH，wei）
     */
    public BigInteger getEthPerToken() {
        Function function = new Function("ethPerToken", List.of(),
                List.of(new TypeReference<Uint256>() {
                }));
        List<Type> out = call(contract.getDocToken(), FunctionEncoder.encode(function),
                function.getOutputParameters());
        return ((Uint256) out.get(0)).getValue();
    }

    /**
     * 管理端调整汇率：调用 DocToken.setEthPerToken(ethPerTokenWei)
     *
     * @param ethPerTokenWei 新汇率（1 枚 DocToken 对应的 ETH，wei）
     * @return 交易 hash
     */
    public String setEthPerToken(BigInteger ethPerTokenWei) {
        Function function = new Function("setEthPerToken",
                List.of(new Uint256(ethPerTokenWei)), List.of());
        return sendTx(contract.getDocToken(), FunctionEncoder.encode(function));
    }

    /** 查询 DocToken 合约的 ETH 兑换次数（buyCount） */
    public BigInteger getBuyCount() {
        Function function = new Function("buyCount", List.of(),
                List.of(new TypeReference<Uint256>() {
                }));
        List<Type> out = call(contract.getDocToken(), FunctionEncoder.encode(function),
                function.getOutputParameters());
        return ((Uint256) out.get(0)).getValue();
    }

    /**
     * 查询某笔交易里是否发生了指定 token 的 Transfer(from → to, value)
     * 用于购买次数时验证用户的链上转账
     *
     * 前端在钱包 RPC 上确认交易后立刻提交，后端所用公共 RPC 对刚出块的交易
     * 可能尚未索引（回执为空，或回执已返回但 logs 为空），因此轮询几次：
     * - 查不到回执 → 等下一轮（节点传播延迟）；
     * - 回执失败（status 非 0x1）→ 转账未发生，直接 false；
     * - 回执 logs 为空 → 等下一轮（公共节点刚确认时可能返回不完整回执）；
     * - 回执 logs 非空但没有匹配 → 确定没有这笔转账，直接 false。
     *
     * @return 匹配返回 true
     */
    public boolean hasTransfer(String txHash, String tokenAddress, String from, String to, BigInteger value) {
        try {
            for (int i = 0; i < hasTransferPollMax; i++) {
                Optional<TransactionReceipt> opt =
                        web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
                if (opt.isPresent()) {
                    TransactionReceipt receipt = opt.get();
                    if (!receipt.isStatusOK()) {
                        return false;
                    }
                    if (matchesTransfer(receipt, tokenAddress, from, to, value)) {
                        return true;
                    }
                    if (!receipt.getLogs().isEmpty()) {
                        return false;
                    }
                }
                Thread.sleep(hasTransferPollMs);
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(500, "链上交易查询被中断: " + e.getMessage());
        } catch (Exception e) {
            throw new BizException(500, "链上交易查询失败: " + e.getMessage());
        }
    }

    /** 检查回执 logs 中是否存在指定 token 的 ERC20 Transfer(from → to, value) */
    private boolean matchesTransfer(TransactionReceipt receipt, String tokenAddress,
                                    String from, String to, BigInteger value) {
        for (Log logItem : receipt.getLogs()) {
            if (!logItem.getAddress().equalsIgnoreCase(tokenAddress)) {
                continue;
            }
            List<String> topics = logItem.getTopics();
            if (topics.size() < 3 || !TRANSFER_TOPIC.equalsIgnoreCase(topics.get(0))) {
                continue;
            }
            String logFrom = addressFromTopic(topics.get(1));
            String logTo = addressFromTopic(topics.get(2));
            BigInteger logValue = parseValue(logItem.getData());
            if (logFrom.equalsIgnoreCase(from) && logTo.equalsIgnoreCase(to) && logValue.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** 从 32 字节 topic 中取地址（末 20 字节） */
    private static String addressFromTopic(String topic) {
        String hex = topic.startsWith("0x") ? topic.substring(2) : topic;
        return "0x" + hex.substring(hex.length() - 40);
    }

    /** 解析 Transfer 事件 data（value） */
    private static BigInteger parseValue(String data) {
        if (data == null || data.isEmpty() || "0x".equals(data)) {
            return BigInteger.ZERO;
        }
        return new BigInteger(1, Numeric.hexStringToByteArray(data));
    }
}
