package net.cyootlabs.rocamocha.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.cyootlabs.rocamocha.command.TeleporterCommands;
import net.cyootlabs.rocamocha.network.TeleporterSyncPayload;
import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.cyootlabs.rocamocha.network.TeleporterSelectionHandler;
import net.cyootlabs.rocamocha.network.TeleporterSelectionPayload;
import net.cyootlabs.rocamocha.network.TeleporterRenameHandler;
import net.cyootlabs.rocamocha.network.TeleporterRenamePayload;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.cyootlabs.rocamocha.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class DestinationNetworksFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Services.setPlatform(new FabricPlatformHelper());
		FabricConfig.load();
		FabricBlocks.register();

		PayloadTypeRegistry.playS2C().register(TeleporterSyncPayload.TYPE, TeleporterSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleporterPickerPayload.TYPE, TeleporterPickerPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleporterSelectionPayload.TYPE, TeleporterSelectionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleporterRenamePayload.TYPE, TeleporterRenamePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TeleporterSelectionPayload.TYPE,
				(payload, context) -> TeleporterSelectionHandler.handle(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(TeleporterRenamePayload.TYPE,
				(payload, context) -> TeleporterRenameHandler.handle(context.player(), payload));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			for (Item item : FabricBlocks.items().values()) {
				entries.accept(item);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(TeleporterCommands.build()));

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (state.getBlock() instanceof TeleporterBlock block && player instanceof ServerPlayer serverPlayer) {
				WorldVariables variables = WorldVariables.get(serverPlayer.level());
				int slot = variables.data().findSlot(block.getColor(), pos.getX(), pos.getY(), pos.getZ());
				if (!variables.data().canBreakBlock(block.getColor(), slot, serverPlayer.getUUID())) {
					serverPlayer.displayClientMessage(Component.literal("Only the owner can break this teleporter"), true);
					return false;
				}
			}
			return true;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			ServerPlayNetworking.send(player, WorldVariables.get(player.level()).makePayload());
		});

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			ServerPlayNetworking.send(player, WorldVariables.get(player.level()).makePayload());
		});
	}
}
