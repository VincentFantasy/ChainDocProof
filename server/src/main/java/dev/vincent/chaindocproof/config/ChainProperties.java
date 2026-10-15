package dev.vincent.chaindocproof.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 链连接配置（chain.*）
 */
@ConfigurationProperties(prefix = "chain")
public class ChainProperties {

    /** RPC 地址 */
    private String rpcUrl;
    /** 链 ID（Sepolia=11155111） */
    private int chainId;
    /** 后端/管理端钱包私钥 */
    private String adminPrivateKey;
    /** Gas 价格（Gwei） */
    private long gasPriceGwei;
    /** Gas 上限 */
    private long gasLimit;

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getAdminPrivateKey() {
        return adminPrivateKey;
    }

    public void setAdminPrivateKey(String adminPrivateKey) {
        this.adminPrivateKey = adminPrivateKey;
    }

    public long getGasPriceGwei() {
        return gasPriceGwei;
    }

    public void setGasPriceGwei(long gasPriceGwei) {
        this.gasPriceGwei = gasPriceGwei;
    }

    public long getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(long gasLimit) {
        this.gasLimit = gasLimit;
    }
}
