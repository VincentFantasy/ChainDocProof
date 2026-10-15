package dev.vincent.chaindocproof.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vincent.chaindocproof.model.dto.LoginVo;
import dev.vincent.chaindocproof.model.dto.UserLoginReq;
import dev.vincent.chaindocproof.service.AuthService;
import dev.vincent.chaindocproof.support.Result;

/**
 * 用户端登录（钱包签名）
 */
@RestController
@RequestMapping("/api/user/auth")
public class UserAuthController {

    private static final Logger log = LoggerFactory.getLogger(UserAuthController.class);

    private final AuthService authService;

    public UserAuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 钱包签名登录：钱包需对 "ChainDocProof:" + 地址 做 EIP-191 签名
     */
    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody UserLoginReq req) {
        // signature 是钱包私钥对应的凭证，敏感参数不打印
        log.debug("login 入参: address={}", req.getAddress());
        String token = authService.login(req.getAddress(), req.getSignature());
        return Result.ok(new LoginVo(token));
    }
}
