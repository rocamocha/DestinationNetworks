package net.cyootlabs.rocamocha;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carries the placing player from {@code setPlacedBy} (or the item) to the block's onPlace link
 * registration. Which of the two runs first differs between loaders, so this holds a pending owner
 * keyed by position until the link exists, and is consumed by the added-procedure.
 */
public final class PlacementContext {
	private static final Map<String, UUID> PENDING = new ConcurrentHashMap<>();

	private PlacementContext() {
	}

	private static String key(TeleporterColor color, int x, int y, int z) {
		return color.getId() + '|' + x + '|' + y + '|' + z;
	}

	public static void put(TeleporterColor color, int x, int y, int z, UUID owner) {
		PENDING.put(key(color, x, y, z), owner);
	}

	public static UUID take(TeleporterColor color, int x, int y, int z) {
		return PENDING.remove(key(color, x, y, z));
	}
}
