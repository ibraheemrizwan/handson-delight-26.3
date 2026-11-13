package com.pockyl.handson_delight.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.station.Frying;
import com.pockyl.handson_delight.station.SkilletCooking;
import com.pockyl.handson_delight.station.SkilletLogic;
import com.pockyl.handson_delight.station.SkilletStation;

import java.util.Collection;
import java.util.List;

/** The skillet's rules against a real skillet of Farmer's Delight on a lit campfire, on every loader. */
public final class SkilletTests {
    /** One beef fries a side in 40 ticks, two in 57: Farmer's Delight's 120 at the station's pace. */
    private static final int TWO_BEEF_SIDE = SkilletCooking.sideTicks(120, 2);

    private SkilletTests() {
    }

    private record Pan(Station station, SkilletLogic logic, SkilletBlockEntity skillet, Player cook) {
        void act(String action, CompoundTag data) {
            logic.onCustomAction(station, cook, action, data);
        }

        void place(Item item, int count) {
            cook.inventoryMenu.setCarried(new ItemStack(item, count));
            act(SkilletStation.ACTION_PLACE, new CompoundTag());
        }

        void toss() {
            act(SkilletStation.ACTION_TOSS, new CompoundTag());
        }

        Frying frying() {
            return SkilletCooking.frying(skillet);
        }

        int jumpedOut(Item item) {
            return station.level.getEntitiesOfClass(ItemEntity.class, new AABB(station.pos).inflate(3)).stream()
                    .filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
        }
    }

    private static Pan pan(GameTestHelper helper) {
        BlockPos fire = new BlockPos(1, 1, 1);
        helper.setBlock(fire, Blocks.CAMPFIRE);
        helper.setBlock(fire.above(), FarmersDelightContent.SKILLET.get());
        BlockPos pos = helper.absolutePos(fire.above());
        Station station = new Station(helper.getLevel(), pos);
        SkilletBlockEntity skillet = (SkilletBlockEntity) helper.getLevel().getBlockEntity(pos);
        return new Pan(station, new SkilletLogic(station), skillet, helper.makeMockPlayer(GameType.CREATIVE));
    }

    public static void aBatchFriesInHalvesAndWaitsToBeTurned(GameTestHelper helper) {
        Pan pan = pan(helper);
        pan.place(Items.BEEF, 2);
        helper.assertTrue(pan.skillet.getStoredStack().getCount() == 2 && pan.cook.inventoryMenu.getCarried().isEmpty(),
                "both go into the pan");
        helper.assertTrue(pan.frying().byHand(), "put in at the station, it fries by hand");
        helper.runAfterDelay(TWO_BEEF_SIDE + 10, () -> {
            helper.assertTrue(pan.frying().down() >= 1.0F && pan.frying().up() == 0.0F, "the side on the pan is done, the top raw");
            helper.assertTrue(pan.frying().waits(), "it waits to be turned");
        });
        helper.runAfterDelay(2 * TWO_BEEF_SIDE + 20, () -> {
            helper.assertTrue(pan.skillet.getStoredStack().is(Items.BEEF) && pan.skillet.getStoredStack().getCount() == 2,
                    "untouched, nothing more happens: it is not done");
            pan.toss();
            helper.assertTrue(pan.frying().up() >= 1.0F && pan.frying().down() == 0.0F, "a toss turns it over");
        });
        helper.runAfterDelay(3 * TWO_BEEF_SIDE + 35, () -> {
            helper.assertTrue(pan.skillet.getStoredStack().is(Items.COOKED_BEEF) && pan.skillet.getStoredStack().getCount() == 2,
                    "both sides done: both are steaks at once, waiting in the pan");
            helper.assertTrue(pan.jumpedOut(Items.COOKED_BEEF) == 0, "nothing jumps out: the cook takes it");
        });
        helper.runAfterDelay(3 * TWO_BEEF_SIDE + 60, () -> {
            helper.assertTrue(pan.skillet.getStoredStack().is(Items.COOKED_BEEF), "still there a while later");
            pan.act(SkilletStation.ACTION_TAKE, new CompoundTag());
            helper.assertTrue(pan.cook.getInventory().countItem(Items.COOKED_BEEF) == 2 && pan.skillet.getStoredStack().isEmpty(),
                    "taken into the inventory");
            helper.assertFalse(pan.frying().byHand(), "an empty pan starts over");
            pan.place(Items.BEEF, 20);
            helper.assertTrue(pan.skillet.getStoredStack().getCount() == SkilletCooking.BATCH
                    && pan.cook.inventoryMenu.getCarried().getCount() == 20 - SkilletCooking.BATCH, "a batch at most goes in");
            helper.succeed();
        });
    }

