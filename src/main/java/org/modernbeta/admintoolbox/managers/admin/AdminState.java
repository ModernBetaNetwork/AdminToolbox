package org.modernbeta.admintoolbox.managers.admin;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AdminState {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	final UUID playerId;

	Status status = Status.SPECTATING;

	private TeleportHistory<Location> teleportHistory;
	private ItemStack[] savedInventory;

	@Nullable
	Boolean savedMapVisibility;

	private AdminState(UUID playerId, TeleportHistory<Location> teleportHistory, ItemStack[] inventory, CompletableFuture<Boolean> mapVisibilityFuture) {
		this.playerId = playerId;
		this.teleportHistory = teleportHistory;
		this.savedInventory = inventory;
		mapVisibilityFuture.thenAccept((mapVisibility) -> {
			this.savedMapVisibility = mapVisibility;
		});
	}

	@SuppressWarnings("UnstableApiUsage")
	static AdminState forPlayer(Player player) {
		CompletableFuture<Boolean> mapVisibilityFuture = new CompletableFuture<>();

		BlueMapAPI.getInstance().ifPresent((blueMap) -> {
			mapVisibilityFuture.complete(blueMap.getWebApp().getPlayerVisibility(player.getUniqueId()));
			blueMap.getWebApp().setPlayerVisibility(player.getUniqueId(), false);
		});

		return new AdminState(
			player.getUniqueId(),
			new TeleportHistory<>(player.getLocation().clone()),
			player.getInventory().getContents(),
			mapVisibilityFuture
		);
	}

	static AdminState fromConfig(UUID playerId, ConfigurationSection playerSection) {
		final int PLAYER_INVENTORY_SLOTS = 41;

		ItemStack[] items = new ItemStack[PLAYER_INVENTORY_SLOTS];
		if (playerSection.contains("inventory")) {
			List<?> invList = playerSection.getList("inventory");
			if (invList != null) {
				for (int i = 0; i < invList.size(); i++) {
					Object obj = invList.get(i);
					if (obj == null) {
						items[i] = null; // preserve empty slots
						continue;
					}
					if (!(obj instanceof ItemStack stack)) {
						AdminToolboxPlugin.getInstance().getLogger().warning(
							String.format(
								"Item in %s's inventory at position %d was not an ItemStack. This is a bug!\n\tFound: %s",
								playerId,
								i,
								obj.toString()
							)
						);
						continue;
					}

					items[i] = stack;
				}
			}
		}

		Status status = Status.valueOf(playerSection.getString("status", Status.SPECTATING.name()));

		Boolean mapVisibility = playerSection.contains("map_visibility") ?
			playerSection.getBoolean("map_visibility") : null;

		TeleportHistory<Location> history = new TeleportHistory<>(null);
		if (playerSection.isConfigurationSection("original_location")) {
			ConfigurationSection locSection = playerSection.getConfigurationSection("original_location");
			if (locSection != null) {
				World world = Bukkit.getWorld(locSection.getString("world"));
				if (world != null) {
					double x = locSection.getDouble("x");
					double y = locSection.getDouble("y");
					double z = locSection.getDouble("z");
					float yaw = (float) locSection.getDouble("yaw");
					float pitch = (float) locSection.getDouble("pitch");
					history = new TeleportHistory<>(new Location(world, x, y, z, yaw, pitch));
				}
			}
		}

		AdminState state = new AdminState(playerId, history, items, CompletableFuture.completedFuture(mapVisibility));
		state.setStatus(status);
		return state;
	}

	public Status getStatus() {
		return this.status;
	}

	protected void setStatus(Status newStatus) {
		this.status = newStatus;
		saveToFile();
	}

	ItemStack[] getSavedInventory() {
		return this.savedInventory;
	}

	public TeleportHistory<Location> getTeleportHistory() {
		return this.teleportHistory;
	}

	Optional<Boolean> getSavedMapVisibility() {
		return Optional.ofNullable(this.savedMapVisibility);
	}

	void saveToFile() {
		FileConfiguration adminState = plugin.getAdminStateConfig();
		ConfigurationSection playerSection = adminState.createSection(playerId.toString());

		playerSection.set("inventory", savedInventory);
		playerSection.set("status", status.name());
		if (savedMapVisibility != null) {
			playerSection.set("map_visibility", savedMapVisibility);
		}

		Location originalLocation = teleportHistory.getOriginalLocation();
		if (originalLocation != null) {
			ConfigurationSection locationSection = playerSection.createSection("original_location");
			locationSection.set("world", originalLocation.getWorld().getName());
			locationSection.set("x", originalLocation.getX());
			locationSection.set("y", originalLocation.getY());
			locationSection.set("z", originalLocation.getZ());
			locationSection.set("yaw", originalLocation.getYaw());
			locationSection.set("pitch", originalLocation.getPitch());
		}

		plugin.saveAdminStateConfig();
	}

	public enum Status {
		SPECTATING, REVEALED;
	}

	public static class TeleportHistory<T extends Location> {
		private final List<T> backLocations = new ArrayList<>();
		private final List<T> forwardLocations = new ArrayList<>();
		private T originalLocation = null;

		public TeleportHistory(T originalLocation) {
			this.originalLocation = originalLocation;
		}

		public void add(T location) {
			backLocations.add(location);
			forwardLocations.clear();
		}

		@Nullable
		public T goBack() {
			if (backLocations.isEmpty())
				return null;

			T previousLocation = backLocations.removeLast();
			forwardLocations.add(previousLocation);
			return previousLocation;
		}

		@Nullable
		public T goForward() {
			if (forwardLocations.isEmpty())
				return null;

			T nextLocation = forwardLocations.removeLast();
			backLocations.add(nextLocation);
			return nextLocation;
		}

		public T getOriginalLocation() {
			return originalLocation;
		}
	}
}
