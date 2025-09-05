package org.extstudios.treasureHunt.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.extstudios.treasureHunt.Model.Treasure;
import org.extstudios.treasureHunt.TreasureHunt;
import org.extstudios.treasureHunt.gui.TreasureGui;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TreasureCommand implements TabExecutor {

    private final TreasureHunt plugin;
    public TreasureCommand(TreasureHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {

        if (!sender.hasPermission("treasurehunt.commands")) {
            sender.sendMessage("You don't have permission to use this command");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /treasure <create|delete|completed|list|reload> ...");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "completed" -> handleCompleted(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "gui" -> handleGui(sender);
            default -> sender.sendMessage("Unknown Command");
        }
        return true;
    }


    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!p.hasPermission("treasurehunt.create")) {
            p.sendMessage("You do not have permission to use this command.");
            return;
        }

        if (args.length < 3) {
            p.sendMessage("Usage: /treasure create <id> <command>");
            p.sendMessage("Example: /treasure create golden_chest say %player% found the Golden Chest!");
            return;
        }

        String id = args[1];

        if (!id.matches("[a-zA-Z0-9_-]{1,64}")) {
            p.sendMessage("Invalid id. Use letters, numbers, _ or -, up to 64 chars.");
            return;
        }

        String cmd = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)).trim();
        if (cmd.isEmpty()) {
            p.sendMessage("Command cannot be empty.");
            return;
        }
        plugin.getPendingCreations().put(p.getUniqueId(),
                new org.extstudios.treasureHunt.TreasureHunt.PendingCreation(id, cmd));

        p.sendMessage("Right-click a block to bind treasure '" + id + "'.");
    }


    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /treasure delete <id>");
            return;
        }
        if (!(sender.hasPermission("treasurehunt.delete"))) {
            sender.sendMessage("You do not have Permission to use this command.");
            return;
        }
        String id = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean ok = plugin.dao().deleteTreasure(id);
                if(ok) {
                    plugin.removeFromCaches(id);
                    sender.sendMessage("Deleted treasure '" + id + "'.");
                } else {
                    sender.sendMessage("Treasure not found: " + id);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage("Failed to delete treasure.");
            }
        });
    }

    private void handleCompleted(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: /treasure completed <id>");
            return;
        }
        if (!(sender.hasPermission("treasurehunt.completed"))) {
            sender.sendMessage("You do not have Permission to use this command.");
            return;
        }
        String id = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<String> names = plugin.dao().getClaimedPlayers(id);
                if (names.isEmpty()) {
                    sender.sendMessage("No players have claimed '" + id + "' yet.");
                } else {
                    sender.sendMessage("Players who claimed '" + id + "' (" + names.size() + "):");
                    sender.sendMessage(String.join(", ", names));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage("Failed to fetch completed list.");
            }
        });
    }

    private void handleList(CommandSender sender) {
        if (!(sender.hasPermission("treasurehunt.list"))) {
            sender.sendMessage("You do not have Permission to use this command.");
            return;
        }
        Collection<Treasure> values = plugin.getTreasuresById().values();
        if (values.isEmpty()) {
            sender.sendMessage("No treasures set.");
        } else {
            sender.sendMessage(" Treasures (" + values.size() + "):");
            for (Treasure t : values.stream().sorted(Comparator.comparing(Treasure::id)).collect(Collectors.toList())) {
                sender.sendMessage(" - " + t.id() + " @ " + t.world() + ": " + t.x() + "," + t.y() + "," + t.z());
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!(sender.hasPermission("treasurehunt.reload"))) {
            sender.sendMessage("You do not have Permission to use this command.");
            return;
        }
        sender.sendMessage("Reloading treasures from database...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
            plugin.reloadFromDatabase();
            sender.sendMessage("Treasure cache reloaded.");
        });
    }

    private void handleGui(CommandSender sender) {
        if (!(sender.hasPermission("treasurehunt.gui"))) {
            sender.sendMessage("You do not have Permission to use this command.");
            return;
        }
        if(!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        TreasureGui.open(p, 0);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("treasure.admin")) return List.of();
        if (args.length == 1) return Arrays.asList("create","delete","completed","list", "gui");
        if (args.length == 2 && ("delete".equalsIgnoreCase(args[0]) || "completed".equalsIgnoreCase(args[0]))) {
            return plugin.getTreasuresById().keySet().stream().sorted().toList();
        }
        return List.of();
    }
}
