package dev.vincent.chaindocproof.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.web3j.utils.Numeric;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.dao.CreditLogDao;
import dev.vincent.chaindocproof.dao.ProofDao;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.model.CreditLog;
import dev.vincent.chaindocproof.model.ProofRecord;
import dev.vincent.chaindocproof.model.User;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.FileHashUtil;

/**
 * 存证核心服务：上传（算 hash 不落盘）→ 存证（扣次数上链）→ 校验（对比 hash 扣次数）
 */
@Service
public class ProofService {

    private final ProofDao proofDao;
    private final UserDao userDao;
    private final CreditLogDao creditLogDao;
    private final ChainGateway chain;
    private final BusinessProperties props;
    /** 可注入时钟，便于单元测试做时间边界验证 */
    private final Clock clock;
    /** 用户最近一次上传时间（内存，MVP 单机足够） */
    private final Map<String, Long> lastUploadAt = new ConcurrentHashMap<>();

    public ProofService(ProofDao proofDao, UserDao userDao, CreditLogDao creditLogDao,
                        ChainGateway chain, BusinessProperties props, Clock clock) {
        this.proofDao = proofDao;
        this.userDao = userDao;
        this.creditLogDao = creditLogDao;
        this.chain = chain;
        this.props = props;
        this.clock = clock;
    }

    /**
     * 上传文件：
     * 1. 同一用户上传冷却检查（间隔小于配置秒数拒绝）
     * 2. 流式计算 SHA-256，文件内容不落盘、不缓存
     * 3. 生成 PENDING 存证记录
     */
    public ProofRecord upload(String address, String fileName, long fileSize, InputStream in) {
        User user = requireUser(address);
        if (fileName == null || fileName.isBlank()) {
            throw new BizException(400, "文件名不能为空");
        }
        long now = clock.millis();
        Long last = lastUploadAt.get(address.toLowerCase());
        long cooldownMs = props.getUpload().getCooldownSeconds() * 1000L;
        if (last != null && now - last < cooldownMs) {
            long waitSeconds = (cooldownMs - (now - last) + 999) / 1000;
            throw new BizException(429, "上传过于频繁，请 " + waitSeconds + " 秒后再试");
        }
        String hash;
        try {
            hash = FileHashUtil.sha256Hex(in);
        } catch (IOException e) {
            throw new BizException(400, "读取文件失败: " + e.getMessage());
        }
        ProofRecord record = new ProofRecord();
        record.setUserId(user.getId());
        record.setFileName(fileName);
        record.setFileSize(fileSize);
        record.setFileHash(hash);
        record.setUploadTs(now / 1000);
        record.setStatus(ProofRecord.STATUS_PENDING);
        proofDao.insert(record);
        lastUploadAt.put(address.toLowerCase(), now);
        return record;
    }

    /**
     * 存证上链：
     * 1. 记录必须存在、属于当前用户、状态为 PENDING
     * 2. 原子扣 1 次次数（余额不足直接失败）
     * 3. 调用 DocProof.storeProof(hash, ts, 用户地址) 上链（只写链上记录），链上失败则回补次数
     * 4. 回写交易 hash 与状态，记流水
     */
    public ProofRecord store(String address, long proofId) {
        User user = requireUser(address);
        ProofRecord proof = proofDao.findByIdAndUser(proofId, user.getId());
        if (proof == null) {
            throw new BizException(404, "存证记录不存在");
        }
        if (!ProofRecord.STATUS_PENDING.equals(proof.getStatus())) {
            throw new BizException(400, "该记录已上链，不能重复存证");
        }
        if (userDao.decrementCredit(user.getId(), 1) == 0) {
            throw new BizException(400, "次数不足，请先购买次数");
        }
        String txHash;
        try {
            txHash = chain.storeProof(Numeric.hexStringToByteArray(proof.getFileHash()), proof.getUploadTs(),
                    address.toLowerCase());
        } catch (BizException e) {
            // 链上失败，回补次数，避免用户白扣
            userDao.incrementCredit(user.getId(), 1);
            throw e;
        }
        proofDao.updateStored(proofId, txHash);
        creditLogDao.insert(user.getId(), CreditLog.TYPE_STORE, -1, proofId, null);
        proof.setStatus(ProofRecord.STATUS_STORED);
        proof.setTxHash(txHash);
        return proof;
    }

    /**
     * 校验文件：重新上传文件计算 SHA-256，与已上链的 hash 对比，扣 1 次次数
     *
     * @return true=文件未被篡改，false=文件已被篡改
     */
    public boolean verify(String address, long proofId, InputStream in) {
        User user = requireUser(address);
        ProofRecord proof = proofDao.findByIdAndUser(proofId, user.getId());
        if (proof == null) {
            throw new BizException(404, "存证记录不存在");
        }
        if (!ProofRecord.STATUS_STORED.equals(proof.getStatus())) {
            throw new BizException(400, "仅已上链的记录可以校验");
        }
        String hash;
        try {
            hash = FileHashUtil.sha256Hex(in);
        } catch (IOException e) {
            throw new BizException(400, "读取文件失败: " + e.getMessage());
        }
        if (userDao.decrementCredit(user.getId(), 1) == 0) {
            throw new BizException(400, "次数不足，请先购买次数");
        }
        creditLogDao.insert(user.getId(), CreditLog.TYPE_VERIFY, -1, proofId, null);
        return proof.getFileHash().equalsIgnoreCase(hash);
    }

    /** 用户存证记录列表 */
    public List<ProofRecord> listByUser(String address) {
        User user = requireUser(address);
        return proofDao.listByUser(user.getId());
    }

    private User requireUser(String address) {
        return userDao.findByAddress(address.toLowerCase())
                .orElseThrow(() -> new BizException(401, "用户不存在，请先登录"));
    }
}
