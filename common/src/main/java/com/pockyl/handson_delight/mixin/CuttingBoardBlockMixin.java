package com.pockyl.handson_delight.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import com.pockyl.handson_delight.station.BoardLayers;
/** Drop the persistent assembly before the block entity is removed. */
@Mixin(CuttingBoardBlockEntity.class)
abstract class CuttingBoardBlockMixin {
    @Inject(method="preRemoveSideEffects",at=@At("HEAD"))
    private void handson$dropLayers(BlockPos pos, BlockState state, CallbackInfo ci) {
        var self=(CuttingBoardBlockEntity)(Object)this;
        var board=(BoardLayers)this;
        if(self.getLevel()!=null && !self.getLevel().isClientSide()) {
            Containers.dropContents(self.getLevel(),pos,board.handson$layers());
            board.handson$layers().clear();
        }
    }
}
