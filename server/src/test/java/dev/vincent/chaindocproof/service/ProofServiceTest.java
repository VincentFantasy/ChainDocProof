package dev.vincent.chaindocproof.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.web3j.utils.Numeric;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.dao.CreditLogDao;
import dev.vincent.chaindocproof.dao.ProofDao;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.model.CreditLog;
import dev.vincent.chaindocproof.model.ProofRecord;
import dev.vincent.chaindocproof.model.User;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.MutableClock;

/**
 * ProofService 测试：覆盖上传冷却边界、SHA-256 向量、存证扣次数/回补、校验对比等分支
 */
@ExtendWith(MockitoExtension.class)
class ProofServiceTest {

    private static final String ADDR = "0x" + "ab".repeat(20);
    private static final String TX_HASH = "0x" + "cd".repeat(32);
    /** "abc" 的 SHA-256 */
    private static final String ABC_HASH = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Mock
    private ProofDao proofDao;
    @Mock
    private UserDao userDao;
    @Mock
    private CreditLogDao creditLogDao;
    @Mock
    private ChainGateway chain;

    private final MutableClock clock = new MutableClock();
    private ProofService service;
    private User user;

    @BeforeEach
    void setUp() {
        BusinessProperties props = new BusinessProperties();
        service = new ProofService(proofDao, userDao, creditLogDao, chain, props, clock);
        clock.set(1_000_000_000L);
        user = new User();
        user.setId(1);
        user.setAddress(ADDR);
    }

    private void mockLoggedIn() {
        when(userDao.findByAddress(ADDR)).thenReturn(java.util.Optional.of(user));
    }

    private static ByteArrayInputStream bytes(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    // ---------- 上传 ----------

    @Test
    void upload_successComputesSha256AndInsertsPending() {
        mockLoggedIn();
        ProofRecord record = service.upload(ADDR, "contract.pdf", 3, bytes("abc"));

        assertEquals(ABC_HASH, record.getFileHash());
        assertEquals(ProofRecord.STATUS_PENDING, record.getStatus());
        assertEquals("contract.pdf", record.getFileName());
        ArgumentCaptor<ProofRecord> captor = ArgumentCaptor.forClass(ProofRecord.class);
        verify(proofDao).insert(captor.capture());
        assertEquals(1, captor.getValue().getUserId());
        verify(userDao, never()).decrementCredit(anyLong(), anyInt());
    }

    @Test
    void upload_blankFileNameRejected() {
        mockLoggedIn();
        BizException ex = assertThrows(BizException.class,
                () -> service.upload(ADDR, "  ", 1, bytes("x")));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(proofDao);
    }

    @Test
    void upload_notLoggedInRejected() {
        when(userDao.findByAddress(ADDR)).thenReturn(java.util.Optional.empty());
        BizException ex = assertThrows(BizException.class,
                () -> service.upload(ADDR, "a.txt", 1, bytes("x")));
        assertEquals(401, ex.getCode());
        verifyNoInteractions(proofDao);
    }

    @Test
    void upload_cooldownBoundary59sRejected60sAllowed() {
        mockLoggedIn();
        // 第一次上传成功（t=1000000000）
        service.upload(ADDR, "a.txt", 3, bytes("abc"));
        // 59 秒后再传：仍在冷却期，拒绝
        clock.advance(59_000L);
        BizException ex = assertThrows(BizException.class,
                () -> service.upload(ADDR, "b.txt", 3, bytes("abc")));
        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("1 秒"), "应提示等待 1 秒: " + ex.getMessage());
        // 恰好 60 秒：放行
        clock.advance(1_000L);
        ProofRecord record = service.upload(ADDR, "c.txt", 3, bytes("abc"));
        assertEquals(ProofRecord.STATUS_PENDING, record.getStatus());
    }

    @Test
    void upload_uppercaseAddressUsesSameCooldown() {
        mockLoggedIn();
        service.upload(ADDR, "a.txt", 3, bytes("abc"));
        clock.advance(10_000L);
        BizException ex = assertThrows(BizException.class,
                () -> service.upload(ADDR.toUpperCase(), "b.txt", 3, bytes("abc")));
        assertEquals(429, ex.getCode());
    }

    // ---------- 存证 ----------

    private ProofRecord pendingProof() {
        ProofRecord p = new ProofRecord();
        p.setId(10);
        p.setUserId(1);
        p.setFileName("a.txt");
        p.setFileHash(ABC_HASH);
        p.setUploadTs(clock.millis() / 1000);
        p.setStatus(ProofRecord.STATUS_PENDING);
        return p;
    }

