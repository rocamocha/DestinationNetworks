package net.cyootlabs.rocamocha.item;

import net.cyootlabs.rocamocha.Constants;
import net.cyootlabs.rocamocha.TeleporterMenu;
import net.cyootlabs.rocamocha.block.TeleporterBlock;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.cyootlabs.rocamocha.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;

/**
 * Enforces the per-player cap and opens the held-colour chat menu when the item is used while aimed
 * at air. Ownership is recorded by the block's placement hooks.
 */
public class TeleporterBlockItem extends BlockItem {
	private final TeleporterBlock teleporter;

	public TeleporterBlockItem(TeleporterBlock block, Item.Properties properties) {
		super(block, properties);
		this.teleporter = block;
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		Player player = context.getPlayer();
		if (player != null) {
			Constants.LOG.info("JAKEDBG item.place client={} player={} color={}",
					context.getLevel().isClientSide(), player.getUUID(), this.teleporter.getColor().getId());
			WorldVariables variables = WorldVariables.get(context.getLevel());
			int limit = Services.get().getMaxTeleportersPerColor();
			if (variables.data().countOwned(this.teleporter.getColor(), player.getUUID()) >= limit) {
				if (!context.getLevel().isClientSide()) {
					player.displayClientMessage(Component.literal("You already have " + limit + " teleporters of this color"), true);
				}
				return InteractionResult.FAIL;
			}
		}
		return super.place(context);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			TeleporterMenu.sendHeldMenu(serverPlayer, this.teleporter.getColor());
		}
		return InteractionResultHolder.success(player.getItemInHand(hand));
	}
}
