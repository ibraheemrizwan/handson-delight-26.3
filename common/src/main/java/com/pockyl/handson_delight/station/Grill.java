package com.pockyl.handson_delight.station;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import java.util.Arrays;

/**
 * What a stove grilling by hand keeps besides Farmer's Delight's own six slots: for each piece, how far each side is done and whether
 * the cook laid it there. Kept in the stove itself (a mixin on its block entity), so the food goes on grilling when the cook walks
 * away, everybody passing by sees it, and it is still there after a restart.
 */
public final class Grill {
    public static final int PLACES = 6;
    /** How finely the clients hear of the doneness of each side. */
    private static final int STEPS = 16;

    private final float[] down = new float[PLACES];
    private final float[] up = new float[PLACES];
    private final boolean[] byHand = new boolean[PLACES];
    private final long[] turnedAt = new long[PLACES];
    /** The doneness last sent to the clients, in steps: they hear of every step, not of every tick. */
    private int sent = -1;

    public Grill() {
        Arrays.fill(turnedAt, Long.MIN_VALUE / 2);
    }

    public float down(int place) {
        return down[place];
    }

    public float up(int place) {
        return up[place];
    }

    /** Laid or turned at the station: the piece grills in halves and waits to be turned. Else Farmer's Delight cooks it its way. */
    public boolean byHand(int place) {
        return byHand[place];
    }

    /** The side on the grill is done: nothing goes on until the piece is turned. */
    public boolean waits(int place) {
        return byHand[place] && down[place] >= 1.0F && up[place] < 1.0F;
    }

    long turnedAt(int place) {
        return turnedAt[place];
    }

    void layByHand(int place) {
        byHand[place] = true;
    }

    /** Back to Farmer's Delight's own cooking, from raw: food put on with its right click, or a place left empty. */
    public void farmersDelightsWay(int place) {
        byHand[place] = false;
        down[place] = 0.0F;
        up[place] = 0.0F;
    }

    void grill(int place, float amount) {
        down[place] = Math.min(1.0F, down[place] + amount);
    }

    /** A piece grilled on one side only cooks through from below and never waits to be turned. */
    void grillThrough(int place, float amount) {
        down[place] = Math.min(1.0F, down[place] + amount);
        up[place] = down[place];
    }

    void turn(int place, long now) {
        float was = down[place];
        down[place] = up[place];
        up[place] = was;
        turnedAt[place] = now;
    }

    /** Done on both sides: the piece became what the fire makes of it and starts over as that. */
    void done(int place) {
        down[place] = 0.0F;
        up[place] = 0.0F;
    }

    /** Whether the clients should hear of the grill again: a piece got a step further, or something else changed. */
    boolean dueToSend() {
        int now = 0;
        for (int place = 0; place < PLACES; place++) {
            now = now * 31 + (Mth.floor(down[place] * STEPS) * (STEPS + 1) + Mth.floor(up[place] * STEPS)) * 2 + (byHand[place] ? 1 : 0);
        }
        if (now == sent) {
            return false;
        }
        sent = now;
        return true;
    }

    public void save(CompoundTag tag) {
        ListTag places = new ListTag();
        boolean any = false;
        for (int place = 0; place < PLACES; place++) {
            CompoundTag one = new CompoundTag();
            if (byHand[place] || down[place] > 0.0F || up[place] > 0.0F) {
                one.putBoolean("by_hand", byHand[place]);
                one.putFloat("down", down[place]);
                one.putFloat("up", up[place]);
                any = true;
            }
            places.add(one);
        }
        if (any) {
            tag.put("places", places);
        }
    }

    public void load(CompoundTag tag) {
        ListTag places = tag.getListOrEmpty("places");
        for (int place = 0; place < PLACES; place++) {
            CompoundTag one = place < places.size() ? places.getCompoundOrEmpty(place) : new CompoundTag();
            byHand[place] = one.getBooleanOr("by_hand", false);
            down[place] = one.getFloatOr("down", 0.0F);
            up[place] = one.getFloatOr("up", 0.0F);
        }
    }
}
