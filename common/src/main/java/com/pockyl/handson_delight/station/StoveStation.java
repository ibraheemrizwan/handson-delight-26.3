package com.pockyl.handson_delight.station;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotRules;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnDelight;

/**
 * The stove's top as a station: six pieces grill on it, each turned by the cook when its lower side is done (as the skillet's food,
 * but piece by piece, as on a grill). Its six slots are windows on Farmer's Delight's own: what is put on its way cooks its way.
 */
public final class StoveStation {
    public static final Identifier ID = HandsOnDelight.id("stove");

    /** What lies on the grill: Farmer's Delight's six slots, one item each. */
    public static final String GRILL = "grill";
    /** Where a stack let go over the grill is caught: one slot that never keeps anything, as at the board. */
    public static final String HAND = "hand";

    /** Data for the clients, per place {@code i}: how far each side is done ({@link #DOWN}{@code i}, {@link #UP}{@code i}), by hand. */
    public static final String DOWN = "down";
    public static final String UP = "up";
    public static final String BY_HAND = "by_hand";
    public static final String LIT = "lit";

    /**
     * Custom actions. {@code place}: the stack on the cursor goes onto the grill, one onto every free place, or with {@link #ONE} one
     * onto {@link #PLACE}; with {@link #FROM}, the stack in that inventory slot (shift + click in the inventory).
     */
    public static final String ACTION_PLACE = "place";
    /** {@code turn}: the piece on {@link #PLACE} is turned over. */
    public static final String ACTION_TURN = "turn";
    /** {@code take}: the piece on {@link #PLACE} goes back to the inventory. */
    public static final String ACTION_TAKE = "take";
    public static final String ONE = "one";
    public static final String FROM = "from";
    public static final String PLACE = "place";

    /** Effects for everybody who sees the stove. Data: {@link #PLACE}, and {@link #STACK} of what is about. */
    public static final Identifier EFFECT_PLACED = HandsOnDelight.id("stove_placed");
    public static final Identifier EFFECT_REFUSED = HandsOnDelight.id("stove_refused");
    public static final Identifier EFFECT_TURNED = HandsOnDelight.id("stove_turned");
    /** A turn the server did not take (the piece was still in the air): the cook's client lays it back. */
    public static final Identifier EFFECT_UNTURNED = HandsOnDelight.id("stove_unturned");
    public static final Identifier EFFECT_TAKEN = HandsOnDelight.id("stove_taken");
    /** A piece is done on both sides: {@link #STACK} what it became, still on the grill. */
    public static final Identifier EFFECT_GRILLED = HandsOnDelight.id("grilled");
    public static final String STACK = "stack";

    public static final StationSchema SCHEMA = StationSchema.builder()
            .slots(GRILL, Grill.PLACES, SlotRules.DEFAULT.kept().accepts(stack -> false))
            .slots(HAND, 1, SlotRules.DEFAULT)
            .build();

    public static final StationType TYPE = Diegesis.station(ID)
            .block(FarmersDelightContent.STOVE)
            .schema(SCHEMA)
            .logic(StoveLogic::new)
            .orientation(StationType.Orientation.BLOCK)
            .openWhen(StoveStation::opens)
            .idleLogic()
            .build();

    private StoveStation() {
    }

    public static void register() {
        Diegesis.register(TYPE);
    }

    public static SlotRef place(int index) {
        return SlotRef.of(GRILL, index);
    }

    /**
     * With food or an empty hand: the grill. Sneaking keeps the stove's own use, and so does anything else in the hand: a flint and
     * steel lights it, a shovel puts it out. A pot or skillet standing on it covers the grill: it is theirs to open.
     */
    private static boolean opens(Player player, BlockPos pos, BlockState state) {
        if (player.isSecondaryUseActive() || !(player.level().getBlockEntity(pos) instanceof AbstractStoveBlockEntity stove)
                || StoveCooking.isCovered(stove)) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        return held.isEmpty() || SkilletCooking.recipe(player.level(), held).isPresent();
    }
}
