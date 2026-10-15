package dev.vincent.chaindocproof.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import dev.vincent.chaindocproof.config.BusinessProperties;
import dev.vincent.chaindocproof.dao.UserDao;
import dev.vincent.chaindocproof.support.AddressUtil;
import dev.vincent.chaindocproof.support.BizException;
import dev.vincent.chaindocproof.support.TokenStore;

/**
 * 用户端钱包签名登录
 */
@Service
public class AuthService {

    /** 登录签名固定文案前缀，完整文案为 "ChainDocProof:" + 地址 */
    public static final String LOGIN_MESSAGE_PREFIX = "ChainDocProof:";

    private final UserDao userDao;
    private final TokenStore tokenStore;
    private final BusinessProperties props;

    public AuthService(UserDao userDao, TokenStore tokenStore, BusinessProperties props) {
        this.userDao = userDao;
        this.tokenStore = tokenStore;
        this.props = props;
    }

    /**
     * 钱包签名登录：ecrecover 验证签名恢复出的地址与声称地址一致，
     * 确保用户真实持有该钱包；通过后确保用户存在并签发 token
     *
     * @return 登录 token
     */
    public String login(String address, String signature) {
        if (!AddressUtil.isValidAddress(address)) {
            throw new BizException(400, "钱包地址格式不合法");
        }
        if (signature == null || signature.isBlank()) {
            throw new BizException(400, "签名不能为空");
        }
        verifySignature(address, signature);
        String lower = address.toLowerCase();
        userDao.ensureUser(lower);
        return tokenStore.issue("user:" + lower, Duration.ofHours(props.getToken().getTtlHours()));
    }

    /**
     * 验证 EIP-191 签名：对 "ChainDocProof:" + 地址 加以太坊前缀后取 keccak256，
     * 用签名恢复公钥再推导地址，必须与声称地址一致
     *
     * @throws BizException 签名不合法或不匹配时抛出 401
     */
    public void verifySignature(String address, String signature) {
        byte[] message = (LOGIN_MESSAGE_PREFIX + address).getBytes(StandardCharsets.UTF_8);
        Sign.SignatureData signatureData;
        try {
            signatureData = Sign.signatureDataFromHex(signature);
        } catch (Exception e) {
            throw new BizException(401, "签名格式不合法");
        }
        BigInteger publicKey;
        try {
            publicKey = Sign.signedPrefixedMessageToKey(message, signatureData);
        } catch (Exception e) {
            throw new BizException(401, "签名验证失败，请使用对应钱包对 " + LOGIN_MESSAGE_PREFIX + "地址 签名");
        }
        String recovered = "0x" + Keys.getAddress(publicKey);
        if (recovered == null || !recovered.equalsIgnoreCase(address)) {
            throw new BizException(401, "签名验证失败，请使用对应钱包对 " + LOGIN_MESSAGE_PREFIX + "地址 签名");
        }
    }
}
