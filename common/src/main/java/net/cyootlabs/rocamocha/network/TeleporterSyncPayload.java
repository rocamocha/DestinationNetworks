package net.cyootlabs.rocamocha.network;

import net.cyootlabs.rocamocha.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client sync of the teleporter links. The payload is loader-agnostic; each loader only
 * registers/handles it with its own networking API.
 */
public record TeleporterSyncPayload(CompoundTag data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TeleporterSyncPayload> TYPE = new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleporterSyncPayload> STREAM_CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeNbt(payload.data()),
			buffer -> new TeleporterSyncPayload(buffer.readNbt()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
