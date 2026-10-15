package dev.vincent.chaindocproof.model;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * DocToken 管理端发放记录（对应 t_token_mint）
 */
public class TokenMint {

    private long id;
    private long adminId;
    private String toAddress;
    private BigInteger amount;
    private String txHash;
    private LocalDateTime createdAt;
    /** 操作管理员用户名（仅查询展示用，不落库） */
    private String adminUsername;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAdminId() {
        return adminId;
    }

    public void setAdminId(long adminId) {
        this.adminId = adminId;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
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

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }
}
