package com.pockyl.handson_delight;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
public final class KitchenNbt {
    private KitchenNbt() {}
    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        return stack.isEmpty() ? new CompoundTag() : (CompoundTag)ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),stack).getOrThrow();
    }
    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),tag).result().orElse(ItemStack.EMPTY);
    }
}
