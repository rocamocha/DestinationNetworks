package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.TeleporterData;
import net.cyootlabs.rocamocha.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/** Builds the server-authoritative destination list sent when a teleporter is used. */
public final class TeleporterPickerService {
	private TeleporterPickerService() {
	}

	public static void open(ServerPlayer player, TeleporterColor sourceColor, int sourceX, int sourceY, int sourceZ) {
		ServerLevel level = (ServerLevel) player.level();
		WorldVariables variables = WorldVariables.get(level);
		int source = variables.data().findSlot(sourceColor, sourceX, sourceY, sourceZ);
		if (source < 0 || !variables.data().canUseBlock(sourceColor, source, player.getUUID()))
			return;

		boolean pruned = false;
		int sourceDuplicate = variables.data().findDuplicateSlot(sourceColor, source);
		if (sourceDuplicate >= 0) {
			variables.data().clearSlot(sourceColor, sourceDuplicate);
			pruned = true;
		}

		CompoundTag data = new CompoundTag();
		data.putString("source_color", sourceColor.getId());
		data.putInt("source_x", sourceX);
		data.putInt("source_y", sourceY);
		data.putInt("source_z", sourceZ);
		data.putString("source_name", variables.data().getName(sourceColor, source));
		data.putString("source_owner", variables.data().getOwner(sourceColor, source) == null ? "" : variables.data().getOwner(sourceColor, source).toString());
		data.putBoolean("source_can_rename", variables.data().isOwner(sourceColor, source, player.getUUID()) || player.hasPermissions(2));
		ListTag destinations = new ListTag();
		for (TeleporterColor color : TeleporterColor.values()) {
			Set<String> seenPositions = new HashSet<>();
			for (TeleporterData.Link link : variables.data().accessibleLinks(player.getUUID(), color)) {
				int linkX = link.pos[0];
				int linkY = link.pos[1];
				int linkZ = link.pos[2];
				if (color == sourceColor && linkX == sourceX && linkY == sourceY && linkZ == sourceZ)
					continue;
				if (TeleporterIntegrity.pruneIfMissing(level, variables, color, linkX, linkY, linkZ)) {
					pruned = true;
					continue;
				}
				// Legacy saves can leave two slots pointing at the same block; keep only the first.
				if (!seenPositions.add(linkX + "," + linkY + "," + linkZ)) {
					int duplicate = variables.data().indexOf(color, link);
					if (duplicate >= 0) {
						variables.data().clearSlot(color, duplicate);
						pruned = true;
					}
					continue;
				}
				CompoundTag entry = new CompoundTag();
				entry.putString("color", color.getId());
				entry.putInt("x", linkX);
				entry.putInt("y", linkY);
				entry.putInt("z", linkZ);
				entry.putString("name", link.name);
				entry.putString("owner", ownerName(player.getServer(), link.owner, link.ownerName));
				destinations.add(entry);
			}
		}
		data.put("destinations", destinations);
		ListTag recent = new ListTag();
		for (TeleporterData.RecentDestination entry : variables.data().recentDestinations(player.getUUID())) {
			if (TeleporterIntegrity.pruneIfMissing(level, variables, entry.color, entry.x(), entry.y(), entry.z())) {
				pruned = true;
				continue;
			}
			int index = variables.data().findSlot(entry.color, entry.x(), entry.y(), entry.z());
			if (index < 0 || !variables.data().canUseBlock(entry.color, index, player.getUUID()))
				continue;
			CompoundTag value = new CompoundTag();
			value.putString("color", entry.color.getId());
			value.putInt("x", entry.x());
			value.putInt("y", entry.y());
			value.putInt("z", entry.z());
			value.putString("name", variables.data().getName(entry.color, index));
			value.putString("owner", ownerName(player.getServer(), variables.data().getOwner(entry.color, index), variables.data().getOwnerName(entry.color, index)));
			recent.add(value);
		}
		data.put("recent", recent);
		if (pruned)
			variables.syncData(level);
		Services.get().sendPickerToPlayer(player, new TeleporterPickerPayload(data));
	}

	private static String ownerName(MinecraftServer server, java.util.UUID owner, String fallback) {
		if (owner == null)
			return "Unknown owner";
		ServerPlayer online = server == null ? null : server.getPlayerList().getPlayer(owner);
		if (online != null)
			return online.getGameProfile().getName();
		if (fallback != null && !fallback.isBlank())
			return fallback;
		return owner.toString();
	}
}