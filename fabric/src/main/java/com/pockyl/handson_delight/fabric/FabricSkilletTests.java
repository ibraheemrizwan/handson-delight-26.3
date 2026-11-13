package com.pockyl.handson_delight.fabric;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import com.pockyl.handson_delight.gametest.SkilletTests;

/** {@link SkilletTests} on Fabric: they run when the game starts with {@code -Dfabric-api.gametest}. */
public final class FabricSkilletTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 260)
    public void aBatchFriesInHalvesAndWaitsToBeTurned(GameTestHelper helper) {
        SkilletTests.aBatchFriesInHalvesAndWaitsToBeTurned(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 160)
    public void farmersDelightsRightClickCooksItsWay(GameTestHelper helper) {
        SkilletTests.farmersDelightsRightClickCooksItsWay(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void anEggFriesWithoutATurn(GameTestHelper helper) {
        SkilletTests.anEggFriesWithoutATurn(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void theFoodComesBackAndOnlyFoodGoesIn(GameTestHelper helper) {
        SkilletTests.theFoodComesBackAndOnlyFoodGoesIn(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void moreFoodTakesLongerButKeepsWhatWasDone(GameTestHelper helper) {
        SkilletTests.moreFoodTakesLongerButKeepsWhatWasDone(helper);
    }
}
