package org.extstudios.treasureHunt.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.extstudios.treasureHunt.TreasureHunt;

import javax.sql.DataSource;


public class Database {
    private final HikariDataSource dataSource;

    public Database(TreasureHunt plugin) {
        HikariConfig cfg = new HikariConfig();
        String host = plugin.getConfig().getString("mysql.host");
        int port = plugin.getConfig().getInt("mysql.port");
        String db = plugin.getConfig().getString("mysql.database");
        boolean useSSL = plugin.getConfig().getBoolean("mysql.useSSL");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" +useSSL + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        cfg.setJdbcUrl(url);
        cfg.setUsername(plugin.getConfig().getString("mysql.username"));
        cfg.setPassword(plugin.getConfig().getString("mysql.password"));
        cfg.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool.maximumPoolSize", 10));
        cfg.setMinimumIdle(plugin.getConfig().getInt("mysql.pool.minimumIdle", 2));
        cfg.setConnectionTimeout(plugin.getConfig().getLong("mysql.pool.connectionTimeoutMs", 10000));
        cfg.setIdleTimeout(plugin.getConfig().getLong("mysql.pool.idleTimeoutMs", 600000));
        cfg.setMaxLifetime(plugin.getConfig().getLong("mysql.pool.maxLifetimeMs", 1800000));
        cfg.setPoolName("TreasureHunt-Hikari");
        this.dataSource = new HikariDataSource(cfg);
    }

    public DataSource getDataSource() {return dataSource;}

    public void shutdown() {dataSource.close();}
}
