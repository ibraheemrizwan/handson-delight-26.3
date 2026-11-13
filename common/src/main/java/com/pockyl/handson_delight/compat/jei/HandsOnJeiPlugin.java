package com.pockyl.handson_delight.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.assembly.BoardDishes;
import com.pockyl.handson_delight.compat.AssemblyLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI shows the sandwiches as put together on the cutting board, and no longer in the crafting table, where a player cannot make
 * them. Found by its annotation on NeoForge, by the {@code jei_mod_plugin} entry point on Fabric.
 */
@JeiPlugin
public final class HandsOnJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return HandsOnDelight.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AssemblyCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AssemblyCategory.TYPE, BoardDishes.all());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AssemblyLayout.icon(), AssemblyCategory.TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var recipes = com.pockyl.handson_delight.KitchenRecipes.access(Minecraft.getInstance().level);
        List<RecipeHolder<CraftingRecipe>> crafting = new ArrayList<>();
        for (Identifier id : BoardDishes.ids()) {
            java.util.Optional.ofNullable(recipes.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,id))).ifPresent(holder -> {
                if (holder.value() instanceof CraftingRecipe recipe) {
                    crafting.add(new RecipeHolder<>(holder.id(), recipe));
                }
            });
        }
        if (!crafting.isEmpty()) {
            runtime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, crafting);
        }
    }
}
