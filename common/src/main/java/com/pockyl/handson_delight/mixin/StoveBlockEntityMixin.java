package com.pockyl.handson_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.station.Grill;
import com.pockyl.handson_delight.station.StoveCooking;
import com.pockyl.handson_delight.station.StoveTop;

/**
 * A stove grilling by hand: the doneness of each piece is kept in Farmer's Delight's block entity, saved with it and sent to the
 * clients in its update tag. Every tick, before Farmer's Delight cooks, the pieces the cook laid grill by hand and Farmer's Delight's
 * count for them stays at nothing; the others cook its way.
 */
@Mixin(AbstractStoveBlockEntity.class)
abstract class StoveBlockEntityMixin implements StoveTop {
    @Unique
    private static final String GRILL = HandsOnDelight.MOD_ID + ":grill";

    @Shadow(remap = false)
    @Final
    private int[] cookingTimes;

    @Shadow(remap = false)
    @Final
    private int[] cookingTimesTotal;

    @Unique
    private final Grill handson$grill = new Grill();

    @Override
    public Grill handson$grill() {
        return handson$grill;
    }

    @Override
    public int[] handson$progress() {
        return cookingTimes;
    }

    @Override
    public int[] handson$times() {
        return cookingTimesTotal;
    }

    @Inject(method = "serverTick", at = @At("HEAD"), remap = false)
    private static void handson$grillByHand(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state, AbstractStoveBlockEntity stove,
            CallbackInfo callback) {
        StoveCooking.tick(stove);
    }

    /** Food put on with Farmer's Delight's own right click is its to cook. */
    @Inject(method = "placeFood", at = @At("HEAD"), remap = false)
    private void handson$farmersDelightsWay(Entity entity, ItemStack stack, RecipeHolder<? extends AbstractCookingRecipe> recipe,
            CallbackInfoReturnable<Boolean> callback) {
        int place = ((AbstractStoveBlockEntity) (Object) this).getNextEmptySlot();
        if (place >= 0 && place < Grill.PLACES) {
            handson$grill.farmersDelightsWay(place);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void handson$saveGrill(net.minecraft.world.level.storage.ValueOutput tag, CallbackInfo callback) {
        CompoundTag grill = new CompoundTag();
        handson$grill.save(grill);
        if (!grill.isEmpty()) {
            tag.store(GRILL, CompoundTag.CODEC, grill);
        }
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void handson$sendGrill(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> callback) {
        CompoundTag grill = new CompoundTag();
        handson$grill.save(grill);
        callback.getReturnValue().put(GRILL, grill);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void handson$loadGrill(net.minecraft.world.level.storage.ValueInput tag, CallbackInfo callback) {
        handson$grill.load(tag.read(GRILL, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }
}
