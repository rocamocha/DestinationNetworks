package net.cyootlabs.rocamocha.neoforge;

import net.cyootlabs.rocamocha.LinkingMode;
import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeConfig {
	public static final int MAX_CONFIGURABLE_TELEPORTERS = 1024;

	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.EnumValue<LinkingMode> LINKING_MODE;
	public static final ModConfigSpec.BooleanValue NEW_BLOCKS_GLOBAL_BY_DEFAULT;
	public static final ModConfigSpec.IntValue MAX_TELEPORTERS_PER_COLOR;
	public static final ModConfigSpec.IntValue INVITE_PERMISSION_LEVEL;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		builder.comment("Teleporter linking settings").push("teleporters");
		LINKING_MODE = builder
				.comment("OWNER: using a personal block routes through the block owner's chain.",
						"SHARED: using a personal block routes through the user's own chain.")
				.defineEnum("linkingMode", LinkingMode.OWNER);
		NEW_BLOCKS_GLOBAL_BY_DEFAULT = builder
				.comment("Whether newly placed teleporters start as global (usable by everyone, no permission required).")
				.define("newBlocksGlobalByDefault", true);
		MAX_TELEPORTERS_PER_COLOR = builder
				.comment("Maximum number of teleporters a single player may place per color (1-" + MAX_CONFIGURABLE_TELEPORTERS + ").",
						"Lowering this value never removes or deactivates existing links; it only limits new placements.")
				.defineInRange("maxTeleportersPerColor", 10, 1, MAX_CONFIGURABLE_TELEPORTERS);
		INVITE_PERMISSION_LEVEL = builder
				.comment("Permission level required to run the invite/uninvite/list commands (0 = everyone).",
						"This is also the tier an invite grants.")
				.defineInRange("invitePermissionLevel", 0, 0, 4);
		builder.pop();

		SPEC = builder.build();
	}
}
