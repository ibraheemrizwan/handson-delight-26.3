package com.pockyl.handson_delight.mixin;

import net.minecraft.world.inventory.ContainerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

/** The cook time the pot's menu reads: 0 = ticks done, 1 = ticks the recipe takes. Nothing else exposes it. */
@Mixin(value = CookingPotBlockEntity.class, remap = false)
public interface CookingPotBlockEntityAccessor {
    @Accessor("cookingPotData")
    ContainerData handson$data();
}
