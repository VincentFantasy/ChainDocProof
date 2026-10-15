package dev.vincent.chaindocproof.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dev.vincent.chaindocproof.model.CreditLog;
import dev.vincent.chaindocproof.model.dto.CreditLogVo;

/**
 * 次数流水数据访问（t_credit_log）
 */
@Repository
public class CreditLogDao {

    private final JdbcTemplate jdbc;

    public CreditLogDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 记一笔流水 */
    public void insert(long userId, String type, int amount, Long proofId, String txHash) {
        jdbc.update("INSERT INTO t_credit_log (user_id, type, amount, proof_id, tx_hash, created_at) "
                + "VALUES (?, ?, ?, ?, ?, NOW())", userId, type, amount, proofId, txHash);
    }

    /** 交易 hash 是否已使用过（购买幂等控制） */
    public boolean existsByTxHash(String txHash) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM t_credit_log WHERE tx_hash = ?", Integer.class, txHash);
        return count != null && count > 0;
    }

    /** 用户累计购买次数 */
    public long sumPurchase(long userId) {
        Long sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM t_credit_log WHERE user_id = ? AND type = ?",
                Long.class, userId, CreditLog.TYPE_PURCHASE);
        return sum == null ? 0 : sum;
    }

    /** 管理端：全库累计购买次数 */
    public long sumAllPurchase() {
        Long sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM t_credit_log WHERE type = ?",
                Long.class, CreditLog.TYPE_PURCHASE);
        return sum == null ? 0 : sum;
    }

    /**
     * 管理端：次数流水列表，可按钱包地址 / 类型筛选（传 null 或空串即不过滤），按 id 倒序
     */
    public List<CreditLogVo> listAll(String address, String type) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.address, c.user_id, c.type, c.amount, c.proof_id, c.tx_hash, c.created_at "
                        + "FROM t_credit_log c JOIN t_user u ON c.user_id = u.id WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (address != null && !address.isEmpty()) {
            sql.append("AND u.address = ? ");
            args.add(address);
        }
        if (type != null && !type.isEmpty()) {
            sql.append("AND c.type = ? ");
            args.add(type);
        }
        sql.append("ORDER BY c.id DESC");
        return jdbc.query(sql.toString(), CREDIT_LOG_VO_MAPPER, args.toArray());
    }

    private static final RowMapper<CreditLogVo> CREDIT_LOG_VO_MAPPER = (rs, rowNum) -> {
        CreditLogVo vo = new CreditLogVo();
        vo.setAddress(rs.getString("address"));
        vo.setUserId(rs.getLong("user_id"));
        vo.setType(rs.getString("type"));
        vo.setAmount(rs.getInt("amount"));
        long proofId = rs.getLong("proof_id");
        vo.setProofId(rs.wasNull() ? null : proofId);
        vo.setTxHash(rs.getString("tx_hash"));
        vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return vo;
    };
}
