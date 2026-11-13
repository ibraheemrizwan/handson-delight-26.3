package com.pockyl.handson_delight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.StoveBlockEntity;

import com.pockyl.handson_delight.client.HandsOnDelightClient;

/**
 * Farmer's Delight puffs dark smoke over every item cooking on its stove; where the stove's scene shows the grill, its own steam and
 * smoke tell how each piece is doing, and the dark puffs read as dirt on the food.
 */
@Mixin(value = StoveBlockEntity.class, remap = false)
abstract class StoveSmokeMixin {
    @Inject(method = "addSmokeParticles", at = @At("HEAD"), cancellable = true)
    private void handson$quietWhileShown(CallbackInfo callback) {
        if (HandsOnDelightClient.isShown(((StoveBlockEntity) (Object) this).getBlockPos())) {
            callback.cancel();
        }
    }
}
