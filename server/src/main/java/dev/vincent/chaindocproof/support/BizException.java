package dev.vincent.chaindocproof.support;

/**
 * 业务异常：由全局异常处理器统一转换为 Result 返回
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
