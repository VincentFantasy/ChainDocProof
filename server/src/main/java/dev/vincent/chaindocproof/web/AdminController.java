package dev.vincent.chaindocproof.web;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.vincent.chaindocproof.model.ProofRecord;
import dev.vincent.chaindocproof.model.TokenMint;
import dev.vincent.chaindocproof.model.dto.AdminStatVo;
import dev.vincent.chaindocproof.model.dto.CreditLogVo;
import dev.vincent.chaindocproof.model.dto.ExchangeRateReq;
import dev.vincent.chaindocproof.model.dto.ExchangeVo;
import dev.vincent.chaindocproof.model.dto.MintReq;
import dev.vincent.chaindocproof.model.dto.ProofVo;
import dev.vincent.chaindocproof.model.dto.UserVo;
import dev.vincent.chaindocproof.service.AdminService;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.Result;

/**
 * 管理端查询与操作
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 普通用户信息（含累计购买次数） */
    @GetMapping("/users")
    public Result<List<UserVo>> users() {
        log.debug("users 入参: 无");
        return Result.ok(adminService.listUsers());
    }

    /** 存证记录，可按钱包地址筛选（address 参数可省略查全部） */
    @GetMapping("/proofs")
    public Result<List<ProofVo>> proofs(@RequestParam(value = "address", required = false) String address) {
        log.debug("proofs 入参: address={}", address);
        List<ProofRecord> records = adminService.listProofs(address);
        return Result.ok(records.stream().map(ProofVo::from).toList());
    }

    /** DocToken 管理端发放记录 */
    @GetMapping("/tokens/mints")
    public Result<List<TokenMint>> mints() {
        log.debug("mints 入参: 无");
        return Result.ok(adminService.listMints());
    }

    /** DocToken 的 ETH 兑换次数（读链上合约） */
    @GetMapping("/tokens/exchanges")
    public Result<ExchangeVo> exchanges() {
        log.debug("exchanges 入参: 无");
        return Result.ok(adminService.getExchangeStat());
    }

    /** 查询当前 ETH 兑换汇率（读链上合约 ethPerToken，wei） */
    @GetMapping("/tokens/exchange-rate")
    public Result<Map<String, String>> exchangeRate() {
        log.debug("exchangeRate 入参: 无");
        return Result.ok(Map.of("ethPerToken", adminService.getEthPerToken().toString()));
    }

    /** 管理端调整 ETH 兑换汇率 {rate: "0.0001"}（1 枚 DocToken 对应的 ETH，十进制字符串） */
    @PostMapping("/tokens/exchange-rate")
    public Result<Map<String, String>> setExchangeRate(@RequestBody ExchangeRateReq req) {
        log.debug("setExchangeRate 入参: rate={}", req.getRate());
        BigInteger wei;
        try {
            // 最多 18 位小数（wei 精度），超出部分拒绝，避免静默截断
            wei = new BigDecimal(req.getRate()).movePointRight(18).toBigIntegerExact();
        } catch (Exception e) {
            throw new BizException(400, "汇率格式不合法（最多 18 位小数）");
        }
        String txHash = adminService.setEthPerToken(wei);
        return Result.ok(Map.of("txHash", txHash));
    }

    /** 管理端统计概览（用户/存证/购买/发放 + 链上兑换次数） */
    @GetMapping("/stats")
    public Result<AdminStatVo> stats() {
        log.debug("stats 入参: 无");
        return Result.ok(adminService.getStats());
    }

    /** 次数流水，可按钱包地址 / 类型（PURCHASE/STORE/VERIFY）筛选（参数均可省略） */
    @GetMapping("/credit-logs")
    public Result<List<CreditLogVo>> creditLogs(@RequestParam(value = "address", required = false) String address,
                                                @RequestParam(value = "type", required = false) String type) {
        log.debug("creditLogs 入参: address={}, type={}", address, type);
        return Result.ok(adminService.listCreditLogs(address, type));
    }

    /** 管理端发放 DocToken 到指定钱包 */
    @PostMapping("/tokens/mint")
    public Result<Map<String, String>> mint(HttpServletRequest request, @RequestBody MintReq req) {
        String username = (String) request.getAttribute("adminUsername");
        log.debug("mint 入参: admin={}, toAddress={}, amount={}",
                username, req.getToAddress(), req.getAmount());
        BigInteger amount;
        try {
            amount = new BigDecimal(req.getAmount()).movePointRight(18).toBigInteger();
        } catch (Exception e) {
            throw new BizException(400, "发放数量格式不合法");
        }
        String txHash = adminService.mint(username, req.getToAddress(), amount);
        return Result.ok(Map.of("txHash", txHash));
    }
}
