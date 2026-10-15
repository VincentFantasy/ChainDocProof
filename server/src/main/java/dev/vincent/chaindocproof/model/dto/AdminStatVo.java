package dev.vincent.chaindocproof.model.dto;

import java.math.BigInteger;

/**
 * 管理端统计概览
 */
public class AdminStatVo {

    /** 总用户数 */
    private long totalUsers;
    /** 存证总数 */
    private long totalProofs;
    /** 已上链存证数（status=STORED） */
    private long storedProofs;
    /** 累计购买次数 */
    private long totalPurchasedCredits;
    /** 管理端发放次数（发放记录条数） */
    private long totalMintCount;
    /** 管理端发放总量（wei） */
    private BigInteger mintedTotalWei;
    /** 链上 ETH 兑换次数（合约 buyCount） */
    private BigInteger buyCount;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalProofs() {
        return totalProofs;
    }

    public void setTotalProofs(long totalProofs) {
        this.totalProofs = totalProofs;
    }

    public long getStoredProofs() {
        return storedProofs;
    }

    public void setStoredProofs(long storedProofs) {
        this.storedProofs = storedProofs;
    }

    public long getTotalPurchasedCredits() {
        return totalPurchasedCredits;
    }

    public void setTotalPurchasedCredits(long totalPurchasedCredits) {
        this.totalPurchasedCredits = totalPurchasedCredits;
    }

    public long getTotalMintCount() {
        return totalMintCount;
    }

    public void setTotalMintCount(long totalMintCount) {
        this.totalMintCount = totalMintCount;
    }

    public BigInteger getMintedTotalWei() {
        return mintedTotalWei;
    }

    public void setMintedTotalWei(BigInteger mintedTotalWei) {
        this.mintedTotalWei = mintedTotalWei;
    }

    public BigInteger getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(BigInteger buyCount) {
        this.buyCount = buyCount;
    }
}
