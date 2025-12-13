package org.modernbeta.admintoolbox.managers.admin;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.managers.admin.AdminState.TeleportHistory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.modernbeta.admintoolbox.commands.TargetCommand.TARGET_PLAYER_PERMISSION;
import static org.modernbeta.admintoolbox.managers.admin.AdminState.Status.REVEALED;
import static org.modernbeta.admintoolbox.managers.admin.AdminState.Status.SPECTATING;

public class AdminManager implements Listener {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	Map<UUID, AdminState> adminStates = new ConcurrentHashMap<>();

	public void target(Player player, Location location, boolean appending) {
		final AdminState state = getAdminState(player).orElse(AdminState.forPlayer(player));

		player.getScheduler().run(plugin, (task) -> {
			if (!isActiveAdmin(player)) {
				player.getInventory().clear();
			} else if (appending) {
				TeleportHistory<Location> history = state.getTeleportHistory();
				if (history != null) {
					history.add(player.getLocation().clone());
				}
			}

			// set spectator before teleporting, so we don't show for a tick
			player.setGameMode(GameMode.SPECTATOR);
			state.setStatus(SPECTATING);
			adminStates.put(player.getUniqueId(), state);

			Component actionBarMessage = MiniMessage.miniMessage().deserialize("<gold>Teleporting...");
			player.sendActionBar(actionBarMessage);

			player.teleportAsync(location, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept((didTeleport) -> {
				// clear the action bar since the teleport is no longer pending
				player.sendActionBar(Component.empty());

				if (!didTeleport) {
					player.sendRichMessage("<red>Error: You couldn't be teleported! This is probably a Paper bug.");
				}
			});
		}, null);
	}

	public void target(Player admin, Location location) {
		target(admin, location, true);
	}

	public void reveal(Player admin) {
		reveal(admin, true);
	}

	public void reveal(Player player, boolean getSafeLocation) {
		if (!isActiveAdmin(player))
			throw new RuntimeException("Tried to reveal \"" + player.getName() + "\", who was not spectating! This is a bug.");

		UUID uuid = player.getUniqueId();
		CompletableFuture<Location> locationFuture;

		if (getSafeLocation) {
			locationFuture = getNextLowestSafeLocation(player.getLocation());
		} else {
			locationFuture = CompletableFuture.completedFuture(player.getLocation());
		}

		locationFuture.thenAccept((location) -> {
			player.teleportAsync(location).thenAccept((didTeleport) -> {
				if (!didTeleport)
					throw new RuntimeException("Couldn't teleport player \"" + player.getName() + "\" for reveal! This is a bug.");

				player.setGameMode(GameMode.SURVIVAL);
				adminStates.get(uuid).setStatus(REVEALED);
			});
		});
	}

	@SuppressWarnings("UnstableApiUsage")
	public void restore(Player player) {
		if (!isActiveAdmin(player))
			throw new RuntimeException("Tried to restore \"" + player.getName() + "\", who was not in an admin state! This is a bug.");

		player.getScheduler().run(plugin, (task) -> {
			UUID uuid = player.getUniqueId();
			AdminState adminState = adminStates.get(uuid);
			Location originalLocation = adminState.getTeleportHistory().getOriginalLocation();
			ItemStack[] originalInventory = adminState.getSavedInventory();

			player.teleportAsync(originalLocation).thenAccept((didTeleport) -> {
				if (!didTeleport) {
					player.sendRichMessage("<red>Error: Could not teleport you back to your original location.");
					throw new RuntimeException("Could not teleport \"" + player.getName() + "\" back to their original location! This is a bug.");
				}

				adminState.setFullbrightEnabled(false);
				player.setGameMode(GameMode.SURVIVAL);
				player.getInventory().setContents(originalInventory);

				adminStates.remove(uuid);
				FileConfiguration adminStateConfig = plugin.getAdminStateConfig();
				adminStateConfig.set(player.getUniqueId().toString(), null);
				plugin.saveAdminStateConfig();

				BlueMapAPI.getInstance().ifPresent((blueMap) -> {
					adminState.getSavedMapVisibility().ifPresent((visibility) -> {
						blueMap.getWebApp().setPlayerVisibility(player.getUniqueId(), visibility);
					});
				});
			});
		}, () -> {
			throw new RuntimeException("Tried to restore a retired player! This is a bug.");
		});
	}

	public void restoreAll() {
		for (UUID adminId : adminStates.keySet()) {
			Player admin = Bukkit.getPlayer(adminId);
			restore(admin);
		}
	}

	public boolean isSpectating(OfflinePlayer player) {
		return getAdminState(player).map((state) -> state.getStatus() == SPECTATING).orElse(false);
	}

	public boolean isRevealed(OfflinePlayer player) {
		return getAdminState(player).map((state) -> state.getStatus() == REVEALED).orElse(false);
	}

	public boolean isActiveAdmin(OfflinePlayer player) {
		return adminStates.containsKey(player.getUniqueId());
	}

	public Optional<AdminState> getAdminState(OfflinePlayer player) {
		return Optional.ofNullable(adminStates.get(player.getUniqueId()));
	}

	private @Nullable AdminState loadStateFromFile(UUID playerId) {
		FileConfiguration state = plugin.getAdminStateConfig();
		if (!state.isConfigurationSection(playerId.toString())) return null;

		ConfigurationSection playerSection = state.getConfigurationSection(playerId.toString());
		assert playerSection != null;

		return AdminState.fromConfig(playerId, playerSection);
	}

	private CompletableFuture<Location> getNextLowestSafeLocation(Location location) {
		CompletableFuture<Location> safeLocationFuture = new CompletableFuture<>();
		final int LOWEST_BLOCK_Y = 0;

		Bukkit.getRegionScheduler().run(plugin, location, (task) -> {
			Location safeLocation = location.clone();

			for (int y = location.getBlockY(); y > LOWEST_BLOCK_Y; y--) {
				safeLocation.setY(y);
				Block block = location.getWorld().getBlockAt(safeLocation);
				if (!block.getType().isSolid()) continue;
				safeLocation.setY(
					block.getBoundingBox().getMaxY()
				);

				safeLocationFuture.complete(safeLocation);
				return;
			}

			// if we couldn't find a safe location, complete with the original location
			safeLocationFuture.complete(location);
		});

		return safeLocationFuture;
	}

	@EventHandler
	void onEntityTargetAdmin(EntityTargetEvent targetEvent) {
		if (!(targetEvent.getEntity() instanceof LivingEntity)) return;
		if (!(targetEvent.getTarget() instanceof Player player)) return;
		if (!isActiveAdmin(player)) return;
		targetEvent.setCancelled(true);
	}

	@SuppressWarnings("UnstableApiUsage")
	@EventHandler(priority = EventPriority.HIGHEST)
	void onAdminHurt(EntityDamageEvent damageEvent) {
		if (!(damageEvent.getEntity() instanceof Player player)) return;
		if (!isActiveAdmin(player)) return;
		// allow PVP damage to actually affect health
		if (damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
			&& damageEvent.getDamageSource().getCausingEntity() instanceof Player) return;
		damageEvent.setDamage(0.0);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void onAdminQuit(PlayerQuitEvent quitEvent) {
		if (!isActiveAdmin(quitEvent.getPlayer())) return;
		AdminState state = getAdminState(quitEvent.getPlayer())
			// if they are an active admin, we should always have state -- throw if we don't, that's a bug!
			.orElseThrow();

		state.saveToFile();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void onPluginDisable(PluginDisableEvent disableEvent) {
		for (AdminState adminState : adminStates.values()) {
			adminState.saveToFile();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void onAdminJoin(PlayerJoinEvent joinEvent) {
		Player player = joinEvent.getPlayer();
		Optional.ofNullable(loadStateFromFile(player.getUniqueId()))
			.ifPresent((state) -> {
				adminStates.put(player.getUniqueId(), state);

				switch (state.getStatus()) {
					case SPECTATING -> player.setGameMode(GameMode.SPECTATOR);
					case REVEALED -> player.setGameMode(GameMode.SURVIVAL);
				}

				BlueMapAPI.getInstance().ifPresent((blueMap) -> {
					blueMap.getWebApp().setPlayerVisibility(player.getUniqueId(), false);
				});

				player.sendRichMessage(
					"<green>Your <revealed>admin session has been restored.",
					Placeholder.unparsed("revealed", state.getStatus() == REVEALED ? "revealed " : "")
				);
			});
	}

	/**
	 * Prevent players in admin mode from using the spectator teleport menu unless they have
	 * permission to teleport to players directly.
	 */
	@EventHandler
	void onAdminTeleport(PlayerTeleportEvent teleportEvent) {
		Player player = teleportEvent.getPlayer();
		if (!isActiveAdmin(player)) return;
		if (teleportEvent.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
		if (player.hasPermission(TARGET_PLAYER_PERMISSION))
			// allow if admin can spectate players directly, allow it
			return;

		player.sendRichMessage("<red>You don't have permission to spectate players");
		teleportEvent.setCancelled(true);
	}
}
