package dev.vincent.chaindocproof.model.dto;

import java.time.LocalDateTime;

import dev.vincent.chaindocproof.model.ProofRecord;

/**
 * 存证记录展示对象
 */
public class ProofVo {

    private long id;
    private String fileName;
    private long fileSize;
    private String fileHash;
    private long uploadTs;
    private String status;
    private String txHash;
    private LocalDateTime storedAt;
    private LocalDateTime createdAt;
    private String ownerAddress;

    public static ProofVo from(ProofRecord r) {
        ProofVo vo = new ProofVo();
        vo.id = r.getId();
        vo.fileName = r.getFileName();
        vo.fileSize = r.getFileSize();
        vo.fileHash = r.getFileHash();
        vo.uploadTs = r.getUploadTs();
        vo.status = r.getStatus();
        vo.txHash = r.getTxHash();
        vo.storedAt = r.getStoredAt();
        vo.createdAt = r.getCreatedAt();
        vo.ownerAddress = r.getOwnerAddress();
        return vo;
    }

    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getUploadTs() {
        return uploadTs;
    }

    public String getStatus() {
        return status;
    }

    public String getTxHash() {
        return txHash;
    }

    public LocalDateTime getStoredAt() {
        return storedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }
}
