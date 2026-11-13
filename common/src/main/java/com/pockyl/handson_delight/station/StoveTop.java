package com.pockyl.handson_delight.station;

/** A stove's top as the station works it: the grill state kept in Farmer's Delight's block entity (a mixin), and its own counts. */
public interface StoveTop {
    Grill handson$grill();

    /** Farmer's Delight's ticks cooked of each slot: kept at nothing for a piece grilled by hand, so it never pops it out. */
    int[] handson$progress();

    /** Farmer's Delight's ticks each slot takes: set when its own right click puts food on, endless for a piece grilled by hand. */
    int[] handson$times();
}
