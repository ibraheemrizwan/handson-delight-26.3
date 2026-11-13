package com.pockyl.handson_delight.station;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * The layers of a dish being put together, kept in the cutting board itself (a mixin on Farmer's Delight's block entity): they stay
 * when the cook walks away, everybody passing by sees them, and they drop with the board.
 */
public interface BoardLayers {
    NonNullList<ItemStack> handson$layers();

    /** Saves the board and tells the clients around. */
    void handson$layersChanged();
}
