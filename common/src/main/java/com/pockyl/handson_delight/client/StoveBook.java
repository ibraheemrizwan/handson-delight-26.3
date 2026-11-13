package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.handson_delight.station.Grill;
import com.pockyl.handson_delight.station.SkilletCooking;
import com.pockyl.handson_delight.station.StoveCooking;
import com.pockyl.handson_delight.station.StoveStation;

import java.util.ArrayList;
import java.util.List;

/** The stove's book: what the fire cooks, each with the time a piece takes on the grill at the station's pace, both sides. */
final class StoveBook implements RecipeBook.Source {
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
            int ticks = 2 * StoveCooking.sideTicks(recipe.cookingTime());
            Component seconds = Component.translatable("handson_delight.book.seconds", Math.round(ticks / 20.0F));
            pages.add(new RecipeBook.Page(holder.id().identifier(), dish.copy(), List.of(recipe.input()),
                    List.of(new ItemStack(Items.CLOCK)), seconds, false));
        }
        return pages;
    }

    @Override
    public List<ItemStack> inStation(StationView view) {
        List<ItemStack> there = new ArrayList<>();
        for (int place = 0; place < Grill.PLACES; place++) {
            there.add(view.state().getItem(StoveStation.place(place)));
        }
        return there;
    }

    /** What grills on the first place that holds raw food. */
    @Override
    public Object current(StationView view) {
        for (ItemStack stack : inStation(view)) {
            Object id = SkilletCooking.holder(view.level(), stack).map(holder -> holder.id().identifier()).orElse(null);
            if (id != null) {
                return id;
            }
        }
        return null;
    }
}
