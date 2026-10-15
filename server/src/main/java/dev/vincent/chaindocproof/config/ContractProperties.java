package dev.vincent.chaindocproof.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 合约地址配置（contract.*），部署合约后填入
 */
@ConfigurationProperties(prefix = "contract")
public class ContractProperties {

    /** DocToken 合约地址 */
    private String docToken;
    /** DocProof 存证合约地址 */
    private String docProof;

    public String getDocToken() {
        return docToken;
    }

    public void setDocToken(String docToken) {
        this.docToken = docToken;
    }

    public String getDocProof() {
        return docProof;
    }

    public void setDocProof(String docProof) {
        this.docProof = docProof;
    }
}
