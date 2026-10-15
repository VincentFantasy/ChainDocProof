package dev.vincent.chaindocproof.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import dev.vincent.chaindocproof.config.ChainProperties;
import dev.vincent.chaindocproof.config.ContractProperties;
import dev.vincent.chaindocproof.support.BizException;

/**
 * ChainGateway.hasTransfer 测试：购买次数时验证用户链上转账，
 * 重点覆盖公共 RPC 索引延迟时的轮询重查行为
 */
@ExtendWith(MockitoExtension.class)
class ChainGatewayTest {

    private static final String TOKEN_ADDR = "0x" + "ef".repeat(20);
    private static final String OTHER_TOKEN_ADDR = "0x" + "99".repeat(20);
    private static final String FROM = "0x" + "ab".repeat(20);
    private static final String TO = "0x" + "cd".repeat(20);
    private static final BigInteger VALUE = BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18));
    private static final String TX_HASH = "0x" + "12".repeat(32);

    @Mock
    private Web3j web3j;

    private ChainGateway gateway;

    @BeforeEach
    void setUp() {
        // 单测轮询间隔 0 避免真实等待，最多轮询 3 次
        gateway = new ChainGateway(web3j, new ChainProperties(), new ContractProperties(), 0L, 3);
    }

    /** 构造一条 RPC 响应（receipt 为 null 表示"查不到回执"） */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Request receiptRequest(TransactionReceipt receipt) {
        EthGetTransactionReceipt resp = new EthGetTransactionReceipt();
        resp.setResult(receipt);
        Request req = mock(Request.class);
        try {
            doReturn(resp).when(req).send();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return req;
    }

    /** 构造一个成功的回执，logs 含给定的事件 */
    private TransactionReceipt successReceipt(Log... logs) {
        TransactionReceipt receipt = new TransactionReceipt();
        receipt.setStatus("0x1");
        receipt.setLogs(List.of(logs));
        return receipt;
    }

    /** 构造一条 ERC20 Transfer 事件 log */
    private Log transferLog(String tokenAddr, String from, String to, BigInteger value) {
        Log log = new Log();
        log.setAddress(tokenAddr);
        log.setTopics(List.of(ChainGateway.TRANSFER_TOPIC,
                "0x" + "0".repeat(24) + from.substring(2),
                "0x" + "0".repeat(24) + to.substring(2)));
        log.setData("0x" + value.toString(16));
        return log;
    }

    @Test
    void hasTransfer_matchOnFirstPollReturnsTrue() {
        Request match = receiptRequest(successReceipt(transferLog(TOKEN_ADDR, FROM, TO, VALUE)));
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(match);

        assertTrue(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(1)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    void hasTransfer_receiptAppearsOnSecondPollReturnsTrue() {
        // 首轮查不到回执（RPC 尚未索引），第二轮出现 → 重试后验证成功
        Request empty = receiptRequest(null);
        Request match = receiptRequest(successReceipt(transferLog(TOKEN_ADDR, FROM, TO, VALUE)));
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(empty, match);

        assertTrue(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(2)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    void hasTransfer_emptyLogsThenMatchReturnsTrue() {
        // 首轮回执已返回但 logs 为空（公共节点刚确认时可能不完整），第二轮补齐
        Request emptyLogs = receiptRequest(successReceipt());
        Request match = receiptRequest(successReceipt(transferLog(TOKEN_ADDR, FROM, TO, VALUE)));
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(emptyLogs, match);

        assertTrue(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(2)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    void hasTransfer_logsCompleteButValueMismatchReturnsFalseImmediately() {
        // logs 非空但金额不匹配 → 确定没有这笔转账，不再轮询
        Request mismatch = receiptRequest(
                successReceipt(transferLog(TOKEN_ADDR, FROM, TO, VALUE.add(BigInteger.ONE))));
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(mismatch);

        assertFalse(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(1)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    void hasTransfer_otherTokenLogReturnsFalseImmediately() {
        // logs 非空但来自其他合约 → 确定不匹配，不再轮询
        Request otherToken = receiptRequest(successReceipt(transferLog(OTHER_TOKEN_ADDR, FROM, TO, VALUE)));
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(otherToken);

        assertFalse(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(1)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    void hasTransfer_wrongFromAddressReturnsFalseImmediately() {
        // 转账发起方不是登录地址（如用户用了钱包里的其他账户）→ 确定不匹配
        Request wrongFrom = receiptRequest(successReceipt(transferLog(TOKEN_ADDR, OTHER_TOKEN_ADDR, TO, VALUE)));
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(wrongFrom);

        assertFalse(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(1)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    void hasTransfer_receiptNeverAppearsReturnsFalseAfterMaxPolls() {
        // 始终查不到回执 → 轮询满上限后返回 false
        Request empty = receiptRequest(null);
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(empty);

        assertFalse(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(3)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    void hasTransfer_failedTxStatusReturnsFalseImmediately() {
        // 交易执行失败（revert）→ 转账未发生，直接 false
        TransactionReceipt failed = new TransactionReceipt();
        failed.setStatus("0x0");
        failed.setLogs(List.of());
        Request failedReq = receiptRequest(failed);
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(failedReq);

        assertFalse(gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        verify(web3j, times(1)).ethGetTransactionReceipt(TX_HASH);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void hasTransfer_rpcExceptionWrappedAsBizException500() {
        Request req = mock(Request.class);
        try {
            when(req.send()).thenThrow(new IOException("rpc down"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        when(web3j.ethGetTransactionReceipt(TX_HASH)).thenReturn(req);

        BizException ex = assertThrows(BizException.class,
                () -> gateway.hasTransfer(TX_HASH, TOKEN_ADDR, FROM, TO, VALUE));
        assertEquals(500, ex.getCode());
        assertTrue(ex.getMessage().startsWith("链上交易查询失败"));
    }
}
