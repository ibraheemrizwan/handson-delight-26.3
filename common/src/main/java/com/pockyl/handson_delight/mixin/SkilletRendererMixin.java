package com.pockyl.handson_delight.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.client.renderer.SkilletRenderer;
import vectorwing.farmersdelight.client.renderer.state.SkilletRenderState;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;
import com.pockyl.handson_delight.client.HandsOnDelightClient;
@Mixin(value=SkilletRenderer.class,remap=false)
abstract class SkilletRendererMixin {
    @Inject(method="extractRenderState(Lvectorwing/farmersdelight/common/block/entity/SkilletBlockEntity;Lvectorwing/farmersdelight/client/renderer/state/SkilletRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",at=@At("TAIL"))
    private void handson$sceneItems(SkilletBlockEntity entity, SkilletRenderState state, float partial, net.minecraft.world.phys.Vec3 camera, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay, CallbackInfo ci) {
        if(HandsOnDelightClient.isShown(entity.getBlockPos())) { state.stack=net.minecraft.world.item.ItemStack.EMPTY; state.displayItem.clear(); }
    }
}
