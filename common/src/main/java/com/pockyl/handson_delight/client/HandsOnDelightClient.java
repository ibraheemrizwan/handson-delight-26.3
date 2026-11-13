package com.pockyl.handson_delight.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.client.LiquidLook;
import com.pockyl.diegesis.api.client.ParticleStyle;
import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.station.CookingPotStation;
import com.pockyl.handson_delight.station.CuttingBoardStation;
import com.pockyl.handson_delight.station.SkilletStation;
import com.pockyl.handson_delight.station.StoveStation;

import java.util.ArrayList;
import java.util.List;

/** Client setup; only loaded on clients. */
public final class HandsOnDelightClient {
    public static final Identifier OIL_POP = HandsOnDelight.id("oil_pop");
    public static final Identifier FAT = HandsOnDelight.id("fat");
    public static final Identifier PAN_SMOKE = HandsOnDelight.id("pan_smoke");
    public static final Identifier HEAT = HandsOnDelight.id("heat");
    @SuppressWarnings("removal")
    private static final Identifier DIEGESIS_BUBBLE = Identifier.fromNamespaceAndPath("diegesis", "bubble");

    private HandsOnDelightClient() {
    }

    public static void init() {
        DiegesisClient.registerBehavior(CookingPotStation.ID, PotBehavior::new);
        DiegesisClient.registerBehavior(CuttingBoardStation.ID, BoardBehavior::new);
        DiegesisClient.registerBehavior(SkilletStation.ID, SkilletBehavior::new);
        DiegesisClient.registerBehavior(StoveStation.ID, StoveBehavior::new);
        DiegesisClient.registerLiquid(HandsOnDelight.id("broth"), LiquidLook.of(0xE0B0A070).thick(0.6F));
        registerParticles();
    }

    private static List<Identifier> frames(String base, int count) {
        List<Identifier> frames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            frames.add(HandsOnDelight.id(base + "_" + i));
        }
        return frames;
    }

    /** The skillet's heat: oil seething round the food, fat spitting off it, smoke of a side that waits, shimmer over the hot pan. */
    @SuppressWarnings("removal")
    private static void registerParticles() {
        List<Identifier> popping = new ArrayList<>(List.of(DIEGESIS_BUBBLE, DIEGESIS_BUBBLE));
        for (int i = 0; i < 5; i++) {
            popping.add(Identifier.fromNamespaceAndPath("diegesis", "bubble_pop_" + i));
        }
        DiegesisClient.registerParticle(OIL_POP, ParticleStyle.builder(popping).color(0xE6F7C27A).size(0.8F, 1.3F).life(0.25F, 0.5F)
                .drag(1.0F).grow(0.4F).pop().build());
        DiegesisClient.registerParticle(FAT, ParticleStyle.builder(Identifier.fromNamespaceAndPath("minecraft", "drip_fall")).color(0xFFFFE7A8)
                .size(0.3F, 0.45F).life(0.3F, 0.6F).gravity(75.0F).drag(0.05F).stretch().pop().build());
        List<Identifier> steam = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            steam.add(Identifier.fromNamespaceAndPath("diegesis", "steam_" + i));
        }
        DiegesisClient.registerParticle(PAN_SMOKE, ParticleStyle.builder(steam).color(0x58C8C6C2).size(1.8F, 2.6F).life(1.6F, 2.4F)
                .gravity(-4.0F).drag(0.75F).grow(3.0F).wobble(1.0F).fadeIn(0.3F).build());
        DiegesisClient.registerParticle(HEAT, ParticleStyle.builder(frames("heat", 4)).color(0x48FFFFFF)
                .size(3.0F, 4.0F).life(0.8F, 1.3F).gravity(-5.0F).drag(0.6F).grow(0.4F).wobble(0.3F).fadeIn(0.4F).build());
    }

    /** Whether a scene stands in for the block at {@code pos} on this client. */
    public static boolean isShown(BlockPos pos) {
        return DiegesisClient.station(pos).isPresent();
    }
}
