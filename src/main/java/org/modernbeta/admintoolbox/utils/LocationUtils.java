package org.modernbeta.admintoolbox.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class LocationUtils {
	public static @NotNull CompletableFuture<Location> getHighestLocation(World world, int x, int z) {
		CompletableFuture<Location> locationFuture = new CompletableFuture<>();
		Location taskLocation = new Location(world, x, 0, z);

		Bukkit.getRegionScheduler().run(AdminToolboxPlugin.getInstance(), taskLocation, (task) -> {
			Block highestYBlock = world.getHighestBlockAt(x, z);
			locationFuture.complete(highestYBlock.getLocation().add(0, 1.1, 0));
		});

		return locationFuture;
	}

	public static World resolveWorld(@NotNull String worldName) {
		@Nullable World exactWorld = Bukkit.getWorld(worldName);
		if (exactWorld != null) {
			return exactWorld;
		}

		World defaultWorld = Bukkit.getWorlds().getFirst();
		// backwards-compatibility: resolve "overworld" to default world
		if (worldName.equalsIgnoreCase("overworld")) return defaultWorld;

		// look up world by short name (like 'nether' for `world_nether`)
		for (World world : Bukkit.getWorlds()) {
			if (getShortWorldName(world).equalsIgnoreCase(worldName)) return world;
		}

		return null;
	}

	public static Stream<String> getWorldNameCompletions(String partialName) {
		String partialNameLower = partialName.toLowerCase();

		Stream<String> fullWorldNames = Bukkit.getWorlds().stream()
			.map(World::getName);

		Stream<String> shortWorldNames = Bukkit.getWorlds().stream()
			.map(LocationUtils::getShortWorldName);

		return Stream.concat(fullWorldNames, shortWorldNames)
			.filter((name) -> name.toLowerCase().startsWith(partialNameLower));
	}

    public static String getShortWorldName(World world) {
        World defaultWorld = Bukkit.getWorlds().getFirst();

        if (world == null) {
            // Fallback: return default world name to avoid NPEs
            return defaultWorld.getName();
        }

        if (world.getName().equals(defaultWorld.getName())) return world.getName();

        // get name without `world_` (+1 accounts for the underscore)
        return world.getName().substring(defaultWorld.getName().length() + 1);
    }

	public static String prettifyCoordinates(Location location) {
		return String.format(
			"%s, %s, %s (%s)",
			location.getBlockX(),
			location.getBlockY(),
			location.getBlockZ(),
			getShortWorldName(location.getWorld())
		);
	}
}
