package dev.vincent.chaindocproof.model.dto;

/**
 * 用户钱包签名登录请求
 */
public class UserLoginReq {

    /** 钱包地址 */
    private String address;
    /** 钱包对 "ChainDocProof:&lt;address&gt;" 的 EIP-191 签名（hex） */
    private String signature;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
