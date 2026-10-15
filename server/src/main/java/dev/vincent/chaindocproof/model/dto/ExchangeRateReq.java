package dev.vincent.chaindocproof.model.dto;

/**
 * 管理端调整汇率请求
 */
public class ExchangeRateReq {

    /** 新汇率：1 枚 DocToken 对应的 ETH 十进制数（字符串避免精度问题），例如 "0.0001" */
    private String rate;

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }
}
