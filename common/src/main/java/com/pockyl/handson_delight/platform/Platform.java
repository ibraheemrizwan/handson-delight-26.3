package com.pockyl.handson_delight.platform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * What differs between Farmer's Delight on NeoForge and Farmer's Delight Refabricated on Fabric: the pot's inventory is NeoForge's
 * {@code ItemStackHandler} in one and Refabricated's own class in the other, and so is the wrapper its recipes match against.
 * Each loader project provides one implementation, listed in its {@code META-INF/services}.
 */
public interface Platform {
    Platform INSTANCE = load(Platform.class);

    ItemStack getItem(CookingPotBlockEntity pot, int slot);

    void setItem(CookingPotBlockEntity pot, int slot, ItemStack stack);

    /** The cooking recipe these items make, laid in the pot's slots from the first one on. */
    Optional<RecipeHolder<CookingPotRecipe>> findRecipe(Level level, List<ItemStack> items);

    List<RecipeHolder<CookingPotRecipe>> cookingRecipes(Level level);

    /** What lies on a cutting board: its one slot holds a stack. */
    ItemStack getItem(CuttingBoardBlockEntity board);

    void setItem(CuttingBoardBlockEntity board, ItemStack stack);

    /** What lies in a skillet: its one slot holds a stack ({@code getStoredStack} reads it in every build). */
    void setItem(SkilletBlockEntity skillet, ItemStack stack);

    /** What lies on a stove's top: six slots of one item each. */
    ItemStack getItem(AbstractStoveBlockEntity stove, int slot);

    void setItem(AbstractStoveBlockEntity stove, int slot, ItemStack stack);

    /**
     * Whether the mod {@code modId} needs Farmer's Delight: an addon, whose food recipes the stations take over too. A mod that only
     * works with it when it is there keeps its crafts.
     */
    boolean isFarmersDelightAddon(String modId);

    /**
     * Whether the player's client has this mod. A client without it cannot work at the stations: it keeps Farmer's Delight's own
     * ways. Players that are no client (game tests, the fake players of machines) count as having it.
     */
    boolean hasThisMod(ServerPlayer player);

    /** The game's config folder. */
    Path configDir();

    /** Sends {@code AssemblyRecipes.book()} to the player's client, for {@code BoardDishes}. */
    void sendBoardDishes(ServerPlayer player);

    static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, type.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + type.getName() + " for this mod loader"));
    }
}
