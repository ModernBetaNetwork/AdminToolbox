package org.modernbeta.admintoolbox.integration.luckperms;

import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

public class LuckPermsIntegration {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	private final @NotNull LuckPerms api;
	private final @NotNull AdminStateContextCalculator adminStateContextCalculator;

	public LuckPermsIntegration(LuckPerms api) {
		this.api = api;
		this.adminStateContextCalculator = new AdminStateContextCalculator(plugin);
	}

	public LuckPerms api() {
		return this.api;
	}

	public void registerCalculator() {
		api().getContextManager().registerCalculator(this.adminStateContextCalculator);
	}

	public void unregisterCalculator() {
		api().getContextManager().unregisterCalculator(this.adminStateContextCalculator);
	}

	public void signalContextUpdate(@NotNull Player player) {
		api().getContextManager().signalContextUpdate(player);
	}
}
