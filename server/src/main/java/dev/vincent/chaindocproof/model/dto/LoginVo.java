package dev.vincent.chaindocproof.model.dto;

/**
 * 登录成功返回
 */
public class LoginVo {

    private String token;

    public LoginVo(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
