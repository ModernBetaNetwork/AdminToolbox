package org.modernbeta.admintoolbox.integration.placeholderapi.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

import javax.annotation.Nullable;

public class StreamerModePlaceholder extends PlaceholderExpansion implements Relational {
	private final AdminToolboxPlugin plugin;

	private static final String SM_VIEW_PERMISSION = "admintoolbox.streamermode.view";
	private static final String SM_WEAR_PERMISSION = "admintoolbox.streamermode.wear";

	public StreamerModePlaceholder(AdminToolboxPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public @NotNull String getIdentifier() {
		return "streamermode";
	}

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public @NotNull String getAuthor() {
		return String.join(", ", plugin.getPluginMeta().getAuthors());
	}

	@SuppressWarnings("UnstableApiUsage")
	@Override
	public @NotNull String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public @Nullable String onPlaceholderRequest(Player viewer, Player wearer, String identifier) {
		if (viewer == null || wearer == null) return null;
		if (!viewer.hasPermission(SM_VIEW_PERMISSION)) return null;
		if (!wearer.hasPermission(SM_WEAR_PERMISSION)) return null;

		return plugin.getStreamerModeManager()
			.filter(sm -> sm.isActive(wearer))
			.map(sm -> ChatColor.RED + "[SM]")
			.orElse(null);
	}
}
