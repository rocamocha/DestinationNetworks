package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.procedures.TeleporterRightClickedProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Validates picker selections on the server before performing a teleport. */
public final class TeleporterSelectionHandler {
	private TeleporterSelectionHandler() {
	}

	public static void handle(ServerPlayer player, TeleporterSelectionPayload payload) {
		if (player == null || payload == null || payload.data() == null)
			return;

		try {
			TeleporterColor sourceColor = TeleporterColor.byId(payload.data().getString("source_color"));
			TeleporterColor targetColor = TeleporterColor.byId(payload.data().getString("target_color"));
			if (sourceColor == null || targetColor == null) {
				player.displayClientMessage(Component.literal("Invalid teleporter selection"), true);
				return;
			}

			int sourceX = payload.data().getInt("source_x");
			int sourceY = payload.data().getInt("source_y");
			int sourceZ = payload.data().getInt("source_z");
			int targetX = payload.data().getInt("target_x");
			int targetY = payload.data().getInt("target_y");
			int targetZ = payload.data().getInt("target_z");
			ServerLevel level = (ServerLevel) player.level();
			WorldVariables variables = WorldVariables.get(level);

			boolean pruned = TeleporterIntegrity.pruneIfMissing(level, variables, sourceColor, sourceX, sourceY, sourceZ);
			pruned |= TeleporterIntegrity.pruneIfMissing(level, variables, targetColor, targetX, targetY, targetZ);
			if (pruned)
				variables.syncData(level);

			int source = variables.data().findSlot(sourceColor, sourceX, sourceY, sourceZ);
			int target = variables.data().findSlot(targetColor, targetX, targetY, targetZ);
			if (source < 0 || target < 0 || (sourceColor == targetColor && sourceX == targetX && sourceY == targetY && sourceZ == targetZ)) {
				player.displayClientMessage(Component.literal("That teleporter is no longer available"), true);
				return;
			}
			if (!variables.data().canUseBlock(sourceColor, source, player.getUUID())
					|| !variables.data().canUseBlock(targetColor, target, player.getUUID())) {
				player.displayClientMessage(Component.literal("You do not have permission to use that teleporter"), true);
				return;
			}

			TeleporterRightClickedProcedure.teleport(player.level(), player, sourceX, sourceY, sourceZ, targetX, targetY, targetZ);
			variables.data().recordRecentDestination(player.getUUID(), targetColor, targetX, targetY, targetZ);
			variables.syncData(player.level());
		} catch (IllegalArgumentException ignored) {
			player.displayClientMessage(Component.literal("Invalid teleporter selection"), true);
		}
	}
}
