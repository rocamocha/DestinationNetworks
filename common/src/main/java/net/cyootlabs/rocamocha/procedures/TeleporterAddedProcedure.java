package net.cyootlabs.rocamocha.procedures;

import java.util.UUID;

import net.cyootlabs.rocamocha.Constants;
import net.cyootlabs.rocamocha.PlacementContext;
import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.cyootlabs.rocamocha.platform.Services;
import net.minecraft.world.level.LevelAccessor;

public class TeleporterAddedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, TeleporterColor color) {
		WorldVariables variables = WorldVariables.get(world);
		int slot = variables.data().findSlot(color, x, y, z);
		if (slot < 0)
			slot = variables.data().findEmptySlot(color);
		variables.data().setSlot(color, slot, x, y, z, Services.get().isNewBlocksGlobalByDefault());

		UUID owner = PlacementContext.take(color, (int) x, (int) y, (int) z);
		Constants.LOG.info("JAKEDBG added color={} at {} {} {} client={} owner={}", color.getId(), x, y, z, world.isClientSide(), owner);
		if (owner != null) {
			variables.data().setOwner(color, x, y, z, owner);
		}
		variables.syncData(world);
	}
}
