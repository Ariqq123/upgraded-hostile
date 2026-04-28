package com.antigravity.upgradedhostile.commands;

import com.antigravity.upgradedhostile.UpgradedHostile;
import com.antigravity.upgradedhostile.guis.ControlGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UHostileCommand implements CommandExecutor {

    private final UpgradedHostile plugin;

    public UHostileCommand(UpgradedHostile plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("upgradedhostile.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (sender instanceof Player player) {
                ControlGUI.open(plugin, player);
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can open the GUI.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "UpgradedHostile configuration reloaded!");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /uhostile [gui|reload]");
        return true;
    }
}
