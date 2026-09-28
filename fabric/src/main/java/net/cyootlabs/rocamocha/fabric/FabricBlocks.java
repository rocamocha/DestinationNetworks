package net.cyootlabs.rocamocha.fabric;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import net.cyootlabs.rocamocha.Constants;
import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.cyootlabs.rocamocha.item.TeleporterBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FabricBlocks {
	private static final Map<TeleporterColor, Block> BLOCKS = new EnumMap<>(TeleporterColor.class);
	private static final Map<TeleporterColor, Item> ITEMS = new EnumMap<>(TeleporterColor.class);
	private static final Map<TeleporterColor, Block> LEGACY_BLOCKS = new EnumMap<>(TeleporterColor.class);
	private static final Map<TeleporterColor, Item> LEGACY_ITEMS = new EnumMap<>(TeleporterColor.class);

	public static void register() {
		for (TeleporterColor color : TeleporterColor.values()) {
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, color.blockId());
			TeleporterBlock block = Registry.register(BuiltInRegistries.BLOCK, id, new TeleporterBlock(color));
			TeleporterBlockItem item = Registry.register(BuiltInRegistries.ITEM, id, new TeleporterBlockItem(block, new Item.Properties()));
			BLOCKS.put(color, block);
			ITEMS.put(color, item);

			ResourceLocation legacyId = ResourceLocation.fromNamespaceAndPath(Constants.LEGACY_MOD_ID, color.blockId());
			TeleporterBlock legacyBlock = Registry.register(BuiltInRegistries.BLOCK, legacyId, new TeleporterBlock(color));
			TeleporterBlockItem legacyItem = Registry.register(BuiltInRegistries.ITEM, legacyId, new TeleporterBlockItem(legacyBlock, new Item.Properties()));
			LEGACY_BLOCKS.put(color, legacyBlock);
			LEGACY_ITEMS.put(color, legacyItem);
		}
	}

	public static Map<TeleporterColor, Block> blocks() {
		return Collections.unmodifiableMap(BLOCKS);
	}

	public static Map<TeleporterColor, Item> items() {
		return Collections.unmodifiableMap(ITEMS);
	}

	public static Map<TeleporterColor, Block> legacyBlocks() {
		return Collections.unmodifiableMap(LEGACY_BLOCKS);
	}

	public static Map<TeleporterColor, Item> legacyItems() {
		return Collections.unmodifiableMap(LEGACY_ITEMS);
	}
}