    @Test
    void store_successDeductsCreditWritesTxHashAndLog() {
        mockLoggedIn();
        ProofRecord proof = pendingProof();
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(proof);
        when(userDao.decrementCredit(1, 1)).thenReturn(1);
        when(chain.storeProof(Numeric.hexStringToByteArray(ABC_HASH), proof.getUploadTs())).thenReturn(TX_HASH);

        ProofRecord result = service.store(ADDR, 10);

        assertEquals(ProofRecord.STATUS_STORED, result.getStatus());
        assertEquals(TX_HASH, result.getTxHash());
        verify(proofDao).updateStored(10, TX_HASH);
        verify(creditLogDao).insert(1, CreditLog.TYPE_STORE, -1, 10L, null);
    }

    @Test
    void store_insufficientCreditRejectedAndChainNotCalled() {
        mockLoggedIn();
        ProofRecord proof = pendingProof();
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(proof);
        when(userDao.decrementCredit(1, 1)).thenReturn(0);

        BizException ex = assertThrows(BizException.class, () -> service.store(ADDR, 10));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(chain);
    }

    @Test
    void store_notFoundReturns404() {
        mockLoggedIn();
        when(proofDao.findByIdAndUser(99, 1)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service.store(ADDR, 99));
        assertEquals(404, ex.getCode());
    }

    @Test
    void store_alreadyStoredRejected() {
        mockLoggedIn();
        ProofRecord proof = pendingProof();
        proof.setStatus(ProofRecord.STATUS_STORED);
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(proof);
        BizException ex = assertThrows(BizException.class, () -> service.store(ADDR, 10));
        assertEquals(400, ex.getCode());
    }

    @Test
    void store_chainFailureRefundsCredit() {
        mockLoggedIn();
        ProofRecord proof = pendingProof();
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(proof);
        when(userDao.decrementCredit(1, 1)).thenReturn(1);
        when(chain.storeProof(any(byte[].class), anyLong()))
                .thenThrow(new BizException(500, "链上交易失败"));

        BizException ex = assertThrows(BizException.class, () -> service.store(ADDR, 10));
        assertEquals(500, ex.getCode());
        // 链上失败必须回补次数
        verify(userDao).incrementCredit(1, 1);
        verify(proofDao, never()).updateStored(anyLong(), anyString());
        verifyNoInteractions(creditLogDao);
    }

    // ---------- 校验 ----------

    private ProofRecord storedProof() {
        ProofRecord p = pendingProof();
        p.setStatus(ProofRecord.STATUS_STORED);
        p.setTxHash(TX_HASH);
        return p;
    }

    @Test
    void verify_matchingFileReturnsTrue() {
        mockLoggedIn();
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(storedProof());
        when(userDao.decrementCredit(1, 1)).thenReturn(1);

        boolean ok = service.verify(ADDR, 10, bytes("abc"));

        assertTrue(ok);
        verify(creditLogDao).insert(1, CreditLog.TYPE_VERIFY, -1, 10L, null);
    }

    @Test
    void verify_tamperedFileReturnsFalse() {
        mockLoggedIn();
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(storedProof());
        when(userDao.decrementCredit(1, 1)).thenReturn(1);

        boolean ok = service.verify(ADDR, 10, bytes("abd"));

        assertFalse(ok);
    }

    @Test
    void verify_pendingProofRejected() {
        mockLoggedIn();
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(pendingProof());
        BizException ex = assertThrows(BizException.class, () -> service.verify(ADDR, 10, bytes("abc")));
        assertEquals(400, ex.getCode());
    }

    @Test
    void verify_insufficientCreditRejected() {
        mockLoggedIn();
        when(proofDao.findByIdAndUser(10, 1)).thenReturn(storedProof());
        when(userDao.decrementCredit(1, 1)).thenReturn(0);
        BizException ex = assertThrows(BizException.class, () -> service.verify(ADDR, 10, bytes("abc")));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(creditLogDao);
    }

    @Test
    void verify_notFoundReturns404() {
        mockLoggedIn();
        when(proofDao.findByIdAndUser(99, 1)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service.verify(ADDR, 99, bytes("abc")));
        assertEquals(404, ex.getCode());
    }

    // ---------- 列表 ----------

    @Test
    void listByUserDelegatesToDao() {
        mockLoggedIn();
        ProofRecord p = pendingProof();
        when(proofDao.listByUser(1)).thenReturn(List.of(p));
        assertEquals(List.of(p), service.listByUser(ADDR));
    }
}
