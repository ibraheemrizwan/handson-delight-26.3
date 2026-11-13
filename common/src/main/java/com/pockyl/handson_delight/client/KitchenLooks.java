package com.pockyl.handson_delight.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.KitchenTags;

import java.util.Optional;

/**
 * How food looks once a cook has handled it, the same at every station: an egg is cracked open and never lies in its shell, a
 * liquid is poured out of its container and the container never lies in the food.
 */
final class KitchenLooks {
    /** A raw egg out of its shell: its white still clear, so a little see-through and greyish. */
    static final int RAW_EGG = 0xC8E6E2D8;
    /** A shell's colour, for the bits that fly off when it cracks. */
    private static final int SHELL = 0xFFEAD9B8;

    private KitchenLooks() {
    }

    /** Whether {@code stack} is cracked open before it goes anywhere: an egg. */
    static boolean isCracked(ItemStack stack) {
        return KitchenTags.is(stack, KitchenTags.CRACKED);
    }

    /** Whether {@code stack} is a liquid in its container, poured out where it goes. */
    static boolean isPoured(ItemStack stack) {
        return KitchenTags.is(stack, KitchenTags.POURED);
    }

    /** What an egg looks like out of its shell: Farmer's Delight's fried egg, drawn raw by {@link #RAW_EGG}; anything else as it is. */
    static ItemStack opened(ItemStack stack) {
        if (!isCracked(stack)) {
            return stack;
        }
        Item fried = BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id("fried_egg"));
        return fried == Items.AIR ? stack : new ItemStack(fried);
    }

    /** The colour of what is in a container: the pixels where the full icon differs from the empty one, ARGB. */
    static int liquidColor(ItemStack stack) {
        return liquid(stack).map(liquid -> liquid.icon().color(liquid.column(), liquid.row()) | 0xFF000000)
                .orElse(0xFF000000 | FoodColors.main(stack));
    }

    /** The icon of a full container and the pixel of it that shows the liquid. */
    record Liquid(ItemVoxels.Icon icon, int column, int row) {
    }

    static Optional<Liquid> liquid(ItemStack stack) {
        Optional<ItemVoxels.Icon> full = ItemVoxels.icon(stack);
        if (full.isEmpty()) {
            return Optional.empty();
        }
        var container = stack.getCraftingRemainder();
        Optional<ItemVoxels.Icon> empty = container == null ? Optional.empty() : ItemVoxels.icon(container.create());
        int[] at = ItemVoxels.contents(full.get(), empty);
        return Optional.of(new Liquid(full.get(), at[0], at[1]));
    }

    /**
     * The tint of a piece frying with its top {@code up} done (0 to 1): an egg's white clears from see-through to white, never
     * brown; other food goes from its raw colours towards the cooked one's (kelp darkens green, a potato goes golden), meat and fish
     * with a little sear on top of that. Done, the cooked icon shows as it is.
     */
    static int fryingTint(ItemStack raw, ItemStack cooked, float up, int seared, float sear) {
        if (up >= 1.0F) {
            return 0xFFFFFFFF;
        }
        float done = Math.max(0.0F, up);
        if (isCracked(raw)) {
            return mix(RAW_EGG, 0xFFFFFFFF, done);
        }
        int from = FoodColors.main(raw);
        int to = FoodColors.main(cooked);
        int target = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            float a = Math.max(1, from >> shift & 0xFF);
            float b = to >> shift & 0xFF;
            target |= Math.round(Math.min(1.0F, b / a) * 255.0F) << shift;
        }
        if (raw.is(ItemTags.MEAT) || raw.is(ItemTags.FISHES)) {
            target = mix(target, seared, 0.4F);
        }
        return mix(0xFFFFFFFF, target, done * sear);
    }

    /** ARGB {@code from} towards {@code to} by {@code amount}, channel by channel. */
    static int mix(int from, int to, float amount) {
        int color = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            float a = from >>> shift & 0xFF;
            float b = to >>> shift & 0xFF;
            color |= Math.round(a + (b - a) * amount) << shift;
        }
        return color;
    }

    /** The egg cracks at {@code at} (scene pixels): a crack and bits of shell. */
    static void crack(StationView view, String node, Vector3f at) {
        view.spawnParticles(DiegesisParticles.DUST, at, new Vector3f(0.0F, 3.0F, 0.0F), 1.0F, 5, SHELL);
        view.playSound(SoundEvents.TURTLE_EGG_CRACK, node, 0.35F, 1.5F);
    }

    /** A stream of {@code stack}'s liquid from {@code from} down to {@code to} (scene pixels), with the sound of its container emptied. */
    static void pour(StationView view, String node, ItemStack stack, Vector3f from, Vector3f to) {
        int color = liquidColor(stack);
        Vector3f fall = new Vector3f(to).sub(from).mul(3.0F);
        view.spawnParticles(DiegesisParticles.DROPLET, from, fall, 0.25F, 10, color);
        view.playSound(pourSound(stack), node, 0.45F, 1.15F);
    }

    private static SoundEvent pourSound(ItemStack stack) {
        var container = stack.getCraftingRemainder();
        return container != null && container.is(Items.BUCKET) ? SoundEvents.BUCKET_EMPTY : SoundEvents.BOTTLE_EMPTY;
    }
}
