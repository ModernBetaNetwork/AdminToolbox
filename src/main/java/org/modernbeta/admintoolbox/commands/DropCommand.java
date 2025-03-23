package org.modernbeta.admintoolbox.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.modernbeta.admintoolbox.admins.AdminManager;
import org.modernbeta.admintoolbox.tools.Drag;

public class DropCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        if (!(commandSender instanceof Player dragger)){
            commandSender.sendMessage(ChatColor.RED + "Only online players can run this command.");
            return true;
        }

        if (!dragger.hasPermission("AdminToolbox.admin")) {
            dragger.sendMessage(ChatColor.RED + "You do not have permission to use this command");
            return true;
        }

        if (!Drag.getDraggingPlayers().containsKey(dragger)) {
            commandSender.sendMessage(ChatColor.RED + "You are not dragging anyone to drop.");
            return true;
        }

        Drag.dropPlayer(dragger);
        return true;
    }
}
