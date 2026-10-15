package dev.vincent.chaindocproof.support;

import java.util.regex.Pattern;

/**
 * 以太坊地址校验工具
 */
public final class AddressUtil {

    private static final Pattern ADDRESS = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    private static final Pattern TX_HASH = Pattern.compile("^0x[0-9a-fA-F]{64}$");

    private AddressUtil() {
    }

    /** 是否为合法以太坊地址 */
    public static boolean isValidAddress(String address) {
        return address != null && ADDRESS.matcher(address).matches();
    }

    /** 是否为合法交易 hash */
    public static boolean isValidTxHash(String txHash) {
        return txHash != null && TX_HASH.matcher(txHash).matches();
    }
}
