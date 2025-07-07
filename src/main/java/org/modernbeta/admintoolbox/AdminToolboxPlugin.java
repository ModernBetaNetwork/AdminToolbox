package org.modernbeta.admintoolbox;

import de.myzelyam.api.vanish.VanishAPI;
import de.myzelyam.supervanish.SuperVanish;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.modernbeta.admintoolbox.commands.*;
import org.modernbeta.admintoolbox.managers.FreezeManager;
import org.modernbeta.admintoolbox.managers.admin.AdminManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class AdminToolboxPlugin extends JavaPlugin {
    static AdminToolboxPlugin instance;

	AdminManager adminManager;
    FreezeManager freezeManager;

	PermissionAudience broadcastAudience;

	@Nullable
	SuperVanish superVanish;

	private File adminStateConfigFile;
	private FileConfiguration adminStateConfig;

	private static final int BSTATS_PLUGIN_ID = 26406; // TODO: find a way to load this from gradle.properties

	private static final String ADMIN_STATE_CONFIG_FILENAME = "admin-state.yml";

	private static final String BROADCAST_AUDIENCE_PERMISSION = "admintoolbox.broadcast.receive";
	public static final String BROADCAST_EXEMPT_PERMISSION = "admintoolbox.broadcast.exempt";

    @Override
    public void onEnable() {
        instance = this;

		this.adminManager = new AdminManager();
        this.freezeManager = new FreezeManager();

		this.broadcastAudience = new PermissionAudience(BROADCAST_AUDIENCE_PERMISSION);

		createAdminStateConfig();
		this.adminStateConfig = getAdminStateConfig();

        getServer().getPluginManager().registerEvents(adminManager, this);
        getServer().getPluginManager().registerEvents(freezeManager, this);

		getCommand("target").setExecutor(new TargetCommand());
		getCommand("reveal").setExecutor(new RevealCommand());
		getCommand("back").setExecutor(new GoBackCommand());
		getCommand("forward").setExecutor(new GoForwardCommand());
		getCommand("freeze").setExecutor(new FreezeCommand());
		getCommand("unfreeze").setExecutor(new UnfreezeCommand());
		getCommand("yell").setExecutor(new YellCommand());
		getCommand("spawn").setExecutor(new SpawnCommand());

		new Metrics(this, BSTATS_PLUGIN_ID);
		// we can track additional metrics if we feel like it

        getLogger().info(String.format("Enabled %s", getPluginMeta().getDisplayName()));
    }

    @Override
    public void onDisable() {
        getLogger().info(String.format("Disabled %s", getPluginMeta().getDisplayName()));
    }

	private void createAdminStateConfig() {
		this.adminStateConfigFile = new File(getDataFolder(), ADMIN_STATE_CONFIG_FILENAME);
		if(!this.adminStateConfigFile.exists()) {
			this.adminStateConfigFile.getParentFile().mkdirs();
			saveResource(ADMIN_STATE_CONFIG_FILENAME, false);
		}

		this.adminStateConfig = YamlConfiguration.loadConfiguration(adminStateConfigFile);
	}

	public FileConfiguration getAdminStateConfig() {
		// TODO: this re-reads the file from file system every time, should not be needed
		// 		but we have run into some desynced state somehow. Figure out why!
		try {
			this.adminStateConfig.load(adminStateConfigFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this.adminStateConfig;
	}

	public void saveAdminStateConfig() {
		try {
			this.adminStateConfig.save(adminStateConfigFile);
		} catch (IOException e) {
			// Throw this, this should never happen with the safeguards we use in onEnable
			throw new RuntimeException(e);
		}
	}

	public static AdminToolboxPlugin getInstance() {
		return instance;
	}

	public AdminManager getAdminManager() {
		return adminManager;
	}

	public FreezeManager getFreezeManager() {
		return freezeManager;
	}

	public PermissionAudience getAdminAudience() {
		return broadcastAudience;
	}



	public Optional<SuperVanish> getVanish() {
		return Optional.ofNullable(VanishAPI.getPlugin());
	}
}
