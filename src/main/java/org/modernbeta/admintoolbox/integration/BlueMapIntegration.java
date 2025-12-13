package org.modernbeta.admintoolbox.integration;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.entity.Player;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

import javax.annotation.Nullable;

public class BlueMapIntegration {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	private @Nullable BlueMapAPI api;

	public BlueMapIntegration() {
		this.api = BlueMapAPI.getInstance().orElseThrow();
	}

	public void setPlayerVisibility(Player player, boolean visible) {
		if (api == null) return;
		api.getWebApp().setPlayerVisibility(player.getUniqueId(), visible);
	}

	public Boolean getPlayerVisibility(Player player) {
		if (api == null) return null;
		return api.getWebApp().getPlayerVisibility(player.getUniqueId());
	}
}
