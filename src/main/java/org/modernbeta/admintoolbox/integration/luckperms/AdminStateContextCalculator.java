package org.modernbeta.admintoolbox.integration.luckperms;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.managers.admin.AdminManager;

public class AdminStateContextCalculator implements ContextCalculator<Player> {
	private static final String CONTEXT_KEY = "admintoolbox:state";

	private final AdminToolboxPlugin plugin;

	public AdminStateContextCalculator(AdminToolboxPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void calculate(@NonNull Player player, @NonNull ContextConsumer consumer) {
		AdminManager adminManager = plugin.getAdminManager();

		String state;
		if (adminManager.isSpectating(player)) {
			state = "spectating";
		} else if (adminManager.isRevealed(player)) {
			state = "revealed";
		} else if (!adminManager.isActiveAdmin(player)) {
			state = "normal";
		} else {
			throw new RuntimeException("Trying to calculate LuckPerms admin state context while player is in an invalid state! Please report this bug.");
		}

		consumer.accept(CONTEXT_KEY, state);
	}

	@Override
	public @NotNull ContextSet estimatePotentialContexts() {
		return ImmutableContextSet.builder()
			.add(CONTEXT_KEY, "spectating")
			.add(CONTEXT_KEY, "revealed")
			.add(CONTEXT_KEY, "normal")
			.build();
	}
}
