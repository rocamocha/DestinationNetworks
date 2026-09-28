package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.TeleporterData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-dimension persistent teleporter links. SavedData is a vanilla API present on both loaders,
 * so this class can live in the common module.
 */
public class WorldVariables extends SavedData {
	public static final String DATA_NAME = "jake_teleports_worldvars";
	public static WorldVariables clientSide = new WorldVariables();

	private final TeleporterData data = new TeleporterData();

	public TeleporterData data() {
		return this.data;
	}

	public static WorldVariables load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		WorldVariables variables = new WorldVariables();
		variables.data.read(tag);
		return variables;
	}

	@Override
	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
		return this.data.write(nbt);
	}

	public TeleporterSyncPayload makePayload() {
		return new TeleporterSyncPayload(this.data.write(new CompoundTag()));
	}

	public void syncData(LevelAccessor world) {
		this.setDirty();
		if (world instanceof ServerLevel level) {
			net.cyootlabs.rocamocha.platform.Services.get().sendToDimension(level, this.makePayload());
		}
	}

	public static WorldVariables get(LevelAccessor world) {
		if (world instanceof ServerLevel level)
			return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldVariables::new, WorldVariables::load, DataFixTypes.LEVEL), DATA_NAME);
		return clientSide;
	}

	public static void applyClient(TeleporterSyncPayload payload) {
		clientSide.data.read(payload.data());
	}
}
