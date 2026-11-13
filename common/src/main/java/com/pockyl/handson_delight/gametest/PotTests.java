package com.pockyl.handson_delight.gametest;

import com.pockyl.handson_delight.KitchenNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import vectorwing.farmersdelight.common.crafting.CookingPotBookCategory;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.platform.Platform;
import com.pockyl.handson_delight.station.CookingPotStation;
import com.pockyl.handson_delight.station.PotLogic;
import com.pockyl.handson_delight.station.StirSkill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The station's rules against a real pot of Farmer's Delight, the same on every loader: each loader project runs these through its
 * own test registration (NeoForge's {@code ModGameTests}, Fabric's {@code FabricPotTests}).
 */
public final class PotTests {
    private PotTests() {
    }

    private static Item fd(String path) {
        return BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id(path));
    }

    /** A pot on a lit campfire, with the logic the station would make for it. */
    private record Setup(TestStation station, PotLogic logic, CookingPotBlockEntity pot) {
    }

    private static Setup pot(GameTestHelper helper) {
        BlockPos fire = new BlockPos(1, 1, 1);
        helper.setBlock(fire, Blocks.CAMPFIRE);
        helper.setBlock(fire.above(), FarmersDelightContent.COOKING_POT.get());
        BlockPos pos = helper.absolutePos(fire.above());
        TestStation station = new TestStation(helper.getLevel(), pos);
        CookingPotBlockEntity pot = (CookingPotBlockEntity) helper.getLevel().getBlockEntity(pos);
        return new Setup(station, new PotLogic(station), pot);
    }

    public static void slotsAreThePotsOwn(GameTestHelper helper) {
        Setup setup = pot(helper);
        SlotRef first = SlotRef.of(CookingPotStation.INGREDIENTS, 0);
        setup.station.state().setItem(first, new ItemStack(Items.CARROT, 3));
        helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).is(Items.CARROT), "the slot writes into the pot");
        Platform.INSTANCE.setItem(setup.pot, CookingPotBlockEntity.OUTPUT_SLOT, new ItemStack(Items.BOWL, 2));
        helper.assertTrue(setup.station.state().getItem(CookingPotStation.SERVED_SLOT).is(Items.BOWL), "and reads from it");
        helper.succeed();
    }

    public static void theMealStaysInThePot(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertFalse(setup.logic.mayTake(setup.station, player, CookingPotStation.MEAL_SLOT), "the meal is only served");
        helper.assertTrue(setup.logic.mayTake(setup.station, player, CookingPotStation.SERVED_SLOT), "servings are taken");
        helper.succeed();
    }

    public static void aBowlClickedOnTheSoupFills(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.inventoryMenu.setCarried(new ItemStack(Items.BOWL, 3));
        // No meal: nothing to scoop, the bowls stay on the cursor.
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_SCOOP, new CompoundTag());
        helper.assertTrue(player.inventoryMenu.getCarried().getCount() == 3, "nothing happens without a meal");

        Platform.INSTANCE.setItem(setup.pot, CookingPotBlockEntity.MEAL_DISPLAY_SLOT, new ItemStack(fd("beef_stew"), 2));
        // What a finished recipe leaves in the pot: it knows its container only from the recipe or from its save.
        CompoundTag saved = setup.pot.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.put("Container", KitchenNbt.save(new ItemStack(Items.BOWL), helper.getLevel().registryAccess()));
        setup.pot.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),saved));
        helper.assertFalse(setup.logic.mayPlace(setup.station, player, SlotRef.of(CookingPotStation.INGREDIENTS, 0), new ItemStack(Items.BOWL)),
                "a bowl for the soup does not go in");
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_SCOOP, new CompoundTag());
        helper.assertTrue(player.inventoryMenu.getCarried().getCount() == 2, "one bowl fills");
        helper.assertTrue(player.getInventory().contains(new ItemStack(fd("beef_stew"))), "and goes to the inventory");
        helper.assertTrue(setup.pot.getMeal().getCount() == 1, "one serving left");

        player.inventoryMenu.setCarried(new ItemStack(Items.BOWL));
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_SCOOP, new CompoundTag());
        helper.assertTrue(player.inventoryMenu.getCarried().is(fd("beef_stew")), "the last bowl in the hand stays in it, full");
        helper.succeed();
    }

    public static void anEmptyHandScoopsWithBowlsFromTheInventory(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getInventory().setItem(5, new ItemStack(Items.BOWL, 3));
        Platform.INSTANCE.setItem(setup.pot, CookingPotBlockEntity.MEAL_DISPLAY_SLOT, new ItemStack(fd("beef_stew"), 4));
        CompoundTag saved = setup.pot.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.put("Container", KitchenNbt.save(new ItemStack(Items.BOWL), helper.getLevel().registryAccess()));
        setup.pot.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),saved));
        CompoundTag one = new CompoundTag();
        one.putBoolean(CookingPotStation.FROM_INVENTORY, true);
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_SCOOP, one);
        helper.assertTrue(player.getInventory().countItem(Items.BOWL) == 2, "one bowl from the inventory fills");
        helper.assertTrue(player.getInventory().countItem(fd("beef_stew")) == 1, "into the inventory");
        helper.assertTrue(player.inventoryMenu.getCarried().isEmpty(), "the hand stays empty");
        CompoundTag all = one.copy();
        all.putBoolean(CookingPotStation.SCOOP_ALL, true);
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_SCOOP, all);
        helper.assertTrue(player.getInventory().countItem(Items.BOWL) == 0, "Shift fills every bowl there is");
        helper.assertTrue(setup.pot.getMeal().getCount() == 1, "and leaves the serving it has no bowl for");
        helper.succeed();
    }

    public static void aStackIsSharedOutForARecipeOfTwoAlike(GameTestHelper helper) {
        Setup setup = pot(helper);
        SlotRef first = SlotRef.of(CookingPotStation.INGREDIENTS, 0);
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(fd("tomato"), 5));
        setup.logic.onSlotChanged(setup.station, first);
        setup.logic.tick(setup.station);
        helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).getCount() == 3, "tomatoes are shared out");
        helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 1).is(fd("tomato")), "over two slots, as tomato sauce wants");
        helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 1).getCount() == 2, "and none get lost");

        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(Items.CARROT, 6));
        Platform.INSTANCE.setItem(setup.pot, 1, ItemStack.EMPTY);
        setup.logic.onSlotChanged(setup.station, first);
        setup.logic.tick(setup.station);
        helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).getCount() == 6, "what makes nothing stays as it is");
        helper.succeed();
    }

    public static void dishesStayOutOfTheWater(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        SlotRef first = SlotRef.of(CookingPotStation.INGREDIENTS, 0);
        helper.assertFalse(setup.logic.mayPlace(setup.station, player, first, new ItemStack(Items.BOWL)), "no bowl in an empty pot");
        helper.assertTrue(setup.logic.mayPlace(setup.station, player, first, new ItemStack(Items.CARROT)), "food goes in");
        helper.succeed();
    }

    public static void shiftFillsTheWholeStack(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        Platform.INSTANCE.setItem(setup.pot, CookingPotBlockEntity.MEAL_DISPLAY_SLOT, new ItemStack(fd("beef_stew"), 2));
        CompoundTag saved = setup.pot.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.put("Container", KitchenNbt.save(new ItemStack(Items.BOWL), helper.getLevel().registryAccess()));
        setup.pot.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),saved));
        player.inventoryMenu.setCarried(new ItemStack(Items.BOWL, 5));
        CompoundTag all = new CompoundTag();
        all.putBoolean(CookingPotStation.SCOOP_ALL, true);
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_SCOOP, all);
        helper.assertTrue(setup.pot.getMeal().isEmpty(), "the soup is gone");
        helper.assertTrue(player.inventoryMenu.getCarried().getCount() == 3, "three bowls are left");
        helper.assertTrue(player.getInventory().countItem(fd("beef_stew")) == 2, "two are full");
        helper.succeed();
    }

    private static void fill(CookingPotBlockEntity pot, int servings) {
        Platform.INSTANCE.setItem(pot, 0, new ItemStack(Items.BEEF, servings));
        Platform.INSTANCE.setItem(pot, 1, new ItemStack(Items.CARROT, servings));
        Platform.INSTANCE.setItem(pot, 2, new ItemStack(Items.POTATO, servings));
    }

    /** The spoon at {@code angle} round the pot, most of the way out. */
    private static CompoundTag spoonAt(double angle) {
        CompoundTag where = new CompoundTag();
        where.putFloat(CookingPotStation.STIR_X, (float) (Math.cos(angle) * 0.9));
        where.putFloat(CookingPotStation.STIR_Z, (float) (Math.sin(angle) * 0.9));
        return where;
    }

    public static void theWholeBatchCooksAtOnce(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        fill(setup.pot, 4);
        for (int tick = 0; tick < 320; tick += 20) {
            helper.runAfterDelay(tick, () -> setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_STIR, new CompoundTag()));
        }
        helper.runAfterDelay(200, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.pot.getMeal().isEmpty(), "no serving comes out on its own");
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.PROGRESS) > 0.5F, "the batch is more than half done");
        });
        helper.runAfterDelay(300, () -> {
            // Four servings take twice the time of one, which is two thirds of Farmer's Delight's: 10 s * 2 / 1.5.
            helper.assertTrue(setup.pot.getMeal().getCount() == 4, "all four come out together, in about thirteen seconds");
            helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).isEmpty(), "out of all the ingredients");
            helper.succeed();
        });
    }

    /** One of an ingredient beside more of the others makes one serving: the rest never cooks without it. */
    public static void theSmallestStackSetsTheBatch(GameTestHelper helper) {
        Setup setup = pot(helper);
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(Items.BEEF, 4));
        Platform.INSTANCE.setItem(setup.pot, 1, new ItemStack(Items.CARROT, 4));
        Platform.INSTANCE.setItem(setup.pot, 2, new ItemStack(Items.POTATO, 1));
        stirFor(helper, setup, 200);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(setup.pot.getMeal().getCount() == 1, "one potato makes one stew, got " + setup.pot.getMeal());
            helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).getCount() == 3, "the beef left over stays");
            helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 1).getCount() == 3, "and the carrots");
            helper.succeed();
        });
    }

    /** Food a hopper brings cooks by itself, Farmer's Delight's way, a serving at a time: nobody stirs a pot in a factory. */
    public static void aHopperFedPotCooksByItself(GameTestHelper helper) {
        Setup setup = pot(helper);
        BlockPos above = new BlockPos(1, 3, 1);
        helper.setBlock(above, Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(above, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.BEEF, 2));
        hopper.setItem(1, new ItemStack(Items.CARROT, 2));
        hopper.setItem(2, new ItemStack(Items.POTATO, 2));
        helper.runAfterDelay(270, () -> {
            helper.assertTrue(setup.pot.getMeal().is(fd("beef_stew")) && setup.pot.getMeal().getCount() == 1,
                    "one stew with nobody stirring, at Farmer's Delight's pace, got " + setup.pot.getMeal());
            helper.succeed();
        });
    }

    /** One spoon: while one cook stirs, another's strokes do not move it; once it has been still a moment, they do. */
    public static void oneSpoonOneHand(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player first = helper.makeMockPlayer(GameType.CREATIVE);
        Player second = helper.makeMockPlayer(GameType.CREATIVE);
        CompoundTag left = new CompoundTag();
        left.putFloat(CookingPotStation.STIR_X, -0.5F);
        CompoundTag right = new CompoundTag();
        right.putFloat(CookingPotStation.STIR_X, 0.5F);
        setup.logic.onCustomAction(setup.station, first, CookingPotStation.ACTION_STIR, left);
        setup.logic.onCustomAction(setup.station, second, CookingPotStation.ACTION_STIR, right);
        helper.assertTrue(setup.station.state().getControl(CookingPotStation.SPOON_X) < 0.0F, "the spoon stays in the first cook's hand");
        helper.runAfterDelay(20, () -> {
            setup.logic.onCustomAction(setup.station, second, CookingPotStation.ACTION_STIR, right);
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.SPOON_X) > 0.0F, "left still, it is the second cook's");
            helper.succeed();
        });
    }

    /** A serving taken with a bowl in the hand, without opening the pot, counts as made: it opens the dish's stars. */
    public static void aServingTakenByHandCounts(GameTestHelper helper) {
        Setup setup = pot(helper);
        ServerPlayer player = TestPlayers.serverPlayer(helper, "server");
        Platform.INSTANCE.setItem(setup.pot, CookingPotBlockEntity.MEAL_DISPLAY_SLOT, new ItemStack(fd("beef_stew"), 2));
        CompoundTag saved = setup.pot.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.put("Container", KitchenNbt.save(new ItemStack(Items.BOWL), helper.getLevel().registryAccess()));
        setup.pot.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),saved));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL, 2));
        BlockPos pos = setup.station.pos();
        helper.getLevel().getBlockState(pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        int made = player.getStats().getValue(Stats.ITEM_CRAFTED.get(fd("beef_stew")));
        helper.assertTrue(setup.pot.getMeal().getCount() == 1, "the bowl took a serving");
        helper.assertTrue(made == 1, "and it counts as made, got " + made);
        helper.succeed();
    }

    /** Steady wide rounds for twelve seconds, by a cook who has made {@code made} beef stews before. */
    private static void stirWell(GameTestHelper helper, Setup setup, int made) {
        ServerPlayer player = TestPlayers.serverPlayer(helper, "cook");
        player.awardStat(Stats.ITEM_CRAFTED.get(fd("beef_stew")), made);
        fill(setup.pot, 2);
        for (int tick = 0; tick < 240; tick += 2) {
            double angle = tick * Math.PI / 10.0;
            helper.runAfterDelay(tick, () -> setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_STIR, spoonAt(angle)));
        }
    }

    public static void aCookWhoKnowsTheDishGetsMore(GameTestHelper helper) {
        Setup setup = pot(helper);
        stirWell(helper, setup, 40);
        helper.runAfterDelay(120, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.STARS) > 2.99F, "steady wide rounds earn three stars");
        });
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(setup.pot.getMeal().getCount() == 3, "and two servings come out as three");
            helper.succeed();
        });
    }

    /** A dish picked in the book fills the pot from the inventory: one set, then with shift all the sets there are, whole sets only. */
    public static void theBookFillsThePot(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getInventory().add(new ItemStack(Items.BEEF, 3));
        player.getInventory().add(new ItemStack(Items.CARROT, 5));
        player.getInventory().add(new ItemStack(Items.POTATO, 3));
        CompoundTag fill = new CompoundTag();
        fill.putString(CookingPotStation.DISH, FarmersDelightContent.id("cooking/beef_stew").toString());
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_FILL, fill);
        int first = 0;
        for (int i = 0; i < 3; i++) {
            first += Platform.INSTANCE.getItem(setup.pot, i).getCount();
        }
        helper.assertTrue(first == 3, "a click puts one set in, got " + first);
        fill.putBoolean(CookingPotStation.SCOOP_ALL, true);
        setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_FILL, fill);
        helper.assertTrue(player.getInventory().countItem(Items.BEEF) == 0 && player.getInventory().countItem(Items.CARROT) == 2,
                "shift puts in every whole set, the carrots left over stay");
        helper.assertTrue(Platform.INSTANCE.findRecipe(helper.getLevel(), List.of(Platform.INSTANCE.getItem(setup.pot, 0),
                Platform.INSTANCE.getItem(setup.pot, 1), Platform.INSTANCE.getItem(setup.pot, 2))).isPresent(), "and the pot makes the stew");
        helper.succeed();
    }

    /** A bonus is rounded down and the rest carried on: a single serving at three stars brings half of one, the next makes it whole. */
    public static void aBonusIsCarriedOn(GameTestHelper helper) {
        helper.assertTrue(PotLogic.bonus(1, 3, 0.0F) == 0, "one serving at three stars brings half of one");
        helper.assertTrue(PotLogic.bonus(1, 3, 0.5F) == 1, "the next one makes it whole");
        helper.assertTrue(PotLogic.bonus(4, 2, 0.0F) == 1 && PotLogic.bonus(3, 2, 0.0F) == 0, "two stars one for every four");
        helper.assertTrue(PotLogic.bonus(16, 3, 0.0F) == 8, "a full pot at three stars half again");
        helper.assertTrue(PotLogic.bonus(16, 1, 0.9F) == 0, "one star none");
        helper.succeed();
    }

    /** When the meal's stack fills before the batch is used up, the sets the stars saved go back to the cook. */
    public static void theSetsSavedGoBack(GameTestHelper helper) {
        Setup setup = pot(helper);
        ServerPlayer cook = TestPlayers.serverPlayer(helper, "cook");
        cook.awardStat(Stats.ITEM_CRAFTED.get(fd("beef_stew")), 40);
        setup.station.users.add(cook);
        Platform.INSTANCE.setItem(setup.pot, CookingPotBlockEntity.MEAL_DISPLAY_SLOT, new ItemStack(fd("beef_stew"), 12));
        CompoundTag saved = setup.pot.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.put("Container", KitchenNbt.save(new ItemStack(Items.BOWL), helper.getLevel().registryAccess()));
        setup.pot.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,helper.getLevel().registryAccess(),saved));
        fill(setup.pot, 6);
        for (int tick = 0; tick < 340; tick += 2) {
            double angle = tick * Math.PI / 10.0;
            helper.runAfterDelay(tick, () -> {
                setup.logic.onCustomAction(setup.station, cook, CookingPotStation.ACTION_STIR, spoonAt(angle));
                setup.logic.tick(setup.station);
            });
        }
        helper.runAfterDelay(345, () -> {
            setup.logic.tick(setup.station);
            // Room for four: three sets at three stars make four, the fourth set is saved.
            helper.assertTrue(setup.pot.getMeal().getCount() == 16, "the stack is full, got " + setup.pot.getMeal());
            helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).getCount() == 2, "three sets cooked and one went back, left "
                    + Platform.INSTANCE.getItem(setup.pot, 0));
            helper.assertTrue(cook.getInventory().countItem(Items.BEEF) == 1, "the cook has the saved set");
            helper.succeed();
        });
    }

    /** Milk poured into the pot leaves its bottle: with the cook at the station it goes to them, not onto the floor. */
    public static void theEmptiedContainerGoesToTheCook(GameTestHelper helper) {
        Setup setup = pot(helper);
        ServerPlayer cook = TestPlayers.serverPlayer(helper, "cook");
        setup.station.users.add(cook);
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(fd("milk_bottle")));
        Platform.INSTANCE.setItem(setup.pot, 1, new ItemStack(Items.SUGAR));
        Platform.INSTANCE.setItem(setup.pot, 2, new ItemStack(Items.COCOA_BEANS));
        Platform.INSTANCE.setItem(setup.pot, 3, new ItemStack(Items.COCOA_BEANS));
        for (int tick = 0; tick < 400; tick++) {
            double angle = tick * Math.PI / 10.0;
            helper.runAfterDelay(tick, () -> {
                setup.logic.onCustomAction(setup.station, cook, CookingPotStation.ACTION_STIR, spoonAt(angle));
                setup.logic.tick(setup.station);
            });
        }
        helper.runAfterDelay(405, () -> {
            helper.assertTrue(setup.pot.getMeal().is(fd("hot_cocoa")), "the cocoa is made, got " + setup.pot.getMeal());
            helper.assertTrue(cook.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "the cook has the bottle back");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(setup.station.pos()).inflate(3)).isEmpty(), "nothing lands beside the pot");
            helper.succeed();
        });
    }

    /** Food a cook put in waits for the spoon while they are at the pot; once they walked away it cooks Farmer's Delight's way. */
    public static void aPotLeftByItsCookCooksOn(GameTestHelper helper) {
        Setup setup = pot(helper);
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(Items.BEEF, 2));
        Platform.INSTANCE.setItem(setup.pot, 1, new ItemStack(Items.CARROT, 2));
        Platform.INSTANCE.setItem(setup.pot, 2, new ItemStack(Items.POTATO, 2));
        setup.logic.tick(setup.station);
        helper.runAfterDelay(20, () -> setup.logic.tick(setup.station));
        helper.runAfterDelay(40, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.PROGRESS) == 0.0F, "with the cook there it waits");
        });
        // The cook leaves at 40: two seconds later it cooks, a serving at a time at Farmer's Delight's pace (ten seconds).
        helper.runAfterDelay(320, () -> {
            helper.assertTrue(setup.pot.getMeal().is(fd("beef_stew")) && setup.pot.getMeal().getCount() == 1,
                    "one stew without the cook, got " + setup.pot.getMeal());
            helper.succeed();
        });
    }

    public static void theFirstTimeHasOneStar(GameTestHelper helper) {
        Setup setup = pot(helper);
        stirWell(helper, setup, 0);
        helper.runAfterDelay(120, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.STARS) == 1.0F, "a dish cooked for the first time opens one star");
            helper.assertTrue(setup.station.state().data().getIntOr(CookingPotStation.OPEN_STARS, 0) == 1, "and the badge shows only that one");
        });
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(setup.pot.getMeal().getCount() == 2, "one star brings nothing more");
            helper.succeed();
        });
    }

    public static void onlyGoodStirringEarnsStars(GameTestHelper helper) {
        StirSkill rounds = new StirSkill();
        StirSkill shaking = new StirSkill();
        StirSkill middle = new StirSkill();
        StirSkill cold = new StirSkill();
        for (int tick = 0; tick <= 200; tick += 2) {
            double angle = tick * Math.PI / 10.0;
            rounds.move(tick, (float) Math.cos(angle) * 0.9F, (float) Math.sin(angle) * 0.9F, true, 3);
            double back = Math.sin(tick * Math.PI / 6.0);
            shaking.move(tick, (float) Math.cos(back) * 0.9F, (float) Math.sin(back) * 0.9F, true, 3);
            middle.move(tick, (float) Math.cos(angle) * 0.2F, (float) Math.sin(angle) * 0.2F, true, 3);
            cold.move(tick, (float) Math.cos(angle) * 0.9F, (float) Math.sin(angle) * 0.9F, false, 3);
        }
        helper.assertTrue(rounds.grade(200) == 3, "wide rounds one way earn three stars, got " + rounds.stars(200));
        helper.assertTrue(shaking.grade(200) == 0, "shaking back and forth earns none, got " + shaking.stars(200));
        helper.assertTrue(middle.grade(200) == 0, "circling the middle earns none, got " + middle.stars(200));
        helper.assertTrue(cold.grade(200) == 0, "stirring a pot that does not cook earns none");
        helper.assertTrue(rounds.grade(230) == 2, "a pause takes a star, got " + rounds.stars(230));
        helper.assertTrue(rounds.grade(400) == 0, "a long one all of them");
        helper.succeed();
    }

    /** Strokes that reach the server bunched by the network are judged by the cook's own clock: an even hand still earns three stars. */
    public static void theCooksClockJudgesThePace(GameTestHelper helper) {
        StirSkill bunched = new StirSkill();
        for (int tick = 0; tick <= 200; tick += 2) {
            double angle = tick * Math.PI / 10.0;
            long arrived = tick / 6 * 6 + 1;
            bunched.move(arrived, tick, (float) Math.cos(angle) * 0.9F, (float) Math.sin(angle) * 0.9F, true, 3);
        }
        helper.assertTrue(bunched.grade(199) == 3, "steady rounds that arrive in bunches earn three stars, got " + bunched.stars(199));
        helper.succeed();
    }

    public static void nothingCooksWithoutStirring(GameTestHelper helper) {
        Setup setup = pot(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(Items.BEEF));
        Platform.INSTANCE.setItem(setup.pot, 1, new ItemStack(Items.CARROT));
        Platform.INSTANCE.setItem(setup.pot, 2, new ItemStack(Items.POTATO));
        // A cook at the station put it in: it waits for the spoon.
        setup.logic.tick(setup.station);
        helper.runAfterDelay(30, () -> {
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.PROGRESS) == 0.0F, "left alone it does not cook");
            CompoundTag where = new CompoundTag();
            where.putFloat(CookingPotStation.STIR_X, 0.5F);
            setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_STIR, where);
            setup.logic.tick(setup.station);
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.STIRRING) > 0.5F, "the spoon counts");
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.SPOON_X) > 0.4F, "and everybody sees where it is");
        });
        helper.runAfterDelay(60, () -> {
            setup.logic.tick(setup.station);
            float stirred = setup.station.state().getControl(CookingPotStation.PROGRESS);
            helper.assertTrue(stirred > 0.0F, "stirred it cooks, got " + setup.station.state().data() + " pot="+setup.pot.saveWithoutMetadata(helper.getLevel().registryAccess()));
        });
        helper.runAfterDelay(100, () -> {
            setup.logic.tick(setup.station);
            float after = setup.station.state().getControl(CookingPotStation.PROGRESS);
            helper.assertTrue(setup.station.state().getControl(CookingPotStation.STIRRING) < 0.5F, "the stir runs out");
            helper.runAfterDelay(20, () -> {
                setup.logic.tick(setup.station);
                helper.assertTrue(setup.station.state().getControl(CookingPotStation.PROGRESS) == after, "and it stops where it got to");
                helper.succeed();
            });
        });
    }

    public static void theStationReadsHeat(GameTestHelper helper) {
        Setup setup = pot(helper);
        setup.logic.tick(setup.station);
        helper.assertTrue(setup.station.state().getControl(CookingPotStation.HEATED) > 0.5F, "a lit campfire heats the pot");
        int water = setup.station.state().getTank(CookingPotStation.BROTH).amount();
        helper.assertTrue(water > 0, "there is water in an empty pot");
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(Items.CARROT));
        setup.logic.tick(setup.station);
        helper.assertTrue(setup.station.state().getTank(CookingPotStation.BROTH).amount() > water, "ingredients raise it");
        helper.succeed();
    }

    /** A recipe as a data pack or another mod would add it, put among the server's recipes. */
    private static void addRecipe(GameTestHelper helper, String name, ItemStack result, ItemStack container, int cookTime, Ingredient... inputs) {
        RecipeManager recipes = helper.getLevel().recipeAccess();
        Identifier id = HandsOnDelight.id("test_" + name);
        List<RecipeHolder<?>> all = new ArrayList<>(recipes.getRecipes());
        all.removeIf(recipe -> recipe.id().identifier().equals(id));
        all.add(new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,id), new CookingPotRecipe("", CookingPotBookCategory.MISC, List.of(inputs), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result),
                container.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(container)), 0.1F, cookTime)));
        var registry = new net.minecraft.core.MappedRegistry<net.minecraft.world.item.crafting.Recipe<?>>(net.minecraft.core.registries.Registries.RECIPE, com.mojang.serialization.Lifecycle.stable());
        for(var holder:all) net.minecraft.core.Registry.register(registry,holder.id(),holder.value());
        registry.freeze();
        ((com.pockyl.handson_delight.mixin.RecipeManagerAccessor)recipes).handson$setRecipes(net.minecraft.world.item.crafting.RecipeMap.create(registry));
    }

    private static void stirFor(GameTestHelper helper, Setup setup, int ticks) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        for (int tick = 0; tick < ticks; tick += 20) {
            helper.runAfterDelay(tick, () -> setup.logic.onCustomAction(setup.station, player, CookingPotStation.ACTION_STIR, new CompoundTag()));
        }
    }

    /** A modded drink: two servings a time, served in a bottle, one ingredient a tag. */
    public static void aModdedRecipeCooksAsABatch(GameTestHelper helper) {
        addRecipe(helper, "nectar", new ItemStack(Items.HONEY_BOTTLE, 2), new ItemStack(Items.GLASS_BOTTLE), 40, Ingredient.of(Items.SUGAR),
                Ingredient.of(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ITEM).getOrThrow(net.minecraft.tags.BlockItemTags.SMALL_FLOWERS.item())));
        Setup setup = pot(helper);
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(Items.SUGAR, 5));
        Platform.INSTANCE.setItem(setup.pot, 1, new ItemStack(Items.DANDELION, 5));
        helper.assertFalse(setup.logic.mayPlace(setup.station, helper.makeMockPlayer(GameType.CREATIVE), SlotRef.of(CookingPotStation.INGREDIENTS, 2),
                new ItemStack(Items.GLASS_BOTTLE)), "what the drink is served in does not go in");
        stirFor(helper, setup, 140);
        helper.runAfterDelay(140, () -> {
            // Five servings of a 2 s recipe: 2 s * sqrt(5) / 1.5, about three seconds.
            helper.assertTrue(setup.pot.getMeal().is(Items.HONEY_BOTTLE) && setup.pot.getMeal().getCount() == 10,
                    "five servings of two come out together, got " + setup.pot.getMeal());
            helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).isEmpty() && Platform.INSTANCE.getItem(setup.pot, 1).isEmpty(),
                    "out of all the ingredients");
            helper.succeed();
        });
    }

    /** A modded dish with no container goes straight out, as Farmer's Delight serves those, the whole batch. */
    public static void aDishWithoutAContainerComesOutWhole(GameTestHelper helper) {
        addRecipe(helper, "loaf", new ItemStack(Items.BREAD), ItemStack.EMPTY, 40, Ingredient.of(Items.WHEAT), Ingredient.of(Items.EGG));
        Setup setup = pot(helper);
        Platform.INSTANCE.setItem(setup.pot, 0, new ItemStack(Items.WHEAT, 20));
        Platform.INSTANCE.setItem(setup.pot, 1, new ItemStack(Items.EGG, 16));
        stirFor(helper, setup, 200);
        helper.runAfterDelay(200, () -> {
            ItemStack served = Platform.INSTANCE.getItem(setup.pot, CookingPotBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(served.is(Items.BREAD) && served.getCount() == 16, "sixteen loaves, as many as there are eggs, got " + served);
            helper.assertTrue(Platform.INSTANCE.getItem(setup.pot, 0).getCount() == 4, "the wheat left over stays");
            helper.succeed();
        });
    }

    private static final class TestStation implements StationContext {
        private final Level level;
        private final BlockPos pos;
        private final StationState state = new StationState(CookingPotStation.SCHEMA);
        private final List<Player> users = new ArrayList<>();

        TestStation(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public StationType type() {
            return CookingPotStation.TYPE;
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public BlockPos pos() {
            return pos;
        }

        @Override
        public BlockState blockState() {
            return level.getBlockState(pos);
        }

        @Override
        public Direction facing() {
            return Direction.NORTH;
        }

        @Override
        public StationState state() {
            return state;
        }

        @Override
        public Collection<? extends Player> users() {
            return users;
        }

        @Override
        public void effect(Identifier effect, String node, CompoundTag data) {
        }
    }
}
