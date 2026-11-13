package com.pockyl.handson_delight.client;

import org.joml.Vector3f;

import com.pockyl.diegesis.api.client.StationView;

import java.util.Optional;

/**
 * Where what the player puts into a station comes from: the player, at the bottom of their screen where the inventory is
 * (owner: "they must fly from the player, not from the book"). An offset on a level plane, scene pixels.
 */
final class FromPlayer {
    /** How low on the screen they start: just over the bottom edge, at the inventory. */
    private static final float BOTTOM = 0.97F;
    /** How far apart along the screen's width things that come one after the other start, a fraction of it. */
    private static final float SPREAD = 0.05F;

    private FromPlayer() {
    }

    /**
     * From the middle of the screen to where the {@code index}-th of {@code count} things starts at its bottom, on the plane at
     * {@code height}; nothing for everybody but the cook, whose things come down from straight above.
     */
    static Vector3f offset(StationView view, float height, int index, int count) {
        Optional<Vector3f> start = point(view, height, index, count);
        Optional<Vector3f> middle = view.screenOn(0.5F, 0.5F, height);
        if (start.isEmpty() || middle.isEmpty()) {
            return new Vector3f();
        }
        return start.get().sub(middle.get()).mul(1.0F, 0.0F, 1.0F);
    }

    /** Where the {@code index}-th of {@code count} things starts, in scene pixels on the plane at {@code height}; empty for the others. */
    static Optional<Vector3f> point(StationView view, float height, int index, int count) {
        return view.screenOn(0.5F + (index - (count - 1) / 2.0F) * SPREAD, BOTTOM, height);
    }
}
