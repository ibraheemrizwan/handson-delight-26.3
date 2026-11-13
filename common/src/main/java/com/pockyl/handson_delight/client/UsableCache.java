package com.pockyl.handson_delight.client;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What a station's drawer filter said of each kind of stack, by item and its data: the library asks for every slot every frame, and
 * the answer means going through all the recipes.
 */
final class UsableCache {
    private final Map<Object, Boolean> known = new HashMap<>();

    boolean test(ItemStack stack, Predicate<ItemStack> usable) {
        Object key = stack.getComponentsPatch().isEmpty() ? stack.getItem() : List.of(stack.getItem(), stack.getComponentsPatch());
        Boolean found = known.get(key);
        if (found == null) {
            found = usable.test(stack);
            known.put(key, found);
        }
        return found;
    }

    void clear() {
        known.clear();
    }
}
