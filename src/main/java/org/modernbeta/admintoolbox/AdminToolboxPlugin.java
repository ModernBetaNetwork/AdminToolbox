package org.modernbeta.admintoolbox;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.modernbeta.admintoolbox.commands.*;
import org.modernbeta.admintoolbox.managers.FreezeManager;
import org.modernbeta.admintoolbox.managers.admin.AdminManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class AdminToolboxPlugin extends JavaPlugin {
	static AdminToolboxPlugin instance;

	AdminManager adminManager;
	FreezeManager freezeManager;

	PermissionAudience broadcastAudience;

	@Nullable
	private LuckPerms luckPermsAPI;

	private File adminStateConfigFile;
	private FileConfiguration adminStateConfig;

	private static final String ADMIN_STATE_CONFIG_FILENAME = "admin-state.yml";

	private static final String BROADCAST_AUDIENCE_PERMISSION = "admintoolbox.broadcast.receive";
	public static final String BROADCAST_EXEMPT_PERMISSION = "admintoolbox.broadcast.exempt";

	@Override
	public void onEnable() {
		instance = this;

		this.adminManager = new AdminManager();
		this.freezeManager = new FreezeManager();

		this.broadcastAudience = new PermissionAudience(BROADCAST_AUDIENCE_PERMISSION);

		{
			RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
			if (provider != null) this.luckPermsAPI = provider.getProvider();
		}

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
		getCommand("streamermode").setExecutor(new StreamerModeCommand());

		initializeConfig();

		getLogger().info(String.format("Enabled %s", getPluginMeta().getDisplayName()));
	}

	@Override
	public void onDisable() {
		getLogger().info(String.format("Disabled %s", getPluginMeta().getDisplayName()));
	}

	private void createAdminStateConfig() {
		this.adminStateConfigFile = new File(getDataFolder(), ADMIN_STATE_CONFIG_FILENAME);
		if (!this.adminStateConfigFile.exists()) {
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

	public Optional<LuckPerms> getLuckPermsAPI() {
		return Optional.ofNullable(this.luckPermsAPI);
	}

	private void initializeConfig() {
		FileConfiguration config = getConfig();
		YamlConfiguration defaults = new YamlConfiguration();

		{
			ConfigurationSection streamerMode = defaults.createSection("streamer-mode");
			streamerMode.set("allow", true);
			streamerMode.set("max-duration", 2400);
			// TODO: fill in default disabled permission(s) -- just admintoolbox.broadcast.receive?
			streamerMode.set("disable-permissions", List.of("admintoolbox.broadcast.receive"));

			// docs
			streamerMode.setInlineComments("allow", List.of("Enable or disable usage of Streamer Mode. 'true' enables usage of Streamer Mode, while 'false' disables Streamer Mode entirely."));
			streamerMode.setInlineComments("max-duration", List.of("The maximum duration a player can enable Streamer Mode for, in minutes."));
			streamerMode.setInlineComments("disable-permissions", List.of("The list of permissions to disable for the given time period."));
		}

		config.setDefaults(defaults);
		config.options().copyDefaults(true);

		saveConfig();
	}
}
