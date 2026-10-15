package dev.vincent.chaindocproof.model.dto;

/**
 * 管理端发放 DocToken 请求
 */
public class MintReq {

    /** 目标钱包地址 */
    private String toAddress;
    /** 发放数量（18 位小数，字符串避免精度问题） */
    private String amount;

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
