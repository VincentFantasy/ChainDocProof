package dev.vincent.chaindocproof.model.dto;

import java.time.LocalDateTime;

/**
 * 管理端次数流水展示对象（t_credit_log JOIN t_user 带出地址）
 */
public class CreditLogVo {

    /** 所属钱包地址（JOIN t_user 带出，仅展示用） */
    private String address;
    private long userId;
    /** 类型：PURCHASE / STORE / VERIFY */
    private String type;
    /** 变动次数（正增负扣） */
    private int amount;
    private Long proofId;
    private String txHash;
    private LocalDateTime createdAt;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Long getProofId() {
        return proofId;
    }

    public void setProofId(Long proofId) {
        this.proofId = proofId;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
