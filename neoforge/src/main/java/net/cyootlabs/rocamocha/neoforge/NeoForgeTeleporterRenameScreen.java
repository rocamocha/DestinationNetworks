package net.cyootlabs.rocamocha.neoforge;

import java.util.HashMap;
import java.util.Map;

import net.cyootlabs.rocamocha.network.TeleporterPickerPayload;
import net.cyootlabs.rocamocha.network.TeleporterRenamePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class NeoForgeTeleporterRenameScreen extends Screen {
	private static final Map<String, String> NICKNAMES = new HashMap<>();
	private final TeleporterPickerPayload payload;
	private final CompoundTag data;
	private EditBox nameInput;

	NeoForgeTeleporterRenameScreen(TeleporterPickerPayload payload) {
		super(Component.literal(payload.data().getBoolean("source_can_rename") ? "Rename teleporter" : "Set local nickname"));
		this.payload = payload;
		this.data = payload.data();
	}

	static String nickname(CompoundTag data) {
		return NICKNAMES.getOrDefault(key(data), "");
	}

	private static String key(CompoundTag data) {
		return data.getString("source_color") + ":" + data.getInt("source_x") + ":" + data.getInt("source_y") + ":" + data.getInt("source_z");
	}

	@Override
	protected void init() {
		this.nameInput = new EditBox(this.font, this.width / 2 - 110, this.height / 2 - 10, 220, 20, Component.literal("Name"));
		this.nameInput.setMaxLength(32);
		this.nameInput.setValue(this.data.getBoolean("source_can_rename") ? this.data.getString("source_name") : nickname(this.data));
		this.addRenderableWidget(this.nameInput);
		this.setInitialFocus(this.nameInput);
		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save()).bounds(this.width / 2 - 105, this.height / 2 + 25, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.back()).bounds(this.width / 2 + 5, this.height / 2 + 25, 100, 20).build());
	}

	private void save() {
		String name = this.nameInput.getValue().trim();
		if (this.data.getBoolean("source_can_rename")) {
			CompoundTag rename = new CompoundTag();
			rename.putString("color", this.data.getString("source_color"));
			rename.putInt("x", this.data.getInt("source_x"));
			rename.putInt("y", this.data.getInt("source_y"));
			rename.putInt("z", this.data.getInt("source_z"));
			rename.putString("name", name);
			PacketDistributor.sendToServer(new TeleporterRenamePayload(rename));
			this.data.putString("source_name", name);
		} else {
			NICKNAMES.put(key(this.data), name);
		}
		this.back();
	}

	private void back() {
		this.minecraft.setScreen(new NeoForgeTeleporterPickerScreen(this.payload));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 45, 0xFFFFFF);
		if (!this.data.getBoolean("source_can_rename"))
			graphics.drawCenteredString(this.font, "Only you will see this nickname", this.width / 2, this.height / 2 + 55, 0xAAAAAA);
	}
}
