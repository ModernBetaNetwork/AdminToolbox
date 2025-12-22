package org.modernbeta.admintoolbox.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.PermissionAudience;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.modernbeta.admintoolbox.utils.LocationUtils.*;

public class TargetCommand implements CommandExecutor, TabCompleter {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	public static final String TARGET_CMD_PERMISSION = "admintoolbox.target";
	public static final String TARGET_PLAYER_PERMISSION = "admintoolbox.target.player";
	public static final String TARGET_COORDINATES_PERMISSION = "admintoolbox.target.location";

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendRichMessage("<red>Error: You must be a player to use this command.");
			return false;
		}

		AtomicReference<String> targetLabel = new AtomicReference<>();
		CompletableFuture<Location> targetLocationFuture = new CompletableFuture<>();

		switch (args.length) {
			// /target (toggle or target self)
			case 0 -> {
				if (plugin.getAdminManager().isSpectating(player)) {
					plugin.getAdminManager().restore(player);
					break;
				}

				if (!player.hasPermission(TARGET_CMD_PERMISSION)) {
					sendNoPermissionMessage(sender);
					return true;
				}

				plugin.getAdminManager().target(player, player.getLocation());
			}
			// /target player
			case 1 -> {
				if (!(player.hasPermission(TARGET_PLAYER_PERMISSION))) {
					sendNoPermissionMessage(sender);
					return true;
				}

				String targetPlayerName = args[0];

				Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
					try {
						OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetPlayerName);
						targetLabel.set(offlineTarget.getName());
						if (offlineTarget.isOnline()) {
							targetLabel.getAndUpdate((existingLabel) -> existingLabel + ".");
						} else {
							targetLabel.getAndUpdate((existingLabel) -> existingLabel + ". (offline player)");
						}
						Location location = offlineTarget.getLocation();
						if (location == null) {
							throw new IllegalArgumentException("Player '" + targetPlayerName + "' has no location data.");
						}
						targetLocationFuture.complete(location);
					} catch (Exception e) {
						targetLocationFuture.completeExceptionally(e);
					}
				});
			}
			// /target x z
			case 2 -> {
				if (!(player.hasPermission(TARGET_COORDINATES_PERMISSION))) {
					sendNoPermissionMessage(sender);
					return true;
				}

				try {
					int x = Integer.parseInt(args[0]);
					int z = Integer.parseInt(args[1]);

					CompletableFuture<Location> highestLocationFuture = getHighestLocation(player.getWorld(), x, z);
					highestLocationFuture.thenAccept((highestLocation) -> {
						targetLabel.set(prettifyCoordinates(highestLocation));
						targetLocationFuture.complete(highestLocation.toCenterLocation());
					}).exceptionally(ex -> {
						targetLocationFuture.completeExceptionally(
							new IllegalArgumentException(
								String.format("Could not find a safe location at %d, %d in %s.", x, z, player.getWorld().getName()))
						);
						return null;
					});
				} catch (Exception e) {
					targetLocationFuture.completeExceptionally(e);
				}
			}
			// /target x y z (OR) /target x z world
			case 3 -> {
				if (!(player.hasPermission(TARGET_COORDINATES_PERMISSION))) {
					sendNoPermissionMessage(sender);
					return true;
				}

				try {
					int x = Integer.parseInt(args[0]);
					int y = Integer.parseInt(args[1]);
					int z = Integer.parseInt(args[2]);

					Location targetLocation = new Location(
						player.getWorld(), x, y, z,
						player.getYaw(), player.getPitch()
					);
					targetLabel.set(prettifyCoordinates(targetLocation));
					targetLocationFuture.complete(targetLocation.toCenterLocation());
				} catch (NumberFormatException e) {
					// try getting highest x/z at provided world if we couldn't parse all the coords
					try {
						int x = Integer.parseInt(args[0]);
						int z = Integer.parseInt(args[1]);

						World world = resolveWorld(args[2]);
						if (world == null) {
							targetLocationFuture.completeExceptionally(
								new IllegalArgumentException("Could not find world '" + args[2] + "'.")
							);
							break;
						}

						CompletableFuture<Location> highestLocationFuture = getHighestLocation(world, x, z);
						highestLocationFuture.thenAccept((highestLocation) -> {
							targetLabel.set(prettifyCoordinates(highestLocation));
							targetLocationFuture.complete(highestLocation.toCenterLocation());
						}).exceptionally(ex -> {
							targetLocationFuture.completeExceptionally(
								new IllegalArgumentException(
									String.format("Could not find a safe location at %d, %d in %s.", x, z, world.getName()))
							);
							return null;
						});
					} catch (Exception ex) {
						targetLocationFuture.completeExceptionally(ex);
					}
				} catch (Exception e) {
					targetLocationFuture.completeExceptionally(e);
				}
			}
			// /target x y z world
			case 4 -> {
				if (!(player.hasPermission(TARGET_COORDINATES_PERMISSION))) {
					sendNoPermissionMessage(sender);
					return true;
				}

				try {
					int x = Integer.parseInt(args[0]);
					int y = Integer.parseInt(args[1]);
					int z = Integer.parseInt(args[2]);

					World world = resolveWorld(args[3]);
					if (world == null) {
						targetLocationFuture.completeExceptionally(
							new IllegalArgumentException("Could not find world '" + args[3] + "'.")
						);
						break;
					}

					Location targetLocation = new Location(world, x, y, z);
					targetLabel.set(prettifyCoordinates(targetLocation));
					targetLocationFuture.complete(targetLocation.toCenterLocation());
				} catch (Exception e) {
					targetLocationFuture.completeExceptionally(e);
				}
			}
			default -> {
				return false;
			}
		}

		targetLocationFuture.thenAccept((location -> {
			player.getScheduler().run(plugin, (task) -> {
				plugin.getAdminManager().target(player, location);
				sender.sendRichMessage(
					"<gold>Spectating at <target>",
					Placeholder.unparsed("target", targetLabel.get())
				);

				if (!sender.hasPermission(AdminToolboxPlugin.BROADCAST_EXEMPT_PERMISSION)) {
					PermissionAudience adminAudience = plugin.getAdminAudience()
						.excluding(player);
					adminAudience
						.sendMessage(MiniMessage.miniMessage().deserialize(
							"<gold><admin> is spectating at <target>",
							Placeholder.unparsed("admin", sender.getName()),
							Placeholder.unparsed("target", targetLabel.get())
						));
				}
			}, null);
		})).exceptionally(ex -> {
			Throwable cause = ex.getCause();
			switch (cause) {
				case NumberFormatException exception ->
					sender.sendRichMessage("<red>Couldn't parse coordinates: " + exception.getMessage());
				case IllegalArgumentException exception ->
					sender.sendRichMessage("<red>" + exception.getMessage());
				case NullPointerException exception -> {
					if (args.length == 1) {
						sender.sendRichMessage("<red>Could not find player '" + args[0] + "'.");
					} else {
						sender.sendRichMessage("<red>" + Optional.of(exception.getMessage()).orElse("Target location not found."));
					}
				}
				case null, default -> {
					sender.sendRichMessage("<red>Couldn't use that location.");
					plugin.getLogger().warning("Error in /target command: " + ex.getMessage());
				}
			}
			return null;
		});

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		switch (args.length) {
			// /target PLAYER
			case 1 -> {
				if(!sender.hasPermission(TARGET_PLAYER_PERMISSION)) return List.of();

				String partialName = args[0].toLowerCase();

				return Bukkit.getOnlinePlayers().stream()
					.map(OfflinePlayer::getName)
					.filter((name) -> name.toLowerCase().startsWith(partialName) && !name.equals(sender.getName()))
					.toList();
			}
			// /target x z WORLD
			case 3 -> {
				if(!sender.hasPermission(TARGET_COORDINATES_PERMISSION)) return List.of();

				if (isInteger(args[0]) && isInteger(args[1])) {
					return getWorldNameCompletions(args[2]).toList();
				}
				return List.of();
			}
			// /target x y z WORLD
			case 4 -> {
				if(!sender.hasPermission(TARGET_COORDINATES_PERMISSION)) return List.of();

				if (isInteger(args[0]) && isInteger(args[1]) && isInteger(args[2])) {
					return getWorldNameCompletions(args[3]).toList();
				}
				return List.of();
			}
			default -> {
				return List.of();
			}
		}
	}

	private boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void sendNoPermissionMessage(CommandSender sender) {
		sender.sendRichMessage("<red>You do not have permission!");
	}
}
