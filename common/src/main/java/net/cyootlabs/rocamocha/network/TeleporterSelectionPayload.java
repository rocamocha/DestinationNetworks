package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-to-server request to confirm a destination selected in the picker. */
public record TeleporterSelectionPayload(CompoundTag data) implements CustomPacketPayload {
	public static final Type<TeleporterSelectionPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "select_destination"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleporterSelectionPayload> STREAM_CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeNbt(payload.data()),
			buffer -> new TeleporterSelectionPayload(buffer.readNbt()));

	@Override
	public Type<? extends TeleporterSelectionPayload> type() {
		return TYPE;
	}
}
