package com.pockyl.handson_delight.fabric;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import com.pockyl.handson_delight.gametest.PotTests;

/** {@link PotTests} on Fabric: they run when the game starts with {@code -Dfabric-api.gametest}. */
public final class FabricPotTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void slotsAreThePotsOwn(GameTestHelper helper) {
        PotTests.slotsAreThePotsOwn(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void theMealStaysInThePot(GameTestHelper helper) {
        PotTests.theMealStaysInThePot(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aBowlClickedOnTheSoupFills(GameTestHelper helper) {
        PotTests.aBowlClickedOnTheSoupFills(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void anEmptyHandScoopsWithBowlsFromTheInventory(GameTestHelper helper) {
        PotTests.anEmptyHandScoopsWithBowlsFromTheInventory(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aStackIsSharedOutForARecipeOfTwoAlike(GameTestHelper helper) {
        PotTests.aStackIsSharedOutForARecipeOfTwoAlike(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void dishesStayOutOfTheWater(GameTestHelper helper) {
        PotTests.dishesStayOutOfTheWater(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void shiftFillsTheWholeStack(GameTestHelper helper) {
        PotTests.shiftFillsTheWholeStack(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 340)
    public void theWholeBatchCooksAtOnce(GameTestHelper helper) {
        PotTests.theWholeBatchCooksAtOnce(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void theCooksClockJudgesThePace(GameTestHelper helper) {
        PotTests.theCooksClockJudgesThePace(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void theBookFillsThePot(GameTestHelper helper) {
        PotTests.theBookFillsThePot(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void aBonusIsCarriedOn(GameTestHelper helper) {
        PotTests.aBonusIsCarriedOn(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 400)
    public void theSetsSavedGoBack(GameTestHelper helper) {
        PotTests.theSetsSavedGoBack(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 500)
    public void theEmptiedContainerGoesToTheCook(GameTestHelper helper) {
        PotTests.theEmptiedContainerGoesToTheCook(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 360)
    public void aPotLeftByItsCookCooksOn(GameTestHelper helper) {
        PotTests.aPotLeftByItsCookCooksOn(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 260)
    public void aCookWhoKnowsTheDishGetsMore(GameTestHelper helper) {
        PotTests.aCookWhoKnowsTheDishGetsMore(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 260)
    public void theFirstTimeHasOneStar(GameTestHelper helper) {
        PotTests.theFirstTimeHasOneStar(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void onlyGoodStirringEarnsStars(GameTestHelper helper) {
        PotTests.onlyGoodStirringEarnsStars(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void nothingCooksWithoutStirring(GameTestHelper helper) {
        PotTests.nothingCooksWithoutStirring(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void theStationReadsHeat(GameTestHelper helper) {
        PotTests.theStationReadsHeat(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 220)
    public void theSmallestStackSetsTheBatch(GameTestHelper helper) {
        PotTests.theSmallestStackSetsTheBatch(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 300)
    public void aHopperFedPotCooksByItself(GameTestHelper helper) {
        PotTests.aHopperFedPotCooksByItself(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 60)
    public void oneSpoonOneHand(GameTestHelper helper) {
        PotTests.oneSpoonOneHand(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aServingTakenByHandCounts(GameTestHelper helper) {
        PotTests.aServingTakenByHandCounts(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 160)
    public void aModdedRecipeCooksAsABatch(GameTestHelper helper) {
        PotTests.aModdedRecipeCooksAsABatch(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 220)
    public void aDishWithoutAContainerComesOutWhole(GameTestHelper helper) {
        PotTests.aDishWithoutAContainerComesOutWhole(helper);
    }
}
