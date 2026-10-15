package dev.vincent.chaindocproof.model;

import java.time.LocalDateTime;

/**
 * 存证记录（对应 t_proof）
 */
public class ProofRecord {

    /** 状态：待上链 */
    public static final String STATUS_PENDING = "PENDING";
    /** 状态：已上链 */
    public static final String STATUS_STORED = "STORED";

    private long id;
    private long userId;
    private String fileName;
    private long fileSize;
    /** 文件 SHA-256（小写 hex） */
    private String fileHash;
    /** 上传时间戳（秒），上链使用 */
    private long uploadTs;
    private String status;
    private String txHash;
    private LocalDateTime storedAt;
    private LocalDateTime createdAt;
    /** 所属钱包地址（仅查询展示用，不落库） */
    private String ownerAddress;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public long getUploadTs() {
        return uploadTs;
    }

    public void setUploadTs(long uploadTs) {
        this.uploadTs = uploadTs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public LocalDateTime getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(LocalDateTime storedAt) {
        this.storedAt = storedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }

    public void setOwnerAddress(String ownerAddress) {
        this.ownerAddress = ownerAddress;
    }
}
