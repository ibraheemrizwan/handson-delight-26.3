package com.pockyl.handson_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.handson_delight.station.SkilletStation;

/**
 * A campfire's smoke rises straight through a skillet standing on it, and from the camera of the skillet's station the big puffs
 * cover the food. While the local player cooks there the campfire under it keeps its smoke; everybody else sees it as before.
 */
@Mixin(CampfireBlock.class)
abstract class CampfireSmokeMixin {
    @Inject(method = "makeParticles", at = @At("HEAD"), cancellable = true)
    private static void handson$clearOverTheSkillet(Level level, BlockPos pos, boolean signal, boolean extraSmoke, CallbackInfo callback) {
        BlockPos above = pos.above();
        if (DiegesisClient.focusedStation().filter(station -> station.pos().equals(above) && station.type().id().equals(SkilletStation.ID))
                .isPresent()) {
            callback.cancel();
        }
    }
}
