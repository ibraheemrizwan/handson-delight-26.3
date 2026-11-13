package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.handson_delight.platform.Platform;
import com.pockyl.handson_delight.station.CookingPotStation;
import com.pockyl.handson_delight.station.PotLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The pot's book: Farmer's Delight's cooking recipes, the dish's container and its time at the station's pace, and its stars. */
final class PotBook implements RecipeBook.Source {
    @Override
    public List<RecipeBook.Page> pages(StationView view) {
        Level level = view.level();
        List<RecipeBook.Page> pages = new ArrayList<>();
        for (RecipeHolder<CookingPotRecipe> holder : Platform.INSTANCE.cookingRecipes(level)) {
            CookingPotRecipe recipe = holder.value();
            List<Ingredient> ingredients = recipe.input().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
            List<ItemStack> foot = new ArrayList<>();
            if (!KitchenRecipes.container(recipe).isEmpty()) {
                foot.add(KitchenRecipes.container(recipe));
            }
            foot.add(new ItemStack(Items.CLOCK));
            Component seconds = Component.translatable("handson_delight.book.seconds", Math.round(recipe.getCookTime() / 20.0F / PotLogic.speed(1)));
            pages.add(new RecipeBook.Page(holder.id().identifier(), KitchenRecipes.result(recipe), ingredients, foot, seconds, true));
        }
        return pages;
    }

    /** A dish picked in the book goes into the pot from the inventory: one set, or as many as a batch cooks with shift. */
    @Override
    public void picked(StationView view, RecipeBook.Page page) {
        CompoundTag fill = new CompoundTag();
        fill.putString(CookingPotStation.DISH, page.key().toString());
        fill.putBoolean(CookingPotStation.SCOOP_ALL, net.minecraft.client.Minecraft.getInstance().hasShiftDown());
        view.sendAction(CookingPotStation.ACTION_FILL, fill);
    }

    @Override
    public List<ItemStack> inStation(StationView view) {
        return List.of(view.state().getItems(CookingPotStation.INGREDIENTS));
    }

    /** The dish the ingredients make, or the soup in the pot. */
    @Override
    public Object current(StationView view) {
        Level level = view.level();
        Optional<RecipeHolder<CookingPotRecipe>> cooking = Platform.INSTANCE.findRecipe(level, inStation(view));
        if (cooking.isPresent()) {
            return cooking.get().id().identifier();
        }
        ItemStack meal = view.state().getItem(CookingPotStation.MEAL_SLOT);
        return meal.isEmpty() ? null : Platform.INSTANCE.cookingRecipes(level).stream()
                .filter(recipe -> KitchenRecipes.result(recipe.value()).is(meal.getItem())).map(holder -> holder.id().identifier()).findFirst()
                .orElse(null);
    }
}
