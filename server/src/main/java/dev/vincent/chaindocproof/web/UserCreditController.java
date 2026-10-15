package dev.vincent.chaindocproof.web;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.model.dto.CreditVo;
import dev.vincent.chaindocproof.model.dto.PurchaseReq;
import dev.vincent.chaindocproof.service.PurchaseService;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.Result;

import java.util.Map;

/**
 * 用户端次数（购买/查询）
 */
@RestController
@RequestMapping("/api/user/credits")
public class UserCreditController {

    private static final Logger log = LoggerFactory.getLogger(UserCreditController.class);

    private final UserDao userDao;
    private final PurchaseService purchaseService;
    private final BusinessProperties props;

    public UserCreditController(UserDao userDao, PurchaseService purchaseService,
                                BusinessProperties props) {
        this.userDao = userDao;
        this.purchaseService = purchaseService;
        this.props = props;
    }

    /** 查询当前次数余额与购买单价 */
    @GetMapping
    public Result<CreditVo> credits(HttpServletRequest request) {
        String address = (String) request.getAttribute("userAddress");
        log.debug("credits 入参: address={}", address);
        int balance = userDao.findByAddress(address.toLowerCase())
                .orElseThrow(() -> new BizException(401, "用户不存在，请先登录"))
                .getCreditBalance();
        return Result.ok(new CreditVo(balance, props.getCredit().getUnitPriceDocToken()));
    }

    /**
     * 确认购买次数：先链上销毁自己的 DocToken（金额 = 单价 × 数量），
     * 再携带销毁交易 hash 调用本接口
     */
    @PostMapping("/purchase")
    public Result<Map<String, Object>> purchase(HttpServletRequest request, @RequestBody PurchaseReq req) {
        String address = (String) request.getAttribute("userAddress");
        log.debug("purchase 入参: address={}, quantity={}, txHash={}",
                address, req.getQuantity(), req.getTxHash());
        int purchased = purchaseService.purchase(address, req.getQuantity(), req.getTxHash());
        int balance = userDao.findByAddress(address.toLowerCase()).orElseThrow().getCreditBalance();
        return Result.ok(Map.of("purchased", purchased, "balance", balance));
    }
}
