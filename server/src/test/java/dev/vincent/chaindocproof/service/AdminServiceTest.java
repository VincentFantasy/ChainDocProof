package dev.vincent.chaindocproof.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCrypt;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.dao.AdminDao;
import dev.vincent.chaindocproof.dao.CreditLogDao;
import dev.vincent.chaindocproof.dao.MintDao;
import dev.vincent.chaindocproof.dao.ProofDao;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.model.Admin;
import dev.vincent.chaindocproof.model.TokenMint;
import dev.vincent.chaindocproof.model.User;
import dev.vincent.chaindocproof.model.dto.AdminStatVo;
import dev.vincent.chaindocproof.model.dto.CreditLogVo;
import dev.vincent.chaindocproof.model.dto.ExchangeVo;
import dev.vincent.chaindocproof.model.dto.UserVo;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.MutableClock;
import dev.vincent.chaindocproof.support.TokenStore;

/**
 * AdminService 测试：登录成功/失败/全局锁定边界、mint 参数边界、查询
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final String ADMIN_PASS = "admin123";
    private static final String TO_ADDR = "0x" + "ab".repeat(20);

    @Mock
    private AdminDao adminDao;
    @Mock
    private UserDao userDao;
    @Mock
    private ProofDao proofDao;
    @Mock
    private CreditLogDao creditLogDao;
    @Mock
    private MintDao mintDao;
    @Mock
    private ChainGateway chain;

    private final TokenStore tokenStore = new TokenStore();
    private final MutableClock clock = new MutableClock();
    private AdminService service;
    private Admin admin;

    @BeforeEach
    void setUp() {
        BusinessProperties props = new BusinessProperties();
        service = new AdminService(adminDao, userDao, proofDao, creditLogDao, mintDao,
                tokenStore, chain, props, clock);
        clock.set(0L);
        admin = new Admin();
        admin.setId(1);
        admin.setUsername("admin");
        // 低 cost 加速测试
        admin.setPasswordHash(BCrypt.hashpw(ADMIN_PASS, BCrypt.gensalt(4)));
    }

    // ---------- 登录 ----------

    @Test
    void login_successIssuesAdminToken() {
        when(adminDao.findByUsername("admin")).thenReturn(Optional.of(admin));

        String token = service.login("admin", ADMIN_PASS);

        assertEquals("admin:admin", tokenStore.resolve(token));
    }

    @Test
    void login_wrongPasswordTriggersGlobalLock() {
        when(adminDao.findByUsername("admin")).thenReturn(Optional.of(admin));

        BizException ex = assertThrows(BizException.class, () -> service.login("admin", "wrong"));
        assertEquals(401, ex.getCode());
        // 锁定后（配置 60s）立即重试被 429 拒绝
        BizException locked = assertThrows(BizException.class, () -> service.login("admin", ADMIN_PASS));
        assertEquals(429, locked.getCode());
        assertTrue(locked.getMessage().contains("60 秒"), "应提示等待 60 秒: " + locked.getMessage());
    }

    @Test
    void login_unknownUserRejectedAndLocked() {
        when(adminDao.findByUsername("nobody")).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.login("nobody", ADMIN_PASS));
        assertEquals(401, ex.getCode());
        BizException locked = assertThrows(BizException.class, () -> service.login("admin", ADMIN_PASS));
        assertEquals(429, locked.getCode());
    }

    @Test
    void login_lockExpiresAfterConfiguredSeconds() {
        when(adminDao.findByUsername("admin")).thenReturn(Optional.of(admin));
        // t=0 失败，锁定到 t=60s
        assertThrows(BizException.class, () -> service.login("admin", "wrong"));
        // 59s 仍在锁定内
        clock.advance(59_000L);
        BizException ex = assertThrows(BizException.class, () -> service.login("admin", ADMIN_PASS));
        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("1 秒"), "应提示等待 1 秒: " + ex.getMessage());
        // 恰好 60s 锁定到期，正确密码可登录
        clock.advance(1_000L);
        String token = service.login("admin", ADMIN_PASS);
        assertEquals("admin:admin", tokenStore.resolve(token));
    }

    // ---------- 查询 ----------

    @Test
    void listUsersMapsVoWithPurchaseTotal() {
        User user = new User();
        user.setId(1);
        user.setAddress(TO_ADDR);
        user.setCreditBalance(3);
        user.setCreatedAt(LocalDateTime.now());
        when(userDao.listAll()).thenReturn(List.of(user));
        when(creditLogDao.sumPurchase(1)).thenReturn(5L);

        List<UserVo> vos = service.listUsers();

        assertEquals(1, vos.size());
        assertEquals(TO_ADDR, vos.get(0).getAddress());
        assertEquals(3, vos.get(0).getCreditBalance());
        assertEquals(5L, vos.get(0).getPurchaseTotal());
    }

    @Test
    void listProofsDelegatesWithFilter() {
        when(proofDao.listAll(null)).thenReturn(List.of());
        assertTrue(service.listProofs(null).isEmpty());
        verify(proofDao).listAll(null);
    }

    @Test
    void listMintsDelegatesToDao() {
        when(mintDao.listAll()).thenReturn(List.of());
        assertTrue(service.listMints().isEmpty());
    }

    @Test
    void getExchangeStatReadsBuyCountFromChain() {
        when(chain.getBuyCount()).thenReturn(BigInteger.valueOf(7));
        ExchangeVo vo = service.getExchangeStat();
        assertEquals(BigInteger.valueOf(7), vo.getBuyCount());
    }

    @Test
    void getStatsAggregatesFromDaoAndChain() {
        when(userDao.count()).thenReturn(2L);
        when(proofDao.countStats()).thenReturn(new ProofDao.ProofCount(5, 3));
        when(creditLogDao.sumAllPurchase()).thenReturn(10L);
        when(mintDao.countAndSum()).thenReturn(new MintDao.MintCount(2, BigInteger.valueOf(20)));
        when(chain.getBuyCount()).thenReturn(BigInteger.valueOf(7));

        AdminStatVo vo = service.getStats();

        assertEquals(2L, vo.getTotalUsers());
        assertEquals(5L, vo.getTotalProofs());
        assertEquals(3L, vo.getStoredProofs());
        assertEquals(10L, vo.getTotalPurchasedCredits());
        assertEquals(2L, vo.getTotalMintCount());
        assertEquals(BigInteger.valueOf(20), vo.getMintedTotalWei());
        assertEquals(BigInteger.valueOf(7), vo.getBuyCount());
    }

    @Test
    void getStatsAllZeroWhenEmpty() {
        when(userDao.count()).thenReturn(0L);
        when(proofDao.countStats()).thenReturn(new ProofDao.ProofCount(0, 0));
        when(creditLogDao.sumAllPurchase()).thenReturn(0L);
        when(mintDao.countAndSum()).thenReturn(new MintDao.MintCount(0, BigInteger.ZERO));
        when(chain.getBuyCount()).thenReturn(BigInteger.ZERO);

        AdminStatVo vo = service.getStats();

        assertEquals(0L, vo.getTotalUsers());
        assertEquals(0L, vo.getTotalProofs());
        assertEquals(0L, vo.getStoredProofs());
        assertEquals(0L, vo.getTotalPurchasedCredits());
        assertEquals(0L, vo.getTotalMintCount());
        assertEquals(BigInteger.ZERO, vo.getMintedTotalWei());
        assertEquals(BigInteger.ZERO, vo.getBuyCount());
    }

    @Test
    void listCreditLogsDelegatesWithFilters() {
        when(creditLogDao.listAll(TO_ADDR, "PURCHASE")).thenReturn(List.of());
        assertTrue(service.listCreditLogs(TO_ADDR, "PURCHASE").isEmpty());
        verify(creditLogDao).listAll(TO_ADDR, "PURCHASE");
    }

    @Test
    void listCreditLogsPassesNullWhenNotFiltered() {
        CreditLogVo vo = new CreditLogVo();
        vo.setAddress(TO_ADDR);
        vo.setType("STORE");
        when(creditLogDao.listAll(null, null)).thenReturn(List.of(vo));

        List<CreditLogVo> vos = service.listCreditLogs(null, null);

        assertEquals(1, vos.size());
        assertEquals(TO_ADDR, vos.get(0).getAddress());
        verify(creditLogDao).listAll(null, null);
    }

    // ---------- 汇率 ----------

    @Test
    void getEthPerTokenReadsFromChain() {
        // 0.0001 ETH = 1e14 wei
        BigInteger rate = BigInteger.valueOf(100_000_000_000_000L);
        when(chain.getEthPerToken()).thenReturn(rate);

        assertEquals(rate, service.getEthPerToken());
    }

    @Test
    void setEthPerTokenSuccessCallsChain() {
        // 0.00005 ETH = 5e13 wei
        BigInteger rate = BigInteger.valueOf(50_000_000_000_000L);
        when(chain.setEthPerToken(rate)).thenReturn("0x" + "ee".repeat(32));

        String txHash = service.setEthPerToken(rate);

        assertEquals("0x" + "ee".repeat(32), txHash);
        verify(chain).setEthPerToken(rate);
    }

    @Test
    void setEthPerTokenNonPositiveRejected() {
        // 边界值：null / 0 / 负数一律拒绝
        for (BigInteger rate : new BigInteger[]{null, BigInteger.ZERO, BigInteger.valueOf(-1)}) {
            BizException ex = assertThrows(BizException.class, () -> service.setEthPerToken(rate));
            assertEquals(400, ex.getCode());
        }
        verifyNoInteractions(chain);
    }

    @Test
    void setEthPerTokenAtUpperBoundAccepted() {
        // 边界值：恰好等于上界（1000 ETH/DocToken）放行
        BigInteger upper = BigInteger.valueOf(1_000).multiply(BigInteger.TEN.pow(18));
        when(chain.setEthPerToken(upper)).thenReturn("0x" + "ff".repeat(32));

        String txHash = service.setEthPerToken(upper);

        assertEquals("0x" + "ff".repeat(32), txHash);
    }

    @Test
    void setEthPerTokenOverUpperBoundRejected() {
        // 边界值：超过上界 1 wei 拒绝
        BigInteger tooLarge = BigInteger.valueOf(1_000).multiply(BigInteger.TEN.pow(18)).add(BigInteger.ONE);
        BizException ex = assertThrows(BizException.class, () -> service.setEthPerToken(tooLarge));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(chain);
    }

    // ---------- mint ----------

    @Test
    void mint_successCallsChainAndPersists() {
        when(adminDao.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(chain.mint(TO_ADDR, BigInteger.TEN)).thenReturn("0x" + "cd".repeat(32));

        String txHash = service.mint("admin", TO_ADDR, BigInteger.TEN);

        assertEquals("0x" + "cd".repeat(32), txHash);
        verify(mintDao).insert(1, TO_ADDR, BigInteger.TEN, txHash);
    }

    @Test
    void mint_invalidAddressRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> service.mint("admin", "0x123", BigInteger.TEN));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(chain);
    }

    @Test
    void mint_nonPositiveAmountRejected() {
        for (BigInteger amount : new BigInteger[]{null, BigInteger.ZERO, BigInteger.valueOf(-1)}) {
            BizException ex = assertThrows(BizException.class,
                    () -> service.mint("admin", TO_ADDR, amount));
            assertEquals(400, ex.getCode());
        }
        verifyNoInteractions(chain);
    }

    @Test
    void mint_unknownAdminRejected401() {
        when(adminDao.findByUsername("nobody")).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class,
                () -> service.mint("nobody", TO_ADDR, BigInteger.TEN));
        assertEquals(401, ex.getCode());
        verifyNoInteractions(chain);
        verify(mintDao, org.mockito.Mockito.never()).insert(anyLong(), anyString(), any(BigInteger.class), anyString());
    }
}
