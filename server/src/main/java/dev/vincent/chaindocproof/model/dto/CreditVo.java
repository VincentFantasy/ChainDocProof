package dev.vincent.chaindocproof.model.dto;

import java.math.BigInteger;

/**
 * 用户次数信息
 */
public class CreditVo {

    private int balance;
    /** 购买 1 次需要的 DocToken（wei，18 位小数，购买时销毁该数量） */
    private BigInteger unitPriceDocToken;

    public CreditVo(int balance, BigInteger unitPriceDocToken) {
        this.balance = balance;
        this.unitPriceDocToken = unitPriceDocToken;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public BigInteger getUnitPriceDocToken() {
        return unitPriceDocToken;
    }

    public void setUnitPriceDocToken(BigInteger unitPriceDocToken) {
        this.unitPriceDocToken = unitPriceDocToken;
    }
}
