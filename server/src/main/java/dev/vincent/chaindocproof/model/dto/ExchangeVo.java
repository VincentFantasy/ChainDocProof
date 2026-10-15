package dev.vincent.chaindocproof.model.dto;

import java.math.BigInteger;

/**
 * DocToken ETH 兑换统计（读自链上合约）
 */
public class ExchangeVo {

    /** 兑换次数（合约 buyCount） */
    private BigInteger buyCount;

    public ExchangeVo(BigInteger buyCount) {
        this.buyCount = buyCount;
    }

    public BigInteger getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(BigInteger buyCount) {
        this.buyCount = buyCount;
    }
}
