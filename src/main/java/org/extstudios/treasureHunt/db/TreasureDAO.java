package org.extstudios.treasureHunt.db;

import org.extstudios.treasureHunt.Model.Treasure;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TreasureDAO {

    private final DataSource ds;

    public TreasureDAO(Database db) {
        this.ds = db.getDataSource();
    }

    public void initTables() throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS treasures (
                    id VARCHAR(64) PRIMARY KEY,
                    world VARCHAR(100) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    material VARCHAR(64) NOT NULL,
                    command TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uniq_world_xyz (world, x, y, z)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS treasure_claims (
                    treasure_id VARCHAR(64) NOT NULL,
                    player_uuid CHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (treasure_id, player_uuid),
                    KEY idx_player_uuid (player_uuid),
                    CONSTRAINT fk_treasure FOREIGN KEY (treasure_id)
                        REFERENCES treasures(id) ON DELETE CASCADE ON UPDATE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);
        }
    }

    public void insertTreasure(Treasure t) throws SQLException {
        String sql = "INSERT INTO treasures (id, world, x, y, z, material, command) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps =c.prepareStatement(sql)) {
            ps.setString(1, t.id());
            ps.setString(2, t.world());
            ps.setInt(3, t.x());
            ps.setInt(4, t.y());
            ps.setInt(5, t.z());
            ps.setString(6, t.material());
            ps.setString(7, t.command());
            ps.executeUpdate();
        }
    }

    public boolean deleteTreasure(String id) throws SQLException {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM treasures WHERE id=?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Treasure> getAllTreasures() throws SQLException {
        String sql = "SELECT id, world, x, y, z, material, command FROM treasures ORDER BY id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<Treasure> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Treasure(
                            rs.getString(1), rs.getString(2),
                            rs.getInt(3), rs.getInt(4), rs.getInt(5),
                            rs.getString(6), rs.getString(7)
                    ));
                }
                return out;
            }
        }
    }

    public Optional<Treasure> getById(String id) throws SQLException {
        String sql = "SELECT id, world, x, y, z, material, command FROM treasures WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Treasure(
                            rs.getString(1), rs.getString(2),
                            rs.getInt(3), rs.getInt(4), rs.getInt(5),
                            rs.getString(6), rs.getString(7)
                    ));
                }
                return Optional.empty();
            }
        }
    }

    public boolean locationHasTreasure(String world, int x, int y, int z) throws SQLException {
        String sql = "SELECT 1 FROM treasures WHERE world=? AND x=? AND y=? AND z=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean recordClaim(String treasureId, UUID playerUuid, String playerName) throws SQLException {
        String sql = "INSERT INTO treasure_claims (treasure_id, player_uuid, player_name) VALUES (?,?,?)";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, treasureId);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, playerName);
            try {
                return ps.executeUpdate() > 0;
            } catch(SQLIntegrityConstraintViolationException dup) {
                return false;
            }
        }
    }

    public List<String> getClaimedPlayers(String treasureId) throws SQLException {
        String sql = "SELECT player_name FROM treasure_claims WHERE treasure_id=? ORDER BY claimed_at";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, treasureId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> names = new ArrayList<>();
                while (rs.next()) names.add(rs.getString(1));
                return names;
            }
        }
    }
}
