package net.cyootlabs.rocamocha.neoforge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.cyootlabs.rocamocha.TeleporterColor;
import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.cyootlabs.rocamocha.network.TeleporterSelectionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.network.PacketDistributor;

final class NeoForgeTeleporterPickerScreen extends Screen {
	private final CompoundTag data;
	private final List<CompoundTag> destinations = new ArrayList<>();
	private final List<CompoundTag> recent = new ArrayList<>();
	private Sort sort = Sort.DISTANCE;
	private int selected = -1;
	private int category;
	private int page;
	private DestinationList destinationList;

	private enum Sort {
		DISTANCE, NAME, OWNER
	}

	NeoForgeTeleporterPickerScreen(TeleporterPickerPayload payload) {
		super(Component.literal("Choose destination"));
		this.data = payload.data();
		ListTag entries = this.data.getList("destinations", 10);
		for (int i = 0; i < entries.size(); i++)
			this.destinations.add(entries.getCompound(i));
		ListTag recentEntries = this.data.getList("recent", 10);
		for (int i = 0; i < recentEntries.size(); i++)
			this.recent.add(recentEntries.getCompound(i));
		TeleporterColor source = TeleporterColor.byId(this.data.getString("source_color"));
		this.category = 2 + (source == null ? 0 : source.ordinal());
	}

