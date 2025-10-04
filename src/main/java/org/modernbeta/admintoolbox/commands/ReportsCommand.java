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
import org.modernbeta.admintoolbox.utils.LocationUtils;

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
    private static final int REPORTS_PER_PAGE = 5;
    private static final int PAGE_BASE = 1;

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
							 @NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission(REPORTS_COMMAND_PERMISSION))
			return false;

		if (args.length >= 2 && args[0].equalsIgnoreCase("resolve")) {
			handleResolve(sender, args[1]);
			return true;
		}

        int page = PAGE_BASE;
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

        int totalPages = (int) Math.ceil((double) openReports.size() / REPORTS_PER_PAGE);
        page = Math.max(PAGE_BASE, Math.min(page, totalPages));

        int startIndex = (page - PAGE_BASE) * REPORTS_PER_PAGE;
        int endIndex = Math.min(startIndex + REPORTS_PER_PAGE, openReports.size());

		sender.sendMessage(Component.text("═══ Open Reports (Page " + page + "/" + totalPages + ") ═══", NamedTextColor.GOLD));

		for (int i = startIndex; i < endIndex; i++) {
			Report report = openReports.get(i);
			Location loc = report.getLocation();
			String coords = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
			String timestamp = report.getTimestamp().format(TIME_FORMATTER);

			Component reportLine;

			Component hoverText = MiniMessage.miniMessage().deserialize(
				"<gold>Reason:</gold> <reason>\n<gold>Time:</gold> <timestamp>\n<green>Click to spectate\n<dark_gray>ID: <id>",
				Placeholder.unparsed("reason", report.getReason()),
				Placeholder.unparsed("timestamp", timestamp),
				Placeholder.unparsed("id", report.getId().toString())
			);

			int bx = loc.getBlockX();
			int by = loc.getBlockY();
			int bz = loc.getBlockZ();
			String worldNameForCommand = LocationUtils.getShortWorldName(loc.getWorld());
			String targetCommand = String.format("/target %d %d %d %s", bx, by, bz, worldNameForCommand);

			Component coordsComponent = Component.text(coords)
				.clickEvent(ClickEvent.runCommand(targetCommand))
				.hoverEvent(HoverEvent.showText(hoverText));

			Component inlineResolve = Component.text("[Resolve]", NamedTextColor.GREEN)
				.clickEvent(ClickEvent.runCommand("/reports resolve " + report.getId()))
				.hoverEvent(HoverEvent.showText(Component.text("Click to resolve this report")));

			reportLine = MiniMessage.miniMessage().deserialize(
				"<gray>[<id>]</gray> <gold><player></gold> at <coords> in <world> <resolve>",
                Placeholder.unparsed("id", String.valueOf(i + PAGE_BASE)),
				Placeholder.unparsed("player", report.getPlayerName()),
				Placeholder.component("coords", coordsComponent),
				Placeholder.component("resolve", inlineResolve),
				Placeholder.unparsed("world", loc.getWorld() != null ? loc.getWorld().getName() : worldNameForCommand)
			);

			reportLine = reportLine.hoverEvent(HoverEvent.showText(hoverText));

			sender.sendMessage(reportLine);

			sender.sendMessage(Component.text("  " + report.getReason(), NamedTextColor.GRAY));
		}

        if (totalPages > PAGE_BASE) {
            Component nav = createNavigationComponent(page, totalPages);
            sender.sendMessage(nav);
        }
    }

	private static @NotNull Component createNavigationComponent(int page, int totalPages) {
		Component nav = Component.text("");
        if (page > PAGE_BASE) {
            nav = nav.append(Component.text("[Previous]", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/reports " + (page - PAGE_BASE)))
                .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (page - PAGE_BASE)))));
        }
        if (page < totalPages) {
            if (page > PAGE_BASE) nav = nav.append(Component.text(" "));
            nav = nav.append(Component.text("[Next]", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/reports " + (page + PAGE_BASE)))
                .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (page + PAGE_BASE)))));
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
