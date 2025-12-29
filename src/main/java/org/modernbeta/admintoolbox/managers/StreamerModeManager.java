package org.modernbeta.admintoolbox.managers;

import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.integration.luckperms.LuckPermsIntegration;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StreamerModeManager {
	public static final String STREAMER_MODE_USE_PERMISSION = "admintoolbox.streamermode";
	public static final String STREAMER_MODE_BYPASS_MAX_DURATION_PERMISSION = "admintoolbox.streamermode.unlimited";
	private static final String STREAMER_MODE_LP_META_KEY = "at-streamer-mode-enabled";

	private final AdminToolboxPlugin plugin;
	private final LuckPermsIntegration luckPerms;

	public StreamerModeManager(AdminToolboxPlugin plugin, LuckPermsIntegration luckPerms) {
		this.plugin = plugin;
		this.luckPerms = luckPerms;
	}

	public record StreamerModeState(
		OfflinePlayer player,
		boolean isEnabled,
		@Nullable Duration duration
	) {
	}

	public CompletableFuture<StreamerModeState> enable(Player player, Duration duration) {
		UserManager userManager = luckPerms.api().getUserManager();
		User user = luckPerms.api().getPlayerAdapter(Player.class).getUser(player);
		List<String> disablePermissions = plugin.getConfig().getStringList("streamer-mode.disable-permissions");

		MetaNode metaNode = MetaNode.builder()
			.key(STREAMER_MODE_LP_META_KEY)
			.value(Boolean.toString(true))
			.expiry(duration)
			.build();

		user.data().clear(NodeType.META.predicate((node) -> node.getMetaKey().equals(STREAMER_MODE_LP_META_KEY)));
		user.data().add(metaNode);

		// using LuckPerms API, add negated/'false' versions of permissions from config.yml to user for duration
		for (String permission : disablePermissions) {
			Node permissionNode = PermissionNode.builder()
				.permission(permission)
				.expiry(duration)
				.negated(true)
				.build();

			user.data().clear(NodeType.PERMISSION.predicate(
				(node) -> node.getPermission().equals(permission) && node.isNegated()
			));
			user.data().add(permissionNode);
		}

		return userManager.saveUser(user)
			.thenApply((_void) -> new StreamerModeState(
				player,
				true,
				duration
			));
	}

	public CompletableFuture<StreamerModeState> disable(Player player) {
		UserManager userManager = luckPerms.api().getUserManager();
		User user = luckPerms.api().getPlayerAdapter(Player.class).getUser(player);
		List<String> disablePermissions = plugin.getConfig().getStringList("streamer-mode.disable-permissions");

		user.data().clear(NodeType.META.predicate((node) -> node.getMetaKey().equals(STREAMER_MODE_LP_META_KEY)));
		user.data().clear(NodeType.PERMISSION.predicate((node) -> // only delete negated, expiring nodes that match configured permissions
			node.isNegated()
				&& node.getExpiryDuration() != null
				&& node.getExpiryDuration().isPositive()
				&& disablePermissions.contains(node.getPermission())
		));

		return userManager.saveUser(user)
			.thenApply((_void) -> new StreamerModeState(
				player,
				false,
				null
			));
	}

	public boolean isActive(Player player) {
		return getState(player).isEnabled();
	}

	public boolean isAllowableDuration(Duration duration, Player player) {
		final double maxDurationMinutes = plugin.getConfig().getDouble("streamer-mode.max-duration");
		return (duration.getSeconds() <= (maxDurationMinutes * 60))
			|| player.hasPermission(STREAMER_MODE_BYPASS_MAX_DURATION_PERMISSION);
	}

	public StreamerModeState getState(Player player) {
		final PlayerAdapter<Player> playerAdapter =
			luckPerms.api().getPlayerAdapter(Player.class);

		boolean isEnabled = playerAdapter
			.getMetaData(player)
			.getMetaValue(STREAMER_MODE_LP_META_KEY, Boolean::valueOf)
			.orElse(false);

		Duration duration = null;
		if (isEnabled) getDuration:{
			Node node = playerAdapter
				.getMetaData(player)
				.queryMetaValue(STREAMER_MODE_LP_META_KEY)
				.node();

			if (node == null || node.hasExpired()) {
				isEnabled = false;
				break getDuration;
			}

			duration = node.getExpiryDuration();
		}

		return new StreamerModeState(
			player,
			isEnabled,
			duration
		);
	}
}
