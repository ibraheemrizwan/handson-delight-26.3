package com.pockyl.handson_delight.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.AbstractStoveBlock;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;

import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.platform.Platform;
import com.pockyl.handson_delight.station.Grill;
import com.pockyl.handson_delight.station.StoveCooking;
import com.pockyl.handson_delight.station.StoveLogic;
import com.pockyl.handson_delight.station.StoveStation;
import com.pockyl.handson_delight.station.StoveTop;

import java.util.Collection;
import java.util.List;

/** The stove's rules against a real lit stove of Farmer's Delight, on every loader. */
public final class StoveTests {
    /** A side of beef: Farmer's Delight's 600 ticks at the station's pace, halved. */
    private static final int BEEF_SIDE = StoveCooking.sideTicks(600);

    private StoveTests() {
    }

    private record Top(Station station, StoveLogic logic, AbstractStoveBlockEntity stove, Player cook) {
        void act(String action, CompoundTag data) {
            logic.onCustomAction(station, cook, action, data);
        }

        void place(Item item, int count) {
            cook.inventoryMenu.setCarried(new ItemStack(item, count));
            act(StoveStation.ACTION_PLACE, new CompoundTag());
        }

        void at(String action, int place) {
            CompoundTag data = new CompoundTag();
            data.putInt(StoveStation.PLACE, place);
            act(action, data);
        }

        ItemStack on(int place) {
            return Platform.INSTANCE.getItem(stove, place);
        }

        Grill grill() {
            return StoveCooking.grill(stove);
        }
    }

    private static Top stove(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, FarmersDelightContent.STOVE.get().defaultBlockState().setValue(AbstractStoveBlock.LIT, true));
        // 1.21's test frame lays its barrier ceiling right above: the top would count as covered.
        helper.setBlock(at.above(), Blocks.AIR);
        BlockPos pos = helper.absolutePos(at);
        Station station = new Station(helper.getLevel(), pos);
        AbstractStoveBlockEntity stove = (AbstractStoveBlockEntity) helper.getLevel().getBlockEntity(pos);
        return new Top(station, new StoveLogic(station), stove, helper.makeMockPlayer(GameType.CREATIVE));
    }

    public static void aPieceGrillsInHalvesAndWaitsToBeTurned(GameTestHelper helper) {
        Top top = stove(helper);
        top.place(Items.BEEF, 3);
        helper.assertTrue(top.on(0).is(Items.BEEF) && top.on(1).is(Items.BEEF) && top.on(2).is(Items.BEEF) && top.on(3).isEmpty(),
                "a stack goes on one by one, a piece a place");
        helper.assertTrue(top.cook.inventoryMenu.getCarried().isEmpty() && top.grill().byHand(0), "all three, laid by hand");
        helper.runAfterDelay(BEEF_SIDE + 10, () -> {
            helper.assertTrue(top.grill().waits(0) && top.grill().up(0) == 0.0F, "the lower side is done, the piece waits");
            top.at(StoveStation.ACTION_TURN, 0);
            helper.assertTrue(top.grill().up(0) >= 1.0F && top.grill().down(0) == 0.0F, "a click turns it over");
        });
        helper.runAfterDelay(2 * BEEF_SIDE + 25, () -> {
            helper.assertTrue(top.on(0).is(Items.COOKED_BEEF), "turned once, it is done and waits on the grill");
            helper.assertTrue(top.on(1).is(Items.BEEF) && top.grill().waits(1), "the others still wait to be turned");
            top.at(StoveStation.ACTION_TAKE, 0);
            helper.assertTrue(top.cook.getInventory().countItem(Items.COOKED_BEEF) == 1 && top.on(0).isEmpty(), "taken");
            helper.assertFalse(top.grill().byHand(0), "the place starts over");
            helper.succeed();
        });
    }

    public static void farmersDelightsWayStaysItsOwn(GameTestHelper helper) {
        Top top = stove(helper);
        ItemStack beef = new ItemStack(Items.BEEF);
        top.stove.placeFood(top.cook, beef, top.stove.getCookingRecipe(beef).orElseThrow());
        helper.assertTrue(top.on(0).is(Items.BEEF) && !top.grill().byHand(0), "Farmer's Delight's own right click: its way");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(((StoveTop) top.stove).handson$progress()[0] > 10, "and it counts its own time");
            top.place(Items.CHICKEN, 1);
            helper.assertTrue(top.on(1).is(Items.CHICKEN) && top.grill().byHand(1), "a piece laid at the station beside it is the cook's");
            helper.succeed();
        });
    }

    public static void onlyFoodGoesOnAnOpenGrill(GameTestHelper helper) {
        Top top = stove(helper);
        top.place(Items.STICK, 2);
        helper.assertTrue(top.on(0).isEmpty() && top.cook.inventoryMenu.getCarried().getCount() == 2, "no stick on the grill");
        top.place(Items.BEEF, 10);
        helper.assertTrue(top.cook.inventoryMenu.getCarried().getCount() == 10 - Grill.PLACES, "six places, six pieces");
        top.at(StoveStation.ACTION_TAKE, 4);
        CompoundTag one = new CompoundTag();
        one.putBoolean(StoveStation.ONE, true);
        one.putInt(StoveStation.PLACE, 4);
        top.cook.inventoryMenu.setCarried(new ItemStack(Items.PORKCHOP, 3));
        top.act(StoveStation.ACTION_PLACE, one);
        helper.assertTrue(top.on(4).is(Items.PORKCHOP) && top.cook.inventoryMenu.getCarried().getCount() == 2, "one onto the place picked");
        helper.setBlock(new BlockPos(1, 2, 1), FarmersDelightContent.COOKING_POT.get());
        helper.assertTrue(StoveCooking.isCovered(top.stove), "a pot on top covers the grill");
        top.place(Items.BEEF, 1);
        helper.assertTrue(top.cook.inventoryMenu.getCarried().getCount() == 1, "nothing goes onto a covered stove");
        helper.succeed();
    }

    /** An egg would run through the bars: the station keeps it off the grill, Farmer's Delight's own use still lays it. */
    public static void anEggStaysOffTheGrill(GameTestHelper helper) {
        Top top = stove(helper);
        top.place(Items.EGG, 3);
        helper.assertTrue(top.on(0).isEmpty() && top.cook.inventoryMenu.getCarried().getCount() == 3, "no egg on the grill");
        helper.assertTrue(top.station.effects.contains(StoveStation.EFFECT_REFUSED), "it bounces back to the hand");
        ItemStack egg = new ItemStack(Items.EGG);
        top.stove.placeFood(top.cook, egg, top.stove.getCookingRecipe(egg).orElseThrow());
        helper.assertTrue(top.on(0).is(Items.EGG) && !top.grill().byHand(0), "Farmer's Delight's right click is its own way");
        helper.succeed();
    }

    /** The stove's station as the library would hold it, without a player at it. */
    private static final class Station implements StationContext {
        private final Level level;
        private final BlockPos pos;
        private final StationState state = new StationState(StoveStation.SCHEMA);

        Station(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public StationType type() {
            return StoveStation.TYPE;
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

        /** The effects sent, for the tests to look at. */
        final java.util.List<Identifier> effects = new java.util.ArrayList<>();

        @Override
        public void effect(Identifier effect, String node, CompoundTag data) {
            effects.add(effect);
        }
    }
}
