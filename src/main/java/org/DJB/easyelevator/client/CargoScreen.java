package org.DJB.easyelevator.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import org.DJB.easyelevator.logic.CargoLoad;
import org.DJB.easyelevator.screen.CargoScreenHandler;

/** 保留原版物品栏操作，沿用电梯面板的深色配色和清晰背景。 */
public class CargoScreen extends AbstractContainerScreen<CargoScreenHandler> {
    public CargoScreen(CargoScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 210;
        inventoryLabelY = 116;
    }

    @Override protected void renderBg(GuiGraphics context, float delta, int xMouse, int yMouse) {
        context.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF161D27);
        context.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF4A5A6D);
        context.fill(leftPos + 1, topPos + 1, leftPos + 175, topPos + 46, 0xFF1E2733);
        for (var slot : menu.slots) {
            context.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF4A5A6D);
            context.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF0F141B);
        }
        context.fill(leftPos + 8, topPos + 43, leftPos + 168, topPos + 45, 0xFF0F141B);
        int used = Math.min(160, menu.cargoItems() * 160 / CargoLoad.MAX_ITEMS);
        context.fill(leftPos + 8, topPos + 43, leftPos + 8 + used, topPos + 45, 0xFFFFBC62);
    }

    @Override protected void renderLabels(GuiGraphics context, int xMouse, int yMouse) {
        context.drawString(font, title, 8, 6, 0xFFEAF2FA, false);
        context.drawString(font, Component.translatable("screen.easyelevator.cargo_count", menu.cargoItems()),
                8, 19, 0xFFB7C7D9, false);
        context.drawString(font, Component.translatable("screen.easyelevator.cargo_capacity", menu.passengerLimit()),
                8, 31, 0xFFFFBC62, false);
        context.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFFB7C7D9, false);
    }

    @Override public void render(GuiGraphics context, int xMouse, int yMouse, float delta) {
        super.render(context, xMouse, yMouse, delta);
        renderTooltip(context, xMouse, yMouse);
        if (xMouse >= leftPos + 8 && xMouse < leftPos + 168 && yMouse >= topPos + 19 && yMouse < topPos + 46)
            context.renderTooltip(font, font.split(
                    Component.translatable("screen.easyelevator.cargo_rule"), 220), xMouse, yMouse);
    }

    @Override public void renderBackground(GuiGraphics context) {
        context.fill(0, 0, width, height, 0x18000000);

    }
}
