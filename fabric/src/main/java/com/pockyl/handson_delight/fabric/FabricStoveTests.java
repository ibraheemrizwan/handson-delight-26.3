package com.pockyl.handson_delight.fabric;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import com.pockyl.handson_delight.gametest.StoveTests;

/** {@link StoveTests} on Fabric: they run when the game starts with {@code -Dfabric-api.gametest}. */
public final class FabricStoveTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 460)
    public void aPieceGrillsInHalvesAndWaitsToBeTurned(GameTestHelper helper) {
        StoveTests.aPieceGrillsInHalvesAndWaitsToBeTurned(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void farmersDelightsWayStaysItsOwn(GameTestHelper helper) {
        StoveTests.farmersDelightsWayStaysItsOwn(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void onlyFoodGoesOnAnOpenGrill(GameTestHelper helper) {
        StoveTests.onlyFoodGoesOnAnOpenGrill(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void anEggStaysOffTheGrill(GameTestHelper helper) {
        StoveTests.anEggStaysOffTheGrill(helper);
    }
}
