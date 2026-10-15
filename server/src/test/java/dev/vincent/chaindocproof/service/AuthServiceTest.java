package dev.vincent.chaindocproof.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.TokenStore;

/**
 * AuthService 测试：用真实 web3j 密钥做 EIP-191 签名，验证登录全分支
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final BigInteger PRIVATE_KEY =
            new BigInteger("4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318", 16);

    @Mock
    private UserDao userDao;

    private final TokenStore tokenStore = new TokenStore();
    private AuthService service;
    private ECKeyPair keyPair;
    private String address;

    @BeforeEach
    void setUp() {
        service = new AuthService(userDao, tokenStore, new BusinessProperties());
        keyPair = ECKeyPair.create(PRIVATE_KEY);
        String raw = Keys.getAddress(keyPair);
        address = raw.startsWith("0x") ? raw : "0x" + raw;
    }

    /** 生成 EIP-191 签名（r||s||v 拼接的 hex，与钱包 MetaMask 风格一致） */
    private String signPrefixed(String message) {
        Sign.SignatureData sd = Sign.signPrefixedMessage(message.getBytes(StandardCharsets.UTF_8), keyPair);
        byte[] sig = new byte[sd.getR().length + sd.getS().length + sd.getV().length];
        int off = 0;
        System.arraycopy(sd.getR(), 0, sig, off, sd.getR().length);
        off += sd.getR().length;
        System.arraycopy(sd.getS(), 0, sig, off, sd.getS().length);
        off += sd.getS().length;
        System.arraycopy(sd.getV(), 0, sig, off, sd.getV().length);
        return Numeric.toHexString(sig);
    }

    private String validSignature() {
        return signPrefixed(AuthService.LOGIN_MESSAGE_PREFIX + address);
    }

    @Test
    void login_successIssuesUserToken() {
        String token = service.login(address, validSignature());
        assertEquals("user:" + address, tokenStore.resolve(token));
        verify(userDao).ensureUser(address);
    }

    @Test
    void login_signatureForDifferentWalletRejected() {
        String otherAddress = "0x" + "f".repeat(40);
        BizException ex = assertThrows(BizException.class,
                () -> service.login(otherAddress, validSignature()));
        assertEquals(401, ex.getCode());
        verifyNoInteractions(userDao);
    }

    @Test
    void login_signedWrongMessageRejected() {
        String wrongSig = signPrefixed(AuthService.LOGIN_MESSAGE_PREFIX + address + " 被篡改");
        BizException ex = assertThrows(BizException.class,
                () -> service.login(address, wrongSig));
        assertEquals(401, ex.getCode());
        verifyNoInteractions(userDao);
    }

    @Test
    void login_invalidAddressFormatRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> service.login("0x123", validSignature()));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(userDao);
    }

    @Test
    void login_blankSignatureRejected() {
        for (String sig : new String[]{null, ""}) {
            BizException ex = assertThrows(BizException.class, () -> service.login(address, sig));
            assertEquals(400, ex.getCode());
        }
        verifyNoInteractions(userDao);
    }

    @Test
    void login_malformedSignatureHexRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> service.login(address, "0x1234"));
        assertEquals(401, ex.getCode());
        assertTrue(ex.getMessage().contains("格式不合法"));
        verifyNoInteractions(userDao);
    }
}
