package org.extstudios.treasureHunt.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.extstudios.treasureHunt.Model.LocationKey;
import org.extstudios.treasureHunt.Model.Treasure;
import org.extstudios.treasureHunt.TreasureHunt;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractListener implements Listener {

    private final TreasureHunt plugin;

    public InteractListener(TreasureHunt plugin) {
        this.plugin = plugin;
    }

    private final Set<String> claimLock = ConcurrentHashMap.newKeySet();
    private String lockKey(String treasureId, UUID uuid) {return treasureId + ":" + uuid;}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        final Action a = e.getAction();

        var pending = plugin.getPendingCreations().get(p.getUniqueId());
        if (pending != null) {
            if (a != Action.RIGHT_CLICK_BLOCK) {
                p.sendMessage("Right-click a block to set the treasure location.");
                return;
            }
            Block b = e.getClickedBlock();
            if (b == null || b.getType() == Material.AIR) {
                p.sendMessage("Invalid block. Try again.");
                return;
            }

            plugin.getPendingCreations().remove(p.getUniqueId());

            var key = LocationKey.of(b.getLocation());
            String id = pending.id();
            String cmd = pending.command();
            String material = b.getType().name();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (plugin.dao().locationHasTreasure(key.world, key.x, key.y, key.z)) {
                        p.sendMessage("A treasure already exists at this location.");
                        return;
                    }
                    if (plugin.dao().getById(id).isPresent()) {
                        p.sendMessage("Treasure id already exists: " + id);
                        return;
                    }
                    Treasure t = new Treasure(id, key.world, key.x, key.y, key.z, material, cmd);
                    plugin.dao().insertTreasure(t);
                    plugin.putInCaches(t);
                    p.sendMessage("Treasure '" + id + "' created at " + key + ".");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    p.sendMessage("Failed to create treasure. Check console.");
                }
            });

            e.setCancelled(true);
            return;
        }

        if (a == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b == null) return;
            var key = LocationKey.of(b.getLocation());
            var cached = plugin.getTreasuresByLoc().get(key);
            if (cached == null) return;

            String lock = lockKey(cached.id(), p.getUniqueId());
            if (!claimLock.add(lock)) return;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    List<String> checkClaimed = plugin.dao().getClaimedPlayers(cached.id());
                    var playerName = p.getName();
                    if (checkClaimed.contains(playerName)) {
                        p.sendMessage("You have already claimed this treasure.");
                        return;
                    }

                    plugin.dao().recordClaim(cached.id(), p.getUniqueId(), p.getName());
                    p.sendMessage("You found treasure " + cached.id() + "!");
                    String toRun = cached.command().replace("%player%", p.getName());
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    p.sendMessage("An error occurred while claiming the treasure.");
                } finally {
                    claimLock.remove(lock);
                }
            });
        }

    }
}
