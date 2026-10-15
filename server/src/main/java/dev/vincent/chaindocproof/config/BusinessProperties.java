package dev.vincent.chaindocproof.config;

import java.math.BigInteger;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务配置（app.*）
 */
@ConfigurationProperties(prefix = "app")
public class BusinessProperties {

    private final Credit credit = new Credit();
    private final Upload upload = new Upload();
    private final Admin admin = new Admin();
    private final Token token = new Token();

    public Credit getCredit() {
        return credit;
    }

    public Upload getUpload() {
        return upload;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Token getToken() {
        return token;
    }

    /** 次数（credit）相关 */
    public static class Credit {
        /** 购买 1 次次数需要的 DocToken（wei） */
        private BigInteger unitPriceDocToken = BigInteger.TEN;
        /** 单笔购买次数上限 */
        private int purchaseMax = 1000;

        public BigInteger getUnitPriceDocToken() {
            return unitPriceDocToken;
        }

        public void setUnitPriceDocToken(BigInteger unitPriceDocToken) {
            this.unitPriceDocToken = unitPriceDocToken;
        }

        public int getPurchaseMax() {
            return purchaseMax;
        }

        public void setPurchaseMax(int purchaseMax) {
            this.purchaseMax = purchaseMax;
        }
    }

    /** 上传相关 */
    public static class Upload {
        /** 同一用户两次上传的最小间隔（秒） */
        private long cooldownSeconds = 60;

        public long getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(long cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    /** 管理端相关 */
    public static class Admin {
        /** 登录失败后的全局锁定时间（秒） */
        private long loginLockSeconds = 60;

        public long getLoginLockSeconds() {
            return loginLockSeconds;
        }

        public void setLoginLockSeconds(long loginLockSeconds) {
            this.loginLockSeconds = loginLockSeconds;
        }
    }

    /** 登录 token 相关 */
    public static class Token {
        /** token 有效期（小时） */
        private long ttlHours = 168;

        public long getTtlHours() {
            return ttlHours;
        }

        public void setTtlHours(long ttlHours) {
            this.ttlHours = ttlHours;
        }
    }
}
