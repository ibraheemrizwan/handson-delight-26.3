package com.pockyl.handson_delight.station;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * What a skillet frying by hand keeps besides Farmer's Delight's own slot: how far each side of the food is done, kept in the
 * skillet itself (a mixin on its block entity), so the food goes on frying when the cook walks away, everybody passing by sees
 * it, and it is still there after a restart.
 */
public final class Frying {
    /** How finely the clients hear of the doneness of each side. */
    private static final int STEPS = 16;

    /** How far the food is done on the side that lies on the pan and on the one that faces up, 0 to 1. */
    private float down;
    private float up;
    /** How many of the stack fry together; the time a side takes grows with it. */
    private int batch;
    /** Put in or turned at the station: the food fries in halves and waits to be turned. Else Farmer's Delight cooks it its way. */
    private boolean byHand;
    /** Who turned it last: the food goes to them when it is done, while they are still at the pan. Not saved. */
    private UUID cook;
    private long turnedAt = Long.MIN_VALUE / 2;
    /** The doneness last sent to the clients, in steps: the clients hear of every step, not of every tick. */
    private int sent = -1;

    public float down() {
        return down;
    }

    public float up() {
        return up;
    }

    public int batch() {
        return batch;
    }

    public boolean byHand() {
        return byHand;
    }

    /** The side on the pan is done: nothing goes on until the food is turned. */
    public boolean waits() {
        return byHand && down >= 1.0F && up < 1.0F;
    }

    UUID cook() {
        return cook;
    }

    long turnedAt() {
        return turnedAt;
    }

    void byHand(UUID cook) {
        byHand = true;
        if (cook != null) {
            this.cook = cook;
        }
    }

    /** Back to Farmer's Delight's own cooking, from raw: food put in with its right click, or an emptied pan. */
    public void farmersDelightsWay() {
        byHand = false;
        reset();
    }

    void reset() {
        down = 0.0F;
        up = 0.0F;
        batch = 0;
    }

    void fry(float amount) {
        down = Math.min(1.0F, down + amount);
    }

    /** Food fried on one side only cooks through from below: the top is done as the bottom is, so it never waits to be turned. */
    void fryThrough(float amount) {
        down = Math.min(1.0F, down + amount);
        up = down;
    }

    void turn(long now) {
        float was = down;
        down = up;
        up = was;
        turnedAt = now;
    }

    /**
     * {@code batch} fry together now: food added starts raw, food taken out leaves the rest, so how far a side got is shared out by
     * the time it takes, the way the pot keeps the time cooked when food is added (a side takes longer by the square root).
     */
    void batch(int batch) {
        if (this.batch > 0 && batch > 0 && batch != this.batch) {
            float scale = (float) Math.sqrt((float) this.batch / batch);
            down = Math.min(1.0F, down * scale);
            up = Math.min(1.0F, up * scale);
        }
        this.batch = batch;
    }

    /** Whether the clients should hear of the food again: it got a step further, or something else changed. */
    boolean dueToSend() {
        int now = state();
        if (now == sent) {
            return false;
        }
        sent = now;
        return true;
    }

    private int state() {
        int steps = Mth.floor(down * STEPS) * (STEPS + 1) + Mth.floor(up * STEPS);
        return (steps * 31 + batch) * 2 + (byHand ? 1 : 0);
    }

    public void save(CompoundTag tag) {
        if (byHand || down > 0.0F || up > 0.0F) {
            tag.putBoolean("by_hand", byHand);
            tag.putFloat("down", down);
            tag.putFloat("up", up);
            tag.putInt("batch", batch);
        }
    }

    public void load(CompoundTag tag) {
        byHand = tag.getBooleanOr("by_hand", false);
        down = tag.getFloatOr("down", 0.0F);
        up = tag.getFloatOr("up", 0.0F);
        batch = tag.getIntOr("batch", 0);
    }
}
