package org.extstudios.treasureHunt;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.extstudios.treasureHunt.Command.CommandRegistrar;
import org.extstudios.treasureHunt.Command.TreasureCommand;
import org.extstudios.treasureHunt.Listeners.InteractListener;
import org.extstudios.treasureHunt.Listeners.ProtectionListener;
import org.extstudios.treasureHunt.Model.LocationKey;
import org.extstudios.treasureHunt.Model.Treasure;
import org.extstudios.treasureHunt.db.Database;
import org.extstudios.treasureHunt.db.TreasureDAO;
import org.extstudios.treasureHunt.gui.TreasureGui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TreasureHunt extends JavaPlugin {

    private Database database;
    private TreasureDAO dao;

    private final Map<String, Treasure> treasuresById = new ConcurrentHashMap<>();
    private final Map<LocationKey, Treasure> treasuresByLoc = new ConcurrentHashMap<>();
    private final Map<UUID, PendingCreation> pendingCreations = new ConcurrentHashMap<>();
    private int refreshTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        int refreshIntervalSeconds = Math.max(0, cfg.getInt("refresh-interval-seconds", 15));

        try {
            database = new Database(this);
            dao = new TreasureDAO(database);
            dao.initTables();
        } catch (Exception e) {
            getLogger().severe("Failed to init database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                var all = dao.getAllTreasures();
                for (Treasure t : all) putInCaches(t);
                getLogger().info("Loaded " + all.size() + "treasures from database.");
            } catch (Exception e) {
                getLogger().severe("Failed to load treasures: " + e.getMessage());
            }
        });

        TreasureCommand handler = new TreasureCommand(this);
        boolean ok = CommandRegistrar.register(
                this,
                "treasure",
                java.util.List.of("th"),
                "treasurehunt.commands",
                "run Treasure Hunt commands",
                "/treasure <create|delete|completed|list|gui>",
                handler,
                handler
        );
        if (!ok) {
            getLogger().severe("Could not register /treasure; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new TreasureGui(this), this);


        if (refreshIntervalSeconds > 0) {
            long ticks = refreshIntervalSeconds * 20L;
            refreshTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::reloadFromDatabase, ticks, ticks).getTaskId();
            getLogger().info("Auto-refresh enabled: every " + refreshIntervalSeconds + "s.");
        } else {
            getLogger().info("Auto-refresh disabled.");
        }
        getLogger().info("TreasureHunt enabled.");
    }

    @Override
    public void onDisable() {
        if (refreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
        if (database != null) {
            database.shutdown();
            getLogger().info("TreasureHunt disabled.");
        }
    }

    public Database db() {return  database;}
    public TreasureDAO dao() {return dao;}

    public Map<String, Treasure> getTreasuresById() {return treasuresById;}
    public Map<LocationKey, Treasure> getTreasuresByLoc() {return treasuresByLoc;}

    public Map<UUID, PendingCreation> getPendingCreations() { return pendingCreations;}

    public void putInCaches(Treasure t) {
        treasuresById.put(t.id(), t);
        treasuresByLoc.put(LocationKey.of(t.world(), t.x(), t.y(), t.z()), t);
    }

    public void removeFromCaches(String id) {
        Treasure t = treasuresById.remove(id);
        if (t != null) treasuresByLoc.remove(LocationKey.of(t.world(), t.x(), t.y(), t.z()), t);
    }

    public void reloadFromDatabase() {
        try {
            List<Treasure> all = dao.getAllTreasures();

            Map<String, Treasure> byId = new HashMap<>();
            Map<LocationKey, Treasure> byLoc = new HashMap<>();
            for (Treasure t : all) {
                byId.put(t.id(), t);
                byLoc.put(LocationKey.of(t.world(), t.x(), t.y(), t.z()), t);
            }

            treasuresById.clear();
            treasuresById.putAll(byId);

            treasuresByLoc.clear();
            treasuresByLoc.putAll(byLoc);
            getLogger().fine("Synced treasures: " + treasuresById.size());
        } catch (Exception e) {
            getLogger().warning("Failed to Load treasures: " + e.getMessage());
        }
    }
    public record PendingCreation(String id, String command) {}
}
