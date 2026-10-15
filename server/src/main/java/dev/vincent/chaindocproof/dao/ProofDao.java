package dev.vincent.chaindocproof.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import dev.vincent.chaindocproof.model.ProofRecord;

/**
 * 存证记录数据访问（t_proof）
 */
@Repository
public class ProofDao {

    private static final RowMapper<ProofRecord> ROW_MAPPER = (rs, rowNum) -> {
        ProofRecord r = new ProofRecord();
        r.setId(rs.getLong("id"));
        r.setUserId(rs.getLong("user_id"));
        r.setFileName(rs.getString("file_name"));
        r.setFileSize(rs.getLong("file_size"));
        r.setFileHash(rs.getString("file_hash"));
        r.setUploadTs(rs.getLong("upload_ts"));
        r.setStatus(rs.getString("status"));
        r.setTxHash(rs.getString("tx_hash"));
        var storedAt = rs.getTimestamp("stored_at");
        r.setStoredAt(storedAt == null ? null : storedAt.toLocalDateTime());
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return r;
    };

    private final JdbcTemplate jdbc;

    public ProofDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 新增存证记录，返回生成的ID */
    public long insert(ProofRecord r) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO t_proof (user_id, file_name, file_size, file_hash, upload_ts, status, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())",
                    new String[]{"id"});
            ps.setLong(1, r.getUserId());
            ps.setString(2, r.getFileName());
            ps.setLong(3, r.getFileSize());
            ps.setString(4, r.getFileHash());
            ps.setLong(5, r.getUploadTs());
            return ps;
        }, keyHolder);
        r.setId(((Number) keyHolder.getKey()).longValue());
        return r.getId();
    }

    /** 按ID和用户查询（防越权） */
    public ProofRecord findByIdAndUser(long id, long userId) {
        List<ProofRecord> list = jdbc.query(
                "SELECT id, user_id, file_name, file_size, file_hash, upload_ts, status, tx_hash, stored_at, created_at "
                        + "FROM t_proof WHERE id = ? AND user_id = ?",
                ROW_MAPPER, id, userId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 上链成功后回写状态和交易hash */
    public void updateStored(long id, String txHash) {
        jdbc.update("UPDATE t_proof SET status = 'STORED', tx_hash = ?, stored_at = NOW() WHERE id = ?", txHash, id);
    }

    /** 用户存证列表 */
    public List<ProofRecord> listByUser(long userId) {
        return jdbc.query(
                "SELECT id, user_id, file_name, file_size, file_hash, upload_ts, status, tx_hash, stored_at, created_at "
                        + "FROM t_proof WHERE user_id = ? ORDER BY id DESC",
                ROW_MAPPER, userId);
    }

    /**
     * 管理端：存证记录列表，可按钱包地址筛选
     *
     * @param address 为空则查全部
     */
    public List<ProofRecord> listAll(String address) {
        String sql = "SELECT p.id, p.user_id, p.file_name, p.file_size, p.file_hash, p.upload_ts, p.status, "
                + "p.tx_hash, p.stored_at, p.created_at, u.address AS owner_address "
                + "FROM t_proof p JOIN t_user u ON p.user_id = u.id "
                + (address == null ? "" : "WHERE u.address = ? ")
                + "ORDER BY p.id DESC";
        return address == null ? jdbc.query(sql, ADMIN_ROW_MAPPER) : jdbc.query(sql, ADMIN_ROW_MAPPER, address);
    }

    private static final RowMapper<ProofRecord> ADMIN_ROW_MAPPER = (rs, rowNum) -> {
        ProofRecord r = ROW_MAPPER.mapRow(rs, rowNum);
        r.setOwnerAddress(rs.getString("owner_address"));
        return r;
    };

    /** 管理端：存证总数与已上链数（一次查询；表为空时 stored 为 0） */
    public ProofCount countStats() {
        return jdbc.queryForObject(
                "SELECT COUNT(*), SUM(CASE WHEN status = 'STORED' THEN 1 ELSE 0 END) FROM t_proof",
                (rs, rowNum) -> new ProofCount(rs.getLong(1), rs.getLong(2)));
    }

    /** 存证总数 / 已上链数 */
    public record ProofCount(long total, long stored) {
    }
}
