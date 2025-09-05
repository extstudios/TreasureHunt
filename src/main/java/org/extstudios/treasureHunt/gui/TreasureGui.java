package org.extstudios.treasureHunt.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.extstudios.treasureHunt.Model.Treasure;
import org.extstudios.treasureHunt.TreasureHunt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreasureGui implements Listener {
    private static TreasureHunt plugin;

    public TreasureGui(TreasureHunt p) {
        plugin = p;
    }

    private static final Map<UUID,Integer> openPages = new ConcurrentHashMap<>();

    public static void open(Player p, int page) {
        Collection< Treasure> all = plugin.getTreasuresById().values();
        List<Treasure> list = new ArrayList<>(all);
        list.sort(Comparator.comparing(Treasure::id));

        int pageSize = 45;
        int totalPages = Math.max(1, (int) Math.ceil(list.size() / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages -1 ));
        int from = page * pageSize;
        int to = Math.min(from + pageSize, list.size());
        List<Treasure> sub = list.subList(from, to);

        Inventory inv = Bukkit.createInventory(null, 54, "Treasure Hunt * Page " + (page + 1) + "/" + totalPages);

        int slot = 0;
        for (Treasure t : sub) {
            Material mat;
            try { mat = Material.valueOf(t.material()); } catch (IllegalArgumentException e) { mat = Material.CHEST; }
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(t.id());
            meta.setLore(java.util.List.of(
                    "Location: " +t.world()+ ":" +t.x()+","+t.y()+","+t.z(),
                    "§7Command:",
                    "§8/" + t.command(),
                    "",
                    "§cClick to §lDELETE§r§c this treasure"
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        pm.setDisplayName("Previous Page");
        prev.setItemMeta(pm);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        nm.setDisplayName("Next page");
        next.setItemMeta(nm);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName("Close");
        close.setItemMeta(cm);

        inv.setItem(45, prev);
        inv.setItem(49, close);
        inv.setItem(53, next);

        p.openInventory(inv);
        openPages.put(p.getUniqueId(), page);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity clicker = e.getWhoClicked();
        if (!(clicker instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (title == null || !title.startsWith("Treasure Hunt")) return;
        e.setCancelled(true);

        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        int slot = e.getRawSlot();
        Integer page = openPages.get(p.getUniqueId());
        if (slot == 45) {
            open(p, Math.max(0, (page==null?0:page)-1));
            return;
        } else if (slot == 53) {
            open(p, (page==null?0:page)+1);
            return;
        } else if (slot == 49) {
            p.closeInventory();
            return;
        }

        ItemMeta meta =current.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String id = meta.getDisplayName();
        p.sendMessage("Deleting treasure '" + id + "'...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean ok = plugin.dao().deleteTreasure(id);
                if (ok) {
                    plugin.removeFromCaches(id);
                    p.sendMessage("Deleted '" + id + "'.");
                    Bukkit.getScheduler().runTask(plugin, () -> open(p, page==null?0:page));
                } else {
                    p.sendMessage("Treasure not found.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                p.sendMessage( "Failed to delete");
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title != null && title.startsWith("Treasure Hunt")) {
            openPages.remove(e.getPlayer().getUniqueId());
        }
    }
}
