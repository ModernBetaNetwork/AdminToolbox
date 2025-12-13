package org.modernbeta.admintoolbox.integration;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Optional;

public class BlueMapIntegration {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	private @Nullable BlueMapAPI api;

	public BlueMapIntegration() {
		try {
			this.api = BlueMapAPI.getInstance().orElseThrow();
		} catch (NoClassDefFoundError | NoSuchElementException e) {
			plugin.getLogger().warning("BlueMap API not found! Some features will be unavailable.");
		}
	}

	public Optional<BlueMapAPI> getAPI() {
		return Optional.ofNullable(api);
	}
}
