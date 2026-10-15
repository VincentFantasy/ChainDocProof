package dev.vincent.chaindocproof.dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dev.vincent.chaindocproof.model.TokenMint;

/**
 * DocToken 发放记录数据访问（t_token_mint）
 */
@Repository
public class MintDao {

    private static final RowMapper<TokenMint> ROW_MAPPER = (rs, rowNum) -> {
        TokenMint m = new TokenMint();
        m.setId(rs.getLong("id"));
        m.setAdminId(rs.getLong("admin_id"));
        m.setToAddress(rs.getString("to_address"));
        m.setAmount(new BigInteger(rs.getString("amount")));
        m.setTxHash(rs.getString("tx_hash"));
        m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        m.setAdminUsername(rs.getString("admin_username"));
        return m;
    };

    private final JdbcTemplate jdbc;

    public MintDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 记录一笔管理端发放 */
    public void insert(long adminId, String toAddress, BigInteger amount, String txHash) {
        jdbc.update("INSERT INTO t_token_mint (admin_id, to_address, amount, tx_hash, created_at) "
                + "VALUES (?, ?, ?, ?, NOW())", adminId, toAddress, amount.toString(), txHash);
    }

    /** 发放记录列表（含操作管理员用户名） */
    public List<TokenMint> listAll() {
        return jdbc.query("SELECT m.id, m.admin_id, m.to_address, m.amount, m.tx_hash, m.created_at, a.username AS admin_username "
                + "FROM t_token_mint m JOIN t_admin a ON m.admin_id = a.id ORDER BY m.id DESC", ROW_MAPPER);
    }

    /** 管理端：发放次数与发放总量（一次查询；amount 为 wei 字符串，用 DECIMAL 求和避免精度丢失） */
    public MintCount countAndSum() {
        return jdbc.queryForObject(
                "SELECT COUNT(*), COALESCE(SUM(amount), 0) FROM t_token_mint",
                (rs, rowNum) -> {
                    BigDecimal sum = rs.getBigDecimal(2);
                    return new MintCount(rs.getLong(1), sum == null ? BigInteger.ZERO : sum.toBigInteger());
                });
    }

    /** 发放次数 / 发放总量（wei） */
    public record MintCount(long count, BigInteger sumAmount) {
    }
}
