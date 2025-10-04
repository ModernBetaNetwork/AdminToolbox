package org.modernbeta.admintoolbox.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.models.Report;
import org.modernbeta.admintoolbox.utils.LocationUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportCommand implements CommandExecutor, TabCompleter {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final long COOLDOWN_MINUTES = 5;
	private final Map<UUID, LocalDateTime> cooldowns = new HashMap<>();

	private static final List<String> REPORT_SUGGESTIONS = List.of(
		"griefing",
		"stealing",
		"bug"
	);

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendRichMessage("<red>You must be a player to use this command.");
			return true;
		}

		if (args.length == 0) {
			player.sendRichMessage("<red>Please provide a reason for your report. Usage: /report <reason>");
			return true;
		}

		LocalDateTime lastReport = cooldowns.get(player.getUniqueId());
		if (lastReport != null) {
			Duration timeSince = Duration.between(lastReport, LocalDateTime.now());
			if (timeSince.toMinutes() < COOLDOWN_MINUTES) {
				long remainingSeconds = COOLDOWN_MINUTES * 60 - timeSince.getSeconds();
				long minutes = remainingSeconds / 60;
				long seconds = remainingSeconds % 60;
				player.sendRichMessage("<red>You must wait <time> before sending another report.",
					Placeholder.unparsed("time", String.format("%d:%02d", minutes, seconds)));
				return true;
			}
		}

		Location loc = player.getLocation();
		String reason = String.join(" ", args);

		Report report = plugin.getReportManager().createReport(
			player.getUniqueId(),
			player.getName(),
			loc,
			reason
		);

		String coords = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
		String timestamp = report.getTimestamp().format(TIME_FORMATTER);

		String worldNameForCommand = LocationUtils.getShortWorldName(loc.getWorld());
		String targetCommand = String.format("/target %d %d %d %s",
				loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), worldNameForCommand);

		Component coordsComponent = Component.text(coords)
			.clickEvent(ClickEvent.runCommand(targetCommand))
			.hoverEvent(HoverEvent.showText(Component.text("Click to spectate here")));

		plugin.getAdminAudience()
			.sendMessage(MiniMessage.miniMessage().deserialize(
				"<gold>[Report] <player> at <gray><coords></gray> in <world> (<timestamp>): <reason>",
				Placeholder.component("player", player.name()),
				Placeholder.component("coords", coordsComponent),
				Placeholder.unparsed("world", loc.getWorld() != null ? loc.getWorld().getName() : worldNameForCommand),
				Placeholder.unparsed("timestamp", timestamp),
				Placeholder.unparsed("reason", reason)
			));

		player.sendRichMessage("<green>Your report has been submitted. Thank you!");
		cooldowns.put(player.getUniqueId(), LocalDateTime.now());

		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {
			return REPORT_SUGGESTIONS;
		}

		String currentArg = args[args.length - 1].toLowerCase();

		List<String> filtered = REPORT_SUGGESTIONS.stream()
			.filter(suggestion -> suggestion.toLowerCase().startsWith(currentArg))
			.collect(Collectors.toList());

		if (args.length == 1) {
			return filtered;
		}

		return filtered.isEmpty() ? List.of() : filtered;
	}
}
