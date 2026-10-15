package dev.vincent.chaindocproof.service;

import java.math.BigInteger;

import org.springframework.stereotype.Service;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.config.ContractProperties;
import dev.vincent.chaindocproof.dao.CreditLogDao;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.model.CreditLog;
import dev.vincent.chaindocproof.support.AddressUtil;
import dev.vincent.chaindocproof.support.BizException;

/**
 * 次数购买服务：用户先链上销毁自己的 DocToken（打进 0 地址），再提交交易 hash 由后端上链验证后发放次数
 */
@Service
public class PurchaseService {

    /** ERC20 0 地址（DocToken burn 的收款方），用于识别销毁转账 */
    private static final String ZERO_ADDRESS = "0x" + "0".repeat(40);

    private final UserDao userDao;
    private final CreditLogDao creditLogDao;
    private final ChainGateway chain;
    private final ContractProperties contract;
    private final BusinessProperties props;

    public PurchaseService(UserDao userDao, CreditLogDao creditLogDao, ChainGateway chain,
                           ContractProperties contract, BusinessProperties props) {
        this.userDao = userDao;
        this.creditLogDao = creditLogDao;
        this.chain = chain;
        this.contract = contract;
        this.props = props;
    }

    /**
     * 确认购买：
     * 1. 数量边界校验（1 ~ 单笔上限）
     * 2. 交易 hash 幂等校验（同一笔交易不能重复兑换）
     * 3. 上链验证该交易中存在 用户→0地址 的 DocToken 销毁，且金额 = 单价 × 数量
     * 4. 校验通过发放次数并记流水
     *
     * @return 购买到的次数
     */
    public int purchase(String address, int quantity, String txHash) {
        if (quantity < 1) {
            throw new BizException(400, "购买次数必须至少为 1");
        }
        if (quantity > props.getCredit().getPurchaseMax()) {
            throw new BizException(400, "超过单笔购买上限 " + props.getCredit().getPurchaseMax() + " 次");
        }
        if (!AddressUtil.isValidTxHash(txHash)) {
            throw new BizException(400, "交易 hash 格式不合法");
        }
        long userId = userDao.ensureUser(address.toLowerCase());
        if (creditLogDao.existsByTxHash(txHash)) {
            throw new BizException(409, "该交易已使用过，不能重复确认");
        }
        BigInteger expectedValue = props.getCredit().getUnitPriceDocToken().multiply(BigInteger.valueOf(quantity));
        boolean ok = chain.hasTransfer(txHash, contract.getDocToken(),
                address.toLowerCase(), ZERO_ADDRESS, expectedValue);
        if (!ok) {
            throw new BizException(400, "链上未找到匹配的 DocToken 销毁记录（需将 " + expectedValue
                    + " wei 的 DocToken 从当前钱包销毁）");
        }
        userDao.incrementCredit(userId, quantity);
        creditLogDao.insert(userId, CreditLog.TYPE_PURCHASE, quantity, null, txHash);
        return quantity;
    }
}
