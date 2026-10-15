package dev.vincent.chaindocproof.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.web3j.utils.Numeric;

/**
 * FileHashUtil 边界值测试
 */
class FileHashUtilTest {

    /** "abc" 的 SHA-256 已知向量 */
    @Test
    void sha256KnownVectorAbc() throws Exception {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                FileHashUtil.sha256Hex(new ByteArrayInputStream("abc".getBytes())));
    }

    /** 空文件（0 字节）的 SHA-256 已知向量 */
    @Test
    void sha256EmptyFile() throws Exception {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                FileHashUtil.sha256Hex(new ByteArrayInputStream(new byte[0])));
    }

    /** 跨缓冲区边界（8192 内部缓冲 + 1 字节）时结果仍正确 */
    @Test
    void sha256CrossBufferBoundary() throws Exception {
        byte[] data = new byte[8193];
        Arrays.fill(data, (byte) 7);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expected = Numeric.toHexStringNoPrefix(digest.digest(data));
        assertEquals(expected, FileHashUtil.sha256Hex(new ByteArrayInputStream(data)));
    }
}
