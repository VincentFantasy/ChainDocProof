package dev.vincent.chaindocproof.service;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.dao.AdminDao;
import dev.vincent.chaindocproof.dao.CreditLogDao;
import dev.vincent.chaindocproof.dao.MintDao;
import dev.vincent.chaindocproof.dao.ProofDao;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.model.Admin;
import dev.vincent.chaindocproof.model.ProofRecord;
import dev.vincent.chaindocproof.model.TokenMint;
import dev.vincent.chaindocproof.model.User;
import dev.vincent.chaindocproof.model.dto.AdminStatVo;
import dev.vincent.chaindocproof.model.dto.CreditLogVo;
import dev.vincent.chaindocproof.model.dto.ExchangeVo;
import dev.vincent.chaindocproof.model.dto.UserVo;
import dev.vincent.chaindocproof.support.AddressUtil;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.TokenStore;

/**
 * 管理端服务：登录（失败全局限流）、用户/存证/发放/兑换查询、DocToken 发放、汇率调整
 */
@Service
public class AdminService {

    /** 汇率合理性上界：1 枚 DocToken 价值不应超过 1000 ETH（防填写汇率时小数点错位） */
    private static final BigInteger MAX_ETH_PER_TOKEN = BigInteger.valueOf(1_000)
            .multiply(BigInteger.TEN.pow(18));

    private final AdminDao adminDao;
    private final UserDao userDao;
    private final ProofDao proofDao;
    private final CreditLogDao creditLogDao;
    private final MintDao mintDao;
    private final TokenStore tokenStore;
    private final ChainGateway chain;
    private final BusinessProperties props;
    private final Clock clock;
    /** 全局登录锁定截止时间戳（毫秒）：任意一次失败后锁定，期间全部拒绝 */
    private final AtomicLong lockUntil = new AtomicLong(0);

    public AdminService(AdminDao adminDao, UserDao userDao, ProofDao proofDao, CreditLogDao creditLogDao,
                        MintDao mintDao, TokenStore tokenStore, ChainGateway chain,
                        BusinessProperties props, Clock clock) {
        this.adminDao = adminDao;
        this.userDao = userDao;
        this.proofDao = proofDao;
        this.creditLogDao = creditLogDao;
        this.mintDao = mintDao;
        this.tokenStore = tokenStore;
        this.chain = chain;
        this.props = props;
        this.clock = clock;
    }

    /**
     * 管理端登录：
     * 1. 处于全局锁定期内直接拒绝（不区分错误类型，避免探测）
     * 2. BCrypt 校验密码，失败则设置全局锁定（持续配置秒数）
     * 3. 成功签发 token
     */
    public String login(String username, String password) {
        long now = clock.millis();
        long until = lockUntil.get();
        if (now < until) {
            long waitSeconds = (until - now + 999) / 1000;
            throw new BizException(429, "登录失败次数过多，请 " + waitSeconds + " 秒后再试");
        }
        Admin admin = username == null ? null : adminDao.findByUsername(username).orElse(null);
        boolean ok = admin != null && password != null
                && BCrypt.checkpw(password, admin.getPasswordHash());
        if (!ok) {
            lockUntil.set(now + props.getAdmin().getLoginLockSeconds() * 1000L);
            throw new BizException(401, "用户名或密码错误");
        }
        return tokenStore.issue("admin:" + admin.getUsername(), Duration.ofHours(props.getToken().getTtlHours()));
    }

    /** 查询普通用户信息（含累计购买次数） */
    public List<UserVo> listUsers() {
        return userDao.listAll().stream().map(user -> {
            UserVo vo = new UserVo();
            vo.setAddress(user.getAddress());
            vo.setCreditBalance(user.getCreditBalance());
            vo.setPurchaseTotal(creditLogDao.sumPurchase(user.getId()));
            vo.setCreatedAt(user.getCreatedAt());
            return vo;
        }).toList();
    }

    /** 查询存证记录，可按钱包地址筛选（null 查全部） */
    public List<ProofRecord> listProofs(String address) {
        return proofDao.listAll(address);
    }

    /** 管理端 DocToken 发放记录 */
    public List<TokenMint> listMints() {
        return mintDao.listAll();
    }

    /** DocToken ETH 兑换次数（读链上合约 buyCount） */
    public ExchangeVo getExchangeStat() {
        return new ExchangeVo(chain.getBuyCount());
    }

    /** 查询当前链上汇率（1 枚 DocToken 对应的 ETH，wei） */
    public BigInteger getEthPerToken() {
        return chain.getEthPerToken();
    }

    /**
     * 管理端调整汇率：参数校验后调用合约 setEthPerToken
     *
     * @param ethPerTokenWei 新汇率（1 枚 DocToken 对应的 ETH，wei，必须大于 0）
     * @return 链上交易 hash
     */
    public String setEthPerToken(BigInteger ethPerTokenWei) {
        if (ethPerTokenWei == null || ethPerTokenWei.signum() <= 0) {
            throw new BizException(400, "汇率必须大于 0");
        }
        if (ethPerTokenWei.compareTo(MAX_ETH_PER_TOKEN) > 0) {
            throw new BizException(400, "汇率过大（上限 1000 ETH/DocToken）");
        }
        return chain.setEthPerToken(ethPerTokenWei);
    }

    /**
     * 管理端统计概览：用户数 / 存证数 / 累计购买 / 发放总量（DB）+ 链上兑换次数
     */
    public AdminStatVo getStats() {
        AdminStatVo vo = new AdminStatVo();
        vo.setTotalUsers(userDao.count());
        ProofDao.ProofCount pc = proofDao.countStats();
        vo.setTotalProofs(pc.total());
        vo.setStoredProofs(pc.stored());
        vo.setTotalPurchasedCredits(creditLogDao.sumAllPurchase());
        MintDao.MintCount mc = mintDao.countAndSum();
        vo.setTotalMintCount(mc.count());
        vo.setMintedTotalWei(mc.sumAmount());
        vo.setBuyCount(chain.getBuyCount());
        return vo;
    }

    /** 管理端：次数流水，可按钱包地址 / 类型筛选（null 或空串不过滤） */
    public List<CreditLogVo> listCreditLogs(String address, String type) {
        return creditLogDao.listAll(address, type);
    }

    /**
     * 管理端发放 DocToken：参数校验后调用合约 mint 并落库
     *
     * @return 链上交易 hash
     */
    public String mint(String adminUsername, String toAddress, BigInteger amount) {
        if (!AddressUtil.isValidAddress(toAddress)) {
            throw new BizException(400, "目标地址格式不合法");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BizException(400, "发放数量必须大于 0");
        }
        Admin admin = adminDao.findByUsername(adminUsername)
                .orElseThrow(() -> new BizException(401, "管理员不存在"));
        String txHash = chain.mint(toAddress, amount);
        mintDao.insert(admin.getId(), toAddress, amount, txHash);
        return txHash;
    }
}
