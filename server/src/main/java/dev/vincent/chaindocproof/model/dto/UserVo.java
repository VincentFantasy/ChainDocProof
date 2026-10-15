package dev.vincent.chaindocproof.model.dto;

import java.time.LocalDateTime;

/**
 * 管理端查看的用户信息
 */
public class UserVo {

    private String address;
    private int creditBalance;
    /** 累计购买次数 */
    private long purchaseTotal;
    private LocalDateTime createdAt;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(int creditBalance) {
        this.creditBalance = creditBalance;
    }

    public long getPurchaseTotal() {
        return purchaseTotal;
    }

    public void setPurchaseTotal(long purchaseTotal) {
        this.purchaseTotal = purchaseTotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
