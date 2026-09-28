package net.cyootlabs.rocamocha.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.cyootlabs.rocamocha.Constants;
import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.cyootlabs.rocamocha.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /jaketeleports ...} — invite management plus per-block chain/global management. The
 * colour and coordinate arguments offer suggestions from the executing player's chain so players
 * can pick a teleporter without typing coordinates. Every action re-validates ownership/permission.
 */
public final class TeleporterCommands {
	private TeleporterCommands() {
	}

	private static final SuggestionProvider<CommandSourceStack> COLOR_SUGGESTIONS = (context, builder) -> {
		for (TeleporterColor color : TeleporterColor.values()) {
			builder.suggest(color.getId());
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> X_SUGGESTIONS = (context, builder) -> {
		Set<Integer> seen = new HashSet<>();
		for (int[] pos : chain(context)) {
			if (seen.add(pos[0]))
				builder.suggest(pos[0]);
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> Y_SUGGESTIONS = (context, builder) -> {
		Integer x = optionalInt(context, "x");
		Set<Integer> seen = new HashSet<>();
		for (int[] pos : chain(context)) {
			if ((x == null || pos[0] == x) && seen.add(pos[1]))
				builder.suggest(pos[1]);
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<CommandSourceStack> Z_SUGGESTIONS = (context, builder) -> {
		Integer x = optionalInt(context, "x");
		Integer y = optionalInt(context, "y");
		Set<Integer> seen = new HashSet<>();
		for (int[] pos : chain(context)) {
			if ((x == null || pos[0] == x) && (y == null || pos[1] == y) && seen.add(pos[2]))
				builder.suggest(pos[2]);
		}
		return builder.buildFuture();
	};

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal(Constants.MOD_ID)
				.then(Commands.literal("invite")
						.requires(TeleporterCommands::canInvite)
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> modifyInvite(context, true))))
				.then(Commands.literal("uninvite")
						.requires(TeleporterCommands::canInvite)
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> modifyInvite(context, false))))
				.then(Commands.literal("list")
						.requires(TeleporterCommands::canInvite)
						.executes(TeleporterCommands::list))
				.then(Commands.literal("link")
						.then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
								.then(coordinateArguments(context -> link(context, true)))))
				.then(Commands.literal("unlink")
						.then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
								.then(coordinateArguments(context -> link(context, false)))))
				.then(Commands.literal("global")
						.then(Commands.literal("on")
								.then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
										.then(coordinateArguments(context -> setGlobal(context, true)))))
						.then(Commands.literal("off")
								.then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
										.then(coordinateArguments(context -> setGlobal(context, false))))))
				.then(Commands.literal("unlinkall")
						.then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
								.then(coordinateArguments(TeleporterCommands::unlinkAll))))
				.then(Commands.literal("assign")
						.then(Commands.argument("target", StringArgumentType.word())
								.then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
										.then(coordinateArguments(TeleporterCommands::assign)))))
				.then(Commands.literal("rename")
						.then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
								.then(Commands.argument("x", IntegerArgumentType.integer())
										.then(Commands.argument("y", IntegerArgumentType.integer())
												.then(Commands.argument("z", IntegerArgumentType.integer())
														.then(Commands.argument("name", StringArgumentType.greedyString())
																.executes(TeleporterCommands::rename)))))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Integer> coordinateArguments(Command<CommandSourceStack> command) {
		return Commands.argument("x", IntegerArgumentType.integer()).suggests(X_SUGGESTIONS)
				.then(Commands.argument("y", IntegerArgumentType.integer()).suggests(Y_SUGGESTIONS)
						.then(Commands.argument("z", IntegerArgumentType.integer()).suggests(Z_SUGGESTIONS)
								.executes(command)));
	}

	private static boolean canInvite(CommandSourceStack source) {
		return source.hasPermission(Services.get().getInvitePermissionLevel());
	}

	private static TeleporterColor color(CommandContext<CommandSourceStack> context) {
		return TeleporterColor.byId(StringArgumentType.getString(context, "color"));
	}

	private static Integer optionalInt(CommandContext<CommandSourceStack> context, String name) {
		try {
			return context.getArgument(name, Integer.class);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static List<int[]> chain(CommandContext<CommandSourceStack> context) {
		TeleporterColor color;
		try {
			color = TeleporterColor.byId(StringArgumentType.getString(context, "color"));
		} catch (IllegalArgumentException e) {
			return List.of();
		}
		ServerPlayer player = context.getSource().getPlayer();
		if (color == null || player == null)
			return List.of();
		return WorldVariables.get(player.level()).data().chainFor(player.getUUID(), color);
	}

	private static int modifyInvite(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException {
		ServerPlayer owner = context.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(context, "player");

		if (owner.getUUID().equals(target.getUUID())) {
			context.getSource().sendFailure(Component.literal("You cannot invite yourself"));
			return 0;
		}

		WorldVariables variables = WorldVariables.get(owner.level());
		String targetName = target.getGameProfile().getName();
		if (add) {
			variables.data().invite(owner.getUUID(), target.getUUID());
			owner.sendSystemMessage(Component.literal("Invited " + targetName + " to use your teleporters"));
		} else {
			variables.data().removeInvite(owner.getUUID(), target.getUUID());
			variables.data().purgeMember(owner.getUUID(), target.getUUID());
			owner.sendSystemMessage(Component.literal("Removed " + targetName + " from your teleporters"));
		}
		variables.syncData(owner.level());
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer owner = context.getSource().getPlayerOrException();
		Set<UUID> invited = WorldVariables.get(owner.level()).data().invitesOf(owner.getUUID());

		if (invited.isEmpty()) {
			owner.sendSystemMessage(Component.literal("You have not invited anyone"));
			return 0;
		}

		owner.sendSystemMessage(Component.literal("Players allowed to use your teleporters:"));
		for (UUID uuid : invited) {
			ServerPlayer online = context.getSource().getServer().getPlayerList().getPlayer(uuid);
			owner.sendSystemMessage(Component.literal(" - " + (online != null ? online.getGameProfile().getName() : uuid.toString())));
		}
		return invited.size();
	}

	private static int link(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TeleporterColor color = color(context);
		if (color == null) {
			context.getSource().sendFailure(Component.literal("Unknown color"));
			return 0;
		}
		int x = IntegerArgumentType.getInteger(context, "x");
		int y = IntegerArgumentType.getInteger(context, "y");
		int z = IntegerArgumentType.getInteger(context, "z");

		WorldVariables variables = WorldVariables.get(player.level());
		int index = variables.data().findSlot(color, x, y, z);
		if (index < 0) {
			context.getSource().sendFailure(Component.literal("No " + color.getId() + " teleporter at " + x + " " + y + " " + z));
			return 0;
		}

		boolean global = variables.data().isGlobal(color, index);
		boolean owner = variables.data().isOwner(color, index, player.getUUID());
		UUID blockOwner = variables.data().getOwner(color, index);
		boolean invited = blockOwner != null && variables.data().isInvited(blockOwner, player.getUUID());
		boolean member = variables.data().isMember(color, index, player.getUUID());
		boolean allowed = global || owner || invited || (add ? false : member);

		if (!allowed) {
			context.getSource().sendFailure(Component.literal("You do not have permission to link that teleporter"));
			return 0;
		}

		if (add) {
			variables.data().addMember(color, index, player.getUUID());
		} else {
			variables.data().removeMember(color, index, player.getUUID());
		}
		variables.syncData(player.level());
		player.sendSystemMessage(Component.literal((add ? "Linked " : "Unlinked ") + x + " " + y + " " + z + " (" + color.getId() + ")"));
		return 1;
	}

	private static int setGlobal(CommandContext<CommandSourceStack> context, boolean global) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TeleporterColor color = color(context);
		if (color == null) {
			context.getSource().sendFailure(Component.literal("Unknown color"));
			return 0;
		}
		int x = IntegerArgumentType.getInteger(context, "x");
		int y = IntegerArgumentType.getInteger(context, "y");
		int z = IntegerArgumentType.getInteger(context, "z");

		WorldVariables variables = WorldVariables.get(player.level());
		int index = variables.data().findSlot(color, x, y, z);
		if (index < 0) {
			context.getSource().sendFailure(Component.literal("No " + color.getId() + " teleporter at " + x + " " + y + " " + z));
			return 0;
		}
		if (!variables.data().isOwner(color, index, player.getUUID()) && !player.hasPermissions(2)) {
			context.getSource().sendFailure(Component.literal("Only the owner (or an operator) can change this"));
			return 0;
		}

		variables.data().setGlobal(color, index, global);
		variables.syncData(player.level());
		player.sendSystemMessage(Component.literal("Teleporter " + x + " " + y + " " + z + " is now " + (global ? "global" : "personal")));
		return 1;
	}

	private static int unlinkAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TeleporterColor color = color(context);
		if (color == null) {
			context.getSource().sendFailure(Component.literal("Unknown color"));
			return 0;
		}
		int x = IntegerArgumentType.getInteger(context, "x");
		int y = IntegerArgumentType.getInteger(context, "y");
		int z = IntegerArgumentType.getInteger(context, "z");

		WorldVariables variables = WorldVariables.get(player.level());
		int index = variables.data().findSlot(color, x, y, z);
		if (index < 0) {
			context.getSource().sendFailure(Component.literal("No " + color.getId() + " teleporter at " + x + " " + y + " " + z));
			return 0;
		}
		UUID owner = variables.data().getOwner(color, index);
		boolean isOwner = owner != null && owner.equals(player.getUUID());
		if (!isOwner && !player.hasPermissions(2)) {
			context.getSource().sendFailure(Component.literal("Only the owner (or an operator) can unlink everyone"));
			return 0;
		}

		variables.data().clearMembers(color, index);
		variables.syncData(player.level());
		player.sendSystemMessage(Component.literal("Unlinked everyone from " + x + " " + y + " " + z + " (" + color.getId() + ")"));
		return 1;
	}

	private static int assign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String targetArg = StringArgumentType.getString(context, "target");
		TeleporterColor color = color(context);
		if (color == null) {
			context.getSource().sendFailure(Component.literal("Unknown color"));
			return 0;
		}
		int x = IntegerArgumentType.getInteger(context, "x");
		int y = IntegerArgumentType.getInteger(context, "y");
		int z = IntegerArgumentType.getInteger(context, "z");

		UUID target = resolveTarget(context, targetArg);
		if (target == null) {
			context.getSource().sendFailure(Component.literal("Unknown player: " + targetArg));
			return 0;
		}

		WorldVariables variables = WorldVariables.get(player.level());
		int index = variables.data().findSlot(color, x, y, z);
		if (index < 0) {
			context.getSource().sendFailure(Component.literal("No " + color.getId() + " teleporter at " + x + " " + y + " " + z));
			return 0;
		}

		UUID owner = variables.data().getOwner(color, index);
		if (!player.hasPermissions(2)) {
			if (owner == null) {
				context.getSource().sendFailure(Component.literal("Only an operator can assign an unowned teleporter"));
				return 0;
			}
			if (!owner.equals(player.getUUID())) {
				context.getSource().sendFailure(Component.literal("Only the owner or an operator can assign this teleporter"));
				return 0;
			}
			if (!variables.data().isInvited(player.getUUID(), target)) {
				context.getSource().sendFailure(Component.literal("You can only assign this block to a player you have invited"));
				return 0;
			}
		}

		variables.data().setOwner(color, x, y, z, target);
		variables.syncData(player.level());
		player.sendSystemMessage(Component.literal("Assigned teleporter " + x + " " + y + " " + z + " to " + targetArg));
		return 1;
	}

	private static int rename(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		TeleporterColor color = color(context);
		String name = StringArgumentType.getString(context, "name").trim();
		int x = IntegerArgumentType.getInteger(context, "x");
		int y = IntegerArgumentType.getInteger(context, "y");
		int z = IntegerArgumentType.getInteger(context, "z");
		WorldVariables variables = WorldVariables.get(player.level());
		int index = color == null ? -1 : variables.data().findSlot(color, x, y, z);
		if (index < 0) {
			context.getSource().sendFailure(Component.literal("No teleporter at " + x + " " + y + " " + z));
			return 0;
		}
		if (!variables.data().isOwner(color, index, player.getUUID()) && !player.hasPermissions(2)) {
			context.getSource().sendFailure(Component.literal("Only the owner (or an operator) can rename this"));
			return 0;
		}
		if (name.length() > 32) {
			context.getSource().sendFailure(Component.literal("Teleporter names must be 32 characters or fewer"));
			return 0;
		}
		variables.data().setName(color, index, name);
		variables.syncData(player.level());
		player.sendSystemMessage(Component.literal("Renamed teleporter " + x + " " + y + " " + z));
		return 1;
	}

	private static UUID resolveTarget(CommandContext<CommandSourceStack> context, String arg) {
		try {
			return UUID.fromString(arg);
		} catch (IllegalArgumentException ignored) {
		}
		ServerPlayer online = context.getSource().getServer().getPlayerList().getPlayerByName(arg);
		return online != null ? online.getUUID() : null;
	}
}
