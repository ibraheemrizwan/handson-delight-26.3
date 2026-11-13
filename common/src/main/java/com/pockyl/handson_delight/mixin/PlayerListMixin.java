package com.pockyl.handson_delight.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.platform.Platform;

import java.util.List;

/**
 * Tells clients which crafting recipes are put together on the board, just before the recipes themselves go out: JEI and EMI read
 * them as soon as the recipes and tags arrive, and a later packet would find them already loaded. Loader events come after that.
 */
@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

    @Inject(method = "placeNewPlayer", at = @At(value = "NEW",
            target = "net/minecraft/network/protocol/game/ClientboundUpdateRecipesPacket"))
    private void handson$boardDishesOnJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo callback) {
        Platform.INSTANCE.sendBoardDishes(player);
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void handson$boardDishesOnReload(CallbackInfo callback) {
        // The tags were bound again after the recipes loaded: the board's dishes are chosen with the new ones.
        AssemblyRecipes.tagsChanged();
        for (ServerPlayer player : players) {
            Platform.INSTANCE.sendBoardDishes(player);
        }
    }
}
