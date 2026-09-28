package net.cyootlabs.rocamocha.neoforge;

import net.cyootlabs.rocamocha.Constants;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Constants.MOD_ID)
public class DestinationNetworksNeoForge {
	public DestinationNetworksNeoForge(IEventBus modEventBus, ModContainer modContainer) {
		Services.setPlatform(new NeoForgePlatformHelper());

		NeoForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::registerNetworking);
		modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);

		NeoForgeBlocks.REGISTRY.register(modEventBus);
		NeoForgeBlocks.LEGACY_REGISTRY.register(modEventBus);
		NeoForgeItems.REGISTRY.register(modEventBus);
		NeoForgeItems.LEGACY_REGISTRY.register(modEventBus);
		NeoForgeTabs.REGISTRY.register(modEventBus);
	}

	private void registerNetworking(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);
		registrar.playToClient(TeleporterSyncPayload.TYPE, TeleporterSyncPayload.STREAM_CODEC, (payload, context) -> WorldVariables.applyClient(payload));
		registrar.playToClient(TeleporterPickerPayload.TYPE, TeleporterPickerPayload.STREAM_CODEC, (payload, context) -> {
			context.enqueueWork(() -> NeoForgeClient.openPicker(payload));
		});
		registrar.playToServer(TeleporterSelectionPayload.TYPE, TeleporterSelectionPayload.STREAM_CODEC,
				(payload, context) -> {
					if (context.player() instanceof ServerPlayer player)
						TeleporterSelectionHandler.handle(player, payload);
				});
		registrar.playToServer(TeleporterRenamePayload.TYPE, TeleporterRenamePayload.STREAM_CODEC,
				(payload, context) -> {
					if (context.player() instanceof ServerPlayer player)
						TeleporterRenameHandler.handle(player, payload);
				});
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(TeleporterCommands.build());
	}

	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getState().getBlock() instanceof TeleporterBlock block && event.getPlayer() instanceof ServerPlayer player) {
			WorldVariables variables = WorldVariables.get(player.level());
			int slot = variables.data().findSlot(block.getColor(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
			if (!variables.data().canBreakBlock(block.getColor(), slot, player.getUUID())) {
				event.setCanceled(true);
				player.displayClientMessage(Component.literal("Only the owner can break this teleporter"), true);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			Services.get().sendToPlayer(player, WorldVariables.get(player.level()).makePayload());
		}
	}

	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			Services.get().sendToPlayer(player, WorldVariables.get(player.level()).makePayload());
		}
	}
}
