package net.cyootlabs.rocamocha.neoforge;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import net.cyootlabs.rocamocha.Constants;
import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgeBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(Constants.MOD_ID);
	public static final DeferredRegister<Block> LEGACY_REGISTRY = DeferredRegister.create(Registries.BLOCK, Constants.LEGACY_MOD_ID);

	private static final Map<TeleporterColor, DeferredBlock<Block>> TELEPORTERS = new EnumMap<>(TeleporterColor.class);
	private static final Map<TeleporterColor, DeferredHolder<Block, Block>> LEGACY_TELEPORTERS = new EnumMap<>(TeleporterColor.class);

	static {
		for (TeleporterColor color : TeleporterColor.values()) {
			TELEPORTERS.put(color, REGISTRY.register(color.blockId(), () -> new TeleporterBlock(color)));
			LEGACY_TELEPORTERS.put(color, LEGACY_REGISTRY.register(color.blockId(), () -> new TeleporterBlock(color)));
		}
	}

	public static Map<TeleporterColor, DeferredBlock<Block>> teleporters() {
		return Collections.unmodifiableMap(TELEPORTERS);
	}

	public static Map<TeleporterColor, DeferredHolder<Block, Block>> legacyTeleporters() {
		return Collections.unmodifiableMap(LEGACY_TELEPORTERS);
	}
}
