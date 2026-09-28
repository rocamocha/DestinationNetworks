package net.cyootlabs.rocamocha;

/**
 * Controls how a player's teleporter network is resolved when a block is used.
 * <ul>
 * <li>{@code OWNER} - personal blocks send the user through the block owner's chain.</li>
 * <li>{@code SHARED} - personal blocks send the user through their own chain.</li>
 * </ul>
 */
public enum LinkingMode {
	OWNER,
	SHARED;

	public static LinkingMode byName(String name) {
		for (LinkingMode mode : values()) {
			if (mode.name().equalsIgnoreCase(name))
				return mode;
		}
		return OWNER;
	}
}
