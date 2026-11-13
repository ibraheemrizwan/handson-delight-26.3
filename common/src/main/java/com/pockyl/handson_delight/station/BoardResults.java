package com.pockyl.handson_delight.station;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Catches the items Farmer's Delight spawns while {@link #catching} runs: the results of a cut, for the cook's inventory. A quiet
 * cut makes no sound: the rest of a batch, after its first item.
 */
public final class BoardResults {
    /** Per thread: the client of a single-player game spawns its own items in its own thread meanwhile. */
    private static final ThreadLocal<List<ItemStack>> CAUGHT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> QUIET = ThreadLocal.withInitial(() -> false);

    private BoardResults() {
    }

    /** Runs {@code cut} and returns what it would have thrown into the world, or null when it did nothing. */
    public static List<ItemStack> catching(boolean quiet, BooleanSupplier cut) {
        List<ItemStack> previous = CAUGHT.get();
        List<ItemStack> caught = new ArrayList<>();
        CAUGHT.set(caught);
        QUIET.set(quiet);
        try {
            return cut.getAsBoolean() ? caught : null;
        } finally {
            CAUGHT.set(previous);
            QUIET.set(false);
        }
    }

    /** Called for every item Farmer's Delight spawns: true when it was caught. */
    public static boolean take(ItemStack stack) {
        List<ItemStack> caught = CAUGHT.get();
        if (caught == null) {
            return false;
        }
        caught.add(stack.copy());
        return true;
    }

    public static boolean isQuiet() {
        return QUIET.get();
    }
}
