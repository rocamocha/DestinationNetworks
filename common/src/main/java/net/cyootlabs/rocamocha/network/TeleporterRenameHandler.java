package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.TeleporterColor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Validates rename requests on the server. */
public final class TeleporterRenameHandler {
	private TeleporterRenameHandler() {
	}

	public static void handle(ServerPlayer player, TeleporterRenamePayload payload) {
		try {
			TeleporterColor color = TeleporterColor.byId(payload.data().getString("color"));
			String name = payload.data().getString("name").trim();
			int x = payload.data().getInt("x");
			int y = payload.data().getInt("y");
			int z = payload.data().getInt("z");
			WorldVariables variables = WorldVariables.get(player.level());
			int index = color == null ? -1 : variables.data().findSlot(color, x, y, z);
			if (index < 0) {
				player.displayClientMessage(Component.literal("That teleporter no longer exists"), true);
				return;
			}
			if (!variables.data().isOwner(color, index, player.getUUID()) && !player.hasPermissions(2)) {
				player.displayClientMessage(Component.literal("Only the owner or an operator can rename this teleporter"), true);
				return;
			}
			if (name.length() > 32) {
				player.displayClientMessage(Component.literal("Teleporter names must be 32 characters or fewer"), true);
				return;
			}
			variables.data().setName(color, index, name);
			variables.syncData(player.level());
		} catch (RuntimeException ignored) {
			player.displayClientMessage(Component.literal("Invalid teleporter rename"), true);
		}
	}
}
