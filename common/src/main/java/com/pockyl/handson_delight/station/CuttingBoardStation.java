package com.pockyl.handson_delight.station;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotRules;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.platform.Platform;

/**
 * The cutting board as a station: what lies on it is cut with chops of the knife, and food that Farmer's Delight makes in the crafting
 * table is put together on it, layer by layer. The board's one slot is a window on Farmer's Delight's own: hoppers, Create's deployers
 * and players without this mod use it as before.
 */
public final class CuttingBoardStation {
    public static final Identifier ID = HandsOnDelight.id("cutting_board");

    /** What lies on the board to be cut: Farmer's Delight's own slot, a stack. */
    public static final String BOARD = "board";
    /** What a dish is being put together from, bottom first: kept in the board (see {@link BoardLayers}). */
    public static final String LAYERS = "layers";
    /** Feasts, pies and the cake take nine things, as many as a crafting grid holds. */
    public static final int MOST_LAYERS = 9;
    /**
     * Where a stack let go on the board is caught: one slot that never keeps anything. The logic takes what it is given there, and the
     * board's own slots refuse stacks the library would move by itself: its guess (a shift-click from the inventory) is never shown.
     */
    public static final String HAND = "hand";
    public static final SlotRef BOARD_SLOT = SlotRef.of(BOARD, 0);

    /** Data the logic tells the clients: the dish the layers make. */
    public static final String DISH = "dish";
    /**
     * The board's own recipes, which a dedicated server sends nowhere else, for the book: {@link #EFFECT_BOOK} brings them when a cook
     * comes to the board, once, not with every change of the board's data.
     */
    public static final String BOOK = "book";
    /** How many items of the stack one cut goes through, and how many dishes one stack of layers makes at most. */
    public static final int BATCH = 16;

    /**
     * Custom actions. {@code place}: the stack on the cursor goes onto the board, {@link #ONE} of it or all; with {@link #FROM}, the
     * stack in that inventory slot instead (shift + click in the drawer).
     */
    public static final String ACTION_PLACE = "place";
    /** {@code chop}: the knife on the cursor comes down on mark {@link #MARK} (the next one to cut), at {@link #AT}, scene pixels along z. */
    public static final String ACTION_CHOP = "chop";
    /** {@code take}: the top layer, or what lies to be cut, back into the inventory. */
    public static final String ACTION_TAKE = "take";
    /** {@code assemble}: the layers become the dish they make. */
    public static final String ACTION_ASSEMBLE = "assemble";
    /**
     * {@code pick}: the dish picked in the book ({@link #DISH}: its recipe id) is made as soon as the layers make it, and what it takes
     * is laid from the inventory: one set, or a batch with {@link #ALL}.
     */
    public static final String ACTION_PICK = "pick";
    public static final String ALL = "all";
    public static final String ONE = "one";
    public static final String FROM = "from";
    public static final String AT = "at";
    public static final String MARK = "mark";
    /** Data: how many of the marks are cut; they are cut in order, and only the next one is shown. */
    public static final String MARKS = "marks";
    /**
     * Data: where the marks lie, chosen by the server for every item: where each crosses the item's middle, as fractions of its length
     * from its far end, the next one first to cut. The server cannot see the icon, so the client lays them over its solid pixels.
     */
    public static final String SPOTS = "spots";
    /** Data: how each mark lies, radians from straight across the item (owner: "random, diagonal ones too"). */
    public static final String ANGLES = "angles";
    /** Effect data of a chop: the spot and angle of the mark it cut. */
    public static final String SPOT = "spot";
    public static final String ANGLE = "angle";

    /** Effects for everybody who sees the board. Data: {@link #AT}, {@link #DONE}, {@link #TOOL}. */
    public static final Identifier EFFECT_CHOPPED = HandsOnDelight.id("chopped");
    public static final Identifier EFFECT_PLACED = HandsOnDelight.id("placed");
    public static final Identifier EFFECT_REFUSED = HandsOnDelight.id("refused");
    /**
     * Data: {@link #FINISH}, how the dish was finished ({@code AssemblyRecipes.Finish}); {@link #BLOCK}, whether it is a block;
     * {@link #LAYERS}, what it was made of, bottom first, one of each (a dish picked in the book is laid and made in one tick).
     */
    public static final Identifier EFFECT_ASSEMBLED = HandsOnDelight.id("assembled");
    public static final String FINISH = "finish";
    public static final String BLOCK = "block";
    /** Data: {@link #BOOK}. */
    public static final Identifier EFFECT_BOOK = HandsOnDelight.id("board_book");
    /** Something comes off the board into the inventory: {@link #STACK} from layer {@link #AT}, or from the board itself with none. */
    public static final Identifier EFFECT_TAKEN = HandsOnDelight.id("taken");
    /** What a {@link #EFFECT_TAKEN} or {@link #EFFECT_REFUSED} is about, saved as a stack. */
    public static final String STACK = "stack";
    public static final String DONE = "done";
    public static final String TOOL = "tool";
    /** How many items the last chop cut. */
    public static final String COUNT = "count";

    public static final StationSchema SCHEMA = StationSchema.builder()
            .slots(BOARD, 1, SlotRules.DEFAULT.kept().accepts(stack -> false))
            .slots(LAYERS, MOST_LAYERS, SlotRules.DEFAULT.maxStack(BATCH).kept().accepts(stack -> false))
            .slots(HAND, 1, SlotRules.DEFAULT)
            .build();

    public static final StationType TYPE = Diegesis.station(ID)
            .block(FarmersDelightContent.CUTTING_BOARD)
            .schema(SCHEMA)
            .logic(BoardLogic::new)
            .orientation(StationType.Orientation.BLOCK)
            .openWhen(CuttingBoardStation::opens)
            .idleLogic()
            .build();

    private CuttingBoardStation() {
    }

    public static void register() {
        Diegesis.register(TYPE);
    }

    /**
     * Sneaking keeps the board's own use, also for carving a knife into it. A carved knife is Farmer's Delight's too: its use gives it
     * back, the station would only show a board it cannot work with. So does what is no cooking: a log stripped with an axe, a flower
     * cut into dye, held in the hand or lying on the board, stays Farmer's Delight's.
     */
    public static boolean opens(Player player, BlockPos pos, BlockState state) {
        if (player.isSecondaryUseActive() || Cutting.isOthers(player.level(), player.getMainHandItem())) {
            return false;
        }
        return !(player.level().getBlockEntity(pos) instanceof CuttingBoardBlockEntity board)
                || !board.isItemCarvingBoard() && !Cutting.isOthers(player.level(), Platform.INSTANCE.getItem(board));
    }
}
