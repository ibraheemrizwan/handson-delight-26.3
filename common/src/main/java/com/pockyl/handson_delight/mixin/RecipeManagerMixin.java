package com.pockyl.handson_delight.mixin;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import java.util.*;
@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @org.spongepowered.asm.mixin.Unique private HolderLookup.Provider handson$registries;
    @Inject(method="<init>",at=@At("RETURN"))
    private void handson$remember(HolderLookup.Provider registries, CallbackInfo ci) { handson$registries=registries; }
    @Inject(method="finalizeRecipeLoading",at=@At("TAIL"))
    private void handson$collect(CallbackInfo ci) {
        var registries=handson$registries;
        Map<Identifier,JsonElement> json=new HashMap<>();
        for(var holder:((RecipeManager)(Object)this).getRecipes()) {
            Recipe.DIRECT_CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE),holder.value())
                .result().ifPresent(value->json.put(holder.id().identifier(),value));
        }
        Map<Identifier,JsonObject> found=new HashMap<>();
        AssemblyRecipes.collect(json,found);
        AssemblyRecipes.load(found,json::containsKey,registries);
    }
}
