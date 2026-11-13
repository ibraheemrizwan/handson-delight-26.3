package com.pockyl.handson_delight.client;

import net.minecraft.sounds.SoundEvent;

import com.pockyl.handson_delight.HandsOnDelight;

/** The pot's sounds. Sounds played on the client need no registry entry: the sound manager finds them by name in sounds.json. */
final class PotSounds {
    static final SoundEvent BODY = event("pot.body");
    static final SoundEvent STIR = event("pot.stir");
    static final SoundEvent PLOP = event("pot.plop");
    static final SoundEvent BURST = event("pot.burst");
    static final SoundEvent BLUB = event("pot.blub");
    static final SoundEvent SCOOP = event("pot.scoop");

    private PotSounds() {
    }

    private static SoundEvent event(String name) {
        return SoundEvent.createVariableRangeEvent(HandsOnDelight.id(name));
    }
}
