package com.pockyl.handson_delight.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.client.renderer.CuttingBoardRenderer;
import vectorwing.farmersdelight.client.renderer.state.CuttingBoardRenderState;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import com.pockyl.handson_delight.client.HandsOnDelightClient;
@Mixin(value=CuttingBoardRenderer.class,remap=false)
abstract class CuttingBoardRendererMixin {
    @Inject(method="extractRenderState(Lvectorwing/farmersdelight/common/block/entity/CuttingBoardBlockEntity;Lvectorwing/farmersdelight/client/renderer/state/CuttingBoardRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",at=@At("TAIL"))
    private void handson$sceneItems(CuttingBoardBlockEntity entity, CuttingBoardRenderState state, float partial, net.minecraft.world.phys.Vec3 camera, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay, CallbackInfo ci) {
        if(!entity.isItemCarvingBoard() && HandsOnDelightClient.isShown(entity.getBlockPos())) { state.boardStack=net.minecraft.world.item.ItemStack.EMPTY; state.displayItem.clear(); }
    }
}
