package dev.vincent.chaindocproof.support;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.web3j.utils.Numeric;

/**
 * 文件 hash 工具：流式计算 SHA-256，文件内容不落盘、不缓存
 */
public final class FileHashUtil {

    private static final int BUFFER_SIZE = 8192;

    private FileHashUtil() {
    }

    /**
     * 流式计算 SHA-256，返回 64 位小写 hex
     *
     * @param in 文件输入流（调用方负责关闭）
     * @return 小写 hex 字符串
     */
    public static String sha256Hex(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while ((n = in.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            return Numeric.toHexStringNoPrefix(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
