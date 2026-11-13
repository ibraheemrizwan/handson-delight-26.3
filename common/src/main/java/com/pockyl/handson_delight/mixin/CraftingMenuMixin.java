package com.pockyl.handson_delight.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.platform.Platform;

import java.util.Optional;

/**
 * A sandwich is put together on the cutting board, by hand: a player's crafting table and inventory grid do not make it (both go
 * through this method). Automation crafts with grids of its own (Create's crafters, AE2's assemblers, Refined Storage) and goes on
 * making it, as without this mod. So does a player whose client lacks the mod: it cannot open the board, the crafting table stays
 * theirs.
 */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
    @ModifyVariable(method = "slotChangedCraftingGrid", at = @At("STORE"), ordinal = 0)
    private static Optional<RecipeHolder<CraftingRecipe>> handson$onTheBoardInstead(Optional<RecipeHolder<CraftingRecipe>> found,
            AbstractContainerMenu menu, net.minecraft.server.level.ServerLevel level, Player player, CraftingContainer grid, ResultContainer result,
            RecipeHolder<CraftingRecipe> last) {
        if (found.isPresent() && HandsOnConfig.removeCraftingRecipes() && AssemblyRecipes.isMadeOnTheBoard(found.get().id().identifier())
                && !(player instanceof ServerPlayer server && !Platform.INSTANCE.hasThisMod(server))) {
            return Optional.empty();
        }
        return found;
    }
}
