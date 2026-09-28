package net.cyootlabs.rocamocha;

public enum TeleporterColor {
	WHITE("white", 0xF9FFFE),
	ORANGE("orange", 0xF9801D),
	MAGENTA("magenta", 0xC74EBD),
	LIGHTBLUE("lightblue", 0x3AB3DA),
	YELLOW("yellow", 0xFED83D),
	LIME("lime", 0x80C71F),
	PINK("pink", 0xF38BAA),
	GRAY("gray", 0x474F52),
	LIGHTGRAY("lightgray", 0x9D9D97),
	CYAN("cyan", 0x169C9C),
	PURPLE("purple", 0x8932B8),
	BLUE("blue", 0x3C44AA),
	BROWN("brown", 0x835432),
	GREEN("green", 0x5E7C16),
	RED("red", 0xB02E26),
	BLACK("black", 0x1D1D21);

	private final String id;
	private final int tintColor;

	TeleporterColor(String id, int tintColor) {
		this.id = id;
		this.tintColor = tintColor;
	}

	public String getId() {
		return this.id;
	}

	public int getTintColor() {
		return this.tintColor;
	}

	public String blockId() {
		return "teleporter_" + this.id;
	}

	public static TeleporterColor byId(String id) {
		for (TeleporterColor color : values()) {
			if (color.id.equalsIgnoreCase(id))
				return color;
		}
		return null;
	}
}
