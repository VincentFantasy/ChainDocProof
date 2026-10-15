package dev.vincent.chaindocproof.support;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 内存版登录 token 存储（MVP 不持久化，重启后需重新登录）
 * owner 格式：user:&lt;钱包地址&gt; / admin:&lt;用户名&gt;
 */
@Component
public class TokenStore {

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    /** 为 owner 签发一个带有效期的 token */
    public String issue(String owner, Duration ttl) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new Entry(owner, System.currentTimeMillis() + ttl.toMillis()));
        return token;
    }

    /** 解析 token，返回 owner；不存在或过期抛出 401 */
    public String resolve(String token) {
        Entry entry = token == null ? null : tokens.get(token);
        if (entry == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            tokens.remove(token);
            throw new BizException(401, "未登录或登录已过期");
        }
        return entry.owner;
    }

    private record Entry(String owner, long expireAt) {
    }
}
