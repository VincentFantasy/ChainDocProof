package dev.vincent.chaindocproof.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import dev.vincent.chaindocproof.model.User;

/**
 * 用户数据访问（t_user）
 */
@Repository
public class UserDao {

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setAddress(rs.getString("address"));
        u.setCreditBalance(rs.getInt("credit_balance"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    };

    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 按地址查询用户，不存在返回空 */
    public Optional<User> findByAddress(String address) {
        List<User> list = jdbc.query("SELECT id, address, credit_balance, created_at FROM t_user WHERE address = ?",
                ROW_MAPPER, address);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** 确保用户存在（不存在则创建），返回用户ID */
    public long ensureUser(String address) {
        Optional<User> exist = findByAddress(address);
        if (exist.isPresent()) {
            return exist.get().getId();
        }
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(con -> {
                var ps = con.prepareStatement("INSERT INTO t_user (address, credit_balance, created_at) VALUES (?, 0, NOW())",
                        new String[]{"id"});
                ps.setString(1, address);
                return ps;
            }, keyHolder);
            return ((Number) keyHolder.getKey()).longValue();
        } catch (DataIntegrityViolationException e) {
            // 并发下另一个请求已创建，直接回查
            return findByAddress(address).orElseThrow().getId();
        }
    }

    /** 增加次数（购买） */
    public int incrementCredit(long userId, int amount) {
        return jdbc.update("UPDATE t_user SET credit_balance = credit_balance + ? WHERE id = ?", amount, userId);
    }

    /**
     * 原子扣减次数：余额不足时返回 0（不扣）
     *
     * @return 受影响行数，0 表示余额不足
     */
    public int decrementCredit(long userId, int amount) {
        return jdbc.update("UPDATE t_user SET credit_balance = credit_balance - ? WHERE id = ? AND credit_balance >= ?",
                amount, userId, amount);
    }

    /** 管理端：查询全部用户 */
    public List<User> listAll() {
        return jdbc.query("SELECT id, address, credit_balance, created_at FROM t_user ORDER BY id", ROW_MAPPER);
    }

    /** 管理端：用户总数 */
    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM t_user", Long.class);
        return c == null ? 0 : c;
    }
}
