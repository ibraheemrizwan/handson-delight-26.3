package com.pockyl.handson_delight.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.client.renderer.AbstractStoveRenderer;
import vectorwing.farmersdelight.client.renderer.state.StoveRenderState;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;
import com.pockyl.handson_delight.client.HandsOnDelightClient;
@Mixin(value=AbstractStoveRenderer.class,remap=false)
abstract class StoveRendererMixin {
    @Inject(method="extractRenderState(Lvectorwing/farmersdelight/common/block/entity/AbstractStoveBlockEntity;Lvectorwing/farmersdelight/client/renderer/state/StoveRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",at=@At("TAIL"))
    private void handson$sceneItems(AbstractStoveBlockEntity entity, StoveRenderState state, float partial, net.minecraft.world.phys.Vec3 camera, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay, CallbackInfo ci) {
        if(HandsOnDelightClient.isShown(entity.getBlockPos())) { for(var item:state.stoveStacks) item.clear(); }
    }
}
