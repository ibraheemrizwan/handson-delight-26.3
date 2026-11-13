package com.pockyl.handson_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.station.Frying;
import com.pockyl.handson_delight.station.FryingPan;
import com.pockyl.handson_delight.station.SkilletCooking;

/**
 * Food put in at the station fries by hand: how far each side is done lives in the skillet, saved with it and sent to the clients in
 * its update tag, and Farmer's Delight's own cooking stays out of it. Food put in with Farmer's Delight's right click cooks its way.
 */
@Mixin(SkilletBlockEntity.class)
abstract class SkilletBlockEntityMixin implements FryingPan {
    @Unique
    private static final String FRYING = HandsOnDelight.MOD_ID + ":frying";

    @Shadow(remap = false)
    private int fireAspectLevel;

    @Unique
    private final Frying handson$frying = new Frying();

    @Override
    public Frying handson$frying() {
        return handson$frying;
    }

    @Override
    public int handson$fireAspect() {
        return fireAspectLevel;
    }

    @Inject(method = "cookAndOutputItems", at = @At("HEAD"), cancellable = true, remap = false)
    private void handson$fryByHand(ItemStack stack, net.minecraft.server.level.ServerLevel level, CallbackInfo callback) {
        if (SkilletCooking.fry((SkilletBlockEntity) (Object) this, stack)) {
            callback.cancel();
        }
    }

    @Inject(method = "cookingTick", at = @At("HEAD"), remap = false)
    private static void handson$startOverWhenEmpty(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state, SkilletBlockEntity skillet,
            CallbackInfo callback) {
        SkilletCooking.check(skillet);
    }

    /** Food right-clicked into an empty skillet is Farmer's Delight's to cook. */
    @Inject(method = "addItemToCook", at = @At("HEAD"), remap = false)
    private void handson$farmersDelightsWay(ItemStack stack, Player player, net.minecraft.server.level.ServerLevel level, CallbackInfoReturnable<ItemStack> callback) {
        if (((SkilletBlockEntity) (Object) this).getStoredStack().isEmpty()) {
            handson$frying.farmersDelightsWay();
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void handson$saveFrying(net.minecraft.world.level.storage.ValueOutput tag, CallbackInfo callback) {
        CompoundTag frying = new CompoundTag();
        handson$frying.save(frying);
        if (!frying.isEmpty()) {
            tag.store(FRYING, CompoundTag.CODEC, frying);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void handson$loadFrying(net.minecraft.world.level.storage.ValueInput tag, CallbackInfo callback) {
        handson$frying.load(tag.read(FRYING, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }
}
