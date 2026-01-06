package org.modernbeta.admintoolbox;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ModrinthUpdateChecker {
	private static final int SEMVER_LENGTH = 3;

	private static final Gson GSON = new Gson();

	public static Optional<ModrinthVersion> getNewerVersion(String currentVersion, String projectId, String loader, String gameVersion) {
		int[] currentVersionParsed;
		try {
			currentVersionParsed = parseSemverParts(currentVersion);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}

		List<ModrinthVersion> compatibleVersions = getCompatibleVersions(projectId, loader, gameVersion);

		for (ModrinthVersion version : compatibleVersions) {
			int[] versionParsed = parseSemverParts(version.versionNumber);
			if (isGreaterVersion(versionParsed, currentVersionParsed)) {
				return Optional.of(version);
			}
		}

		return Optional.empty(); // no newer version found
	}

	private static @Nonnull List<ModrinthVersion> getCompatibleVersions(String projectId, String loader, String gameVersion) {
		try (HttpClient client = HttpClient.newHttpClient()) {
			// modrinth api has stupid non-standard query param expectations,
			// so we must wrap them in javascript arrays
			String queryString = Map.of(
					"loaders", "[\"" + loader + "\"]",
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

			List<ModrinthVersion> versions = new ArrayList<>();
			JsonArray rawVersionList = GSON.fromJson(rawBody, JsonArray.class);
			for (JsonElement element : rawVersionList) {
				if (!element.isJsonObject()) continue;

				JsonObject object = element.getAsJsonObject();
				String versionNumber = object.get("version_number").getAsString();
				String versionType = object.get("version_type").getAsString();
				String status = object.get("status").getAsString();
				Instant datePublished = Instant.parse(object.get("date_published").getAsString());

				List<ModrinthFile> files = new ArrayList<>();
				for (JsonElement rawFile : object.get("files").getAsJsonArray()) {
					if (!rawFile.isJsonObject()) continue;

					JsonObject fileObject = rawFile.getAsJsonObject();
					String url = fileObject.get("url").getAsString();
					boolean primary = fileObject.get("primary").getAsBoolean();

					files.add(new ModrinthFile(url, primary));
				}

				versions.add(new ModrinthVersion(versionNumber, versionType, status, datePublished, files));
			}

			return versions;
		} catch (IOException | InterruptedException e) {
			return List.of(); // request failed; fail check silently
		} catch (DateTimeParseException e) {
			// TODO: log date parse failure
			return List.of();
		} catch (URISyntaxException e) {
			// TODO: log uri build failure
			return List.of();
		}
	}

	public record ModrinthVersion(String versionNumber, String versionType, String status,
						   Instant datePublished, List<ModrinthFile> files) {
	}

	public record ModrinthFile(String url, boolean primary) {
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

	private static int[] parseSemverParts(String version) throws NumberFormatException {
		final String[] parts = version.split("\\.");
		if (parts.length != SEMVER_LENGTH)
			throw new NumberFormatException("Version is not valid untagged semver! (expected: x.x.x)");

		return new int[]{
			Integer.parseInt(parts[0]),
			Integer.parseInt(parts[1]),
			Integer.parseInt(parts[2]),
		};
	}
}
