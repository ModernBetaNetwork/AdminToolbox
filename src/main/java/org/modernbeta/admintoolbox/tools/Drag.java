package org.modernbeta.admintoolbox.tools;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.modernbeta.admintoolbox.AdminToolbox;
import org.modernbeta.admintoolbox.admins.Admin;
import org.modernbeta.admintoolbox.admins.AdminManager;
import org.modernbeta.admintoolbox.admins.AdminState;

import java.util.*;

public class Drag implements Listener {
    static Map<Player, Player> draggingPlayers = new HashMap<>();

    public static void dragPlayer(Player dragger, Player target) {
        // dragger starts at targets location
        Admin admin = AdminManager.getOnlineAdmin(dragger);
        admin.toggleAdminMode(target, null);
        draggingPlayers.put(dragger, target);

        // initiate dragging process for target
        target.setGameMode(GameMode.SPECTATOR);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AdminToolbox.getInstance(), () -> {
            target.setSpectatorTarget(dragger);
        },  4L);

        // alerts admins and victim
        String draggerName = dragger.getName();
        target.sendMessage(ChatColor.RED + draggerName + " is dragging you!");
        List<Admin> onlineAdmins = AdminManager.getOnlineAdmins();
        for (Admin otherAdmin : onlineAdmins) {
            if (otherAdmin.equals(admin)) continue;
            admin.getPlayer().sendMessage(ChatColor.GOLD + draggerName + " is dragging " + target.getName() + "!");
        }
    }

    public static void dropPlayer(Player dragger) {
        Player victim = draggingPlayers.get(dragger);
        draggingPlayers.remove(dragger);

        victim.setSpectatorTarget(null);
        Location victimLocation = victim.getLocation();
        for (int y = 0; y < 100; y++)
        {
            // teleport admin to first valid block location below them
            Location victimTpLocation = victimLocation.clone().subtract(0, y, 0);
            if (victimTpLocation.getBlock().getType() == Material.AIR) continue;

            victim.teleport(victimTpLocation.add(0, 1, 0));
            break;
        }

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AdminToolbox.getInstance(), () -> {
            victim.setGameMode(GameMode.SURVIVAL);
        },  1L);

        String draggerName = dragger.getName();
        victim.sendMessage(ChatColor.RED + draggerName + " has dropped you!");

        List<Admin> onlineAdmins = AdminManager.getOnlineAdmins();
        for (Admin admin : onlineAdmins) {
            admin.getPlayer().sendMessage(ChatColor.GOLD + draggerName + " has dropped " + victim.getName() + "!");
        }
    }

    public static Map<Player, Player> getDraggingPlayers() {
        return draggingPlayers;
    }

    // force victim back into spectating dragger when they try to exit
    @EventHandler
    public void onDragVictimEscapeAttempt(PlayerStopSpectatingEntityEvent event) {
        Player player = event.getPlayer();
        Player dragger = getDragger(player);
        if (dragger == null || !player.getGameMode().equals(GameMode.SPECTATOR)) return;

        event.setCancelled(true);
        player.setSpectatorTarget(dragger);
    }

    @EventHandler
    public void onDragVictimRejoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Player dragger = getDragger(player);
        if (dragger == null) return;

        dragPlayer(dragger, player);
    }

    @EventHandler
    public void onDragVictimLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!draggingPlayers.containsValue(player)) return;

        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(player.getLocation().toHighestLocation());
    }

    @EventHandler
    public void onDraggerLeave(PlayerQuitEvent event) {
        Player dragger = event.getPlayer();
        if (!draggingPlayers.containsKey(dragger)) return;

        Player victim = draggingPlayers.get(dragger); 
        victim.setGameMode(GameMode.SURVIVAL);
        victim.teleport(victim.getLocation().toHighestLocation().add(0, 2, 0));
    }

    @EventHandler
    public void onDraggerRejoin(PlayerJoinEvent event) {
        Player dragger = event.getPlayer();
        if (!draggingPlayers.containsKey(dragger)) return;

        Player victim = draggingPlayers.get(dragger);
        dragPlayer(dragger, victim);
    }

    Player getDragger(Player victim) {
        Player dragger = null;
        for (Map.Entry<Player, Player> dragCombo : draggingPlayers.entrySet()) {
            if (dragCombo.getValue().equals(victim)) {
                dragger = dragCombo.getKey();
            }
        }

        return dragger;
    }
}
