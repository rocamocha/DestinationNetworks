package net.cyootlabs.rocamocha.procedures;

import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.minecraft.world.level.LevelAccessor;

public class TeleporterDestroyedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, TeleporterColor color) {
		WorldVariables variables = WorldVariables.get(world);
		int slot = variables.data().findSlot(color, x, y, z);
		if (slot >= 0) {
			variables.data().clearSlot(color, slot);
			variables.syncData(world);
		}
	}
}