	@Override
	protected void init() {
		this.destinationList = new DestinationList(this.width / 2 - 155, 83, 310, 150, this.visibleEntries());
		this.addRenderableWidget(this.destinationList);
		int rowX = this.width / 2 - 155;
		int arrowWidth = 20;
		int gap = 4;
		int wideWidth = 310 - arrowWidth * 2 - gap * 2;
		this.addRenderableWidget(Button.builder(Component.literal("<<"), button -> this.changeCategory(-1)).bounds(rowX, 33, arrowWidth, 20).build());
		this.addRenderableWidget(new CategoryLabel(rowX + arrowWidth + gap, 33, wideWidth, 20));
		this.addRenderableWidget(Button.builder(Component.literal(">>"), button -> this.changeCategory(1)).bounds(rowX + 310 - arrowWidth, 33, arrowWidth, 20).build());
		Button previous = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1)).bounds(rowX, 57, arrowWidth, 20).build());
		previous.active = this.page > 0;
		this.addRenderableWidget(CycleButton.<Sort>builder(value -> Component.literal(valueLabel(value)))
				.withValues(Sort.values()).withInitialValue(this.sort)
				.create(rowX + arrowWidth + gap, 57, wideWidth, 20, Component.literal("Sort"), (button, value) -> this.setSort(value)));
		Button next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1)).bounds(rowX + 310 - arrowWidth, 57, arrowWidth, 20).build());
		next.active = this.page + 1 < this.pageCount();
		this.addRenderableWidget(new SourceLabel(this.width / 2 - 131, 16, 262, 14));
	}

	private void setSort(Sort sort) {
		this.sort = sort;
		if (sort == Sort.OWNER)
			this.category = 0;
		this.selected = -1;
		this.page = 0;
		this.clearWidgets();
		this.init();
	}

	private void changeCategory(int amount) {
		this.category = Math.floorMod(this.category + amount, this.categoryCount());
		this.page = 0;
		this.selected = -1;
		this.clearWidgets();
		this.init();
	}

	private void changePage(int amount) {
		this.page = Math.max(0, Math.min(this.page + amount, this.pageCount() - 1));
		this.selected = -1;
		this.clearWidgets();
		this.init();
	}

	private String valueLabel(Sort value) {
		return switch (value) {
		case DISTANCE -> "Distance";
		case NAME -> "Name";
		case OWNER -> "Owner";
		};
	}

	private void openRename() {
		this.minecraft.setScreen(new NeoForgeTeleporterRenameScreen(new TeleporterPickerPayload(this.data)));
	}

	private List<CompoundTag> visibleEntries() {
		List<CompoundTag> visible = new ArrayList<>();
		if (this.category == 0)
			visible.addAll(this.destinations);
		else if (this.category == 1)
			visible.addAll(this.recent);
		else if (this.sort == Sort.OWNER) {
			String owner = this.ownerNames().get(this.category - 2);
			for (CompoundTag entry : this.destinations) {
				if (this.ownerLabel(entry).equals(owner))
					visible.add(entry);
			}
		} else {
			String color = TeleporterColor.values()[this.category - 2].getId();
			for (CompoundTag entry : this.destinations) {
				if (entry.getString("color").equals(color))
					visible.add(entry);
			}
		}
		visible.sort(this.comparator());
		return visible;
	}

	private int pageCount() {
		return Math.max(1, (this.visibleEntries().size() + 9) / 10);
	}

	private int categoryCount() {
		return this.sort == Sort.OWNER ? 2 + this.ownerNames().size() : 2 + TeleporterColor.values().length;
	}

	private List<String> ownerNames() {
		Set<String> names = new LinkedHashSet<>();
		for (CompoundTag entry : this.destinations)
			names.add(this.ownerLabel(entry));
		return new ArrayList<>(names);
	}

	private String ownerLabel(CompoundTag entry) {
		String owner = entry.getString("owner");
		return owner.isBlank() ? "Unknown owner" : owner;
	}

	private int categoryColor() {
		if (this.category >= 2 && this.sort != Sort.OWNER) {
			TeleporterColor teleporterColor = TeleporterColor.values()[this.category - 2];
			if (teleporterColor != TeleporterColor.BLACK && teleporterColor != TeleporterColor.GRAY && teleporterColor != TeleporterColor.LIGHTGRAY)
				return teleporterColor.getTintColor();
		}
		return 0xFFFFFF;
	}

	private int categoryBackground() {
		int tint = this.categoryColor();
		if (tint == 0xFFFFFF)
			return 0xFF3A3A3A;
		int r = (tint >> 16 & 0xFF) / 4;
		int g = (tint >> 8 & 0xFF) / 4;
		int b = (tint & 0xFF) / 4;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private String categoryBaseName() {
		if (this.category == 0)
			return this.sort == Sort.OWNER ? "All Owners" : "All Colors";
		if (this.category == 1)
			return "Recent";
		if (this.sort == Sort.OWNER)
			return this.ownerNames().get(this.category - 2);
		return TeleporterColor.values()[this.category - 2].getId();
	}

	private Component categoryLabel() {
		return Component.literal(this.categoryBaseName() + "  " + (this.page + 1) + "/" + this.pageCount()).withStyle(Style.EMPTY.withColor(this.categoryColor()));
	}

	/** Clips and, if the text overflows the given width, scrolls it left continuously like a news ticker. */
	private void renderScrollingText(GuiGraphics graphics, Component text, int x, int y, int width, int height) {
		int textWidth = this.font.width(text);
		if (textWidth <= width) {
			graphics.drawString(this.font, text, x, y, 0xFFFFFF);
			return;
		}
		graphics.enableScissor(x, y, x + width, y + height);
		int gapPx = 20;
		int totalWidth = textWidth + gapPx;
		int offset = (int) ((System.currentTimeMillis() / 20L) % totalWidth);
		graphics.drawString(this.font, text, x - offset, y, 0xFFFFFF);
		graphics.drawString(this.font, text, x - offset + totalWidth, y, 0xFFFFFF);
		graphics.disableScissor();
	}

	private String sourceLabel() {
		String name = this.data.getString("source_name");
		String nickname = NeoForgeTeleporterRenameScreen.nickname(this.data);
		if (name.isBlank())
			name = "Unnamed";
		return nickname.isBlank() ? name : name + " (" + nickname + ")";
	}

	private Comparator<CompoundTag> comparator() {
		Comparator<CompoundTag> comparator = switch (this.sort) {
		case NAME -> Comparator.comparing(entry -> entry.getString("name").isBlank() ? entry.getString("color") : entry.getString("name"), String.CASE_INSENSITIVE_ORDER);
		case OWNER -> Comparator.comparing(entry -> entry.getString("owner"), String.CASE_INSENSITIVE_ORDER);
		case DISTANCE -> Comparator.comparingDouble(this::distanceSquared);
		};
		return comparator.thenComparing(entry -> entry.getString("color"), String.CASE_INSENSITIVE_ORDER)
				.thenComparingInt(entry -> entry.getInt("x"))
				.thenComparingInt(entry -> entry.getInt("y"))
				.thenComparingInt(entry -> entry.getInt("z"));
	}

	private double distanceSquared(CompoundTag entry) {
		double x = entry.getInt("x") - this.data.getInt("source_x");
		double y = entry.getInt("y") - this.data.getInt("source_y");
		double z = entry.getInt("z") - this.data.getInt("source_z");
		return x * x + y * y + z * z;
	}

	private Component label(CompoundTag entry) {
		String name = entry.getString("name");
		if (name.isBlank())
			name = entry.getString("color");
		String owner = entry.getString("owner");
		if (owner.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"))
			owner = "*";
		return Component.literal(name + "  ")
				.append(Component.literal("[ " + owner + " ]").withStyle(Style.EMPTY.withColor(0x888888)))
				.append(Component.literal("  "))
				.append(Component.literal(Integer.toString(entry.getInt("x"))).withStyle(Style.EMPTY.withColor(0xFF5555)))
				.append(Component.literal(" "))
				.append(Component.literal(Integer.toString(entry.getInt("y"))).withStyle(Style.EMPTY.withColor(0x55FF55)))
				.append(Component.literal(" "))
				.append(Component.literal(Integer.toString(entry.getInt("z"))).withStyle(Style.EMPTY.withColor(0x5555FF)));
	}

	private final class SourceLabel extends AbstractWidget {
		private SourceLabel(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty());
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, Component.literal("From: " + NeoForgeTeleporterPickerScreen.this.sourceLabel()));
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int color = this.isHovered ? 0xFFFFFF : 0xDDDDDD;
			graphics.drawCenteredString(NeoForgeTeleporterPickerScreen.this.font, "From: " + NeoForgeTeleporterPickerScreen.this.sourceLabel(),
					this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, color);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!this.active || !this.visible || button != 0 || mouseX < this.getX() || mouseX >= this.getX() + this.width || mouseY < this.getY() || mouseY >= this.getY() + this.height)
				return false;
			NeoForgeTeleporterPickerScreen.this.openRename();
			return true;
		}
	}

	private final class CategoryLabel extends AbstractWidget {
		private CategoryLabel(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty());
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, NeoForgeTeleporterPickerScreen.this.categoryLabel());
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, NeoForgeTeleporterPickerScreen.this.categoryBackground());
			String pageText = (NeoForgeTeleporterPickerScreen.this.page + 1) + "/" + NeoForgeTeleporterPickerScreen.this.pageCount();
			int pageAreaWidth = NeoForgeTeleporterPickerScreen.this.font.width(pageText) + 8;
			int textY = this.getY() + (this.height - 8) / 2;
			Component nameComponent = Component.literal(NeoForgeTeleporterPickerScreen.this.categoryBaseName())
					.withStyle(Style.EMPTY.withColor(NeoForgeTeleporterPickerScreen.this.categoryColor()));
			NeoForgeTeleporterPickerScreen.this.renderScrollingText(graphics, nameComponent, this.getX() + 4, textY,
					this.width - pageAreaWidth - 8, this.height);
			graphics.drawString(NeoForgeTeleporterPickerScreen.this.font, pageText, this.getX() + this.width - pageAreaWidth + 4, textY, 0xAAAAAA);
		}
	}

	private final class DestinationList extends AbstractWidget {
		private final List<CompoundTag> entries;

		private DestinationList(int x, int y, int width, int height, List<CompoundTag> entries) {
			super(x, y, width, height, Component.empty());
			this.entries = entries;
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, this.getMessage());
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xAA10151B);
			for (int i = NeoForgeTeleporterPickerScreen.this.page * 10; i < Math.min(NeoForgeTeleporterPickerScreen.this.page * 10 + 10, this.entries.size()); i++) {
				int row = i - NeoForgeTeleporterPickerScreen.this.page * 10;
				int rowY = this.getY() + row * 15;
				boolean hovered = mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= rowY && mouseY < rowY + 15;
				boolean selectedRow = NeoForgeTeleporterPickerScreen.this.selected == i;
				if (selectedRow || hovered)
					graphics.fill(this.getX() + 1, rowY, this.getX() + this.width - 1, rowY + 14, selectedRow ? 0xAA3F6B85 : 0x66556666);
				graphics.drawString(NeoForgeTeleporterPickerScreen.this.font, NeoForgeTeleporterPickerScreen.this.label(this.entries.get(i)), this.getX() + 6, rowY + 3, 0xFFFFFF);
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!this.active || !this.visible || button != 0 || mouseX < this.getX() || mouseX >= this.getX() + this.width || mouseY < this.getY() || mouseY >= this.getY() + this.height)
				return false;
			int row = (int) ((mouseY - this.getY()) / 15);
			int index = NeoForgeTeleporterPickerScreen.this.page * 10 + row;
			if (index < this.entries.size()) {
				boolean alreadySelected = NeoForgeTeleporterPickerScreen.this.selected == index;
				NeoForgeTeleporterPickerScreen.this.selected = index;
				if (alreadySelected)
					NeoForgeTeleporterPickerScreen.this.confirm();
				return true;
			}
			return false;
		}
	}

	private void confirm() {
		List<CompoundTag> visible = this.visibleEntries();
		if (this.selected < 0 || this.selected >= visible.size())
			return;
		CompoundTag entry = visible.get(this.selected);
		CompoundTag selection = new CompoundTag();
		selection.putString("source_color", this.data.getString("source_color"));
		selection.putInt("source_x", this.data.getInt("source_x"));
		selection.putInt("source_y", this.data.getInt("source_y"));
		selection.putInt("source_z", this.data.getInt("source_z"));
		selection.putString("target_color", entry.getString("color"));
		selection.putInt("target_x", entry.getInt("x"));
		selection.putInt("target_y", entry.getInt("y"));
		selection.putInt("target_z", entry.getInt("z"));
		PacketDistributor.sendToServer(new TeleporterSelectionPayload(selection));
		this.onClose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
		if (this.visibleEntries().isEmpty())
			graphics.drawCenteredString(this.font, "No available destinations", this.width / 2, this.height / 2, 0xFF5555);
	}
}
