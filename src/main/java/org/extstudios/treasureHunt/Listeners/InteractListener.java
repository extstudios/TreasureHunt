package org.extstudios.treasureHunt.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.extstudios.treasureHunt.Model.LocationKey;
import org.extstudios.treasureHunt.Model.Treasure;
import org.extstudios.treasureHunt.TreasureHunt;

public class InteractListener implements Listener {

    private final TreasureHunt plugin;

    public InteractListener(TreasureHunt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        final Action a = e.getAction();

        // Check pending creation
        var pending = plugin.getPendingCreations().get(p.getUniqueId());
        if (pending != null) {
            p.sendMessage("§8[debug] pendingCreation found for you: id=" + pending.id());
            if (a != Action.RIGHT_CLICK_BLOCK) {
                p.sendMessage("§cRight-click a block to set the treasure location.");
                return;
            }
            Block b = e.getClickedBlock();
            if (b == null || b.getType() == Material.AIR) {
                p.sendMessage("§cInvalid block. Try again.");
                return;
            }

            // consume the pending
            plugin.getPendingCreations().remove(p.getUniqueId());

            var key = LocationKey.of(b.getLocation());
            String id = pending.id();
            String cmd = pending.command();
            String material = b.getType().name();

            // Write to DB async
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (plugin.dao().locationHasTreasure(key.world, key.x, key.y, key.z)) {
                        p.sendMessage("§cA treasure already exists at this location.");
                        return;
                    }
                    if (plugin.dao().getById(id).isPresent()) {
                        p.sendMessage("§cTreasure id already exists: " + id);
                        return;
                    }
                    Treasure t = new Treasure(id, key.world, key.x, key.y, key.z, material, cmd);
                    plugin.dao().insertTreasure(t);
                    plugin.putInCaches(t);
                    p.sendMessage("§aTreasure '" + id + "' created at §e" + key + "§a.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    p.sendMessage("§cFailed to create treasure. Check console.");
                }
            });

            e.setCancelled(true); // optional: prevent block use
            return;
        }

        if (a == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b == null) return;
            var key = LocationKey.of(b.getLocation());
            var cached = plugin.getTreasuresByLoc().get(key);
            if (cached == null) return;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    boolean inserted = plugin.dao().recordClaim(cached.id(), p.getUniqueId(), p.getName());
                    if (!inserted) {
                        p.sendMessage("You have already claimed this treasure.");
                        return;
                    }
                    p.sendMessage("You found treasure " + cached.id() + "!");
                    String toRun = cached.command().replace("%player%", p.getName());
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    p.sendMessage("An error occurred while claiming the treasure.");
                }
            });
        }

    }
}
