package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Detects and removes teleporter link data left over from a block that no longer exists in the world. */
public final class TeleporterIntegrity {
	private TeleporterIntegrity() {
	}

	/** Returns true only if the chunk is loaded and the block there does not match the recorded link. */
	public static boolean isConfirmedMissing(ServerLevel level, TeleporterColor color, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
			return false;
		return !(level.getBlockState(pos).getBlock() instanceof TeleporterBlock block) || block.getColor() != color;
	}

	/** Clears the link's data if it points to a location with no matching teleporter block. */
	public static boolean pruneIfMissing(ServerLevel level, WorldVariables variables, TeleporterColor color, int x, int y, int z) {
		if (!isConfirmedMissing(level, color, x, y, z))
			return false;
		int index = variables.data().findSlot(color, x, y, z);
		if (index < 0)
			return false;
		variables.data().clearSlot(color, index);
		return true;
	}
}
