package com.pockyl.handson_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.CookingPotBlock;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

import com.pockyl.handson_delight.client.HandsOnDelightClient;
import com.pockyl.handson_delight.platform.Platform;

/** The pot's own boiling sounds come at random and know nothing of what is in it: a scene showing a pot with food has its own, an empty pot keeps them. */
@Mixin(CookingPotBlock.class)
abstract class CookingPotBlockMixin {
    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void handson$quietWhileShown(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (HandsOnDelightClient.isShown(pos) && level.getBlockEntity(pos) instanceof CookingPotBlockEntity pot && handson$hasFood(pot)) {
            callback.cancel();
        }
    }

    private static boolean handson$hasFood(CookingPotBlockEntity pot) {
        for (int slot = 0; slot <= CookingPotBlockEntity.MEAL_DISPLAY_SLOT; slot++) {
            if (!Platform.INSTANCE.getItem(pot, slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
