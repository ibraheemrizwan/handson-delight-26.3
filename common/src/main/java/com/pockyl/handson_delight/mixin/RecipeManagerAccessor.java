package com.pockyl.handson_delight.mixin;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {
    @Accessor("recipes") RecipeMap handson$recipes();
    @org.spongepowered.asm.mixin.Mutable @Accessor("recipes") void handson$setRecipes(RecipeMap recipes);
}
