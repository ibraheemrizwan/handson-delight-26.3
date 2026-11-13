package com.pockyl.handson_delight.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Meals without a bowl have no template; REI needs an empty ingredient for that slot. */
@Pseudo
@Mixin(targets="vectorwing.farmersdelight.integration.rei.display.CookingPotDisplay",remap=false)
abstract class FarmersReiContainerMixin {
    @WrapOperation(method="<init>(Lnet/minecraft/world/item/crafting/RecipeHolder;)V",at=@At(value="INVOKE",target="Lme/shedaniel/rei/api/common/util/EntryIngredients;of(Lnet/minecraft/world/item/ItemStackTemplate;)Lme/shedaniel/rei/api/common/entry/EntryIngredient;"))
    private static EntryIngredient handson$optionalContainer(ItemStackTemplate template, Operation<EntryIngredient> original) {
        return template==null ? EntryIngredient.empty() : original.call(template);
    }
}
