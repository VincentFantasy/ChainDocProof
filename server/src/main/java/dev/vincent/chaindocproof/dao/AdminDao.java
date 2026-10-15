package dev.vincent.chaindocproof.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dev.vincent.chaindocproof.model.Admin;

/**
 * 管理员数据访问（t_admin）
 */
@Repository
public class AdminDao {

    private static final RowMapper<Admin> ROW_MAPPER = (rs, rowNum) -> {
        Admin a = new Admin();
        a.setId(rs.getLong("id"));
        a.setUsername(rs.getString("username"));
        a.setPasswordHash(rs.getString("password_hash"));
        return a;
    };

    private final JdbcTemplate jdbc;

    public AdminDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 按用户名查询管理员 */
    public Optional<Admin> findByUsername(String username) {
        List<Admin> list = jdbc.query("SELECT id, username, password_hash FROM t_admin WHERE username = ?",
                ROW_MAPPER, username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
