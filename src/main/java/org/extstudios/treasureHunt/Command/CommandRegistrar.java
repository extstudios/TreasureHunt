package org.extstudios.treasureHunt.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

public final class CommandRegistrar {
    private CommandRegistrar() {}

    public static boolean register(Plugin plugin,
                                   String label,
                                   List<String> aliases,
                                   String permission,
                                   String description,
                                   String usage,
                                   org.bukkit.command.CommandExecutor executor,
                                   org.bukkit.command.TabCompleter tabCompleter) {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap map = (CommandMap) f.get(Bukkit.getServer());

            Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            c.setAccessible(true);
            PluginCommand cmd = c.newInstance(label, plugin);

            cmd.setDescription(description);
            cmd.setUsage(usage);
            if (permission != null && !permission.isEmpty()) cmd.setPermission(permission);
            if (aliases != null && !aliases.isEmpty()) cmd.setAliases(aliases);

            cmd.setExecutor(executor);
            if (tabCompleter != null) cmd.setTabCompleter(tabCompleter);

            map.register(plugin.getName().toLowerCase(), cmd);

            plugin.getLogger().info("Registered /" + label + " (aliases: " + aliases + ")");
            return true;
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to register /" + label + ": " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
}
