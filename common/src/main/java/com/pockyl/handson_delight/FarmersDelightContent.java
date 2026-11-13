package com.pockyl.handson_delight;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Blocks and sounds of Farmer's Delight, by id. Its own registry fields are NeoForge's {@code DeferredHolder} in one build and a
 * plain {@code Supplier} in Refabricated, so the shared code does not read them.
 */
public final class FarmersDelightContent {
    public static final String MOD_ID = "farmersdelight";

    public static final Supplier<Block> COOKING_POT = () -> BuiltInRegistries.BLOCK.get(id("cooking_pot")).orElseThrow().value();
    public static final Supplier<Block> CUTTING_BOARD = () -> BuiltInRegistries.BLOCK.get(id("cutting_board")).orElseThrow().value();
    public static final Supplier<Block> SKILLET = () -> BuiltInRegistries.BLOCK.get(id("skillet")).orElseThrow().value();
    public static final Supplier<Block> STOVE = () -> BuiltInRegistries.BLOCK.get(id("stove")).orElseThrow().value();
    public static final Supplier<RecipeType<?>> CUTTING = () -> BuiltInRegistries.RECIPE_TYPE.get(id("cutting")).orElseThrow().value();

    public static final SoundEvent BOIL_SOUP = sound("block.cooking_pot.boil_soup");
    public static final SoundEvent SKILLET_SIZZLE = sound("block.skillet.sizzle");
    public static final SoundEvent SKILLET_ADD_FOOD = sound("block.skillet.add_food");
    public static final SoundEvent KNIFE_CUT = sound("block.cutting_board.knife_cut");
    public static final SoundEvent BOARD_PLACE = sound("block.cutting_board.place_item");
    public static final SoundEvent BOARD_REMOVE = sound("block.cutting_board.remove_item");

    private FarmersDelightContent() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** A sound event plays by its id, so one made here sounds the same as the registered one. */
    private static SoundEvent sound(String path) {
        return SoundEvent.createVariableRangeEvent(id(path));
    }
}
