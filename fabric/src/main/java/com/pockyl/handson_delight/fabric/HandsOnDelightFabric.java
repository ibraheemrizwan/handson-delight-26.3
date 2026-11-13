package com.pockyl.handson_delight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.assembly.BoardDishes;

/** Fabric entry point, both sides. */
public final class HandsOnDelightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HandsOnDelight.init();
        net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(net.minecraft.world.item.crafting.SmeltingRecipe.SERIALIZER);
        net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(net.minecraft.world.item.crafting.CampfireCookingRecipe.SERIALIZER);
        net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(net.minecraft.world.item.crafting.ShapedRecipe.SERIALIZER);
        net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(net.minecraft.world.item.crafting.ShapelessRecipe.SERIALIZER);
        PayloadTypeRegistry.clientboundPlay().register(BoardDishes.Payload.TYPE, BoardDishes.Payload.CODEC);
    }
}
