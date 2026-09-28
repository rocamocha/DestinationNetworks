package net.cyootlabs.rocamocha.neoforge;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import net.cyootlabs.rocamocha.Constants;
import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.cyootlabs.rocamocha.item.TeleporterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgeItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(Constants.MOD_ID);
	public static final DeferredRegister<Item> LEGACY_REGISTRY = DeferredRegister.create(Registries.ITEM, Constants.LEGACY_MOD_ID);

	private static final Map<TeleporterColor, DeferredItem<Item>> TELEPORTERS = new EnumMap<>(TeleporterColor.class);
	private static final Map<TeleporterColor, DeferredHolder<Item, Item>> LEGACY_TELEPORTERS = new EnumMap<>(TeleporterColor.class);

	static {
		for (Map.Entry<TeleporterColor, DeferredBlock<Block>> entry : NeoForgeBlocks.teleporters().entrySet()) {
			DeferredBlock<Block> block = entry.getValue();
			TELEPORTERS.put(entry.getKey(), REGISTRY.register(block.getId().getPath(), () -> new TeleporterBlockItem((TeleporterBlock) block.get(), new Item.Properties())));
		}
		for (Map.Entry<TeleporterColor, DeferredHolder<Block, Block>> entry : NeoForgeBlocks.legacyTeleporters().entrySet()) {
			DeferredHolder<Block, Block> block = entry.getValue();
			LEGACY_TELEPORTERS.put(entry.getKey(), LEGACY_REGISTRY.register(block.getId().getPath(), () -> new TeleporterBlockItem((TeleporterBlock) block.get(), new Item.Properties())));
		}
	}

	public static Map<TeleporterColor, DeferredItem<Item>> teleporters() {
		return Collections.unmodifiableMap(TELEPORTERS);
	}

	public static Map<TeleporterColor, DeferredHolder<Item, Item>> legacyTeleporters() {
		return Collections.unmodifiableMap(LEGACY_TELEPORTERS);
	}
}