    /** An egg fries sunny side up: through from below in the time both sides of a steak take, never waiting for a toss. */
    public static void anEggFriesWithoutATurn(GameTestHelper helper) {
        Pan pan = pan(helper);
        pan.place(Items.EGG, 2);
        helper.assertTrue(pan.skillet.getStoredStack().is(Items.EGG) && pan.skillet.getStoredStack().getCount() == 2,
                "both eggs go into the pan, got " + pan.skillet.getStoredStack());
        helper.runAfterDelay(TWO_BEEF_SIDE + 5, () -> {
            helper.assertTrue(pan.skillet.getStoredStack().is(Items.EGG), "past one side's time it still fries");
            helper.assertFalse(pan.frying().waits(), "and does not wait to be turned");
            helper.assertTrue(pan.frying().up() == pan.frying().down() && pan.frying().down() > 0.4F, "it cooks through from below");
        });
        helper.runAfterDelay(2 * TWO_BEEF_SIDE + 10, () -> {
            ItemStack inPan = pan.skillet.getStoredStack();
            helper.assertTrue(inPan.getCount() == 2 && !inPan.is(Items.EGG), "done in the time of both sides, never tossed, got " + inPan);
            helper.succeed();
        });
    }

    public static void farmersDelightsRightClickCooksItsWay(GameTestHelper helper) {
        Pan pan = pan(helper);
        pan.skillet.addItemToCook(new ItemStack(Items.BEEF), pan.cook, helper.getLevel());
        helper.assertTrue(pan.skillet.getStoredStack().is(Items.BEEF), "Farmer's Delight takes it");
        helper.assertFalse(pan.frying().byHand(), "and cooks it");
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(pan.skillet.getStoredStack().isEmpty() && pan.jumpedOut(Items.COOKED_BEEF) == 1,
                    "done in its own time, never waiting to be turned");
            helper.succeed();
        });
    }

    public static void theFoodComesBackAndOnlyFoodGoesIn(GameTestHelper helper) {
        Pan pan = pan(helper);
        pan.place(Items.STICK, 3);
        helper.assertTrue(pan.skillet.getStoredStack().isEmpty(), "a stick is no food for the pan");
        helper.assertTrue(pan.cook.inventoryMenu.getCarried().getCount() == 3, "it stays in the hand");
        pan.cook.inventoryMenu.setCarried(ItemStack.EMPTY);
        pan.place(Items.CHICKEN, 3);
        pan.cook.inventoryMenu.setCarried(new ItemStack(Items.BEEF));
        pan.act(SkilletStation.ACTION_PLACE, new CompoundTag());
        helper.assertTrue(pan.skillet.getStoredStack().is(Items.CHICKEN) && pan.cook.inventoryMenu.getCarried().is(Items.BEEF),
                "one kind at a time");
        pan.cook.inventoryMenu.setCarried(ItemStack.EMPTY);
        pan.cook.getInventory().setItem(5, new ItemStack(Items.CHICKEN, 2));
        CompoundTag shift = new CompoundTag();
        shift.putInt(SkilletStation.FROM, 5);
        pan.act(SkilletStation.ACTION_PLACE, shift);
        helper.assertTrue(pan.skillet.getStoredStack().getCount() == 5 && pan.cook.getInventory().getItem(5).isEmpty(),
                "shift + click in the inventory puts the whole stack in");
        pan.toss();
        helper.assertTrue(pan.frying().byHand(), "tossed");
        pan.act(SkilletStation.ACTION_TAKE, new CompoundTag());
        helper.assertTrue(pan.skillet.getStoredStack().isEmpty(), "taken out");
        helper.assertTrue(pan.cook.getInventory().countItem(Items.CHICKEN) == 5, "all of it, into the inventory");
        helper.assertFalse(pan.frying().byHand(), "the pan starts over");
        helper.succeed();
    }

    public static void moreFoodTakesLongerButKeepsWhatWasDone(GameTestHelper helper) {
        Pan pan = pan(helper);
        pan.place(Items.BEEF, 1);
        helper.runAfterDelay(20, () -> {
            float one = pan.frying().down();
            helper.assertTrue(one > 0.3F, "one beef is well on its way");
            pan.place(Items.BEEF, 3);
            helper.assertTrue(Math.abs(pan.frying().down() - one) < 0.001F, "until the next tick nothing changes");
        });
        helper.runAfterDelay(21, () -> {
            float four = pan.frying().down();
            helper.assertTrue(four < 0.3F && four > 0.15F, "four fry slower: how far it got is shared out, not lost");
            helper.succeed();
        });
    }

    /** The skillet's station as the library would hold it, without a player at it. */
    private static final class Station implements StationContext {
        private final Level level;
        private final BlockPos pos;
        private final StationState state = new StationState(SkilletStation.SCHEMA);

        Station(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public StationType type() {
            return SkilletStation.TYPE;
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
            return List.of();
        }

        @Override
        public void effect(Identifier effect, String node, CompoundTag data) {
        }
    }
}
