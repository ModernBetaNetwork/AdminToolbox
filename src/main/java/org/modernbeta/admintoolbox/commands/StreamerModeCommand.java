package org.modernbeta.admintoolbox.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.managers.StreamerModeManager;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.modernbeta.admintoolbox.managers.StreamerModeManager.STREAMER_MODE_USE_PERMISSION;

public class StreamerModeCommand implements CommandExecutor, TabCompleter {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();
	private final StreamerModeManager manager;

	public StreamerModeCommand(StreamerModeManager streamerModeManager) {
		this.manager = streamerModeManager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission(STREAMER_MODE_USE_PERMISSION))
			return false; // Bukkit should handle this for us, just a sanity-check
		if (!(sender instanceof Player player)) {
			sender.sendRichMessage("<red>Only players may use streamer mode.");
			return true;
		}

		if (!plugin.getConfig().getBoolean("streamer-mode.allow", false)) {
			sender.sendRichMessage("<red>Streamer mode is disabled on this server.<addendum>",
				Placeholder.unparsed("addendum", player.isOp() ? " (streamer-mode -> allow is 'false' in config.yml)" : ""));
			return true;
		}
		if (plugin.getLuckPerms().isEmpty()) {
			sender.sendRichMessage("<red>LuckPerms is required to use streamer mode. Is it enabled?");
			return true;
		}

		if (args.length == 0 && manager.isActive(player)) {
			manager.disable(player)
				.thenAccept(state -> {
					sender.sendRichMessage("<gold>Streamer mode has been disabled.");
				});
			return true;
		}

		if (args.length < 1) {
			sender.sendRichMessage("<red>You must provide a duration for streamer mode!");
			return false;
		} else if (args.length > 1) {
			return false;
		}

		Optional<Duration> parsedDuration = parseDuration(args[0]);

		if (parsedDuration.isEmpty()) {
			sender.sendRichMessage("<red>Invalid duration: \"<gray><input></gray>\"",
				Placeholder.unparsed("input", args[0]));
			return true;
		}

		Duration duration = parsedDuration.get();

		if (!manager.isAllowableDuration(duration, player)) {
			sender.sendRichMessage("<red>That duration is above the maximum allowed!");
			return true;
		}

		manager.enable(player, duration)
			.thenAccept(state -> {
				sender.sendRichMessage("<gold>Streamer mode will be enabled for <green><duration></green>.",
					Placeholder.unparsed("duration", formatDuration(state.duration())));
			});
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length != 1) return List.of();
		if (plugin.getLuckPerms().isEmpty()) return List.of();
		if (!(sender instanceof Player)) return List.of();

		String partialEntry = args[0];

		if (partialEntry.isEmpty()) {
			// Suggest durations if nothing is entered yet -- this is a good UX hint for how to use the command!
			return List.of("15m", "30m", "5h", "8h");
		}

		// if arg is int-parseable, suggest time suffixes m/h (for minutes/hours)
		try {
			Integer.parseUnsignedInt(partialEntry);
		} catch (NumberFormatException e) {
			return List.of();
		}

		List<String> supportedUnits = List.of("h", "m");

		return supportedUnits.stream()
			.map((unit) -> partialEntry + unit) // suggests typed number with units at end - i.e. 15 -> 15m, 15h
			.toList();
	}

	/// Rudimentary regex-based parser for durations.
	///
	/// ## Examples
	/// - `5h` -> 5 hours
	/// - `15m` -> 15 minutes
	///
	/// ## Note
	/// <strong>Only one duration segment is supported.</strong> That means durations such as
	/// '1h15m' will fail to parse.
	private Optional<Duration> parseDuration(String input) {
		Pattern durationPattern = Pattern.compile("^\\s*(?<num>[1-9]\\d{0,2})(?<unit>[mh])\\s*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = durationPattern.matcher(input);

		if (!matcher.matches())
			return Optional.empty();

		String inputNumber = matcher.group("num");
		String inputUnit = matcher.group("unit");

		// skipping try/catch on NumberFormatException here because this capture group can only
		// contain digits (\d)!
		int durationNumber = Integer.parseInt(inputNumber);
		TemporalUnit unit;

		switch (inputUnit.toLowerCase()) {
			case "h" -> unit = ChronoUnit.HOURS;
			case "m" -> unit = ChronoUnit.MINUTES;
			default -> { // unit is invalid!
				return Optional.empty();
			}
		}

		return Optional.of(Duration.of(durationNumber, unit));
	}

	private String formatDuration(Duration duration) {
		long days = duration.toDaysPart();
		int hours = duration.toHoursPart();
		int minutes = duration.toMinutesPart();

		List<String> resultList = new ArrayList<>();

		if (days > 0) resultList.add(days + " day" + (days == 1 ? "" : "s"));
		if (hours > 0) resultList.add(hours + " hour" + (hours == 1 ? "" : "s"));
		if (minutes > 0) resultList.add(minutes + " minute" + (minutes == 1 ? "" : "s"));

		return String.join(" ", resultList);
	}
}
