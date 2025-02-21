package org.modernbeta.admintoolbox.admins;

import org.modernbeta.admintoolbox.AdminToolbox;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Admin
{
    Player adminPlayer;
    AdminState adminState;
    ItemStack[] savedPriorInventory;
    Location savedPriorLocation;
    ArrayList<Location> currentLocationHistory = new ArrayList<>();
    int locationHistoryOffset = 0;


    public Admin(Player adminPlayer)
    {
        this.adminPlayer = adminPlayer;
        setAdminState(AdminState.FREEROAM);
    }


    public void teleportBackwardInHistory()
    {
        boolean startingFromFreeRoam = adminState == AdminState.FREEROAM;
        if (!startingFromFreeRoam) locationHistoryOffset++;
        else locationHistoryOffset = 0;

        int locationIndex = currentLocationHistory.size() - locationHistoryOffset - 1;
        if (locationIndex < 0)
        {
            if (!startingFromFreeRoam) locationHistoryOffset--;
            adminPlayer.sendMessage(ChatColor.RED + "You have no more previous teleport history.");
            return;
        }

        setAdminState(AdminState.SPECTATING);
        adminPlayer.teleportAsync(currentLocationHistory.get(locationIndex));
    }

    public void teleportForwardInHistory()
    {
        if (locationHistoryOffset == 0)
        {
            adminPlayer.sendMessage(ChatColor.RED + "You have no more forward teleport history.");
            return;
        }

        locationHistoryOffset--;
        int locationIndex = currentLocationHistory.size() - locationHistoryOffset - 1;
        if (locationIndex >= currentLocationHistory.size())
        {
            locationHistoryOffset++;
            adminPlayer.sendMessage(ChatColor.RED + "You have no more previous teleport history.");
            return;
        }

        Location teleportLocation = currentLocationHistory.get(locationIndex);
        setAdminState(AdminState.SPECTATING);
        adminPlayer.teleportAsync(teleportLocation);
    }


    /*
      * /target -> toggles on/off spectator
      * /target <player> -> spectator and tps to target
      */
    public void toggleAdminMode(OfflinePlayer player, Location targetLocation)
    {
        // if somewhere in the middle of your history and teleporting somewhere, clear everything from the current point onward
        // the new location is your new beginning (0 offset)
        if (locationHistoryOffset > 0)
        {
            for (int i = 0; i < locationHistoryOffset; i++)
            {
                currentLocationHistory.remove(currentLocationHistory.size() - 1);
            }

            locationHistoryOffset = 0;
        }

        if (adminState == AdminState.FREEROAM)
        {
            currentLocationHistory.add(adminPlayer.getLocation());
            locationHistoryOffset = 0;
        }

        if (player != null || targetLocation != null)
        {

            if (player != null) {
                targetLocation = player.getLocation();
                if (targetLocation == null) {
                    this.adminPlayer.sendMessage(ChatColor.RED + "This player has never joined before.");
                    return;
                }
            }
            currentLocationHistory.add(targetLocation);
            locationHistoryOffset = 0;

            List<Admin> onlineAdmins = AdminManager.getOnlineAdmins();

            setAdminState(AdminState.SPECTATING);
            Location finalTargetLocation = targetLocation;
            adminPlayer.getScheduler().runDelayed(AdminToolbox.getInstance(), (task) -> {
                adminPlayer.teleportAsync(finalTargetLocation); // tp to self so player goes to ground (tick later,
                // doesn't TP otherwise)
            }, null, 2L);

            for (Admin admin : onlineAdmins)
            {
                if (admin.adminPlayer == this.adminPlayer) continue;

                admin.adminPlayer.sendMessage(ChatColor.GOLD + this.adminPlayer.getName() + " is spectating " + (player != null ? player.getName() + (!player.isOnline() ? " (offline player)" : "") : "at [" + targetLocation.getBlockX() + ", " + targetLocation.getBlockY() + ", " + targetLocation.getBlockZ() + (targetLocation.getWorld().getName().equals("world_nether") ? " (nether)]" : "]")));
            }

            return;
        }

        if (adminState.equals(AdminState.FREEROAM) || adminState.equals(AdminState.REVEALED))
            setAdminState(AdminState.SPECTATING);
        else
            setAdminState(AdminState.FREEROAM);
    }


    public void setAdminState(AdminState newAdminState)
    {
        AdminState previousAdminState = this.adminState;
        this.adminState = newAdminState;
        switch (newAdminState)
        {
            case FREEROAM:
                if (previousAdminState == AdminState.FREEROAM) break;
                adminPlayer.setGameMode(GameMode.SURVIVAL);
                loadPriorData();
                break;


            case SPECTATING:
                if (previousAdminState.equals(AdminState.FREEROAM)) savePriorData();
                adminPlayer.setGameMode(GameMode.SPECTATOR);
                break;


            case REVEALED:
                if (!previousAdminState.equals(AdminState.SPECTATING)) break;

                Location adminLocation = adminPlayer.getLocation();
                for (int y = 0; y < 100; y++)
                {
                    // teleport admin to first valid block location below them
                    Location adminTpLocation = adminLocation.clone().subtract(0, y, 0);
                    if (adminTpLocation.getBlock().getType() == Material.AIR) continue;

                    adminPlayer.teleportAsync(adminTpLocation.add(0, 1, 0));
                    adminLocation.add(adminTpLocation);
                    break;
                }

                // reveal the admin player by setting their gamemode to survival
                adminPlayer.getScheduler().runDelayed(AdminToolbox.getInstance(), (task) -> {
                    adminPlayer.setGameMode(GameMode.SURVIVAL);
                }, null, 1L);

                break;

            default:
                break;
        }
    }

    public AdminState getAdminState()
    {
        return adminState;
    }


    void savePriorData()
    {
        savedPriorLocation = adminPlayer.getLocation();
        savedPriorInventory = adminPlayer.getInventory().getContents();
        adminPlayer.getInventory().clear();
    }


    void loadPriorData()
    {
        if (savedPriorLocation != null) adminPlayer.teleportAsync(savedPriorLocation);
        savedPriorLocation = null;

        if (savedPriorInventory != null) adminPlayer.getInventory().setContents(savedPriorInventory);
        savedPriorInventory = null;
    }

    public Player getPlayer() {
        return adminPlayer;
    }
}
