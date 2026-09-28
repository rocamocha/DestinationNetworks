package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-to-client data used to populate the destination picker. */
public record TeleporterPickerPayload(CompoundTag data) implements CustomPacketPayload {
	public static final Type<TeleporterPickerPayload> TYPE = new Type<>(
			ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "picker"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleporterPickerPayload> STREAM_CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeNbt(payload.data()),
			buffer -> new TeleporterPickerPayload(buffer.readNbt()));

	@Override
	public Type<? extends TeleporterPickerPayload> type() {
		return TYPE;
	}
}
