package org.modernbeta.admintoolbox.integration.luckperms;

import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

public class LuckPermsIntegration {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	private final @NotNull LuckPerms api;

	public LuckPermsIntegration(LuckPerms api) {
		this.api = api;
	}

	public LuckPerms api() {
		return this.api;
	}
}
