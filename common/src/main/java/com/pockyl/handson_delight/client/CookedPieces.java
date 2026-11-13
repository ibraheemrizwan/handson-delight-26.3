package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import com.pockyl.diegesis.api.client.Floater;
import com.pockyl.handson_delight.KitchenTags;
import com.pockyl.handson_delight.platform.Platform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What swims in a soup: the ingredients of its recipe, cooked and cut up, read from the recipes so that modded soups work. A meal says
 * nothing about what went in, and in a pot that was left and found again the ingredients are long gone.
 */
final class CookedPieces {
    private static final float BIG = 1.65F;
    private static final float SMALL = 1.2F;
    private static final float HERB = 1.3F;
    private static final float FAT = 0.75F;
    private static final int EGG_WHITE = 0xF4F1E6;
    private static final int EGG_YOLK = 0xF2C230;
    /** Grains of rice: this many, this small. */
    private static final int GRAINS = 3;
    private static final float GRAIN = 0.75F;
    /** Pasta floats in strands; Farmer's Delight's tag, and the common one. */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> PASTA = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "foods/pasta"));
    private static final float FAT_LIGHT = 0.55F;
    /** Fat floats on everything that is not clear: one drop for so many pieces of food. */
    private static final int PIECES_PER_DROP = 2;

    private CookedPieces() {
    }

    /** The pieces of {@code meal}; {@code broth} is the soup's RGB, for the drops of fat on it. */
    static List<Floater> of(Level level, ItemStack meal, int broth) {
        List<Floater> pieces = new ArrayList<>();
        for (Item item : ingredients(level, meal)) {
            cut(new ItemStack(item), pieces);
        }
        int food = pieces.size();
        int fat = FoodColors.lighter(broth, FAT_LIGHT);
        for (int i = 0; i < food / PIECES_PER_DROP; i++) {
            pieces.add(i * (PIECES_PER_DROP + 1) + PIECES_PER_DROP, new Floater(Floater.DROP, fat, FAT, false));
        }
        return pieces;
    }

    private static Set<Item> ingredients(Level level, ItemStack meal) {
        Set<Item> found = new LinkedHashSet<>();
        for (RecipeHolder<CookingPotRecipe> holder : Platform.INSTANCE.cookingRecipes(level)) {
            CookingPotRecipe recipe = holder.value();
            if (KitchenRecipes.result(recipe).is(meal.getItem())) {
                for (Ingredient ingredient : recipe.input()) {
                    ItemStack[] choices = KitchenRecipes.choices(ingredient);
                    // A bucket of milk leaves its bucket: it is no food to find in the soup; nor is sugar, which dissolves, nor a
                    // bone, which is taken out again.
                    if (choices.length > 0 && choices[0].getCraftingRemainder() == null && !leavesNoPieces(choices[0])) {
                        found.add(cooked(level, choices[0]).getItem());
                    }
                }
                break;
            }
        }
        return found;
    }

    private static boolean leavesNoPieces(ItemStack stack) {
        return KitchenLooks.isPoured(stack) || KitchenTags.is(stack, KitchenTags.DISSOLVES) || KitchenTags.is(stack, KitchenTags.STRAINED);
    }

    /** What a furnace makes of it, if that is still food: a steak of raw beef; a carrot stays a carrot. */
    private static ItemStack cooked(Level level, ItemStack raw) {
        return KitchenRecipes.find(level, RecipeType.SMELTING, new SingleRecipeInput(raw))
                .map(recipe -> KitchenRecipes.result(recipe.value()))
                .filter(stack -> stack.has(DataComponents.FOOD))
                .orElse(raw);
    }

    /** Meat goes in lumps, greens in leaves and strips, the rest in slices and small pieces, all in the food's main colour. */
    private static void cut(ItemStack food, List<Floater> pieces) {
        if (KitchenLooks.isCracked(food)) {
            // An egg stirred into a hot soup: ribbons of white and bits of yolk, never slices of shell.
            pieces.add(new Floater(Floater.SHRED, EGG_WHITE, HERB));
            pieces.add(new Floater(Floater.NUGGET, EGG_YOLK, SMALL));
            return;
        }
        int color = FoodColors.main(food);
        if (KitchenTags.is(food, KitchenTags.GRAINS)) {
            // Rice and the like: many small grains.
            for (int i = 0; i < GRAINS; i++) {
                pieces.add(new Floater(Floater.NUGGET, color, GRAIN));
            }
            return;
        }
        if (KitchenTags.is(food, PASTA)) {
            pieces.add(new Floater(Floater.SHRED, color, HERB));
            pieces.add(new Floater(Floater.SHRED, color, HERB));
            return;
        }
        if (food.is(ItemTags.MEAT)) {
            pieces.add(new Floater(Floater.CHUNK, color, BIG));
            pieces.add(new Floater(Floater.NUGGET, color, SMALL));
        } else if (FoodColors.isGreen(color)) {
            pieces.add(new Floater(Floater.LEAF, color, HERB));
            pieces.add(new Floater(Floater.SHRED, color, HERB));
        } else {
            pieces.add(new Floater(Floater.COIN, color, BIG * 0.9F));
            pieces.add(new Floater(Floater.NUGGET, color, SMALL));
        }
    }
}

