package org.modernbeta.admintoolbox.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.managers.admin.AdminManager;
import org.modernbeta.admintoolbox.managers.admin.AdminState;

import java.util.List;

public class FullbrightCommand implements CommandExecutor, TabCompleter {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();

	private static final String FULLBRIGHT_COMMAND_PERMISSION = "admintoolbox.fullbright";

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission(FULLBRIGHT_COMMAND_PERMISSION)) return false;
		if (args.length > 1) return false;

		if (!(sender instanceof Player player)) {
			sender.sendRichMessage("<red>You must be a player to run this command!");
			return true;
		}

		AdminManager adminManager = plugin.getAdminManager();

		if (!adminManager.isActiveAdmin(player)) {
			sender.sendRichMessage("<red>You are not in admin mode!");
			return true;
		}

		AdminState adminState = adminManager.getAdminState(player).orElseThrow();

		switch (args.length) {
			case 0 -> {
				adminState.setFullbrightEnabled(!adminState.isFullbrightEnabled());
			}
			case 1 -> {
				String value = args[0];
				boolean shouldEnable;
				if (value.equalsIgnoreCase("on")) shouldEnable = true;
				else if (value.equalsIgnoreCase("off")) shouldEnable = false;
				else throw new IllegalArgumentException("Invalid argument! Expected [on|off]");
			}
		}

		boolean isEnabled = adminState.isFullbrightEnabled();

		TextComponent.Builder feedback = Component.text()
			.color(NamedTextColor.GOLD);
		if (isEnabled) {
			feedback
				.append(Component.text("Fullbright has been "))
				.append(Component.text("enabled", NamedTextColor.GREEN))
				.append(Component.text("."));
		} else {
			feedback
				.append(Component.text("Fullbright has been "))
				.append(Component.text("disabled", NamedTextColor.RED))
				.append(Component.text("."));
		}
		player.sendMessage(feedback.build());

		return true;
	}

	private static final List<String> TOGGLE_OPTIONS = List.of("on", "off");

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		return switch (args.length) {
			case 1 -> TOGGLE_OPTIONS.stream()
				.filter((opt) -> opt.startsWith(args[0].toLowerCase()))
				.toList();
			default -> List.of();
		};
	}
}
