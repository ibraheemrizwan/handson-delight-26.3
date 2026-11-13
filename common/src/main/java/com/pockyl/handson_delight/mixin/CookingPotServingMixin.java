package com.pockyl.handson_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.CookingPotBlock;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

/**
 * A serving taken with a bowl in the hand, without opening the pot, counts as made, as one taken from the menu's result slot does:
 * the stars of a dish open by the servings a player has made, and the quickest way to serve must not leave them shut.
 */
@Mixin(CookingPotBlock.class)
abstract class CookingPotServingMixin {
    /** The meal as it was before the use, per thread: the client of a single-player game uses the block in its own thread. */
    @Unique
    private static final ThreadLocal<ItemStack> handson$mealBefore = new ThreadLocal<>();

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void handson$rememberTheMeal(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callback) {
        handson$mealBefore.set(!level.isClientSide() && level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot ? pot.getMeal().copy()
                : ItemStack.EMPTY);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void handson$countTheServing(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack before = handson$mealBefore.get();
        handson$mealBefore.remove();
        if (before == null || before.isEmpty() || !(level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)) {
            return;
        }
        ItemStack now = pot.getMeal();
        int served = now.isEmpty() ? before.getCount() : ItemStack.isSameItemSameComponents(now, before) ? before.getCount() - now.getCount() : 0;
        if (served > 0) {
            ItemStack serving = before.copyWithCount(served);
            serving.onCraftedBy(player, served);
        }
    }
}
