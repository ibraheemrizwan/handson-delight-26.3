package com.pockyl.handson_delight.fabric;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import com.pockyl.handson_delight.gametest.BoardTests;

/** {@link BoardTests} on Fabric: they run when the game starts with {@code -Dfabric-api.gametest}. */
public final class FabricBoardTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void burgersLeaveTheCraftingTable(GameTestHelper helper) {
        BoardTests.burgersLeaveTheCraftingTable(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void bowlDishesLeaveTheCraftingTable(GameTestHelper helper) {
        BoardTests.bowlDishesLeaveTheCraftingTable(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void everyFoodCraftComesToTheBoard(GameTestHelper helper) {
        BoardTests.everyFoodCraftComesToTheBoard(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void doughKeepsItsWater(GameTestHelper helper) {
        BoardTests.doughKeepsItsWater(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aPieGoesIntoItsCrust(GameTestHelper helper) {
        BoardTests.aPieGoesIntoItsCrust(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void milkGivesItsBucketBack(GameTestHelper helper) {
        BoardTests.milkGivesItsBucketBack(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void cookiesComeEightARound(GameTestHelper helper) {
        BoardTests.cookiesComeEightARound(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aFeastIsLaidFromTheBook(GameTestHelper helper) {
        BoardTests.aFeastIsLaidFromTheBook(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aSaladStartsFromItsBowl(GameTestHelper helper) {
        BoardTests.aSaladStartsFromItsBowl(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void theBookLaysADishFromTheInventory(GameTestHelper helper) {
        BoardTests.theBookLaysADishFromTheInventory(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aBurgerIsBuiltFromTheBreadUp(GameTestHelper helper) {
        BoardTests.aBurgerIsBuiltFromTheBreadUp(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void theTopLayerComesBack(GameTestHelper helper) {
        BoardTests.theTopLayerComesBack(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void aHamTakesTwoChopsAndFeedsTheCook(GameTestHelper helper) {
        BoardTests.aHamTakesTwoChopsAndFeedsTheCook(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void noKnifeNoCut(GameTestHelper helper) {
        BoardTests.noKnifeNoCut(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void anEmptyHandCutsWithTheKnifeInTheInventory(GameTestHelper helper) {
        BoardTests.anEmptyHandCutsWithTheKnifeInTheInventory(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void anEmptyHandWithoutAKnifeCutsNothing(GameTestHelper helper) {
        BoardTests.anEmptyHandWithoutAKnifeCutsNothing(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void sixteenAreCutAtOnce(GameTestHelper helper) {
        BoardTests.sixteenAreCutAtOnce(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void layersStayOnTheBoard(GameTestHelper helper) {
        BoardTests.layersStayOnTheBoard(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void woodStaysWithFarmersDelight(GameTestHelper helper) {
        BoardTests.woodStaysWithFarmersDelight(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void sixteenBurgersAtOnce(GameTestHelper helper) {
        BoardTests.sixteenBurgersAtOnce(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void shiftClickLaysTheStackFromTheInventory(GameTestHelper helper) {
        BoardTests.shiftClickLaysTheStackFromTheInventory(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void marksAreCutInOrderAtNewPlaces(GameTestHelper helper) {
        BoardTests.marksAreCutInOrderAtNewPlaces(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void marksFollowWhatTheFoodFallsInto(GameTestHelper helper) {
        BoardTests.marksFollowWhatTheFoodFallsInto(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void aCarvedKnifeStaysFarmersDelights(GameTestHelper helper) {
        BoardTests.aCarvedKnifeStaysFarmersDelights(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void anIgnoredChopIsAnswered(GameTestHelper helper) {
        BoardTests.anIgnoredChopIsAnswered(helper);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void anotherCookKeepsTheMarks(GameTestHelper helper) {
        BoardTests.anotherCookKeepsTheMarks(helper);
    }
}
