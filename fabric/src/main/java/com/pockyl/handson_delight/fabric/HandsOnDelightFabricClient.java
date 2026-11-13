package com.pockyl.handson_delight.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.pockyl.handson_delight.assembly.BoardDishes;
import com.pockyl.handson_delight.client.HandsOnDelightClient;

/** Fabric entry point, client. */
public final class HandsOnDelightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandsOnDelightClient.init();
        ClientPlayNetworking.registerGlobalReceiver(BoardDishes.Payload.TYPE, (payload, context) -> BoardDishes.receive(payload));
        // The next server may not have the mod: its sandwiches are crafted.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BoardDishes.clear());
    }
}
