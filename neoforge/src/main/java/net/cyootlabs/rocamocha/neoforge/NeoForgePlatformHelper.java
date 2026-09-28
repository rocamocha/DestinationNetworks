package net.cyootlabs.rocamocha.neoforge;

import net.cyootlabs.rocamocha.LinkingMode;
import net.cyootlabs.rocamocha.network.TeleporterSyncPayload;
import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.cyootlabs.rocamocha.platform.IPlatformHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePlatformHelper implements IPlatformHelper {
	@Override
	public int getMaxTeleportersPerColor() {
		return NeoForgeConfig.MAX_TELEPORTERS_PER_COLOR.get();
	}

	@Override
	public boolean isNewBlocksGlobalByDefault() {
		return NeoForgeConfig.NEW_BLOCKS_GLOBAL_BY_DEFAULT.get();
	}

	@Override
	public LinkingMode getLinkingMode() {
		return NeoForgeConfig.LINKING_MODE.get();
	}

	@Override
	public int getInvitePermissionLevel() {
		return NeoForgeConfig.INVITE_PERMISSION_LEVEL.get();
	}

	@Override
	public void sendToDimension(ServerLevel level, TeleporterSyncPayload payload) {
		PacketDistributor.sendToPlayersInDimension(level, payload);
	}

	@Override
	public void sendToPlayer(ServerPlayer player, TeleporterSyncPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	@Override
	public void sendPickerToPlayer(ServerPlayer player, TeleporterPickerPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}
}
