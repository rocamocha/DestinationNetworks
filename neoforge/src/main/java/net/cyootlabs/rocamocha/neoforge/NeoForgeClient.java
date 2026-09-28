package net.cyootlabs.rocamocha.neoforge;

import java.util.ArrayList;
import java.util.List;

import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoForgeClient {
	public static void openPicker(TeleporterPickerPayload payload) {
		Minecraft.getInstance().setScreen(new NeoForgeTeleporterPickerScreen(payload));
	}

	@SubscribeEvent
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		List<Block> blocks = new ArrayList<>();
		for (DeferredBlock<Block> block : NeoForgeBlocks.teleporters().values()) {
			blocks.add(block.get());
		}
		for (DeferredHolder<Block, Block> block : NeoForgeBlocks.legacyTeleporters().values()) {
			blocks.add(block.get());
		}
		event.register((state, level, pos, tintIndex) -> ((TeleporterBlock) state.getBlock()).getTintColor(), blocks.toArray(new Block[0]));
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		for (DeferredBlock<Block> block : NeoForgeBlocks.teleporters().values()) {
			event.register((stack, tintIndex) -> ((TeleporterBlock) block.get()).getTintColor(), block.get().asItem());
		}
		for (DeferredHolder<Block, Block> block : NeoForgeBlocks.legacyTeleporters().values()) {
			event.register((stack, tintIndex) -> ((TeleporterBlock) block.get()).getTintColor(), block.get().asItem());
		}
	}
}
