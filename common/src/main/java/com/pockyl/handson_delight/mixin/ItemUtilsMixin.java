package com.pockyl.handson_delight.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.utility.ItemUtils;

import com.pockyl.handson_delight.station.BoardResults;

/**
 * Farmer's Delight throws what a cut makes off the board into the world, where a cook whose camera is at the board would have to
 * walk after it. While the board's station cuts, the pieces are caught and handed to the cook instead.
 */
@Mixin(value = ItemUtils.class, remap = false)
abstract class ItemUtilsMixin {
    @Inject(method = "spawnItemEntity", at = @At("HEAD"), cancellable = true)
    private static void handson$catch(Level level, ItemStack stack, double x, double y, double z, double dx, double dy, double dz,
            CallbackInfo callback) {
        if (BoardResults.take(stack)) {
            callback.cancel();
        }
    }
}
