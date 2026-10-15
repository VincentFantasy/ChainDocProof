package dev.vincent.chaindocproof.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vincent.chaindocproof.model.dto.AdminLoginReq;
import dev.vincent.chaindocproof.model.dto.LoginVo;
import dev.vincent.chaindocproof.service.AdminService;
import dev.vincent.chaindocproof.support.Result;

/**
 * 管理端登录（账户密码，失败全局限流）
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 管理端登录 */
    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody AdminLoginReq req) {
        // 密码为敏感参数，不打印
        log.debug("login 入参: username={}", req.getUsername());
        String token = adminService.login(req.getUsername(), req.getPassword());
        return Result.ok(new LoginVo(token));
    }
}
