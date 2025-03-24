package org.modernbeta.admintoolbox.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.modernbeta.admintoolbox.admins.Admin;

public interface AdminToolboxAPI {
    void target(Player admin, Location location);
    void target(Player admin, Player target);

    Admin[] getOnlineAdmins();
}
