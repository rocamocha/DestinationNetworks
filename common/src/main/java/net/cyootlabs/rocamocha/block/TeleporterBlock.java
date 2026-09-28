package net.cyootlabs.rocamocha.block;

import net.cyootlabs.rocamocha.PlacementContext;
import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.TeleporterMenu;
import net.cyootlabs.rocamocha.network.WorldVariables;
import net.cyootlabs.rocamocha.network.TeleporterPickerService;
import net.cyootlabs.rocamocha.procedures.TeleporterAddedProcedure;
import net.cyootlabs.rocamocha.procedures.TeleporterDestroyedProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TeleporterBlock extends Block {
	private final TeleporterColor color;

	public TeleporterBlock(TeleporterColor color) {
		super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.0F, 10.0F));
		this.color = color;
	}

	public TeleporterColor getColor() {
		return this.color;
	}

	public int getTintColor() {
		return this.color.getTintColor();
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		TeleporterAddedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), this.color);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (!world.isClientSide() && placer instanceof Player player) {
			net.cyootlabs.rocamocha.Constants.LOG.info("JAKEDBG setPlacedBy pos={} {} {} placer={}",
					pos.getX(), pos.getY(), pos.getZ(), player.getUUID());
			WorldVariables variables = WorldVariables.get(world);
			int index = variables.data().findSlot(this.color, pos.getX(), pos.getY(), pos.getZ());
			if (index >= 0) {
				variables.data().setOwner(this.color, pos.getX(), pos.getY(), pos.getZ(), player.getUUID(), player.getGameProfile().getName());
			} else {
				// onPlace will register the link after this; remember the owner for it.
				PlacementContext.put(this.color, pos.getX(), pos.getY(), pos.getZ(), player.getUUID());
			}
			variables.syncData(world);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player entity) {
		BlockState retval = super.playerWillDestroy(world, pos, state, entity);
		TeleporterDestroyedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), this.color);
		return retval;
	}

	@Override
	public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
		super.wasExploded(world, pos, explosion);
		TeleporterDestroyedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ(), this.color);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		if (entity.isShiftKeyDown()) {
			if (!world.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
				TeleporterMenu.sendBlockMenu(serverPlayer, this.color, pos.getX(), pos.getY(), pos.getZ());
			}
			return InteractionResult.SUCCESS;
		}
		if (!world.isClientSide() && entity instanceof ServerPlayer serverPlayer)
			TeleporterPickerService.open(serverPlayer, this.color, pos.getX(), pos.getY(), pos.getZ());
		return InteractionResult.SUCCESS;
	}
}
