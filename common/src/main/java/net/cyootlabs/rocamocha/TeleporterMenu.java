package net.cyootlabs.rocamocha;

import java.util.Set;
import java.util.UUID;

import net.cyootlabs.rocamocha.network.WorldVariables;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds the chat messages (with clickable links) used by the crouch+right-click menu and the
 * use-in-air menu. The raw command text is never shown to the player; only labelled buttons are.
 */
public final class TeleporterMenu {
	private TeleporterMenu() {
	}

	private static MutableComponent button(String label, ChatFormatting color, String command) {
		return Component.literal(label).withStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
	}

	private static String coordCommand(String action, TeleporterColor color, int[] pos) {
		return "/" + Constants.MOD_ID + " " + action + " " + color.getId() + " " + pos[0] + " " + pos[1] + " " + pos[2];
	}

	private static String ownerName(MinecraftServer server, UUID owner) {
		if (owner == null)
			return "nobody (public)";
		ServerPlayer online = server.getPlayerList().getPlayer(owner);
		return online != null ? online.getGameProfile().getName() : owner.toString();
	}

	/** Crouch+right-click on a placed teleporter. */
	public static void sendBlockMenu(ServerPlayer player, TeleporterColor color, int x, int y, int z) {
		WorldVariables variables = WorldVariables.get(player.level());
		int index = variables.data().findSlot(color, x, y, z);
		if (index < 0)
			return;

		MinecraftServer server = player.getServer();
		int[] pos = variables.data().getSlot(color, index);
		UUID owner = variables.data().getOwner(color, index);
		boolean global = variables.data().isGlobal(color, index);
		boolean isOwner = owner != null && owner.equals(player.getUUID());
		boolean isOp = player.hasPermissions(2);
		boolean canManage = isOwner || isOp;
		boolean member = variables.data().isMember(color, index, player.getUUID());
		boolean invited = owner != null && variables.data().isInvited(owner, player.getUUID());
		Set<UUID> members = variables.data().membersOf(color, index);

		player.sendSystemMessage(Component.literal("Teleporter (").withStyle(ChatFormatting.GRAY)
				.append(Component.literal("" + pos[0] + " " + pos[1] + " " + pos[2]).withStyle(ChatFormatting.AQUA))
				.append(Component.literal(")").withStyle(ChatFormatting.GRAY)));

		player.sendSystemMessage(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(ownerName(server, owner)).withStyle(ChatFormatting.YELLOW))
				.append(Component.literal("  Global: ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(global ? "yes" : "no").withStyle(global ? ChatFormatting.GREEN : ChatFormatting.RED)));

		if (!members.isEmpty()) {
			MutableComponent networks = Component.literal("In chains: ").withStyle(ChatFormatting.GRAY);
			boolean first = true;
			for (UUID memberId : members) {
				if (!first)
					networks.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
				networks.append(Component.literal(ownerName(server, memberId)).withStyle(ChatFormatting.WHITE));
				first = false;
			}
			player.sendSystemMessage(networks);
		}

		MutableComponent actions = Component.empty();
		boolean hasAction = false;

		if (!isOwner) {
			if (member) {
				actions.append(button("[Unlink from my network]", ChatFormatting.RED, coordCommand("unlink", color, pos)));
				hasAction = true;
			} else if (global || invited) {
				actions.append(button("[Link to my network]", ChatFormatting.GREEN, coordCommand("link", color, pos)));
				hasAction = true;
			}
		}

		if (canManage) {
			if (hasAction)
				actions.append(Component.literal(" "));
			actions.append(button(global ? "[Make personal]" : "[Make global]", ChatFormatting.GOLD,
					"/" + Constants.MOD_ID + " global " + (global ? "off" : "on") + " " + color.getId() + " " + pos[0] + " " + pos[1] + " " + pos[2]));
			hasAction = true;
			if (!members.isEmpty()) {
				actions.append(Component.literal(" "));
				actions.append(button("[Unlink all]", ChatFormatting.RED,
						"/" + Constants.MOD_ID + " unlinkall " + color.getId() + " " + pos[0] + " " + pos[1] + " " + pos[2]));
			}
		}

		if (hasAction)
			player.sendSystemMessage(actions);

		if (isOwner) {
			Set<UUID> invitedPlayers = variables.data().invitesOf(player.getUUID());
			if (!invitedPlayers.isEmpty()) {
				MutableComponent assign = Component.literal("Assign this block: ").withStyle(ChatFormatting.GRAY);
				boolean first = true;
				for (UUID invitedId : invitedPlayers) {
					if (!first)
						assign.append(Component.literal(" "));
					assign.append(button("[" + ownerName(server, invitedId) + "]", ChatFormatting.AQUA,
							"/" + Constants.MOD_ID + " assign " + invitedId + " " + color.getId() + " " + pos[0] + " " + pos[1] + " " + pos[2]));
					first = false;
				}
				player.sendSystemMessage(assign);
			}
		}
	}

	/** Using a teleporter item while aimed at air. */
	public static void sendHeldMenu(ServerPlayer player, TeleporterColor color) {
		WorldVariables variables = WorldVariables.get(player.level());
		MinecraftServer server = player.getServer();

		player.sendSystemMessage(Component.literal("Teleporters: " + color.getId()).withStyle(ChatFormatting.GRAY));
		boolean any = false;
		int count = variables.data().slotCount(color);
		for (int i = 0; i < count; i++) {
			if (!variables.data().isSlotOccupied(color, i))
				continue;
			UUID owner = variables.data().getOwner(color, i);
			if (owner == null)
				continue;
			boolean isOwner = owner.equals(player.getUUID());
			boolean member = variables.data().isMember(color, i, player.getUUID());
			if (!isOwner && !member)
				continue;

			any = true;
			int[] pos = variables.data().getSlot(color, i);
			boolean global = variables.data().isGlobal(color, i);

			MutableComponent row = Component.literal(pos[0] + " " + pos[1] + " " + pos[2]).withStyle(ChatFormatting.AQUA)
					.append(Component.literal(" (" + ownerName(server, owner) + (global ? ", global" : "") + ")").withStyle(ChatFormatting.DARK_GRAY))
					.append(Component.literal(" "));
			if (isOwner) {
				if (!variables.data().membersOf(color, i).isEmpty()) {
					row.append(button("[Unlink all]", ChatFormatting.RED,
							"/" + Constants.MOD_ID + " unlinkall " + color.getId() + " " + pos[0] + " " + pos[1] + " " + pos[2]));
				}
			} else {
				row.append(button("[Unlink]", ChatFormatting.RED, coordCommand("unlink", color, pos)));
			}
			player.sendSystemMessage(row);
		}
		if (!any) {
			int total = 0;
			for (int i = 0; i < count; i++) {
				if (variables.data().isSlotOccupied(color, i))
					total++;
			}
			if (total == 0) {
				player.sendSystemMessage(Component.literal("No teleporters of this color yet").withStyle(ChatFormatting.RED));
			} else {
				player.sendSystemMessage(Component.literal("No teleporters of this color in your network (" + total + " exist but are public or owned by others)").withStyle(ChatFormatting.RED));
			}
		}
	}
}
