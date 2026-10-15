package dev.vincent.chaindocproof.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.config.ContractProperties;
import dev.vincent.chaindocproof.dao.CreditLogDao;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.model.CreditLog;
import dev.vincent.chaindocproof.support.BizException;

/**
 * PurchaseService 测试：数量边界、txHash 幂等、链上销毁金额校验（用户 burn 到 0 地址）
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final String ADDR = "0x" + "ab".repeat(20);
    private static final String ZERO_ADDR = "0x" + "0".repeat(40);
    private static final String TOKEN_ADDR = "0x" + "ef".repeat(20);
    private static final String TX_HASH = "0x" + "cd".repeat(32);

    @Mock
    private UserDao userDao;
    @Mock
    private CreditLogDao creditLogDao;
    @Mock
    private ChainGateway chain;

    private PurchaseService service;

    @BeforeEach
    void setUp() {
        ContractProperties contract = new ContractProperties();
        contract.setDocToken(TOKEN_ADDR);
        BusinessProperties props = new BusinessProperties();
        props.getCredit().setUnitPriceDocToken(BigInteger.TEN);
        props.getCredit().setPurchaseMax(100);
        service = new PurchaseService(userDao, creditLogDao, chain, contract, props);
    }

    /** 打桩进入链上校验前所需的全部 DAO/链调用 */
    private void stubHappyPath() {
        when(userDao.ensureUser(ADDR)).thenReturn(1L);
        when(creditLogDao.existsByTxHash(TX_HASH)).thenReturn(false);
    }

    @Test
    void purchase_successQtyOne() {
        stubHappyPath();
        when(chain.hasTransfer(TX_HASH, TOKEN_ADDR, ADDR, ZERO_ADDR, BigInteger.TEN)).thenReturn(true);

        int credits = service.purchase(ADDR, 1, TX_HASH);

        assertEquals(1, credits);
        verify(userDao).incrementCredit(1, 1);
        verify(creditLogDao).insert(1, CreditLog.TYPE_PURCHASE, 1, null, TX_HASH);
    }

    @Test
    void purchase_qtyAtMaxBoundaryAllowed() {
        stubHappyPath();
        BigInteger expected = BigInteger.TEN.multiply(BigInteger.valueOf(100));
        when(chain.hasTransfer(TX_HASH, TOKEN_ADDR, ADDR, ZERO_ADDR, expected)).thenReturn(true);

        int credits = service.purchase(ADDR, 100, TX_HASH);

        assertEquals(100, credits);
        verify(userDao).incrementCredit(1, 100);
    }

    @Test
    void purchase_expectedAmountIsUnitPriceTimesQty() {
        stubHappyPath();
        when(chain.hasTransfer(anyString(), anyString(), anyString(), anyString(), eq(BigInteger.valueOf(50)))).thenReturn(true);

        service.purchase(ADDR, 5, TX_HASH);

        verify(chain).hasTransfer(TX_HASH, TOKEN_ADDR, ADDR, ZERO_ADDR, BigInteger.valueOf(50));
    }

    @Test
    void purchase_qtyZeroRejected() {
        BizException ex = assertThrows(BizException.class, () -> service.purchase(ADDR, 0, TX_HASH));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(chain);
    }

    @Test
    void purchase_qtyOverMaxRejected() {
        BizException ex = assertThrows(BizException.class, () -> service.purchase(ADDR, 101, TX_HASH));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(chain);
    }

    @Test
    void purchase_badTxHashRejected() {
        BizException ex = assertThrows(BizException.class, () -> service.purchase(ADDR, 1, "0x1234"));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(creditLogDao, chain);
    }

    @Test
    void purchase_duplicateTxHashRejected409() {
        when(userDao.ensureUser(ADDR)).thenReturn(1L);
        when(creditLogDao.existsByTxHash(TX_HASH)).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> service.purchase(ADDR, 1, TX_HASH));
        assertEquals(409, ex.getCode());
        verify(userDao, never()).incrementCredit(anyLong(), anyInt());
    }

    @Test
    void purchase_noMatchingTransferOnChainRejected() {
        stubHappyPath();
        when(chain.hasTransfer(TX_HASH, TOKEN_ADDR, ADDR, ZERO_ADDR, BigInteger.TEN)).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> service.purchase(ADDR, 1, TX_HASH));
        assertEquals(400, ex.getCode());
        verify(userDao, never()).incrementCredit(anyLong(), anyInt());
        verify(creditLogDao, never()).insert(anyLong(), anyString(), anyInt(), any(), anyString());
    }
}
