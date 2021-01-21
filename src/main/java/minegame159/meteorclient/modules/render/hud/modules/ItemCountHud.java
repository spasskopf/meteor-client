/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemCountHud extends HudModule {
    public final Item item;

    public ItemCountHud(HUD hud, Item item) {
        super(hud, item.getName().getString() + "Hud", "Displays the count of " + item.getName().getString() + ".");
        this.item = item;
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * hud.itemCountScale(), 16 * hud.itemCountScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if(mc.player == null || mc.currentScreen instanceof HudEditorScreen) {
            RenderUtils.drawItem(item.getDefaultStack(), (int) (x / hud.itemCountScale()), (int) (y / hud.itemCountScale()), hud.itemCountScale(), true);
        } else if(InvUtils.findItemWithCount(item).count > 0) {
            RenderUtils.drawItem(new ItemStack(item, InvUtils.findItemWithCount(item).count), (int) (x / hud.itemCountScale()), (int) (y / hud.itemCountScale()), hud.itemCountScale(), true);
        }
    }
}

