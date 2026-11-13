package com.pockyl.handson_delight.station;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotRules;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnDelight;

/**
 * The skillet as a station: food goes into the pan, fries on the side that lies on it, and is turned with a toss of the pan by its
 * handle (see {@link SkilletCooking}). The pan's slot is a window on Farmer's Delight's own; the scene draws the skillet itself
 * while somebody works at it, so the pan can move. The scene turns with the block: the camera stands at the handle, which points
 * back at the player who placed the skillet.
 */
public final class SkilletStation {
    public static final Identifier ID = HandsOnDelight.id("skillet");

    /** What lies in the pan: Farmer's Delight's own slot, a stack. */
    public static final String PAN = "pan";
    /** Where a stack let go over the pan is caught: one slot that never keeps anything, as at the board. */
    public static final String HAND = "hand";
    public static final SlotRef PAN_SLOT = SlotRef.of(PAN, 0);

    /** Data for the clients: how far each side is done, how many fry together, whether it fries by hand, whether it is hot. */
    public static final String DOWN = "down";
    public static final String UP = "up";
    public static final String BATCH = "batch";
    public static final String BY_HAND = "by_hand";
    public static final String HEATED = "heated";

    /**
     * Custom actions. {@code place}: the stack on the cursor goes into the pan, {@link #ONE} of it or all; with {@link #FROM}, the
     * stack in that inventory slot (shift + click in the inventory).
     */
    public static final String ACTION_PLACE = "place";
    /** {@code toss}: the pan is tossed by its handle, {@link #STRENGTH} 0 to 1 how hard; the food turns over. */
    public static final String ACTION_TOSS = "toss";
    /** {@code take}: what lies in the pan goes back to the inventory. */
    public static final String ACTION_TAKE = "take";
    public static final String ONE = "one";
    public static final String FROM = "from";
    public static final String STRENGTH = "strength";

    /** Effects for everybody who sees the skillet. */
    public static final Identifier EFFECT_PLACED = HandsOnDelight.id("skillet_placed");
    public static final Identifier EFFECT_REFUSED = HandsOnDelight.id("skillet_refused");
    public static final Identifier EFFECT_TOSSED = HandsOnDelight.id("tossed");
    /** A toss the server did not take (the last one was still in the air): the cook's client turns the food back. */
    public static final Identifier EFFECT_UNTOSSED = HandsOnDelight.id("untossed");
    public static final Identifier EFFECT_TAKEN = HandsOnDelight.id("skillet_taken");
    /** A batch is done: {@link #STACK} what it became, {@link #TO_COOK} whether it went to the cook or jumped out of the pan. */
    public static final Identifier EFFECT_FRIED = HandsOnDelight.id("fried");
    public static final String STACK = "stack";
    public static final String TO_COOK = "to_cook";
    /** With {@link #EFFECT_FRIED}: the done batch stays in the pan for the cook to take. */
    public static final String IN_PAN = "in_pan";

    public static final StationSchema SCHEMA = StationSchema.builder()
            .slots(PAN, 1, SlotRules.DEFAULT.kept().accepts(stack -> false))
            .slots(HAND, 1, SlotRules.DEFAULT)
            .build();

    public static final StationType TYPE = Diegesis.station(ID)
            .block(FarmersDelightContent.SKILLET)
            .schema(SCHEMA)
            .logic(SkilletLogic::new)
            .orientation(StationType.Orientation.BLOCK)
            .openWhen(SkilletStation::opens)
            .hidesBlock()
            .idleLogic()
            .build();

    private SkilletStation() {
    }

    public static void register() {
        Diegesis.register(TYPE);
    }

    /** Sneaking keeps the skillet's own use: food in and out with a right click, cooked Farmer's Delight's way. */
    private static boolean opens(Player player, BlockPos pos, BlockState state) {
        return !player.isSecondaryUseActive();
    }
}
