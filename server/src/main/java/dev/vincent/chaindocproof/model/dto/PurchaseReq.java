package dev.vincent.chaindocproof.model.dto;

/**
 * 购买次数请求：先链上转 DocToken 给后端地址，再提交交易 hash 确认
 */
public class PurchaseReq {

    /** 购买次数 */
    private int quantity;
    /** 链上 Transfer 交易 hash */
    private String txHash;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
