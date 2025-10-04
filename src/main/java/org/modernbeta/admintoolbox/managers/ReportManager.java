package org.modernbeta.admintoolbox.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.modernbeta.admintoolbox.AdminToolboxPlugin;
import org.modernbeta.admintoolbox.models.Report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportManager {
	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();
	private final Map<UUID, Report> reports = new HashMap<>();
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public ReportManager() {
		loadReports();
	}

	public Report createReport(UUID playerUUID, String playerName, Location location, String reason) {
		Report report = new Report(playerUUID, playerName, location, reason);
		reports.put(report.getId(), report);
		saveReports();
		return report;
	}

	public List<Report> getOpenReports() {
		return reports.values().stream()
			.filter(report -> !report.isResolved())
			.sorted(Comparator.comparing(Report::getTimestamp))
			.collect(Collectors.toList());
	}

	public List<Report> getAllReports() {
		return new ArrayList<>(reports.values());
	}

	public Optional<Report> getReport(UUID id) {
		return Optional.ofNullable(reports.get(id));
	}

	public void resolveReport(UUID reportId, UUID resolvedBy) {
		Report report = reports.get(reportId);
		if (report != null) {
			report.resolve(resolvedBy);
			saveReports();
		}
	}

	private void loadReports() {
		FileConfiguration config = plugin.getReportsConfig();
		ConfigurationSection reportsSection = config.getConfigurationSection("reports");

		if (reportsSection == null) {
			return;
		}

		for (String key : reportsSection.getKeys(false)) {
			ConfigurationSection reportSection = reportsSection.getConfigurationSection(key);
			if (reportSection == null) continue;

			try {
				UUID id = UUID.fromString(key);
				UUID playerUUID = UUID.fromString(Objects.requireNonNull(reportSection.getString("playerUUID")));
				String playerName = reportSection.getString("playerName");
				Location location = deserializeLocation(reportSection.getConfigurationSection("location"));
				LocalDateTime timestamp = LocalDateTime.parse(Objects.requireNonNull(reportSection.getString("timestamp")), FORMATTER);
				String reason = reportSection.getString("reason");
				boolean resolved = reportSection.getBoolean("resolved", false);
				UUID resolvedBy = reportSection.getString("resolvedBy") != null
					? UUID.fromString(Objects.requireNonNull(reportSection.getString("resolvedBy")))
					: null;
				LocalDateTime resolvedAt = reportSection.getString("resolvedAt") != null
					? LocalDateTime.parse(Objects.requireNonNull(reportSection.getString("resolvedAt")), FORMATTER)
					: null;

				Report report = new Report(id, playerUUID, playerName, location, timestamp, reason, resolved, resolvedBy, resolvedAt);
				reports.put(id, report);
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to load report: " + key);
			}
		}
	}

	private void saveReports() {
		FileConfiguration config = plugin.getReportsConfig();
		config.set("reports", null);

		ConfigurationSection reportsSection = config.createSection("reports");

		for (Report report : reports.values()) {
			ConfigurationSection reportSection = reportsSection.createSection(report.getId().toString());
			reportSection.set("playerUUID", report.getPlayerUUID().toString());
			reportSection.set("playerName", report.getPlayerName());
			reportSection.set("location", serializeLocation(report.getLocation()));
			reportSection.set("timestamp", report.getTimestamp().format(FORMATTER));
			reportSection.set("reason", report.getReason());
			reportSection.set("resolved", report.isResolved());
			if (report.getResolvedBy() != null) {
				reportSection.set("resolvedBy", report.getResolvedBy().toString());
			}
			if (report.getResolvedAt() != null) {
				reportSection.set("resolvedAt", report.getResolvedAt().format(FORMATTER));
			}
		}

		plugin.saveReportsConfig();
	}

    private Map<String, Object> serializeLocation(Location loc) {
        Map<String, Object> map = new HashMap<>();
        String worldName = (loc.getWorld() != null)
            ? loc.getWorld().getName()
            : Bukkit.getWorlds().getFirst().getName();
        map.put("world", worldName);
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        return map;
    }

	private Location deserializeLocation(ConfigurationSection section) {
		if (section == null) return null;

		String worldName = section.getString("world");
		double x = section.getDouble("x");
		double y = section.getDouble("y");
		double z = section.getDouble("z");

		assert worldName != null;
		return new Location(Bukkit.getWorld(worldName), x, y, z);
	}
}
