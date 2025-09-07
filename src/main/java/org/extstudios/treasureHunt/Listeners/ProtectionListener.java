package org.extstudios.treasureHunt.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.extstudios.treasureHunt.Model.LocationKey;
import org.extstudios.treasureHunt.TreasureHunt;


public class ProtectionListener implements Listener {

    TreasureHunt plugin;

    public ProtectionListener (TreasureHunt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        var key = LocationKey.of(e.getBlock().getLocation());
        if (plugin.getTreasuresByLoc().containsKey(key)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("This block is a protected treasure.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        e.getBlockAgainst();
        var against = LocationKey.of(e.getBlockAgainst().getLocation());
        var placed = LocationKey.of(e.getBlockPlaced().getLocation());

        if (plugin.getTreasuresByLoc().containsKey(against) || plugin.getTreasuresByLoc().containsKey(placed)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("You cannot place blocks on/against a treasure.");
        }
    }
}
