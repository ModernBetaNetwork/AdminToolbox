package org.modernbeta.admintoolbox.integration.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.integration.placeholderapi.expansion.StreamerModePlaceholder;

public class PlaceholderAPIIntegration {
	private final AdminToolboxPlugin plugin;

	private final StreamerModePlaceholder streamerModePlaceholder;

	public PlaceholderAPIIntegration(AdminToolboxPlugin plugin) {
		this.plugin = plugin;
		this.streamerModePlaceholder = new StreamerModePlaceholder(plugin);
	}

	public boolean registerPlaceholders() {
		return this.streamerModePlaceholder.register();
	}
}
