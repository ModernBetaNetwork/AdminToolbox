package org.modernbeta.admintoolbox;

import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ModrinthUpdateChecker {
	private static final int SEMVER_LENGTH = 3;

	private final AdminToolboxPlugin plugin = AdminToolboxPlugin.getInstance();
	private final Gson gson = new Gson();

	public TextComponent getUpdateMessage(String projectId) {
		String pluginName = plugin.getPluginMeta().getName();
		String currentVersion = plugin.getPluginMeta().getVersion();
		String loader = Bukkit.getName();
		String gameVersion = Bukkit.getServer().getMinecraftVersion();

		Optional<ModrinthVersion> newestCompatibleVersion =
			getNewestCompatibleVersion(projectId, currentVersion, loader, gameVersion);

		return newestCompatibleVersion.map((version) -> {
			TextComponent.Builder builder = Component.text()
				.color(NamedTextColor.GOLD)
				.appendNewline()
				.append(Component.text("Version " + version.versionNumber()
					+ " of " + pluginName + " is now available!"
				).decorate(TextDecoration.BOLD))
				.appendNewline()
				.append(Component.text("You are running version " + currentVersion + "."));

			if (version.downloadUrl() != null)
				builder
					.appendNewline()
					.append(Component.text("Download it here: " + version.downloadUrl()));

			return builder.appendNewline().build();
		}).orElseGet(() -> Component.text("You're running the latest release of " + pluginName + "."));
	}

	public Optional<ModrinthVersion> getNewestCompatibleVersion(String projectId, String currentVersion, String loader, String gameVersion) {
		int[] currentVersionParsed;
		try {
			currentVersionParsed = parseSemverParts(currentVersion);
		} catch (NumberFormatException e) {
			plugin.getLogger().warning("Could not parse current version: " + currentVersion);
			return Optional.empty();
		}

		plugin.getLogger()
			.info("Checking for updates compatible with " + loader + " " + gameVersion + "...");

		try (HttpClient client = HttpClient.newHttpClient()) {
			// modrinth api has stupid non-standard query param expectations,
			// so we must wrap them in javascript arrays
			String queryString = Map.of(
					"loaders", "[\"" + loader.toLowerCase() + "\"]",
					"game_versions", "[\"" + gameVersion + "\"]"
				)
				.entrySet().stream()
				.map(entry ->
					URLEncoder.encode(entry.getKey(), UTF_8) + "="
						+ URLEncoder.encode(entry.getValue(), UTF_8))
				.collect(Collectors.joining("&"));

			URI requestUri = new URI(
				"https",
				"api.modrinth.com",
				"/v2/project/" + URLEncoder.encode(projectId, StandardCharsets.UTF_8) + "/version",
				queryString,
				null
			);

			HttpRequest req = HttpRequest.newBuilder()
				.uri(requestUri)
				.GET()
				.build();

			HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
			String rawBody = res.body();

			JsonArray rawVersionList = gson.fromJson(rawBody, JsonArray.class);
			for (JsonElement element : rawVersionList) {
				if (!element.isJsonObject()) continue;

				JsonObject object = element.getAsJsonObject();

				String versionType = object.get("version_type").getAsString();
				if (!versionType.equals("release")) continue;

				String status = object.get("status").getAsString();
				if (!status.equals("listed")) continue;

				String versionNumber = object.get("version_number").getAsString();
				Instant datePublished = Instant.parse(object.get("date_published").getAsString());

				if (isGreaterVersion(parseSemverParts(versionNumber), currentVersionParsed)) {
					String downloadUrl = null;
					for (JsonElement rawFile : object.get("files").getAsJsonArray()) {
						if (!rawFile.isJsonObject()) continue;

						JsonObject fileObject = rawFile.getAsJsonObject();
						boolean primary = fileObject.get("primary").getAsBoolean();
						if (!primary) continue;

						downloadUrl = fileObject.get("url").getAsString();
						break;
					}

					return Optional.of(
						new ModrinthVersion(versionNumber, datePublished, downloadUrl));
				}
				;
			}
		} catch (IOException | InterruptedException e) {
			plugin.getLogger().severe("Failed request plugin versions from Modrinth!");
			plugin.getLogger().severe(e.toString());
			return Optional.empty();
		} catch (JsonParseException e) {
			plugin.getLogger().severe("Failed to parse plugin versions response from Modrinth!");
			plugin.getLogger().severe(e.toString());
			return Optional.empty();
		} catch (DateTimeParseException e) {
			plugin.getLogger().severe("Failed to parse version published_date from Modrinth!");
			plugin.getLogger().severe(e.toString());
			return Optional.empty();
		} catch (URISyntaxException e) {
			plugin.getLogger().severe("Failed to create URL to check updates from Modrinth!");
			plugin.getLogger().severe(e.toString());
			return Optional.empty();
		}

		return Optional.empty(); // no newer version found
	}

	public record ModrinthVersion(String versionNumber, Instant datePublished,
								  @Nullable String downloadUrl) {
	}

	private static boolean isGreaterVersion(int[] a, int[] b) throws IllegalArgumentException {
		if (a.length != SEMVER_LENGTH || b.length != SEMVER_LENGTH)
			throw new IllegalArgumentException("Compared semver version has incorrect size!");

		for (int i = 0; i < SEMVER_LENGTH; i++) {
			if (a[i] > b[i]) return true;
			if (a[i] < b[i]) return false;
		}
		return false; // versions are equal
	}

	private static final Pattern SEMVER_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");

	private static int[] parseSemverParts(String version) throws NumberFormatException {
		Matcher matcher = SEMVER_PATTERN.matcher(version);
		if (!matcher.find())
			throw new NumberFormatException("Version is not valid semantic version! (expected: x.x.x)");

		return new int[]{
			Integer.parseInt(matcher.group(1)),
			Integer.parseInt(matcher.group(2)),
			Integer.parseInt(matcher.group(3)),
		};
	}
}
