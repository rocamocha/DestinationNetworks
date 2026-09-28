package net.cyootlabs.rocamocha.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.cyootlabs.rocamocha.network.TeleporterSyncPayload;
import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class DestinationNetworksFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TeleporterSyncPayload.TYPE,
				(payload, context) -> context.client().execute(() -> WorldVariables.applyClient(payload)));
		ClientPlayNetworking.registerGlobalReceiver(TeleporterPickerPayload.TYPE,
				(payload, context) -> context.client().execute(() -> Minecraft.getInstance().setScreen(new FabricTeleporterPickerScreen(payload))));

		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> ((TeleporterBlock) state.getBlock()).getTintColor(),
				java.util.stream.Stream.concat(FabricBlocks.blocks().values().stream(), FabricBlocks.legacyBlocks().values().stream()).toArray(Block[]::new));

		ColorProviderRegistry.ITEM.register(
				(stack, tintIndex) -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TeleporterBlock teleporter
						? teleporter.getTintColor()
						: 0xFFFFFF,
				java.util.stream.Stream.concat(FabricBlocks.items().values().stream(), FabricBlocks.legacyItems().values().stream()).toArray(Item[]::new));
	}
}
