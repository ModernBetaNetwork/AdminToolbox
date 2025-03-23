package org.modernbeta.admintoolbox.admins;

import org.modernbeta.admintoolbox.AdminToolbox;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AdminManager implements Listener
{
    static HashMap<UUID, Admin> onlineAdmins = new HashMap<>();


    public void addAdmin(Player player)
    {
        if (player.hasPermission("AdminToolbox.admin"))
            onlineAdmins.put(player.getUniqueId(), new Admin(player));
    }

    public void removeAdmin(Player player)
    {
        if (!isAdmin(player)) return;

        getOnlineAdmin(player).setAdminState(AdminState.FREEROAM);
        onlineAdmins.remove(player.getUniqueId());
    }

    public void clearAdmins()
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (!isAdmin(player)) continue;

            getOnlineAdmin(player).setAdminState(AdminState.FREEROAM);
        }

        onlineAdmins.clear();
    }

    public static Admin getOnlineAdmin(Player player)
    {
        return onlineAdmins.get(player.getUniqueId());
    }

    public static Admin getOnlineAdmin(UUID uuid)
    {
        return onlineAdmins.get(uuid);
    }

    public static List<Admin> getOnlineAdmins()
    {
        return onlineAdmins.values().stream().toList();
    }

    public static boolean isAdmin(Player player)
    {
        return onlineAdmins.containsKey(player.getUniqueId());
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event)
    {
        addAdmin(event.getPlayer());
    }

    @EventHandler
    void onPlayerLeave(PlayerQuitEvent event)
    {
        removeAdmin(event.getPlayer());
    }

    /*@EventHandler
    void onAdminModifyingContainer(InventoryClickEvent event)
    {
        if (event.getClickedInventory() == null) return;

        // get if player is an admin not in freeroam
        Player player = (Player)event.getWhoClicked();
        if (!isAdmin(player) || getOnlineAdmin(player).adminState == AdminState.FREEROAM)
            return;

        // if the inventory clicked isn't their own, cancel it
        boolean ownInventoryClicked = event.getClickedInventory() instanceof PlayerInventory;
        InventoryHolder inventoryHolder = event.getClickedInventory().getHolder();
        boolean isContainerInventory = inventoryHolder instanceof ShulkerBox || inventoryHolder instanceof Furnace || inventoryHolder instanceof Dropper;
        if (isContainerInventory || (event.isShiftClick() && ownInventoryClicked))
            event.setCancelled(true);
    }

    @EventHandler
    void onAdminModifyingContainer(InventoryDragEvent event)
    {
        // get if player is an admin not in freeroam
        Player player = (Player)event.getWhoClicked();
        if (!isAdmin(player) || getOnlineAdmin(player).adminState == AdminState.FREEROAM)
            return;

        // loop through all the modified slots that were dragged on
        InventoryView inventoryView = event.getView();
        for (Integer slot : event.getRawSlots()) {

            // if any of the dragged items are in the double chest then mirror the contents (next tick) and exit loop
            Inventory inventory = inventoryView.getInventory(slot);
            if (inventory != null && !inventory.equals(player.getInventory()))
            {
                event.setCancelled(true);
                return;
            }

        }
    }

    @EventHandler
    void onAdminThrowingItem(PlayerDropItemEvent event)
    {
        // get if player is an admin not in freeroam
        Player player = event.getPlayer();
        if (isAdmin(player) && getOnlineAdmin(player).adminState != AdminState.FREEROAM)
            event.setCancelled(true);
    }

    @EventHandler
    void onAdminPickingUpItem(EntityPickupItemEvent event)
    {
        if (event.getEntity() instanceof Player player)
        {
            if (isAdmin(player) && getOnlineAdmin(player).adminState != AdminState.FREEROAM)
                event.setCancelled(true);
        }
    }*/

    @EventHandler
    void onAdminHurt(EntityDamageEvent event)
    {
        // admins not in free roam can't be hurt
        if (event.getEntity() instanceof Player player)
        {
            if (!isAdmin(player)) return;

            Admin admin = getOnlineAdmin(player);
            if (admin.getAdminState() == AdminState.FREEROAM) return;

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AdminToolbox.getInstance(), () -> {
                player.setHealth(20);
            },  1L);
        }
    }

    @EventHandler
    void onAdminHurt(PlayerDeathEvent event)
    {
        // admins not in free roam can't be hurt
        Player player = event.getPlayer();
        if (!isAdmin(player)) return;

        Admin admin = getOnlineAdmin(player);
        if (admin.getAdminState() != AdminState.FREEROAM)
            event.setCancelled(true);
    }
}
