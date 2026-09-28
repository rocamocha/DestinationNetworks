package net.cyootlabs.rocamocha.procedures;

import java.util.List;
import java.util.UUID;

import net.cyootlabs.rocamocha.LinkingMode;
import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.cyootlabs.rocamocha.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class TeleporterRightClickedProcedure {
	private static final SoundEvent TELEPORT_SOUND = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.chorus_fruit.teleport"));

	@SuppressWarnings("unused")
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, TeleporterColor color) {
		if (entity == null)
			return;

		WorldVariables variables = WorldVariables.get(world);
		int clicked = variables.data().findSlot(color, x, y, z);
		if (clicked < 0)
			return;

		if (entity instanceof ServerPlayer serverPlayer && !variables.data().canUseBlock(color, clicked, serverPlayer.getUUID())) {
			serverPlayer.displayClientMessage(Component.literal("You do not have permission to use this teleporter"), true);
			return;
		}

		boolean global = variables.data().isGlobal(color, clicked);
		UUID owner = variables.data().getOwner(color, clicked);
		UUID chainOwner = (Services.get().getLinkingMode() == LinkingMode.OWNER && !global && owner != null) ? owner : entity.getUUID();

		List<int[]> chain = variables.data().chainFor(chainOwner, color);
		if (chain.size() <= 1) {
			if (!world.isClientSide() && world.getServer() != null) {
				world.getServer().getPlayerList().broadcastSystemMessage(Component.literal("You only have one teleporter of this color"), false);
			}
			return;
		}

		int index = variables.data().indexInChain(chain, x, y, z);
		if (index < 0)
			index = 0;

		for (int offset = 1; offset < chain.size(); offset++) {
			int[] target = chain.get((index + offset) % chain.size());
			teleport(world, entity, x, y, z, target[0], target[1], target[2]);
			return;
		}
	}

	public static void teleport(LevelAccessor world, Entity entity, double sourceX, double sourceY, double sourceZ, double targetX, double targetY, double targetZ) {
		entity.teleportTo(targetX, targetY + 1.0D, targetZ);
		if (entity instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.teleport(targetX + 0.5D, targetY + 1.0D, targetZ + 0.5D, entity.getYRot(), entity.getXRot());
		}
		if (world instanceof Level level) {
			if (!level.isClientSide()) {
				level.playSound(null, BlockPos.containing(sourceX, sourceY, sourceZ), TELEPORT_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
			} else {
				level.playLocalSound(sourceX, sourceY, sourceZ, TELEPORT_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
			}
		}
	}
}
