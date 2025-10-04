package org.modernbeta.admintoolbox.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportsCommand implements CommandExecutor, TabCompleter {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();
	private static final String REPORTS_COMMAND_PERMISSION = "admintoolbox.reports";
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
							 @NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission(REPORTS_COMMAND_PERMISSION))
			return false;

		if (args.length >= 2 && args[0].equalsIgnoreCase("resolve")) {
			handleResolve(sender, args[1]);
			return true;
		}

		int page = 1;
		if (args.length >= 1) {
			try {
				page = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				sender.sendRichMessage("<red>Invalid page number.");
				return true;
			}
		}

		showReports(sender, page);
		return true;
	}

	private void showReports(CommandSender sender, int page) {
		List<Report> openReports = plugin.getReportManager().getOpenReports();

		if (openReports.isEmpty()) {
			sender.sendRichMessage("<green>No open reports.");
			return;
		}

		int reportsPerPage = 10;
		int totalPages = (int) Math.ceil((double) openReports.size() / reportsPerPage);
		page = Math.max(1, Math.min(page, totalPages));

		int startIndex = (page - 1) * reportsPerPage;
		int endIndex = Math.min(startIndex + reportsPerPage, openReports.size());

		sender.sendMessage(Component.text("═══ Open Reports (Page " + page + "/" + totalPages + ") ═══", NamedTextColor.GOLD));

		for (int i = startIndex; i < endIndex; i++) {
			Report report = openReports.get(i);
			Location loc = report.getLocation();
			String coords = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
			String timestamp = report.getTimestamp().format(TIME_FORMATTER);

			Component reportLine = MiniMessage.miniMessage().deserialize(
				"<gray>[<id>]</gray> <gold><player></gold> at <coords> in <world>",
				Placeholder.unparsed("id", String.valueOf(i + 1)),
				Placeholder.unparsed("player", report.getPlayerName()),
				Placeholder.unparsed("coords", coords),
				Placeholder.unparsed("world", loc.getWorld().getName())
			);

			Component hoverText = MiniMessage.miniMessage().deserialize(
				"<gold>Reason:</gold> <reason>\n<gold>Time:</gold> <timestamp>\n<green>Click to teleport\n<gray>Shift+Click to resolve",
				Placeholder.unparsed("reason", report.getReason()),
				Placeholder.unparsed("timestamp", timestamp)
			);

			String tpCommand = String.format("/tp %s %.1f %.1f %.1f",
				sender.getName(), loc.getX(), loc.getY(), loc.getZ());

			reportLine = reportLine
				.hoverEvent(HoverEvent.showText(hoverText))
				.clickEvent(ClickEvent.runCommand(tpCommand));

			sender.sendMessage(reportLine);

			Component resolveButton = Component.text("[Resolve]", NamedTextColor.GREEN)
				.clickEvent(ClickEvent.runCommand("/reports resolve " + report.getId()))
				.hoverEvent(HoverEvent.showText(Component.text("Click to resolve this report")));

			sender.sendMessage(Component.text("  ").append(resolveButton)
				.append(Component.text(" " + report.getReason(), NamedTextColor.GRAY)));
		}

		if (totalPages > 1) {
			Component nav = createNavigationComponent(page, totalPages);
			sender.sendMessage(nav);
		}
	}

	private static @NotNull Component createNavigationComponent(int page, int totalPages) {
		Component nav = Component.text("");
		if (page > 1) {
			nav = nav.append(Component.text("[Previous]", NamedTextColor.YELLOW)
				.clickEvent(ClickEvent.runCommand("/reports " + (page - 1)))
				.hoverEvent(HoverEvent.showText(Component.text("Go to page " + (page - 1)))));
		}
		if (page < totalPages) {
			if (page > 1) nav = nav.append(Component.text(" "));
			nav = nav.append(Component.text("[Next]", NamedTextColor.YELLOW)
				.clickEvent(ClickEvent.runCommand("/reports " + (page + 1)))
				.hoverEvent(HoverEvent.showText(Component.text("Go to page " + (page + 1)))));
		}
		return nav;
	}

	private void handleResolve(CommandSender sender, String reportIdStr) {
		UUID reportId;
		try {
			reportId = UUID.fromString(reportIdStr);
		} catch (IllegalArgumentException e) {
			sender.sendRichMessage("<red>Invalid report ID.");
			return;
		}

		Optional<Report> reportOpt = plugin.getReportManager().getReport(reportId);
		if (reportOpt.isEmpty()) {
			sender.sendRichMessage("<red>Report not found.");
			return;
		}

		Report report = reportOpt.get();
		if (report.isResolved()) {
			sender.sendRichMessage("<red>Report is already resolved.");
			return;
		}

		UUID resolvedBy = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
		plugin.getReportManager().resolveReport(reportId, resolvedBy);

		sender.sendRichMessage("<green>Report resolved.");

		plugin.getAdminAudience()
			.excluding(sender)
			.sendMessage(MiniMessage.miniMessage().deserialize(
				"<gray><admin> resolved a report from <player>.",
				Placeholder.component("admin", sender.name()),
				Placeholder.unparsed("player", report.getPlayerName())
			));
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
												@NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {
			return Stream.of("resolve")
				.filter(s -> s.startsWith(args[0].toLowerCase()))
				.collect(Collectors.toList());
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("resolve")) {
			return plugin.getReportManager().getOpenReports().stream()
				.map(report -> report.getId().toString())
				.filter(id -> id.startsWith(args[1].toLowerCase()))
				.collect(Collectors.toList());
		}

		return List.of();
	}
}
