package dev.vincent.chaindocproof.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import dev.vincent.chaindocproof.interceptor.AdminTokenInterceptor;
import dev.vincent.chaindocproof.interceptor.UserTokenInterceptor;

/**
 * Web 配置：拦截器注册与 CORS
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserTokenInterceptor userTokenInterceptor;
    private final AdminTokenInterceptor adminTokenInterceptor;

    public WebConfig(UserTokenInterceptor userTokenInterceptor, AdminTokenInterceptor adminTokenInterceptor) {
        this.userTokenInterceptor = userTokenInterceptor;
        this.adminTokenInterceptor = adminTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userTokenInterceptor)
                .addPathPatterns("/api/user/**")
                .excludePathPatterns("/api/user/auth/login");
        registry.addInterceptor(adminTokenInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
