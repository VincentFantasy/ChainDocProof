package dev.vincent.chaindocproof.model;

import java.time.LocalDateTime;

/**
 * 次数流水（对应 t_credit_log）
 */
public class CreditLog {

    /** 类型：购买 */
    public static final String TYPE_PURCHASE = "PURCHASE";
    /** 类型：存证 */
    public static final String TYPE_STORE = "STORE";
    /** 类型：校验 */
    public static final String TYPE_VERIFY = "VERIFY";

    private long id;
    private long userId;
    private String type;
    private int amount;
    private Long proofId;
    private String txHash;
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
