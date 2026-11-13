package com.pockyl.handson_delight.fabric;

import com.pockyl.handson_delight.KitchenRecipes;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;
import vectorwing.farmersdelight.refabricated.inventory.ItemStackHandler;
import vectorwing.farmersdelight.refabricated.inventory.RecipeWrapper;

import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.assembly.BoardDishes;
import com.pockyl.handson_delight.platform.Platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Farmer's Delight Refabricated: the pot keeps its items in Refabricated's own port of NeoForge's item handler. */
public final class FabricPlatform implements Platform {
    @Override
    public ItemStack getItem(CookingPotBlockEntity pot, int slot) {
        return pot.getInventory().getStackInSlot(slot);
    }

    @Override
    public void setItem(CookingPotBlockEntity pot, int slot, ItemStack stack) {
        pot.getInventory().setStackInSlot(slot, stack);
    }

    @Override
    public Optional<RecipeHolder<CookingPotRecipe>> findRecipe(Level level, List<ItemStack> items) {
        ItemStackHandler slots = new ItemStackHandler(CookingPotBlockEntity.INVENTORY_SIZE) {
            @Override public it.unimi.dsi.fastutil.ints.IntList getInputSlotIndexes() {
                return it.unimi.dsi.fastutil.ints.IntList.of(0, 1, 2, 3, 4, 5);
            }
        };
        for (int i = 0; i < Math.min(items.size(), CookingPotBlockEntity.MEAL_DISPLAY_SLOT); i++) {
            slots.setStackInSlot(i, items.get(i).copy());
        }
        return KitchenRecipes.find(level, ModRecipeTypes.COOKING.get(), new RecipeWrapper(slots));
    }

    @Override
    public List<RecipeHolder<CookingPotRecipe>> cookingRecipes(Level level) {
        return KitchenRecipes.all(level, ModRecipeTypes.COOKING.get());
    }

    @Override
    public ItemStack getItem(CuttingBoardBlockEntity board) {
        return board.getInventory().getStackInSlot(0);
    }

    @Override
    public void setItem(CuttingBoardBlockEntity board, ItemStack stack) {
        board.getInventory().setStackInSlot(0, stack);
    }

    @Override
    public void setItem(SkilletBlockEntity skillet, ItemStack stack) {
        skillet.getInventory().setStackInSlot(0, stack);
    }

    @Override
    public ItemStack getItem(AbstractStoveBlockEntity stove, int slot) {
        return stove.getItems().getStackInSlot(slot);
    }

    @Override
    public void setItem(AbstractStoveBlockEntity stove, int slot, ItemStack stack) {
        stove.getItems().setStackInSlot(slot, stack);
    }

    @Override
    public boolean isFarmersDelightAddon(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> mod.getMetadata().getDependencies().stream()
                        .anyMatch(dependency -> dependency.getKind() == ModDependency.Kind.DEPENDS
                                && dependency.getModId().equals(FarmersDelightContent.MOD_ID)))
                .orElse(false);
    }

    /** The client names the channels it listens on while it configures, before it joins the world. */
    @Override
    public boolean hasThisMod(ServerPlayer player) {
        // No connection, or one that goes nowhere (game tests): no client.
        return player instanceof FakePlayer || player.connection == null || player.connection.getRemoteAddress() == null
                || ServerPlayNetworking.canSend(player, BoardDishes.Payload.TYPE);
    }

    @Override
    public void sendBoardDishes(ServerPlayer player) {
        if (player.connection != null && ServerPlayNetworking.canSend(player, BoardDishes.Payload.TYPE)) {
            ServerPlayNetworking.send(player, BoardDishes.payload());
        }
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
