package org.modernbeta.admintoolbox.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UnavailableCommand implements CommandExecutor, TabCompleter {
	private final Component response;

	public static UnavailableCommand error(String message) {
		Component response = Component.text()
			.color(NamedTextColor.GRAY)
			.append(Component
				.text("Unavailable:", NamedTextColor.RED, TextDecoration.BOLD))
			.appendSpace()
			.append(Component.text(message))
			.build();

		return new UnavailableCommand(response);
	}

	public static UnavailableCommand custom(Component message) {
		return new UnavailableCommand(message);
	}

	private UnavailableCommand(Component response) {
		this.response = response;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		sender.sendMessage(response);
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		return List.of();
	}
}
