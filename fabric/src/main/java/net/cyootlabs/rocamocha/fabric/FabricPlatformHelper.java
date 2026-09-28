package net.cyootlabs.rocamocha.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.cyootlabs.rocamocha.LinkingMode;
import net.cyootlabs.rocamocha.network.TeleporterSyncPayload;
import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.cyootlabs.rocamocha.platform.IPlatformHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlatformHelper implements IPlatformHelper {
	@Override
	public int getMaxTeleportersPerColor() {
		return FabricConfig.getMaxTeleportersPerColor();
	}

	@Override
	public boolean isNewBlocksGlobalByDefault() {
		return FabricConfig.isNewBlocksGlobalByDefault();
	}

	@Override
	public LinkingMode getLinkingMode() {
		return FabricConfig.getLinkingMode();
	}

	@Override
	public int getInvitePermissionLevel() {
		return FabricConfig.getInvitePermissionLevel();
	}

	@Override
	public void sendToDimension(ServerLevel level, TeleporterSyncPayload payload) {
		for (ServerPlayer player : level.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	@Override
	public void sendToPlayer(ServerPlayer player, TeleporterSyncPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	@Override
	public void sendPickerToPlayer(ServerPlayer player, TeleporterPickerPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
