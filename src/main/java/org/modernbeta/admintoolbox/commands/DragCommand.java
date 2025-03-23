package org.modernbeta.admintoolbox.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.modernbeta.admintoolbox.admins.Admin;
import org.modernbeta.admintoolbox.admins.AdminManager;
import org.modernbeta.admintoolbox.tools.Drag;
import org.modernbeta.admintoolbox.tools.Freeze;

public class DragCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if (args.length == 0) {
            commandSender.sendMessage(ChatColor.RED + "You did not provide a player to Drag. /drag <player>");
            return false;
        }

        if (!(commandSender instanceof Player dragger)){
            commandSender.sendMessage(ChatColor.RED + "Only online players can run this command.");
            return true;
        }

        if (!dragger.hasPermission("AdminToolbox.admin")) {
            dragger.sendMessage(ChatColor.RED + "You do not have permission to use this command");
            return true;
        }

        Player target = Bukkit.getServer().getPlayer(args[0]);
        if (target == null) {
            commandSender.sendMessage(ChatColor.RED + "This player is not online.");
            return false;
        }

        if (AdminManager.isAdmin(target)) {
            dragger.sendMessage(ChatColor.RED + "You can't drag other staff.");
            return true;
        }

        if (Drag.getDraggingPlayers().containsKey(dragger)) {
            commandSender.sendMessage(ChatColor.RED + "You are already dragging " + Drag.getDraggingPlayers().get(dragger).getName());
            return true;
        }

        Drag.dragPlayer(dragger, target);
        return true;
    }
}
