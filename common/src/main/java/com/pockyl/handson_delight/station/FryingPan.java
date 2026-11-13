package com.pockyl.handson_delight.station;

/** A skillet of Farmer's Delight that fries by hand too: a mixin on its block entity keeps the state. */
public interface FryingPan {
    Frying handson$frying();

    /** Fire Aspect on the skillet item it was placed from: Farmer's Delight's cooking time is shorter by it. */
    int handson$fireAspect();
}
