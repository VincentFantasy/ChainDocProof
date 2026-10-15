package dev.vincent.chaindocproof.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * AddressUtil 边界值测试
 */
class AddressUtilTest {

    @Test
    void addressValid() {
        assertTrue(AddressUtil.isValidAddress("0x" + "ab".repeat(20)));
        assertTrue(AddressUtil.isValidAddress("0x" + "AB".repeat(20)));
    }

    @Test
    void addressInvalid() {
        assertFalse(AddressUtil.isValidAddress(null));
        assertFalse(AddressUtil.isValidAddress(""));
        assertFalse(AddressUtil.isValidAddress("0x" + "ab".repeat(19)));
        assertFalse(AddressUtil.isValidAddress("0x" + "ab".repeat(21)));
        assertFalse(AddressUtil.isValidAddress("ab" + "ab".repeat(20)));
        assertFalse(AddressUtil.isValidAddress("0x" + "g".repeat(40)));
    }

    @Test
    void txHashValidAndInvalid() {
        assertTrue(AddressUtil.isValidTxHash("0x" + "cd".repeat(32)));
        assertFalse(AddressUtil.isValidTxHash(null));
        assertFalse(AddressUtil.isValidTxHash("0x" + "cd".repeat(31)));
        assertFalse(AddressUtil.isValidTxHash("0x" + "cd".repeat(33)));
        assertFalse(AddressUtil.isValidTxHash("0x" + "ab".repeat(20)));
    }
}
