package com.pockyl.handson_delight;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item tags that say how food behaves in the kitchen, so that what happens at a station is what a cook would see. Each has
 * defaults for Farmer's Delight; packs and addons add their own items.
 */
public final class KitchenTags {
    /** Cut in even slices square across its length: a roll. */
    public static final TagKey<Item> SLICED_STRAIGHT = tag("sliced_straight");
    /** Cut in wedges through its middle: a pie, a cake, a melon, a pumpkin. */
    public static final TagKey<Item> CUT_IN_WEDGES = tag("cut_in_wedges");
    /** Cut in thin strips side by side: dough into pasta. */
    public static final TagKey<Item> CUT_IN_STRIPS = tag("cut_in_strips");
    /** A liquid in its container: poured out, the container never lies in the food. */
    public static final TagKey<Item> POURED = tag("poured");
    /** Cracked open before it goes in: the shell never goes into the food. */
    public static final TagKey<Item> CRACKED = tag("cracked");
    /** Dissolves in a soup: no pieces of it float there. */
    public static final TagKey<Item> DISSOLVES = tag("dissolves");
    /** Cooks in a soup whole and is taken out again: no pieces of it float there. */
    public static final TagKey<Item> STRAINED = tag("strained");
    /** Grains: many small pieces in a soup. */
    public static final TagKey<Item> GRAINS = tag("grains");
    /** Runs through a grill's bars: the stove's grill does not take it. */
    public static final TagKey<Item> NOT_ON_GRILL = tag("not_on_grill");
    /**
     * Fried on one side only, never turned: an egg, sunny side up. It cooks through from below in the time both sides of other
     * food take, and never waits to be tossed or turned.
     */
    public static final TagKey<Item> ONE_SIDED = tag("one_sided");
    /** A dish on a stick that is frozen together, not skewered. */
    public static final TagKey<Item> FROZEN = tag("frozen");
    /** A dish whose bread is rolled round the filling. */
    public static final TagKey<Item> WRAPPED = tag("wrapped");
    /** A dish in a bowl that is tossed: a salad. Without the tag a bowl of plants is tossed, anything else arranged. */
    public static final TagKey<Item> TOSSED = tag("tossed");
    /** A dish in a bowl that is arranged, never tossed. */
    public static final TagKey<Item> ARRANGED = tag("arranged");

    private KitchenTags() {
    }

    public static boolean is(ItemStack stack, TagKey<Item> tag) {
        return !stack.isEmpty() && stack.is(tag);
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, HandsOnDelight.id(name));
    }
}
