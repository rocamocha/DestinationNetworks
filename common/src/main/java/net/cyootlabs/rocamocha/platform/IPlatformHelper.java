package net.cyootlabs.rocamocha.platform;

import net.cyootlabs.rocamocha.LinkingMode;
import net.cyootlabs.rocamocha.network.TeleporterSyncPayload;
import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface IPlatformHelper {
	int getMaxTeleportersPerColor();

	boolean isNewBlocksGlobalByDefault();

	LinkingMode getLinkingMode();

	int getInvitePermissionLevel();

	/**
	 * Sends a sync payload to every player currently in the given dimension.
	 */
	void sendToDimension(ServerLevel level, TeleporterSyncPayload payload);

	/**
	 * Sends a sync payload to a single player.
	 */
	void sendToPlayer(ServerPlayer player, TeleporterSyncPayload payload);

	void sendPickerToPlayer(ServerPlayer player, TeleporterPickerPayload payload);
}
