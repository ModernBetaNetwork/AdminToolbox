package org.modernbeta.admintoolbox;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.modernbeta.admintoolbox.admins.AdminManager;
import org.modernbeta.admintoolbox.api.AdminToolboxAPI;
import org.modernbeta.admintoolbox.commands.*;
import org.modernbeta.admintoolbox.tools.Freeze;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminToolbox extends JavaPlugin implements AdminToolboxAPI {
    static AdminToolbox instance;
    AdminManager adminManager = new AdminManager();

    Freeze freeze = new Freeze();

    @Override
    public void onEnable()
    {
        instance = this;
        getServer().getPluginManager().registerEvents(adminManager, this);
        getServer().getPluginManager().registerEvents(freeze, this);
        getInstance().getCommand("target").setExecutor(new TargetCommand());
        getInstance().getCommand("reveal").setExecutor(new RevealCommand());
        getInstance().getCommand("back").setExecutor(new BackCommand());
        getInstance().getCommand("forward").setExecutor(new ForwardCommand());
        getInstance().getCommand("freeze").setExecutor(new FreezeCommand());
        getInstance().getCommand("release").setExecutor(new ReleaseCommand());
        getInstance().getCommand("yell").setExecutor(new YellCommand());

        getServer().getServicesManager().register(AdminToolboxAPI.class, this, this, ServicePriority.Normal);
    }

    @Override
    public void onDisable()
    {
        adminManager.clearAdmins();
    }

    public static AdminToolbox getInstance()
    {
        return instance;
    }

    public AdminManager getAdminManager() {
        return adminManager;
    }
}
