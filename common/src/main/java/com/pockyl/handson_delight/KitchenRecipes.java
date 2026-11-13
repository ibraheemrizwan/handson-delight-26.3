package com.pockyl.handson_delight;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

/** The server recipe map and Fabric's synchronized client recipes share this access path. */
public final class KitchenRecipes {
    private KitchenRecipes() {}
    public static SynchronizedRecipes access(Level level) { return level.recipeAccess().getSynchronizedRecipes(); }
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> all(Level level, RecipeType<T> type) {
        return level.recipeAccess() instanceof RecipeManager manager ? List.copyOf(manager.getAllOfType(type)) : List.copyOf(access(level).getAllOfType(type));
    }
    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> find(Level level, RecipeType<T> type, I input) {
        return level.recipeAccess() instanceof RecipeManager manager ? manager.getRecipeFor(type,input,level) : access(level).getFirstMatch(type, input, level);
    }
    public static ItemStack[] choices(Ingredient ingredient) { return ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new); }
    public static ItemStack stack(ItemStackTemplate template) { return template == null ? ItemStack.EMPTY : template.create(); }
    public static ItemStack container(CookingPotRecipe recipe) { return stack(recipe.container()); }
    public static ItemStack result(SingleItemRecipe recipe) { return recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY)); }
    public static ItemStack result(CookingPotRecipe recipe) { return recipe.result().create(); }
    public static ItemStack result(CraftingRecipe recipe) { return recipe.assemble(CraftingInput.EMPTY); }
}
