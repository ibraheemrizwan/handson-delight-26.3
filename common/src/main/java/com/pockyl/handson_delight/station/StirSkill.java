package com.pockyl.handson_delight.station;

import net.minecraft.util.Mth;

/**
 * How well a pot is stirred, in stars from 0 to 3, judged on the server from where the spoon is. Wide rounds at a steady pace one
 * way fill the stars; circling the middle, shaking back and forth or an uneven hand hold them lower, and they drain when the spoon
 * stops. Only the stars the cook has opened by cooking the dish before can fill. The stars when a batch is done decide its bonus
 * servings. The pace is judged by the cook's own clock, sent with every stroke: on a server the strokes come in bunched by the
 * network, and a slow server ticks fewer times a second than the hand turns. The server's ticks still say when the spoon stopped.
 */
public final class StirSkill {
    public static final float MOST = 3.0F;
    /** Stars a second the meter rises and falls towards what the stirring is worth. */
    private static final float RISE = 0.75F;
    private static final float FALL = 0.5F;
    /** Stars a second it loses while the spoon stands still, after a short breath. */
    private static final float DRAIN = 0.75F;
    private static final int GRACE = 10;
    /** A stroke is judged over this many ticks; a longer stop starts the next one afresh. */
    private static final int WINDOW = 5;
    private static final int BREAK = 12;
    /** Ticks the cook's clock may run ahead of the server's between two strokes: the network bunches them. */
    private static final float SLACK = 10.0F;
    /** From this part of the spoon's reach a round counts as wide, from the other one fully. */
    private static final float NARROW = 0.3F;
    private static final float WIDE = 0.7F;
    /** Radians a second: a good pace lies between the inner two, nothing counts outside the outer two. */
    private static final float SLOWEST = 1.2F;
    private static final float SLOW = 3.0F;
    private static final float FAST = 13.0F;
    private static final float FASTEST = 22.0F;
    /** Quality from which the stirring is worth all three stars: nobody stirs perfectly. */
    private static final float ENOUGH = 0.8F;

    private float stars;
    private int open = 1;
    private long settled;
    private long lastMove = Long.MIN_VALUE / 2;
    private float lastX;
    private float lastZ;
    private double windowStart;
    /** The cook's clock at the last stroke, in ticks, and the server's then. */
    private double lastAt = Double.NaN;
    private double lastAtRaw;
    private long lastAtServer;
    private float turned;
    private float wideness;
    private int steps;
    private boolean shook;
    /** Which way the rounds go, and how fast the last strokes went, radians a second. */
    private float way;
    private float pace = Float.NaN;

    public float stars(long now) {
        long idle = now - Math.max(lastMove + GRACE, settled);
        return idle > 0 ? Math.max(0.0F, stars - DRAIN * idle / 20.0F) : stars;
    }

    /** Full stars, what a batch done now is graded. */
    public int grade(long now) {
        return Mth.floor(stars(now) + 1.0e-4F);
    }

    /**
     * The spoon's end is at {@code x, z}, -1 to 1 of its reach; only while the pot cooks does it count for the stars, and only
     * {@code open} of them can fill.
     */
    public void move(long now, float x, float z, boolean cooking, int open) {
        move(now, Float.NaN, x, z, cooking, open);
    }

    /**
     * As {@link #move(long, float, float, boolean, int)}, with {@code at} the cook's clock in ticks (NaN: none, the server's own). It
     * may run on from one stroke to the next by at most the server's ticks and half a second more, never back.
     */
    public void move(long now, float at, float x, float z, boolean cooking, int open) {
        // In doubles: a server's clock counts far past what a float keeps to the tick.
        double clock;
        if (Float.isNaN(at) || Double.isNaN(lastAt) || now - lastMove > BREAK) {
            clock = Double.isNaN(lastAt) ? now : lastAt + (now - lastAtServer);
        } else {
            clock = lastAt + Mth.clamp(at - lastAtRaw, 0.0, (now - lastAtServer) + SLACK);
        }
        lastAtRaw = Float.isNaN(at) ? clock : at;
        lastAt = clock;
        lastAtServer = now;
        this.open = open;
        stars = Math.min(open, stars(now));
        settled = now;
        if (now - lastMove > BREAK) {
            startStroke(clock);
            pace = Float.NaN;
        } else {
            float radius = Math.min((float) Math.hypot(x, z), (float) Math.hypot(lastX, lastZ));
            if (radius > 0.15F) {
                float step = Mth.wrapDegrees((float) (Math.atan2(z, x) - Math.atan2(lastZ, lastX)) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
                if (Math.abs(step) > 0.05F && Math.abs(turned) > 0.05F && Math.signum(step) != Math.signum(turned)) {
                    shook = true;
                }
                turned += step;
            }
            wideness += Mth.clamp((radius - NARROW) / (WIDE - NARROW), 0.0F, 1.0F);
            steps++;
            if (clock - windowStart >= WINDOW) {
                judge(clock, cooking);
            }
        }
        lastMove = now;
        lastX = x;
        lastZ = z;
    }

    private void judge(double now, boolean cooking) {
        float seconds = (float) (now - windowStart) / 20.0F;
        float speed = Math.abs(turned) / seconds;
        float pacing = speed < SLOW ? (speed - SLOWEST) / (SLOW - SLOWEST) : (FASTEST - speed) / (FASTEST - FAST);
        // Half again as fast or slow as the strokes before still counts as steady; twice and three times less and less.
        float change = Float.isNaN(pace) || speed <= 0.0F ? 0.0F : Math.abs((float) Math.log(speed / pace));
        float steady = 1.0F - Math.max(0.0F, change - 0.2F) / 0.9F;
        boolean oneWay = !shook && (way == 0.0F || Math.signum(turned) == way);
        float quality = oneWay ? Mth.clamp(pacing, 0.0F, 1.0F) * Mth.clamp(steady, 0.0F, 1.0F) * wideness / steps : 0.0F;
        if (cooking) {
            float worth = Math.min(open, MOST * Math.min(1.0F, quality / ENOUGH));
            stars = worth > stars ? Math.min(worth, stars + RISE * seconds) : Math.max(worth, stars - FALL * seconds);
        }
        if (Math.abs(turned) > 0.1F) {
            way = Math.signum(turned);
        }
        pace = Float.isNaN(pace) ? speed : Mth.lerp(0.4F, pace, speed);
        startStroke(now);
    }

    private void startStroke(double now) {
        windowStart = now;
        turned = 0.0F;
        wideness = 0.0F;
        steps = 0;
        shook = false;
    }
}
