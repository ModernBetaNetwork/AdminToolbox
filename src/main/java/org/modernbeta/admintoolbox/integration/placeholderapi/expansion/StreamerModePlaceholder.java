package org.modernbeta.admintoolbox.integration.placeholderapi.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

import javax.annotation.Nullable;

public class StreamerModePlaceholder extends PlaceholderExpansion implements Relational {
	private final AdminToolboxPlugin plugin;

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
		return null;
	}
}
