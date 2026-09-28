package net.cyootlabs.rocamocha.fabric;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;
import net.cyootlabs.rocamocha.Constants;
import net.cyootlabs.rocamocha.LinkingMode;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public class FabricConfig {
	public static final int MAX_CONFIGURABLE_TELEPORTERS = 1024;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static LinkingMode linkingMode = LinkingMode.OWNER;
	private static boolean newBlocksGlobalByDefault = true;
	private static int maxTeleportersPerColor = 10;
	private static int invitePermissionLevel = 0;

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(Constants.MOD_ID + ".json");
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				JsonObject json = GsonHelper.parse(reader);
				if (json.has("linkingMode")) {
					linkingMode = LinkingMode.byName(json.get("linkingMode").getAsString());
				}
				if (json.has("newBlocksGlobalByDefault")) {
					newBlocksGlobalByDefault = json.get("newBlocksGlobalByDefault").getAsBoolean();
				}
				if (json.has("maxTeleportersPerColor")) {
					maxTeleportersPerColor = json.get("maxTeleportersPerColor").getAsInt();
				}
				if (json.has("invitePermissionLevel")) {
					invitePermissionLevel = json.get("invitePermissionLevel").getAsInt();
				}
			} catch (IOException | RuntimeException e) {
				Constants.LOG.warn("Failed to read {}, using defaults", path, e);
			}
		}
		maxTeleportersPerColor = Mth.clamp(maxTeleportersPerColor, 1, MAX_CONFIGURABLE_TELEPORTERS);
		invitePermissionLevel = Mth.clamp(invitePermissionLevel, 0, 4);
		save(path);
	}

	private static void save(Path path) {
		JsonObject json = new JsonObject();
		json.addProperty("linkingMode", linkingMode.name());
		json.addProperty("newBlocksGlobalByDefault", newBlocksGlobalByDefault);
		json.addProperty("maxTeleportersPerColor", maxTeleportersPerColor);
		json.addProperty("invitePermissionLevel", invitePermissionLevel);
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(json, writer);
			}
		} catch (IOException e) {
			Constants.LOG.warn("Failed to write {}", path, e);
		}
	}

	public static LinkingMode getLinkingMode() {
		return linkingMode;
	}

	public static boolean isNewBlocksGlobalByDefault() {
		return newBlocksGlobalByDefault;
	}

	public static int getMaxTeleportersPerColor() {
		return maxTeleportersPerColor;
	}

	public static int getInvitePermissionLevel() {
		return invitePermissionLevel;
	}
}
