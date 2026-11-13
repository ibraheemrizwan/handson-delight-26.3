package com.pockyl.handson_delight.assembly;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.List;

/**
 * A 3 x 3 crafting grid the board lays a dish's layers in, one item each where the recipe wants it, so the recipe itself makes the
 * dish and says what is left of its ingredients: a bucket of milk gives back the bucket, FD's dough keeps its water. No player's
 * menu owns it: the crafting table's rule does not apply.
 */
final class AssemblyGrid {
    private static final int SIZE = 3;

    /** The recipe's input: the grid cut down to what it holds, as the crafting table cuts it. */
    private final CraftingInput.Positioned positioned;

    /** One item of every layer, layer i where its ingredient lies in the recipe. */
    AssemblyGrid(AssemblyRecipes.Assembly dish, List<ItemStack> layers, int[] taken) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE * SIZE, ItemStack.EMPTY);
        for (int layer = 0; layer < layers.size(); layer++) {
            items.set(dish.slots()[taken[layer]], layers.get(layer).copyWithCount(1));
        }
        positioned = CraftingInput.ofPositioned(SIZE, SIZE, items);
    }

    CraftingInput input() {
        return positioned.input();
    }

    /** The grid slot of layer {@code layer}. */
    static int slot(AssemblyRecipes.Assembly dish, int[] taken, int layer) {
        return dish.slots()[taken[layer]];
    }

    /** What the recipe left in grid slot {@code slot}, out of its remainders, which are listed for the cut-down input. */
    ItemStack remainder(List<ItemStack> remainders, int slot) {
        int column = slot % SIZE - positioned.left();
        int row = slot / SIZE - positioned.top();
        CraftingInput input = positioned.input();
        if (column < 0 || row < 0 || column >= input.width() || row >= input.height()) {
            return ItemStack.EMPTY;
        }
        int index = row * input.width() + column;
        return index < remainders.size() ? remainders.get(index) : ItemStack.EMPTY;
    }
}
