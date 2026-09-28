package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-to-server request to rename a teleporter. */
public record TeleporterRenamePayload(CompoundTag data) implements CustomPacketPayload {
	public static final Type<TeleporterRenamePayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "rename_teleporter"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleporterRenamePayload> STREAM_CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeNbt(payload.data()),
			buffer -> new TeleporterRenamePayload(buffer.readNbt()));

	@Override
	public Type<? extends TeleporterRenamePayload> type() {
		return TYPE;
	}
}
