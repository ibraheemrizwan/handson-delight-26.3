package com.pockyl.handson_delight.assembly;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A dish made of the layers on the board by its own recipe, round after round as the crafting table would make it: each round takes
 * an item of every layer, and what the recipe leaves of it (a bucket, a bottle) goes back onto that layer when it is the same thing
 * (FD's dough keeps its water) or to the cook.
 */
public final class Assembling {
    private Assembling() {
    }

    /** What the rounds made: the dishes, what is left of every layer, and what the recipe left that is no layer's. */
    public record Made(List<ItemStack> dishes, List<ItemStack> layers, List<ItemStack> leftovers, int rounds) {
    }

    /** Up to {@code most} rounds of {@code dish} from {@code layers} (bottom first, not changed). */
    public static Made make(AssemblyRecipes.Assembly dish, List<ItemStack> layers, Level level, int most) {
        List<ItemStack> left = new ArrayList<>();
        layers.forEach(layer -> left.add(layer.copy()));
        List<ItemStack> dishes = new ArrayList<>();
        List<ItemStack> leftovers = new ArrayList<>();
        int[] taken = AssemblyRecipes.assign(dish, left, true);
        int rounds = 0;
        while (taken != null && rounds < most && left.stream().noneMatch(ItemStack::isEmpty)) {
            AssemblyGrid grid = new AssemblyGrid(dish, left, taken);
            if (!dish.recipe().matches(grid.input(), level)) {
                break;
            }
            ItemStack made = dish.recipe().assemble(grid.input());
            if (made.isEmpty()) {
                break;
            }
            NonNullList<ItemStack> remainders = dish.recipe().getRemainingItems(grid.input());
            for (int layer = 0; layer < left.size(); layer++) {
                ItemStack stack = left.get(layer);
                ItemStack remainder = grid.remainder(remainders, AssemblyGrid.slot(dish, taken, layer));
                stack.shrink(1);
                if (remainder.isEmpty()) {
                    continue;
                }
                if (stack.isEmpty()) {
                    left.set(layer, remainder.copy());
                } else if (ItemStack.isSameItemSameComponents(stack, remainder)
                        && stack.getCount() + remainder.getCount() <= stack.getMaxStackSize()) {
                    stack.grow(remainder.getCount());
                } else {
                    leftovers.add(remainder.copy());
                }
            }
            add(dishes, made);
            rounds++;
        }
        // A layer that turned into something else (milk's bucket) is no ingredient any more: it goes to the cook.
        for (int layer = 0; layer < left.size(); layer++) {
            ItemStack stack = left.get(layer);
            if (!stack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, layers.get(layer))) {
                leftovers.add(stack);
                left.set(layer, ItemStack.EMPTY);
            }
        }
        return new Made(dishes, left, leftovers, rounds);
    }

    /** Adds {@code stack} to the first stack it joins, else as a new one. */
    private static void add(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack there : stacks) {
            if (ItemStack.isSameItemSameComponents(there, stack)) {
                there.grow(stack.getCount());
                return;
            }
        }
        stacks.add(stack.copy());
    }
}
