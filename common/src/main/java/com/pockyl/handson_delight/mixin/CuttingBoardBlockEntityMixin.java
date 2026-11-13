package com.pockyl.handson_delight.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.station.BoardLayers;
import com.pockyl.handson_delight.station.BoardResults;
import com.pockyl.handson_delight.station.CuttingBoardStation;

/**
 * The layers of a dish lie on the board for good: saved with it, sent to the clients in its update tag, cleared with it. Also keeps
 * the board quiet while it cuts the rest of a batch: Farmer's Delight's sound plays once, not for every item.
 */
@Mixin(CuttingBoardBlockEntity.class)
abstract class CuttingBoardBlockEntityMixin implements BoardLayers {
    @Unique
    private static final String LAYERS = HandsOnDelight.MOD_ID + ":layers";

    @Unique
    private final NonNullList<ItemStack> handson$layers = NonNullList.withSize(CuttingBoardStation.MOST_LAYERS, ItemStack.EMPTY);

    @Override
    public NonNullList<ItemStack> handson$layers() {
        return handson$layers;
    }

    @Override
    public void handson$layersChanged() {
        BlockEntity self = (BlockEntity) (Object) this;
        self.setChanged();
        if (self.getLevel() != null) {
            self.getLevel().sendBlockUpdated(self.getBlockPos(), self.getBlockState(), self.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void handson$saveLayers(net.minecraft.world.level.storage.ValueOutput tag, CallbackInfo callback) {
        if (handson$layers.stream().anyMatch(stack -> !stack.isEmpty())) {
            ContainerHelper.saveAllItems(tag.child(LAYERS), handson$layers);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void handson$loadLayers(net.minecraft.world.level.storage.ValueInput tag, CallbackInfo callback) {
        handson$layers.clear();
        ContainerHelper.loadAllItems(tag.childOrEmpty(LAYERS), handson$layers);
    }

    @Inject(method = "playProcessingSound", at = @At("HEAD"), cancellable = true, remap = false)
    private void handson$oneSoundForABatch(SoundEvent sound, ItemStack tool, ItemStack item, CallbackInfo callback) {
        if (BoardResults.isQuiet()) {
            callback.cancel();
        }
    }
}
