package org.modernbeta.admintoolbox.models;

import org.bukkit.Location;

import java.time.LocalDateTime;
import java.util.UUID;

public class Report {
	private final UUID id;
	private final UUID playerUUID;
	private final String playerName;
	private final Location location;
	private final LocalDateTime timestamp;
	private final String reason;
	private boolean resolved;
	private UUID resolvedBy;
	private LocalDateTime resolvedAt;

	public Report(UUID playerUUID, String playerName, Location location, String reason) {
		this.id = UUID.randomUUID();
		this.playerUUID = playerUUID;
		this.playerName = playerName;
		this.location = location;
		this.timestamp = LocalDateTime.now();
		this.reason = reason;
		this.resolved = false;
	}

	public Report(UUID id, UUID playerUUID, String playerName, Location location, LocalDateTime timestamp, String reason, boolean resolved, UUID resolvedBy, LocalDateTime resolvedAt) {
		this.id = id;
		this.playerUUID = playerUUID;
		this.playerName = playerName;
		this.location = location;
		this.timestamp = timestamp;
		this.reason = reason;
		this.resolved = resolved;
		this.resolvedBy = resolvedBy;
		this.resolvedAt = resolvedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getPlayerUUID() {
		return playerUUID;
	}

	public String getPlayerName() {
		return playerName;
	}

	public Location getLocation() {
		return location;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public String getReason() {
		return reason;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void resolve(UUID resolvedBy) {
		this.resolved = true;
		this.resolvedBy = resolvedBy;
		this.resolvedAt = LocalDateTime.now();
	}

	public UUID getResolvedBy() {
		return resolvedBy;
	}

	public LocalDateTime getResolvedAt() {
		return resolvedAt;
	}
}
