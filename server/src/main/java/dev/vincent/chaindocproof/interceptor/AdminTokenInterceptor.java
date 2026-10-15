package dev.vincent.chaindocproof.interceptor;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import dev.vincent.chaindocproof.support.Result;
import dev.vincent.chaindocproof.support.TokenStore;

/**
 * 管理端登录鉴权拦截器：校验 Authorization: Bearer &lt;token&gt;，
 * 通过后把管理员用户名放入 request 属性 adminUsername
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    private static final String OWNER_PREFIX = "admin:";

    private final TokenStore tokenStore;

    public AdminTokenInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String token = UserTokenInterceptor.extractToken(request);
        String owner;
        try {
            owner = tokenStore.resolve(token);
        } catch (dev.vincent.chaindocproof.support.BizException e) {
            writeUnauthorized(response, e.getMessage());
            return false;
        }
        if (!owner.startsWith(OWNER_PREFIX)) {
            writeUnauthorized(response, "token 类型不合法");
            return false;
        }
        request.setAttribute("adminUsername", owner.substring(OWNER_PREFIX.length()));
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(Result.fail(401, message)));
    }
}
