package com.pockyl.handson_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

import com.pockyl.handson_delight.client.HandsOnDelightClient;

/** Its own steam and bubbles rise over the whole pot and would fill the camera: the scene has its own, in the soup. */
@Mixin(value = CookingPotBlockEntity.class, remap = false)
abstract class CookingPotBlockEntityMixin {
    @Inject(method = "animationTick", at = @At("HEAD"), cancellable = true)
    private static void handson$quietWhileShown(Level level, BlockPos pos, BlockState state, CookingPotBlockEntity pot, CallbackInfo callback) {
        if (HandsOnDelightClient.isShown(pos)) {
            callback.cancel();
        }
    }
}
