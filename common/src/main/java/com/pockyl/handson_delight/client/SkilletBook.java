package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.block.SkilletBlock;

import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.handson_delight.station.SkilletCooking;
import com.pockyl.handson_delight.station.SkilletStation;

import java.util.ArrayList;
import java.util.List;

/** The skillet's book: what the campfire cooks, and how long one takes in the pan, both sides, at the station's pace. */
final class SkilletBook implements RecipeBook.Source {
    @Override
    public List<RecipeBook.Page> pages(StationView view) {
        Level level = view.level();
        List<RecipeBook.Page> pages = new ArrayList<>();
        for (RecipeHolder<CampfireCookingRecipe> holder : KitchenRecipes.all(level, RecipeType.CAMPFIRE_COOKING)) {
            CampfireCookingRecipe recipe = holder.value();
            ItemStack dish = KitchenRecipes.result(recipe);
            if (dish.isEmpty() || recipe.input().isEmpty()) {
                continue;
            }
            int ticks = 2 * SkilletCooking.sideTicks(SkilletBlock.getSkilletCookingTime(recipe.cookingTime(), 0), 1);
            Component seconds = Component.translatable("handson_delight.book.seconds", Math.round(ticks / 20.0F));
            pages.add(new RecipeBook.Page(holder.id().identifier(), dish.copy(), List.of(recipe.input()),
                    List.of(new ItemStack(Items.CLOCK)), seconds, false));
        }
        return pages;
    }

    @Override
    public List<ItemStack> inStation(StationView view) {
        return List.of(view.state().getItem(SkilletStation.PAN_SLOT));
    }

    /** What fries in the pan. */
    @Override
    public Object current(StationView view) {
        return SkilletCooking.holder(view.level(), view.state().getItem(SkilletStation.PAN_SLOT)).map(holder -> holder.id().identifier()).orElse(null);
    }
}
