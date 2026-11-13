package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenNbt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.MeshShape;
import com.pockyl.diegesis.api.client.NodeView;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.scene.Visual;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.station.BoardLogic;
import com.pockyl.handson_delight.station.Cutting;
import com.pockyl.handson_delight.station.CuttingBoardStation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What the board shows, to the cook and to everybody around: the item lying on it, built from its icon so that it falls apart where
 * the knife came down, the cook's own knife over it, and a dish growing layer by layer.
 */
public final class BoardBehavior implements StationBehavior {
    /** The board's top and the middle of the place an item lies on, scene pixels. */
    private static final float TOP = 1.0F;
    private static final float MIDDLE_X = 6.5F;
    private static final float MIDDLE_Z = 8.0F;
    /** How big an item lying to be cut is, and a layer of a dish; how far layers lie apart, how high the dish's promise floats. */
    private static final float ITEM_SIZE = 10.0F;
    private static final float LAYER_SIZE = 9.0F;
    /** How far cut pieces move apart, pixels, and how long it takes them, seconds. */
    private static final float GAP = 0.8F;
    private static final float GAP_TIME = 0.12F;
    /** Where the knife can cut: this far from the middle of the item along z. */
    private static final float REACH = 4.0F;
    /** The knife over the item, ready; at rest at the board's near edge; how long a chop takes down and up. */
    private static final float HOVER = 3.0F;
    private static final float REST_HEIGHT = 4.0F;
    private static final float REST_Z = -6.0F;
    private static final float CHOP_DOWN = 0.05F;
    private static final float CHOP_UP = 0.14F;
    /** The knife's edge lies this far below its icon's middle, pixels. */
    private static final float EDGE = 1.0F;
    /** The marks where the knife must come down: a dashed line this wide, dashes and gaps this long; how near the knife snaps to one. */
    private static final float MARK_WIDTH = 0.16F;
    private static final float DASH = 0.6F;
    private static final float DASH_GAP = 0.45F;
    private static final float SNAP = 1.4F;
    private static final int MARK_COLOR = 0xA0FFFFFF;
    /** A block-like item, drawn as an item: its size on the board, pixels. */
    private static final float BLOCK_SIZE = 8.0F;
    @SuppressWarnings("removal")
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("minecraft", "block/white_concrete");
    /** The layers sink into one dish, which pops up where they were: seconds. */
    private static final float MELT_TIME = 0.28F;
    private static final float POP_TIME = 0.35F;
    private static final float POP_HEIGHT = 1.6F;
    /** A toss: how high what lies in the bowl flies, how long it is in the air and rests after, how the bowl itself jerks. */
    private static final float TOSS_HEIGHT = 4.0F;
    private static final float TOSS_TIME = 0.42F;
    private static final float TOSS_REST = 0.12F;
    private static final float TOSS_JERK = 0.9F;
    private static final float TOSS_JERK_TIME = 0.16F;
    private static final float TOSS_TILT = 12.0F;
    private static final float TOSS_TUMBLE = 40.0F;
    /** A roll: the layers close up, then roll away and back under the hands, shorter each time. */
    private static final float ROLL_GATHER = 0.12F;
    private static final int ROLLS = 3;
    private static final float ROLL_STROKE = 0.14F;
    private static final float ROLL_TRAVEL = 1.6F;
    private static final float ROLL_TURN = 25.0F;
    /** A skewer: how the pieces go onto the stick, stand across it and are pushed along. */
    private static final float SKEWER_SLIDE = 0.18F;
    private static final float SKEWER_STAGGER = 0.1F;
    private static final float SKEWER_PUSH = 0.16F;
    /** Where the pieces sit along the stick, as fractions of its length from the tip: the handle's end stays bare. */
    private static final float SKEWER_FIRST = 0.12F;
    private static final float SKEWER_LAST = 0.62F;
    /**
     * How far a piece stands up from lying flat, degrees, its face to the handle and so to the camera: upright, or facing the tip,
     * it would show only its edges (the voxel pieces have no underside).
     */
    private static final float SKEWER_STAND = -60.0F;
    private static final float SKEWER_SCALE = 0.7F;
    /** Arranged in a bowl: the main piece in the middle, the rest round it this far out, each sliding in after the last. */
    private static final float ARRANGE_SLIDE = 0.18F;
    private static final float ARRANGE_STAGGER = 0.08F;
    private static final float ARRANGE_RING = 2.4F;
    private static final float ARRANGE_SCALE = 0.8F;
    /** A sandwich: the loaf's top half lifts, comes over the filling and closes it with a press. */
    private static final float LID_LIFT = 0.16F;
    private static final float LID_CLOSE = 0.16F;
    private static final float LID_PRESS = 0.1F;
    private static final float LID_HEIGHT = 3.0F;
    /** Nigiri: the rice pressed into a block, the slices laid along it one by one, one press. */
    private static final float SHAPE_GATHER = 0.14F;
    private static final float SHAPE_LAY = 0.12F;
    private static final float SHAPE_PRESS = 0.1F;
    /** A popsicle: the fruit gathers round the stick's top, the ice is gone in a frost. */
    private static final float FREEZE_GATHER = 0.2F;
    private static final float FREEZE_SET = 0.3F;
    private static final float FREEZE_AT = 0.28F;
    private static final int FROST = 0xFFE8F4FF;
    /** A liquid poured over the layers: its container hangs over them, tips, and goes back to the cook. */
    private static final float POUR_TIP = 0.14F;
    private static final float POUR_FLOW = 0.3F;
    private static final float POUR_HEIGHT = 3.5F;
    private static final float POUR_TILT = 35.0F;
    private static final float POUR_SCALE = 0.7F;
    private static final float FILM_SHARE = 0.85F;
    private static final float FILM_THICK = 0.12F;
    /** Kneading: pressed into a lump, then pushed flat and gathered again a few times. */
    private static final float KNEAD_PRESS = 0.12F;
    private static final int KNEAD_PUSHES = 3;
    private static final float KNEAD_PUSH = 0.09F;
    private static final float KNEAD_TURN = 22.0F;
    private static final float KNEAD_SPREAD = 1.2F;
    /** How far apart the layers of a dish lie, as the scene repeats them. */
    private static final float LAYER_STEP = 0.65F;
    /**
     * Putting down and taking off (owner: "everything cozy and animated"): a stack let go lands from the cursor with a hop and settles
     * with a little bounce; what is taken lifts and melts away, the drawer showing it come in; what the board refuses comes down and
     * bounces back to the hand. Seconds and pixels.
     */
    private static final float LAND_TIME = 0.2F;
    private static final float LAND_HOP = 0.8F;
    private static final float SETTLE_TIME = 0.12F;
    private static final float SETTLE_HOP = 0.25F;
    /** The dust a board gives off when dough or bread is worked on it. */
    private static final int FLOUR = 0xFFEDE3CC;
    /** How see-through the knife offered from the inventory stands over the food. */
    private static final int OFFERED = 0x8CFFFFFF;
    /** Seconds between layers that go on together, and how long one takes from the player to the board. */
    private static final float LAYER_STAGGER = 0.16F;
    private static final float PLAYER_LAND_TIME = 0.32F;
    private static final float DROP_HEIGHT = 3.0F;
    private static final float FROM_CURSOR = 10.0F;
    private static final float LIFT_TIME = 0.1F;
    private static final float LIFT_HEIGHT = 2.5F;
    private static final float AWAY_TIME = 0.2F;
    private static final float BOUNCE_DOWN = 0.09F;
    private static final float BOUNCE_UP = 0.22F;
    /** Ghosts of what comes off or bounces back: the scene's {@code lift_N}. */
    private static final int GHOSTS = 3;
    /** How long a chop drawn at once waits for the server's word before it is undone, seconds. */
    private static final float PENDING_WAIT = 1.0F;
    /** How long a new mark takes to appear, seconds. */
    private static final float MARK_FADE = 0.18F;

    private ItemStack shownItem = ItemStack.EMPTY;
    private List<ItemVoxels.Cut> shownCuts = List.of();
    private float gap = GAP;
    private boolean opening;
    private final ItemStack[] shownLayers = new ItemStack[CuttingBoardStation.MOST_LAYERS];
    /** Whether the layers made a dish when last seen: the stack settles once as it becomes one. */
    private boolean whole;
    /** The layers as last seen, for the dish they turn into; how long that still takes, seconds, NaN when nothing turns. */
    private final List<ItemStack> lastLayers = new ArrayList<>();
    /** Where each layer rests while the dish is finished (x and z from the middle, height over the board): a toss mixes them up. */
    private final float[] restX = new float[CuttingBoardStation.MOST_LAYERS];
    private final float[] restY = new float[CuttingBoardStation.MOST_LAYERS];
    private final float[] restZ = new float[CuttingBoardStation.MOST_LAYERS];
    private float melting = Float.NaN;
    private final BoardBook pages = new BoardBook();
    private final RecipeBook book = new RecipeBook(pages);
    /** What the drawer shows, by item: what is cut here or goes into a dish here, as at the pot. */
    private final UsableCache usable = new UsableCache();
    /** The dishes the drawer's choice was made with: the station's data brings them after the board opens. */
    private int usableFor = -1;
    private Item knife;
    private float knifeX;
    private float knifeZ = REST_Z;
    /** How the knife is turned: along the mark it is to cut, radians from straight across. */
    private float knifeAngle;
    private float knifeY = REST_HEIGHT;
    private float chopAge = Float.NaN;
    /**
     * The mark the knife stands over now, -1 off the marks; the chops of the local hand not answered yet, drawn at once so the knife
     * never waits for the cut.
     */
    private int overMark = -1;
    private int pendingChops;
    /** When the last chop was drawn before the server's word: with no word for {@link #PENDING_WAIT} it is forgotten. */
    private float pendingSince;
    private int drawnMarks = -1;
    private float markFade = 1.0F;
    private ItemStack lastCut = ItemStack.EMPTY;
    /** When the liquid being poured over the layers has run out, on {@link #clock}. */
    private float pourUntil;
    /** The item {@link #plan} was read for. */
    private ItemStack planned;
    private Cutting.Plan plan;
    /** Where the item that is being cut was cut so far: the pieces fly off split there. */
    private final List<ItemVoxels.Cut> lastCuts = new ArrayList<>();
    /** The first look at the board after its scene is made: what lies there is just there, nothing lands. */
    private boolean quiet = true;
    private int ghost;
    /** While what the board refused bounces back, the hand's own stack is not drawn. */
    private float bouncing;
    private float clock;
    /** How high the item's node was last put to rest: a pile under it that came or went meanwhile moves it there. */
    private float itemRestY;
    private final List<Step> steps = new ArrayList<>();

    /** A part of an animation that comes after another: at {@link #clock} {@code at}. */
    private record Step(float at, Runnable run) {
    }

    public BoardBehavior(StationView view) {
        Arrays.fill(shownLayers, ItemStack.EMPTY);
    }

    @Override
    public void onSceneReady(StationView view) {
        shownItem = ItemStack.EMPTY;
        Arrays.fill(shownLayers, ItemStack.EMPTY);
        knife = null;
        steps.clear();
        quiet = true;
        view.node("book_open").ifPresent(node -> node.setPainter(book));
        refresh(view);
    }

    @Override
    public void onOpen(StationView view) {
        book.opened(view);
        usable.clear();
        view.setInventoryFilter(stack -> usable.test(stack, it -> usable(view, it)));
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && view.openedByUse()) {
            useHeld(view, player);
        }
    }

    /**
     * What the cook came with is what they meant to do: a right click with food lays it on an empty board, as Farmer's Delight's own
     * board takes it, and a knife comes in hand, ready to cut. Never slower than the board without the station.
     */
    private void useHeld(StationView view, LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        int slot = player.getInventory().getSelectedSlot();
        if (held.isEmpty() || !player.inventoryMenu.getCarried().isEmpty()) {
            return;
        }
        if (Cutting.all(view.level()).stream().anyMatch(recipe -> recipe.getTool().test(held))) {
            view.takeFromInventory(slot);
            return;
        }
        ItemStack onBoard = view.state().getItem(CuttingBoardStation.BOARD_SLOT);
        boolean free = onBoard.isEmpty() && BoardLogic.layers(view.state()).isEmpty();
        if (free || ItemStack.isSameItemSameComponents(onBoard, held)) {
            onInventoryShiftClick(view, slot, held);
        }
    }

    @Override
    public void onClose(StationView view) {
        book.shut(view);
        pendingChops = 0;
        refresh(view);
    }

    @Override
    public void onStateChanged(StationView view, StationState.Changes changes) {
        pendingChops = 0;
        refresh(view);
    }

    /** Everything the state says, put in order: the item and how it is cut, the layers, the promise of the dish. */
    private void refresh(StationView view) {
        StationState state = view.state();
        ItemStack onBoard = carved(view) ? ItemStack.EMPTY : state.getItem(CuttingBoardStation.BOARD_SLOT);
        List<ItemVoxels.Cut> cuts = cutsMade(view, onBoard);
        if (!ItemStack.matches(onBoard, shownItem) || !cuts.equals(shownCuts)) {
            if (cuts.size() > shownCuts.size() && ItemStack.isSameItemSameComponents(onBoard, shownItem)) {
                gap = 0.0F;
                opening = true;
            }
            // Put down by a hand, not made of the layers (the dish pops up by itself) nor what is left after a cut.
            boolean landed = !quiet && !onBoard.isEmpty() && Arrays.stream(shownLayers).allMatch(ItemStack::isEmpty)
                    && (shownItem.isEmpty() || ItemStack.isSameItemSameComponents(onBoard, shownItem) && onBoard.getCount() > shownItem.getCount());
            shownItem = onBoard.copy();
            shownCuts = cuts;
            if (!cuts.isEmpty()) {
                lastCut = onBoard.copyWithCount(1);
                lastCuts.clear();
                lastCuts.addAll(cuts);
            }
            drawItem(view);
            if (landed) {
                boolean flat = ItemVoxels.icon(onBoard).isPresent();
                view.node(flat ? "item" : "item_block").ifPresent(node -> land(view, node, 0.0F, flat ? pileLift() : 0.0F, 0.0F));
            }
            view.node("pile").ifPresent(pile -> {
                // Not under layers being finished: the dish's pile shows when it pops up.
                pile.setVisible(onBoard.getCount() > 1 && Float.isNaN(melting));
                if (onBoard.getCount() > 1) {
                    pile.setShape(shape(onBoard, ITEM_SIZE, List.of(), 0.0F), sprite(onBoard));
                }
            });
        }
        List<ItemStack> layers = BoardLogic.layers(state);
        // Several layers at once (a dish picked in the book) come from the player one after the other (owner: "cozy", "they must
        // fly from the player"); one put down by hand lands from the hand.
        int arriving = 0;
        for (int i = 0; i < CuttingBoardStation.MOST_LAYERS && Float.isNaN(melting) && !quiet; i++) {
            ItemStack layer = i < layers.size() ? layers.get(i) : ItemStack.EMPTY;
            arriving += shownLayers[i].isEmpty() && !layer.isEmpty() ? 1 : 0;
        }
        boolean together = arriving > 1;
        int landing = 0;
        // While the layers turn into their dish they are the animation's.
        for (int i = 0; i < CuttingBoardStation.MOST_LAYERS && Float.isNaN(melting); i++) {
            ItemStack layer = i < layers.size() ? layers.get(i) : ItemStack.EMPTY;
            if (ItemStack.isSameItemSameComponents(layer, shownLayers[i])) {
                continue;
            }
            boolean arrives = together && shownLayers[i].isEmpty() && !layer.isEmpty();
            shownLayers[i] = layer.copy();
            int index = i;
            int order = arrives ? landing++ : 0;
            float delay = order * LAYER_STAGGER;
            view.node("layer_" + i).ifPresent(node -> {
                node.setVisible(!layer.isEmpty() && !arrives);
                if (!layer.isEmpty()) {
                    dress(node, index, layer, layers);
                    node.rotateTo(0.0F, turn(index), 0.0F, 0.0F);
                    node.moveTo(jitterX(index), 0.0F, jitterZ(index), 0.0F);
                    if (arrives) {
                        after(delay, () -> {
                            if (ItemStack.isSameItemSameComponents(shownLayers[index], layer)) {
                                node.setVisible(true);
                                arrives(view, node, index, layer, true);
                            }
                        });
                    } else if (!quiet) {
                        arrives(view, node, index, layer, false);
                    }
                }
            });
        }
        float lastLanded = landing > 0 ? (landing - 1) * LAYER_STAGGER + PLAYER_LAND_TIME : 0.0F;
        if (!layers.isEmpty()) {
            lastLayers.clear();
            layers.forEach(layer -> lastLayers.add(layer.copy()));
        }
        boolean nowWhole = !state.data().getStringOr(CuttingBoardStation.DISH, "").isEmpty();
        if (nowWhole && !whole) {
            // The last layer went on: the stack settles into one dish, and nothing floats over it to say so.
            int count = layers.size();
            Runnable settle = () -> {
                for (int i = 0; i < count; i++) {
                    int index = i;
                    view.node("layer_" + i).ifPresent(node -> node.hopTo(jitterX(index), 0.0F, jitterZ(index), SETTLE_HOP, SETTLE_TIME));
                }
                view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.5F, 0.8F);
            };
            if (lastLanded > 0.0F) {
                after(lastLanded, settle);
            } else {
                settle.run();
            }
        }
        whole = nowWhole;
        quiet = false;
    }

    /** Laid by hand: a layer a little turned and off the middle, never two the same way. */
    private static float turn(int layer) {
        return layer == 0 ? 0.0F : ((layer * 7919) % 31 - 15) * 1.1F;
    }

    private static float jitterX(int layer) {
        return ((layer * 4513) % 7 - 3) * 0.12F;
    }

    private static float jitterZ(int layer) {
        return ((layer * 2971) % 7 - 3) * 0.12F;
    }

    /**
     * The top item rests on the pile under it. An animation that ended (the dish popping up as the stack it joins came in) can
     * leave it at another height: inside the pile, the two flicker through each other.
     */
    private void keepOnPile(StationView view) {
        view.node("item").ifPresent(item -> {
            float want = pileLift();
            if (!item.isMoving() && Math.abs(want - itemRestY) > 0.001F) {
                item.moveTo(0.0F, want, 0.0F, 0.08F);
                itemRestY = want;
            }
        });
    }

    /** A pile below lifts the top item. */
    private float pileLift() {
        return shownItem.getCount() > 1 ? 0.65F : 0.0F;
    }

    /**
     * {@code node} comes down to its place ({@code x, y, z} from the scene's) from the cook's cursor, or from a little above for
     * everybody else, with a hop, and settles.
     */
    private void land(StationView view, NodeView node, float x, float y, float z) {
        Vector3f from = fromHand(view, node, y + DROP_HEIGHT);
        node.moveTo(from.x, from.y, from.z, 0.0F);
        node.hopTo(x, y, z, LAND_HOP, LAND_TIME);
        after(LAND_TIME, () -> node.hopTo(x, y, z, SETTLE_HOP, SETTLE_TIME));
    }

    /** {@code node} comes from the player over to its place with a hop, and settles. */
    private void landFromPlayer(StationView view, NodeView node, float x, float y, float z) {
        Vector3f from = FromPlayer.offset(view, TOP + DROP_HEIGHT, 0, 1);
        node.moveTo(x + from.x, y + DROP_HEIGHT, z + from.z, 0.0F);
        node.hopTo(x, y, z, LAND_HOP * 2.5F, PLAYER_LAND_TIME);
        after(PLAYER_LAND_TIME, () -> node.hopTo(x, y, z, SETTLE_HOP, SETTLE_TIME));
    }

    /** Where the cook's hand is over {@code node}, as the node's move, at {@code height} over it; straight above for the others. */
    private static Vector3f fromHand(StationView view, NodeView node, float height) {
        Vector3f home = new Vector3f(node.definition().transform().pos());
        Optional<Vector3f> cursor = view.isFocused() ? view.cursorOn(home.y + height) : Optional.empty();
        if (cursor.isEmpty()) {
            return new Vector3f(0.0F, height, 0.0F);
        }
        Vector3f away = new Vector3f(cursor.get().x - home.x, 0.0F, cursor.get().z - home.z);
        if (away.length() > FROM_CURSOR) {
            away.normalize(FROM_CURSOR);
        }
        return away.add(0.0F, height, 0.0F);
    }

    private void after(float seconds, Runnable run) {
        steps.add(new Step(clock + seconds, run));
    }

    /** The next ghost, shaped as {@code stack}: built from its icon, or the item drawn as an item when it is block-like. */
    private Optional<NodeView> ghost(StationView view, ItemStack stack, float size) {
        ghost = (ghost + 1) % GHOSTS;
        Optional<NodeView> node = view.node("lift_" + ghost);
        node.ifPresent(it -> {
            Optional<ItemVoxels.Icon> icon = ItemVoxels.icon(stack);
            if (icon.isPresent()) {
                it.setVisual(null);
                it.setShape(ItemVoxels.build(icon.get(), size, List.of(), 0.0F), icon.get().sprite());
            } else {
                it.setShape(null, null);
                it.setVisual(Visual.ofItem(stack.getItem()).withAnchor(new Vector3f(8.0F, 8.0F, 8.0F)));
            }
            it.rotateTo(0.0F, 0.0F, 0.0F, 0.0F);
            it.scaleTo(1.0F, 0.0F);
            it.setVisible(true);
        });
        return node;
    }

    /** How high a block-like item's middle stands over a flat one's place. */
    private static float rise(ItemStack stack) {
        return ItemVoxels.icon(stack).isPresent() ? 0.0F : BLOCK_SIZE / 2.0F;
    }

    /** {@code stack} came off the board, from {@code layer} or (-1) from where items lie to be cut: it lifts and melts away. */
    private void taken(StationView view, ItemStack stack, int layer) {
        float x = layer >= 0 ? jitterX(layer) : 0.0F;
        float z = layer >= 0 ? jitterZ(layer) : 0.0F;
        float y = (layer >= 0 ? layer * LAYER_STEP : stack.getCount() > 1 ? 0.65F : 0.0F) + rise(stack);
        // The real one goes at once; the state that says so may come a moment later.
        if (layer >= 0) {
            view.node("layer_" + layer).ifPresent(node -> node.setVisible(false));
        } else {
            view.node("item").ifPresent(node -> node.setVisible(false));
            view.node("item_block").ifPresent(node -> node.setVisible(false));
            view.node("pile").ifPresent(node -> node.setVisible(false));
        }
        ghost(view, stack, layer > 0 ? LAYER_SIZE : ITEM_SIZE).ifPresent(node -> {
            node.rotateTo(0.0F, layer >= 0 ? turn(layer) : 0.0F, 0.0F, 0.0F);
            node.moveTo(x, y, z, 0.0F);
            node.moveTo(x, y + LIFT_HEIGHT, z, LIFT_TIME);
            // Then it melts away where it is: the drawer shows it arriving (Diegesis' gains).
            after(LIFT_TIME, () -> {
                node.moveTo(x, y + LIFT_HEIGHT + 1.0F, z, AWAY_TIME);
                node.scaleTo(0.0F, AWAY_TIME);
            });
            after(LIFT_TIME + AWAY_TIME, () -> node.setVisible(false));
        });
        view.playSound(SoundEvents.ITEM_PICKUP, "surface", 0.3F, 1.5F);
    }

    /** The board will not have {@code stack}: it comes down on the top of what lies there and bounces back up to the hand. */
    private void bounced(StationView view, ItemStack stack) {
        List<ItemStack> layers = BoardLogic.layers(view.state());
        float top = !layers.isEmpty() ? layers.size() * LAYER_STEP : shownItem.isEmpty() ? 0.0F : itemHeight() + 0.3F;
        float y = top + rise(stack);
        ghost(view, stack, LAYER_SIZE).ifPresent(node -> {
            Vector3f hand = fromHand(view, node, y + DROP_HEIGHT);
            node.moveTo(hand.x, hand.y, hand.z, 0.0F);
            node.moveTo(hand.x * 0.3F, y, hand.z * 0.3F, BOUNCE_DOWN);
            after(BOUNCE_DOWN, () -> node.hopTo(hand.x, hand.y, hand.z, 1.2F, BOUNCE_UP));
            after(BOUNCE_DOWN + BOUNCE_UP, () -> node.setVisible(false));
        });
        bouncing = BOUNCE_DOWN + BOUNCE_UP;
    }

    /** The marks cut so far on {@code stack}: what the server says and what the local hand has just done. */
    private List<ItemVoxels.Cut> cutsMade(StationView view, ItemStack stack) {
        List<ItemVoxels.Cut> cuts = new ArrayList<>();
        int made = Math.min(plan(view, stack).marks(), view.state().data().getIntOr(CuttingBoardStation.MARKS, 0) + pendingChops);
        for (int mark = 0; mark < made && !stack.isEmpty(); mark++) {
            cuts.add(mark(view, stack, mark));
        }
        return cuts;
    }

    /** A knife carved into the board is Farmer's Delight's to draw. */
    private static boolean carved(StationView view) {
        return view.level().getBlockEntity(view.pos()) instanceof CuttingBoardBlockEntity board && board.isItemCarvingBoard();
    }

    private void drawItem(StationView view) {
        NodeView item = view.node("item").orElse(null);
        NodeView block = view.node("item_block").orElse(null);
        if (item == null || block == null) {
            return;
        }
        boolean flat = !shownItem.isEmpty() && ItemVoxels.icon(shownItem).isPresent();
        item.setVisible(flat);
        block.setVisible(!shownItem.isEmpty() && !flat);
        if (flat) {
            item.setShape(ItemVoxels.build(ItemVoxels.icon(shownItem).get(), ITEM_SIZE, shownCuts, gap), sprite(shownItem));
            // Not while it lands or pops up: once it stops, keepOnPile puts it right.
            if (!item.isMoving()) {
                item.moveTo(0.0F, pileLift(), 0.0F, 0.0F);
                itemRestY = pileLift();
            }
        } else if (!shownItem.isEmpty()) {
            block.setVisual(Visual.ofItem(shownItem.getItem()).withAnchor(new Vector3f(8.0F, 8.0F, 8.0F)));
        }
    }

    private static MeshShape shape(ItemStack stack, float size, List<ItemVoxels.Cut> cuts, float gap) {
        return ItemVoxels.icon(stack).map(icon -> ItemVoxels.build(icon, size, cuts, gap)).orElse(null);
    }

    private static Identifier sprite(ItemStack stack) {
        return ItemVoxels.icon(stack).map(ItemVoxels.Icon::sprite).orElse(null);
    }

    @Override
    public void frame(StationView view, float dt) {
        clock += dt;
        bouncing = Math.max(0.0F, bouncing - dt);
        if (!steps.isEmpty()) {
            List<Step> due = steps.stream().filter(step -> step.at() <= clock).toList();
            steps.removeAll(due);
            due.forEach(step -> step.run().run());
        }
        if (opening) {
            gap = Math.min(GAP, gap + GAP * dt / GAP_TIME);
            opening = gap < GAP;
            drawItem(view);
        }
        if (pendingChops > 0 && clock - pendingSince > PENDING_WAIT) {
            pendingChops = 0;
            refresh(view);
        }
        keepOnPile(view);
        moveKnife(view, dt);
        book.frame(view, dt);
    }

    /**
     * The knife that cuts what lies on the board: the one on the cook's cursor, or with an empty hand one in the inventory, offered
     * see-through over the food (owner, 2026-11-09: "I need not take the knife from its slot to cut").
     */
    private Optional<ItemStack> tool(StationView view) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !view.isFocused() || shownItem.isEmpty() || !BoardLogic.layers(view.state()).isEmpty()) {
            return Optional.empty();
        }
        ItemStack carried = player.inventoryMenu.getCarried();
        if (carried.isEmpty()) {
            int slot = Cutting.toolSlot(view.level(), shownItem, player.getInventory());
            return slot < 0 ? Optional.empty() : Optional.of(player.getInventory().getItem(slot));
        }
        return Cutting.findWith(view.level(), shownItem, carried).map(recipe -> carried);
    }

    /** The knife over the food is only offered: the hand is empty. */
    private static boolean offered() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.inventoryMenu.getCarried().isEmpty();
    }

    /**
     * The knife taken on the cursor: over the item it is drawn by the board, edge down over the mark it would cut, instead of the
     * library's icon on the cursor; elsewhere the cursor shows it as any stack.
     */
    private void moveKnife(StationView view, float dt) {
        NodeView node = view.node("knife").orElse(null);
        NodeView turn = view.node("knife_turn").orElse(null);
        if (node == null || turn == null) {
            return;
        }
        Optional<ItemStack> tool = tool(view);
        drawMarks(view, tool.isPresent(), dt);
        overMark = -1;
        Optional<Vector3f> over = tool.isPresent() ? overItem(view) : Optional.empty();
        boolean drawn = over.isPresent() || !Float.isNaN(chopAge) && tool.isPresent();
        node.setVisible(drawn);
        view.setCarriedHidden(drawn || bouncing > 0.0F);
        if (!drawn) {
            knifeY = HOVER + REST_HEIGHT;
            return;
        }
        if (tool.get().getItem() != knife) {
            knife = tool.get().getItem();
            node.setVisual(Visual.ofItem(knife).withAnchor(new Vector3f(8.0F, 8.0F, 8.0F)));
        }
        // Offered from the inventory it stands see-through, and comes down solid when it cuts.
        node.setTint(offered() && Float.isNaN(chopAge) ? OFFERED : 0xFFFFFFFF);
        float wantX = knifeX;
        float wantZ = knifeZ;
        float wantY = HOVER;
        int next = nextMark(view);
        if (next >= 0) {
            knifeAngle = mark(view, shownItem, next).angle();
        }
        if (over.isPresent()) {
            wantX = over.get().x;
            wantZ = over.get().z;
            // Near the mark the knife stands right over it, along it: that is where it will come down.
            if (next >= 0) {
                ItemVoxels.Cut mark = mark(view, shownItem, next);
                float off = mark.distance(wantX, wantZ);
                if (Math.abs(off) < SNAP) {
                    overMark = next;
                    wantX -= mark.normalX() * off;
                    wantZ -= mark.normalZ() * off;
                }
            }
        }
        float follow = 1.0F - (float) Math.exp(-dt * 22.0F);
        knifeX += (wantX - knifeX) * follow;
        knifeZ += (wantZ - knifeZ) * follow;
        knifeY += (wantY - knifeY) * follow;
        float y = knifeY;
        if (!Float.isNaN(chopAge)) {
            chopAge += dt;
            float surface = itemHeight() - knifeY;
            float down = chopAge < CHOP_DOWN ? chopAge / CHOP_DOWN : Math.max(0.0F, 1.0F - (chopAge - CHOP_DOWN) / CHOP_UP);
            y += surface * down;
            if (chopAge > CHOP_DOWN + CHOP_UP) {
                chopAge = Float.NaN;
            }
        }
        turn.moveTo(knifeX, y + EDGE, knifeZ, 0.0F);
        turn.rotateTo(0.0F, -knifeAngle * Mth.RAD_TO_DEG, 0.0F, 0.0F);
    }

    /** How high the top of the item is over the board. */
    private float itemHeight() {
        if (!shownItem.isEmpty() && ItemVoxels.icon(shownItem).isEmpty()) {
            return BLOCK_SIZE;
        }
        return (shownItem.getCount() > 1 ? 0.65F : 0.0F) + ITEM_SIZE / 16.0F;
    }

    /**
     * What is solid of {@code stack} lying on the board, node pixels from the item's middle: {@code {right, left, near, far}} (x grows
     * to the cook's left). A block drawn as an item is a square.
     */
    private static float[] extent(ItemStack stack) {
        float half = BLOCK_SIZE / 2.0F;
        Optional<ItemVoxels.Icon> icon = stack.isEmpty() ? Optional.empty() : ItemVoxels.icon(stack);
        if (icon.isEmpty()) {
            return new float[] {-half, half, -half, half};
        }
        int[] bounds = icon.get().bounds();
        float pixel = ITEM_SIZE / Math.max(icon.get().width(), icon.get().height());
        return new float[] {(icon.get().width() / 2.0F - bounds[1] - 1) * pixel, (icon.get().width() / 2.0F - bounds[0]) * pixel,
                (icon.get().height() / 2.0F - bounds[3] - 1) * pixel, (icon.get().height() / 2.0F - bounds[2]) * pixel};
    }

    /** How {@code stack} is cut, read again only when another item lies there. */
    private Cutting.Plan plan(StationView view, ItemStack stack) {
        if (stack.isEmpty()) {
            return new Cutting.Plan(1, Cutting.Style.ACROSS);
        }
        if (planned == null || planned.getItem() != stack.getItem()) {
            planned = stack.copyWithCount(1);
            plan = Cutting.plan(view.level(), planned);
        }
        return plan;
    }

    /**
     * Mark {@code mark} on {@code stack}, where the server chose: a spot along the item's own length and a lean from square across
     * it (see {@code BoardLogic.restart}). Without word from the server yet, square across at even places.
     */
    private ItemVoxels.Cut mark(StationView view, ItemStack stack, int mark) {
        CompoundTag data = view.state().data();
        ListTag spots = data.getListOrEmpty(CuttingBoardStation.SPOTS);
        ListTag angles = data.getListOrEmpty(CuttingBoardStation.ANGLES);
        Cutting.Plan plan = plan(view, stack);
        float spot = mark < spots.size() ? spots.getFloatOr(mark, 0.0F) : (mark + 1.0F) / (plan.marks() + 1.0F);
        return line(stack, plan.style(), spot, mark < angles.size() ? angles.getFloatOr(mark, 0.0F) : 0.0F);
    }

    /** How far a mark across a long item leans from square at most: a fish is cut in steaks, never along its spine. */
    private static final float LONG_LEAN = 20.0F * Mth.DEG_TO_RAD;
    /** Longer than wide by this much, an item is long. */
    private static final float LONG = 1.4F;

    /** The line of a mark: through the item's own length at {@code spot}, square across it give or take its lean. */
    private static ItemVoxels.Cut line(ItemStack stack, Cutting.Style style, float spot, float angle) {
        ItemVoxels.Axis axis = axis(stack);
        if (style == Cutting.Style.WEDGES) {
            return new ItemVoxels.Cut(axis.x(), axis.z(), ItemVoxels.Axis.normalized(angle));
        }
        float[] at = axis.at(spot);
        float lean = style == Cutting.Style.ACROSS ? angle * (axis.elongation() >= LONG ? LONG_LEAN : BoardLogic.MOST_LEAN) : 0.0F;
        return new ItemVoxels.Cut(at[0], at[1], ItemVoxels.Axis.normalized(axis.across() + lean));
    }

    /** The length of {@code stack} lying on the board to be cut. */
    private static ItemVoxels.Axis axis(ItemStack stack) {
        return ItemVoxels.icon(stack).map(icon -> icon.axis(ITEM_SIZE)).orElse(ItemVoxels.Axis.square(BLOCK_SIZE / 2.0F));
    }

    /** The mark to cut next, -1 when none: what the server says is cut and what the local hand has just done. */
    private int nextMark(StationView view) {
        int next = view.state().data().getIntOr(CuttingBoardStation.MARKS, 0) + pendingChops;
        return next < plan(view, shownItem).marks() ? next : -1;
    }

    /**
     * A dashed line across the item where it is to be cut next, for the cook alone (owner: "one mark at a time"); the second shows,
     * fading in, once the first is cut.
     */
    private void drawMarks(StationView view, boolean shown, float dt) {
        NodeView node = view.node("marks").orElse(null);
        if (node == null) {
            return;
        }
        int next = shown ? nextMark(view) : -1;
        ItemVoxels.Cut mark = next < 0 ? null : mark(view, shownItem, next);
        int key = next < 0 ? 0 : Objects.hash(next, mark, itemHeight(), shownItem.getItem());
        if (markFade < 1.0F) {
            markFade = Math.min(1.0F, markFade + dt / MARK_FADE);
            node.setTint(fade(markFade));
        }
        if (key == drawnMarks) {
            return;
        }
        drawnMarks = key;
        node.setVisible(next >= 0);
        if (next < 0) {
            return;
        }
        markFade = 0.0F;
        // The line runs across what is solid of the item, at its angle: over its pixels, or a block's box.
        float alongX = Mth.cos(mark.angle());
        float alongZ = Mth.sin(mark.angle());
        Optional<ItemVoxels.Icon> icon = ItemVoxels.icon(shownItem);
        float[] ends;
        if (icon.isPresent()) {
            ends = icon.get().solidSpan(mark.x(), mark.z(), mark.angle(), ITEM_SIZE);
        } else {
            float[] extent = extent(shownItem);
            ends = clip(mark.x(), alongX, extent[0], extent[1], -100.0F, 100.0F);
            ends = clip(mark.z(), alongZ, extent[2], extent[3], ends[0], ends[1]);
        }
        float y = itemHeight() + 0.03F;
        float wideX = mark.normalX() * MARK_WIDTH;
        float wideZ = mark.normalZ() * MARK_WIDTH;
        MeshShape.Builder shape = MeshShape.builder();
        for (float t = ends[0]; t < ends[1]; t += DASH + DASH_GAP) {
            float end = Math.min(ends[1], t + DASH);
            float ax = mark.x() + alongX * t;
            float az = mark.z() + alongZ * t;
            float bx = mark.x() + alongX * end;
            float bz = mark.z() + alongZ * end;
            shape.corner(ax - wideX, y, az - wideZ, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F)
                    .corner(ax + wideX, y, az + wideZ, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F)
                    .corner(bx + wideX, y, bz + wideZ, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F)
                    .corner(bx - wideX, y, bz - wideZ, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        }
        node.setTint(fade(markFade));
        node.setShape(shape.build(), WHITE, true);
    }

    /** Where a line {@code start + t * along} is between {@code low} and {@code high}: the range of t, narrowed from {@code from, to}. */
    private static float[] clip(float start, float along, float low, float high, float from, float to) {
        if (Math.abs(along) < 1.0e-4F) {
            return start >= low && start <= high ? new float[] {from, to} : new float[] {0.0F, 0.0F};
        }
        float a = (low - start) / along;
        float b = (high - start) / along;
        return new float[] {Math.max(from, Math.min(a, b)), Math.min(to, Math.max(a, b))};
    }

    private static int fade(float amount) {
        return Math.round((MARK_COLOR >>> 24) * amount) << 24 | MARK_COLOR & 0xFFFFFF;
    }

    /** Where on the item the cursor points, node pixels from its middle (y unused), while it is over the item. */
    private static Optional<Vector3f> overItem(StationView view) {
        return view.cursorOn(TOP)
                .filter(point -> Math.abs(point.x - MIDDLE_X) < ITEM_SIZE / 2.0F + 1.0F
                        && Math.abs(point.z - MIDDLE_Z) < ITEM_SIZE / 2.0F + 1.0F)
                .map(point -> new Vector3f(Mth.clamp(point.x - MIDDLE_X, -REACH, REACH), 0.0F, Mth.clamp(point.z - MIDDLE_Z, -REACH, REACH)));
    }

    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (event instanceof SceneEvent.Click click && "book_open".equals(click.node())) {
            book.click(view);
            return;
        }
        if (!(event instanceof SceneEvent.Click click) || !"surface".equals(click.node())) {
            return;
        }
        StationState state = view.state();
        if (!BoardLogic.layers(state).isEmpty()) {
            boolean ready = !state.data().getStringOr(CuttingBoardStation.DISH, "").isEmpty();
            view.sendAction(ready ? CuttingBoardStation.ACTION_ASSEMBLE : CuttingBoardStation.ACTION_TAKE, new CompoundTag());
            return;
        }
        if (!shownItem.isEmpty()) {
            // A left click with an empty hand cuts with the knife offered over the food, which stays in the inventory. Without one,
            // or with a right click, what lies there goes back.
            if (click.button() == 0 && tool(view).isPresent()) {
                if (overItem(view).isPresent()) {
                    chop(view);
                }
            } else {
                view.sendAction(CuttingBoardStation.ACTION_TAKE, new CompoundTag());
            }
        }
    }

    /** The knife on the cursor comes down: on a mark it cuts there, off the marks it only knocks. */
    private void chop(StationView view) {
        Optional<Vector3f> over = overItem(view);
        if (over.isEmpty() || !Float.isNaN(chopAge)) {
            return;
        }
        chopAge = 0.0F;
        if (overMark < 0) {
            // Off the marks the knife only knocks on the item: a cut goes where it is marked.
            view.playSound(SoundEvents.WOOD_HIT, "item", 0.25F, 1.6F);
            return;
        }
        pendingChops++;
        pendingSince = clock;
        CompoundTag at = new CompoundTag();
        at.putInt(CuttingBoardStation.MARK, overMark);
        at.putFloat(CuttingBoardStation.AT, MIDDLE_Z + knifeZ);
        view.sendAction(CuttingBoardStation.ACTION_CHOP, at);
        refresh(view);
    }

    /**
     * The knife on the cursor chops, and the right button puts it away (owner, 2026-11-08: "a right click must put away what is in
     * the hand, a knife for one"); any other stack let go on the board goes where the board says: onto the dish or to be cut, all
     * of it or, with the right button, one.
     */
    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        if ("book_open".equals(node)) {
            book.click(view);
            return true;
        }
        if (button == 1 && Cutting.all(view.level()).stream().anyMatch(recipe -> recipe.getTool().test(stack))) {
            view.putAwayCarried();
            return true;
        }
        if (tool(view).isPresent()) {
            chop(view);
            return true;
        }
        CompoundTag how = new CompoundTag();
        how.putBoolean(CuttingBoardStation.ONE, button == 1);
        view.sendAction(CuttingBoardStation.ACTION_PLACE, how);
        return true;
    }

    /** Shift + click on food in the drawer puts the whole stack on the board, as at the pot: to be cut, or as the next layer. */
    @Override
    public boolean onInventoryShiftClick(StationView view, int slot, ItemStack stack) {
        if (stack.isEmpty() || Cutting.recipesFor(view.level(), stack).isEmpty() && !pages.goesIntoADish(view, stack)) {
            return false;
        }
        CompoundTag how = new CompoundTag();
        how.putInt(CuttingBoardStation.FROM, slot);
        view.sendAction(CuttingBoardStation.ACTION_PLACE, how);
        return true;
    }

    @Override
    public void onEffect(StationView view, Identifier effect, String node, CompoundTag data) {
        if (CuttingBoardStation.EFFECT_CHOPPED.equals(effect)) {
            chopped(view, data);
        } else if (CuttingBoardStation.EFFECT_PLACED.equals(effect)) {
            // Each layer a little higher: the dish grows.
            float pitch = data.contains(CuttingBoardStation.AT) ? 1.15F + 0.05F * data.getIntOr(CuttingBoardStation.AT, 0) : 1.0F;
            view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.7F, pitch);
        } else if (CuttingBoardStation.EFFECT_REFUSED.equals(effect)) {
            ItemStack refused = KitchenNbt.parse(view.level().registryAccess(), data.getCompoundOrEmpty(CuttingBoardStation.STACK));
            if (!refused.isEmpty()) {
                bounced(view, refused);
            }
            // A chop drawn at once that the server did not take: the cut closes again.
            if (pendingChops > 0) {
                pendingChops = 0;
                refresh(view);
            }
            view.playSound(SoundEvents.WOOD_HIT, "surface", 0.4F, 1.4F);
        } else if (CuttingBoardStation.EFFECT_TAKEN.equals(effect)) {
            ItemStack taken = KitchenNbt.parse(view.level().registryAccess(), data.getCompoundOrEmpty(CuttingBoardStation.STACK));
            if (!taken.isEmpty()) {
                taken(view, taken, data.getIntOr(CuttingBoardStation.AT, 0));
            }
        } else if (CuttingBoardStation.EFFECT_ASSEMBLED.equals(effect)) {
            List<ItemStack> made = new ArrayList<>();
            ListTag layers = data.getListOrEmpty(CuttingBoardStation.LAYERS);
            for (int i = 0; i < layers.size(); i++) {
                made.add(KitchenNbt.parse(view.level().registryAccess(), layers.getCompoundOrEmpty(i)));
            }
            assembled(view, finish(data.getStringOr(CuttingBoardStation.FINISH, "")), made);
            book.statsChanged();
        } else if (CuttingBoardStation.EFFECT_BOOK.equals(effect)) {
            BoardBook.received(data.getListOrEmpty(CuttingBoardStation.BOOK));
        }
    }

    /** A chop: the tool's sound and bits of the food; the last one sends the pieces off to the cook. */
    private void chopped(StationView view, CompoundTag data) {
        Identifier toolId = Identifier.tryParse(data.getStringOr(CuttingBoardStation.TOOL, ""));
        Item tool = toolId == null ? null : BuiltInRegistries.ITEM.getValue(toolId);
        ItemStack food = shownItem.isEmpty() ? lastCut : shownItem;
        int color = food.isEmpty() ? 0xFFE0D0A0 : 0xFF000000 | FoodColors.main(food);
        Vector3f at = new Vector3f(MIDDLE_X, TOP + itemHeight(), data.getFloatOr(CuttingBoardStation.AT, 0.0F));
        // Bits of the food, not drops: a splash in the food's colour read as green water off a cabbage.
        view.spawnParticles(DiegesisParticles.DUST, at, new Vector3f(0.0F, 4.0F, 0.0F), 1.2F, 4, color);
        if (!data.getBooleanOr(CuttingBoardStation.DONE, false)) {
            // The last chop sounds through Farmer's Delight, as its own cut does.
            view.playSound(chopSound(tool), "item", 0.8F, 0.95F + view.level().getRandom().nextFloat() * 0.15F);
            return;
        }
        view.node("item_gone").ifPresent(gone -> {
            if (lastCut.isEmpty() || ItemVoxels.icon(lastCut).isEmpty()) {
                return;
            }
            // Split where it was really cut: the first cut as last seen, the last one now.
            Cutting.Plan cut = Cutting.plan(view.level(), lastCut);
            List<ItemVoxels.Cut> spread = new ArrayList<>(lastCuts.subList(0, Math.min(lastCuts.size(), cut.marks() - 1)));
            spread.add(line(lastCut, cut.style(), data.getFloatOr(CuttingBoardStation.SPOT, 0.0F), data.getFloatOr(CuttingBoardStation.ANGLE, 0.0F)));
            gone.setShape(ItemVoxels.build(ItemVoxels.icon(lastCut).get(), ITEM_SIZE, spread, GAP * 1.6F), sprite(lastCut));
            gone.setVisible(true);
            gone.moveTo(0.0F, 0.0F, 0.0F, 0.0F);
            gone.scaleTo(1.0F, 0.0F);
            gone.moveTo(0.0F, 2.5F, 0.0F, 0.25F);
            gone.scaleTo(0.0F, 0.25F);
        });
        view.playSound(SoundEvents.ITEM_PICKUP, "item", 0.35F, 1.3F);
    }

    /** Food the board cuts, a knife it cuts with, or what goes into a dish put together on it. */
    private boolean usable(StationView view, ItemStack stack) {
        return !Cutting.recipesFor(view.level(), stack).isEmpty() || pages.goesIntoADish(view, stack)
                || Cutting.all(view.level()).stream().anyMatch(recipe -> recipe.getTool().test(stack));
    }

    private static AssemblyRecipes.Finish finish(String name) {
        for (AssemblyRecipes.Finish finish : AssemblyRecipes.Finish.values()) {
            if (finish.name().equals(name)) {
                return finish;
            }
        }
        return AssemblyRecipes.Finish.PRESS;
    }

    /**
     * The layers are finished as their kind of dish is (a salad is tossed in its bowl first), then sink into each other and the dish
     * pops up in their place, with crumbs and a glint. {@code made}: the layers the dish was made of; those not on the board yet (a
     * dish picked in the book is laid and made at once) first come from the player one after the other.
     */
    private void assembled(StationView view, AssemblyRecipes.Finish finish, List<ItemStack> made) {
        for (int i = 0; i < CuttingBoardStation.MOST_LAYERS; i++) {
            rest(i, jitterX(i), i * LAYER_STEP, jitterZ(i));
        }
        // A liquid still being poured over the stack runs out before the dish is finished.
        float arrive = Math.max(arrive(view, made), pourUntil - clock);
        int count = lastLayers.size();
        float finishing = switch (finish) {
            case TOSS -> TOSS_TIME + TOSS_REST;
            case ROLL -> ROLL_GATHER + (ROLLS + 1) * ROLL_STROKE;
            case SKEWER -> SKEWER_SLIDE + (count - 1) * SKEWER_STAGGER + SKEWER_PUSH;
            case KNEAD -> KNEAD_PRESS + KNEAD_PUSHES * KNEAD_PUSH * 2.0F;
            case ARRANGE -> ARRANGE_SLIDE + Math.max(0, count - 2) * ARRANGE_STAGGER;
            case SANDWICH -> LID_LIFT + LID_CLOSE + LID_PRESS;
            case SHAPE -> SHAPE_GATHER + Math.max(1, count - 1) * SHAPE_LAY + SHAPE_PRESS;
            case FREEZE -> FREEZE_GATHER + FREEZE_SET;
            case PRESS -> 0.0F;
        };
        melting = arrive + finishing + MELT_TIME;
        // The dish (an item lying flat or a block standing) waits unseen until it pops up.
        for (String dish : List.of("item", "item_block")) {
            view.node(dish).ifPresent(item -> item.scaleTo(arrive + finishing > 0.0F ? 0.0F : 0.3F, 0.0F));
        }
        view.node("pile").ifPresent(pile -> pile.setVisible(false));
        then(arrive, () -> {
            switch (finish) {
                case TOSS -> toss(view);
                case ROLL -> roll(view);
                case SKEWER -> skewer(view);
                case KNEAD -> knead(view);
                case ARRANGE -> arrange(view);
                case SANDWICH -> sandwich(view);
                case SHAPE -> shape(view);
                case FREEZE -> freeze(view);
                case PRESS -> {
                }
            }
            then(finishing, () -> pop(view, finish));
        });
    }

    private void rest(int layer, float x, float y, float z) {
        restX[layer] = x;
        restY[layer] = y;
        restZ[layer] = z;
    }

    /** {@code run} after {@code seconds}, or now. */
    private void then(float seconds, Runnable run) {
        if (seconds > 0.0F) {
            after(seconds, run);
        } else {
            run.run();
        }
    }

    /**
     * Shows the layers {@code made} of: those the board already showed where they lie, the others coming from the player one after the
     * other. Returns when the last has landed.
     */
    private float arrive(StationView view, List<ItemStack> made) {
        List<ItemStack> layers = made.isEmpty() ? List.copyOf(lastLayers) : made;
        float landed = 0.0F;
        int order = 0;
        for (int i = 0; i < layers.size() && i < CuttingBoardStation.MOST_LAYERS; i++) {
            ItemStack layer = layers.get(i);
            boolean seen = made.isEmpty() || ItemStack.isSameItemSameComponents(shownLayers[i], layer);
            float delay = order * LAYER_STAGGER;
            if (!seen) {
                order++;
                // A liquid is poured, which takes longer than landing.
                float takes = i > 0 && KitchenLooks.isPoured(layer) ? POUR_TIP + POUR_FLOW : PLAYER_LAND_TIME + SETTLE_TIME;
                landed = Math.max(landed, delay + takes);
            }
            int index = i;
            view.node("layer_" + i).ifPresent(node -> {
                dress(node, index, layer, layers);
                node.rotateTo(0.0F, turn(index), 0.0F, 0.0F);
                node.scaleTo(1.0F, 0.0F);
                node.setVisible(seen);
                if (seen) {
                    node.moveTo(jitterX(index), 0.0F, jitterZ(index), 0.0F);
                    return;
                }
                after(delay, () -> {
                    node.setVisible(true);
                    arrives(view, node, index, layer, true);
                });
            });
        }
        lastLayers.clear();
        layers.forEach(layer -> lastLayers.add(layer.copy()));
        return landed;
    }

    /** The layers sink into each other and the dish pops up in their place. */
    private void pop(StationView view, AssemblyRecipes.Finish finish) {
        for (int i = 0; i < lastLayers.size(); i++) {
            int index = i;
            // Each sinks from where it rests towards the middle of the board.
            float x = restX[i] * 0.4F;
            float y = restY[i] * 0.08F - index * LAYER_STEP;
            float z = restZ[i] * 0.4F;
            view.node("layer_" + i).ifPresent(node -> {
                node.scaleTo(0.1F, MELT_TIME);
                node.moveTo(x, y, z, MELT_TIME);
            });
        }
        boolean flat = ItemVoxels.icon(shownItem).isPresent();
        view.node(flat ? "item" : "item_block").ifPresent(item -> {
            item.scaleTo(0.3F, 0.0F);
            item.scaleTo(1.0F, POP_TIME);
            item.hopTo(0.0F, flat ? pileLift() : 0.0F, 0.0F, POP_HEIGHT, POP_TIME);
        });
        // Under the layers still melting it would fight them for the same place.
        after(MELT_TIME, () -> view.node("pile").ifPresent(pile -> pile.setVisible(shownItem.getCount() > 1)));
        // Flour dust off what was pressed, rolled or kneaded; a dish tossed in its bowl only glints (owner: "no particles that do not
        // fit, green ones": crumbs in the food's own colour flew green off every salad).
        Vector3f middle = new Vector3f(MIDDLE_X, TOP + 1.0F, MIDDLE_Z);
        if (finish == AssemblyRecipes.Finish.FREEZE) {
            view.spawnParticles(DiegesisParticles.DUST, middle, new Vector3f(0.0F, 2.0F, 0.0F), 2.5F, 12, FROST);
        } else if (finish != AssemblyRecipes.Finish.TOSS && finish != AssemblyRecipes.Finish.ARRANGE) {
            view.spawnParticles(DiegesisParticles.DUST, middle, new Vector3f(0.0F, 3.0F, 0.0F), 2.5F, 12, FLOUR);
        }
        view.spawnParticles(DiegesisParticles.GLINT, middle, new Vector3f(0.0F, 2.0F, 0.0F), 2.0F, 5, 0xFFFFE9A0);
        view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.8F, 0.75F);
        view.playSound(SoundEvents.ITEM_PICKUP, "surface", 0.35F, 1.4F);
    }

    /**
     * The bowl jerks up and what lies in it flies, each piece its own height and turn, and falls back in another order: a salad tossed.
     */
    private void toss(StationView view) {
        view.node("layer_0").ifPresent(bowl -> {
            bowl.hopTo(jitterX(0), 0.0F, jitterZ(0), TOSS_JERK, TOSS_JERK_TIME);
            bowl.rotateTo(-TOSS_TILT, 0.0F, 0.0F, TOSS_JERK_TIME / 2.0F);
            after(TOSS_JERK_TIME / 2.0F, () -> bowl.rotateTo(0.0F, 0.0F, 0.0F, TOSS_JERK_TIME / 2.0F));
        });
        int count = lastLayers.size();
        for (int i = 1; i < count; i++) {
            int index = i;
            // Back in another order: the top lands lowest, the bottom on top.
            int landsAt = count - i;
            rest(i, jitterX(landsAt), landsAt * LAYER_STEP, jitterZ(landsAt));
            float height = TOSS_HEIGHT + (i * 37 % 5) * 0.6F;
            // Tumbling, never upside down: a piece is built from the icon's top and has no underside to show.
            float side = i % 2 == 0 ? 1.0F : -1.0F;
            float spin = turn(landsAt) + side * (120.0F + (index * 53 % 80));
            view.node("layer_" + i).ifPresent(node -> {
                node.hopTo(jitterX(landsAt), (landsAt - index) * LAYER_STEP, jitterZ(landsAt), height, TOSS_TIME);
                node.rotateTo(side * TOSS_TUMBLE, (turn(index) + spin) / 2.0F, -side * TOSS_TUMBLE * 0.6F, TOSS_TIME / 2.0F);
                after(TOSS_TIME / 2.0F, () -> node.rotateTo(0.0F, spin, 0.0F, TOSS_TIME / 2.0F));
            });
            after(TOSS_TIME - 0.02F * i, () -> view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.3F, 1.3F + 0.05F * index));
        }
        view.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), "surface", 0.5F, 1.4F);
    }

    /**
     * Rolled under the hands: the layers close up into one log, which rolls away from the cook and back, a little less each time.
     * It turns about its length only as far as its pieces still show their tops: they have no underside.
     */
    private void roll(StationView view) {
        int count = lastLayers.size();
        for (int i = 0; i < count; i++) {
            int index = i;
            float y = i * LAYER_STEP * 0.6F;
            rest(i, 0.0F, y, 0.0F);
            view.node("layer_" + i).ifPresent(node -> {
                node.moveTo(0.0F, y - index * LAYER_STEP, 0.0F, ROLL_GATHER);
                node.rotateTo(0.0F, turn(index) * 0.3F, 0.0F, ROLL_GATHER);
            });
        }
        for (int stroke = 0; stroke <= ROLLS; stroke++) {
            // Away, back, away, and at last to rest: each stroke shorter.
            float amount = stroke == ROLLS ? 0.0F : (stroke % 2 == 0 ? 1.0F : -1.0F) * (ROLLS - stroke) / (float) ROLLS;
            int at = stroke;
            after(ROLL_GATHER + stroke * ROLL_STROKE, () -> {
                for (int i = 0; i < count; i++) {
                    int index = i;
                    view.node("layer_" + i).ifPresent(node -> {
                        node.moveTo(0.0F, restY[index] - index * LAYER_STEP, amount * ROLL_TRAVEL, ROLL_STROKE);
                        node.rotateTo(amount * ROLL_TURN, turn(index) * 0.3F, 0.0F, ROLL_STROKE);
                    });
                }
                if (at < ROLLS) {
                    view.playSound(SoundEvents.WOOL_STEP, "surface", 0.3F, 1.2F + 0.1F * at);
                }
            });
        }
    }

    /**
     * Skewered: one after the other each piece goes onto the stick at its tip, standing across it, and is pushed down towards the
     * handle; the first one in goes farthest. The stick lies as its icon does (vanilla draws sticks from the handle at the bottom
     * left to the tip at the top right), so its line is read from the icon.
     */
    private void skewer(StationView view) {
        int count = lastLayers.size();
        int pieces = Math.max(1, count - 1);
        ItemVoxels.Axis stick = axis(lastLayers.get(0));
        float baseX = jitterX(0);
        float baseZ = jitterZ(0);
        float[] tip = stick.at(0.0F);
        float stand = (float) Math.toDegrees(Math.atan2(stick.dirX(), stick.dirZ()));
        float y = LAYER_STEP;
        for (int i = 1; i < count; i++) {
            int index = i;
            // The first piece on goes deepest, towards the handle; the last stays near the tip.
            float share = pieces == 1 ? (SKEWER_FIRST + SKEWER_LAST) / 2.0F
                    : SKEWER_LAST - (SKEWER_LAST - SKEWER_FIRST) * (i - 1) / (pieces - 1.0F);
            float[] at = stick.at(share);
            float x = baseX + at[0];
            float z = baseZ + at[1];
            rest(i, x, y, z);
            after((i - 1) * SKEWER_STAGGER, () -> view.node("layer_" + index).ifPresent(node -> {
                node.moveTo(baseX + tip[0], y - index * LAYER_STEP, baseZ + tip[1], SKEWER_SLIDE);
                node.rotateTo(SKEWER_STAND, stand, 0.0F, SKEWER_SLIDE);
                node.scaleTo(SKEWER_SCALE, SKEWER_SLIDE);
                after(SKEWER_SLIDE * 0.8F, () -> view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.3F, 1.4F + 0.06F * index));
            }));
        }
        after(SKEWER_SLIDE + (count - 1) * SKEWER_STAGGER, () -> {
            for (int i = 1; i < count; i++) {
                int index = i;
                view.node("layer_" + i).ifPresent(node ->
                        node.moveTo(restX[index], restY[index] - index * LAYER_STEP, restZ[index], SKEWER_PUSH));
            }
            view.playSound(SoundEvents.WOOD_HIT, "surface", 0.25F, 1.6F);
        });
    }

    /**
     * Kneaded: pressed together into one lump, then worked over: pushed flat so it spreads, turned, gathered again, a few times.
     */
    private void knead(StationView view) {
        int count = lastLayers.size();
        for (int i = 0; i < count; i++) {
            int index = i;
            float y = i * LAYER_STEP * 0.35F;
            rest(i, jitterX(i) * 0.5F, y, jitterZ(i) * 0.5F);
            view.node("layer_" + i).ifPresent(node -> node.moveTo(restX[index], y - index * LAYER_STEP, restZ[index], KNEAD_PRESS));
        }
        view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.5F, 0.7F);
        for (int push = 0; push < KNEAD_PUSHES; push++) {
            float twist = (push % 2 == 0 ? 1.0F : -1.0F) * KNEAD_TURN;
            float at = KNEAD_PRESS + push * KNEAD_PUSH * 2.0F;
            after(at, () -> {
                for (int i = 0; i < count; i++) {
                    int index = i;
                    // Pushed down, every piece spreads its own way out from the middle.
                    float out = (index % 2 == 0 ? 1.0F : -1.0F) * KNEAD_SPREAD;
                    view.node("layer_" + i).ifPresent(node -> {
                        node.moveTo(restX[index] + out, -index * LAYER_STEP, restZ[index] + out * 0.6F, KNEAD_PUSH);
                        node.rotateTo(0.0F, turn(index) + twist, 0.0F, KNEAD_PUSH * 2.0F);
                    });
                }
                view.playSound(SoundEvents.WOOL_STEP, "surface", 0.45F, 0.75F);
            });
            after(at + KNEAD_PUSH, () -> {
                for (int i = 0; i < count; i++) {
                    int index = i;
                    view.node("layer_" + i).ifPresent(node ->
                            node.moveTo(restX[index], restY[index] - index * LAYER_STEP, restZ[index], KNEAD_PUSH));
                }
            });
        }
    }

    /**
     * Laid in the bowl: the main piece slides to the middle, the others to their places round it, one after the other; nothing
     * flies (a steak and fried eggs are not tossed like a salad).
     */
    private void arrange(StationView view) {
        int count = lastLayers.size();
        int sides = Math.max(1, count - 2);
        for (int i = 1; i < count; i++) {
            int index = i;
            float x = jitterX(0);
            float z = jitterZ(0);
            if (i > 1) {
                float around = Mth.TWO_PI * (i - 2) / sides + 0.6F;
                x += Mth.cos(around) * ARRANGE_RING;
                z += Mth.sin(around) * ARRANGE_RING;
            }
            float y = LAYER_STEP;
            rest(i, x, y, z);
            float px = x;
            float pz = z;
            after(Math.max(0, i - 2) * ARRANGE_STAGGER, () -> view.node("layer_" + index).ifPresent(node -> {
                node.moveTo(px, y - index * LAYER_STEP, pz, ARRANGE_SLIDE);
                node.scaleTo(index == 1 ? 1.0F : ARRANGE_SCALE, ARRANGE_SLIDE);
                node.rotateTo(0.0F, turn(index) * 0.5F, 0.0F, ARRANGE_SLIDE);
                after(ARRANGE_SLIDE * 0.8F, () -> view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.3F, 1.25F + 0.05F * index));
            }));
        }
    }

    /**
     * Closed: the loaf is split along its length, its top half lifts off the bottom one, comes over the filling and closes it, and
     * the whole is pressed down once. The bottom half stays where the loaf lay.
     */
    private void sandwich(StationView view) {
        int count = lastLayers.size();
        ItemStack bread = lastLayers.get(0);
        Optional<ItemVoxels.Icon> icon = ItemVoxels.icon(bread);
        if (icon.isEmpty()) {
            return;
        }
        ItemVoxels.Icon bottom = icon.get().half(false);
        ItemVoxels.Icon top = icon.get().half(true);
        view.node("layer_0").ifPresent(node -> node.setShape(ItemVoxels.build(bottom, ITEM_SIZE, List.of(), 0.0F), bottom.sprite()));
        float x = jitterX(0);
        float z = jitterZ(0);
        float over = count * LAYER_STEP;
        ghost(view, bread, ITEM_SIZE).ifPresent(lid -> {
            lid.setShape(ItemVoxels.build(top, ITEM_SIZE, List.of(), 0.0F), top.sprite());
            lid.moveTo(x, 0.0F, z, 0.0F);
            lid.moveTo(x, over + LID_HEIGHT, z, LID_LIFT);
            after(LID_LIFT, () -> lid.moveTo(x, over, z, LID_CLOSE));
            after(LID_LIFT + LID_CLOSE, () -> {
                view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.6F, 0.9F);
                lid.moveTo(x, over - 0.3F, z, LID_PRESS / 2.0F);
                after(LID_PRESS / 2.0F, () -> lid.moveTo(x, over, z, LID_PRESS / 2.0F));
            });
            // It melts into the dish with the layers.
            after(LID_LIFT + LID_CLOSE + LID_PRESS, () -> {
                lid.scaleTo(0.1F, MELT_TIME);
                lid.moveTo(x * 0.4F, over * 0.08F, z * 0.4F, MELT_TIME);
            });
            after(LID_LIFT + LID_CLOSE + LID_PRESS + MELT_TIME, () -> lid.setVisible(false));
        });
        for (int i = 0; i < count; i++) {
            rest(i, jitterX(i), i * LAYER_STEP, jitterZ(i));
        }
    }

    /**
     * Nigiri: the rice is pressed together into a block where it lies, the slices are laid along its top one after the other, and
     * a last press holds them.
     */
    private void shape(StationView view) {
        int count = lastLayers.size();
        ItemVoxels.Axis rice = axis(lastLayers.get(0));
        float along = (float) Math.toDegrees(-rice.along());
        view.node("layer_0").ifPresent(node -> {
            node.scaleTo(0.8F, SHAPE_GATHER);
            node.hopTo(jitterX(0), 0.0F, jitterZ(0), 0.3F, SHAPE_GATHER);
        });
        view.playSound(SoundEvents.WOOL_STEP, "surface", 0.35F, 1.1F);
        for (int i = 1; i < count; i++) {
            int index = i;
            float shift = (i - (count - 1) / 2.0F - 0.5F) * 0.8F;
            float x = jitterX(0) + rice.dirX() * shift;
            float z = jitterZ(0) + rice.dirZ() * shift;
            float y = LAYER_STEP * 0.8F + (i - 1) * 0.15F;
            rest(i, x, y, z);
            after(SHAPE_GATHER + (i - 1) * SHAPE_LAY, () -> view.node("layer_" + index).ifPresent(node -> {
                node.moveTo(x, y - index * LAYER_STEP, z, SHAPE_LAY);
                node.rotateTo(0.0F, along + 45.0F, 0.0F, SHAPE_LAY);
                view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.3F, 1.3F + 0.05F * index);
            }));
        }
        rest(0, jitterX(0), 0.0F, jitterZ(0));
        float pressAt = SHAPE_GATHER + Math.max(1, count - 1) * SHAPE_LAY;
        after(pressAt, () -> {
            for (int i = 0; i < count; i++) {
                int index = i;
                view.node("layer_" + i).ifPresent(node ->
                        node.hopTo(restX[index], restY[index] - index * LAYER_STEP, restZ[index], -0.25F, SHAPE_PRESS));
            }
            view.playSound(SoundEvents.WOOL_STEP, "surface", 0.4F, 0.9F);
        });
    }

    /**
     * Frozen on the stick: the fruit gathers round the stick's top into one lump, the ice gives its cold and is gone in a frost,
     * and the lump turns frosty before it becomes the popsicle.
     */
    private void freeze(StationView view) {
        int count = lastLayers.size();
        ItemVoxels.Axis stick = axis(lastLayers.get(0));
        float[] top = stick.at(FREEZE_AT);
        float x = jitterX(0) + top[0];
        float z = jitterZ(0) + top[1];
        for (int i = 1; i < count; i++) {
            int index = i;
            boolean ice = lastLayers.get(i).is(Items.ICE) || lastLayers.get(i).is(Items.PACKED_ICE);
            float ox = ((i * 37) % 5 - 2) * 0.35F;
            float oz = ((i * 53) % 5 - 2) * 0.35F;
            float y = LAYER_STEP + i * 0.12F;
            rest(i, x + ox, y, z + oz);
            view.node("layer_" + i).ifPresent(node -> {
                node.moveTo(x + ox, y - index * LAYER_STEP, z + oz, FREEZE_GATHER);
                node.scaleTo(ice ? 0.0F : 0.75F, ice ? FREEZE_GATHER + FREEZE_SET * 0.5F : FREEZE_GATHER);
            });
        }
        after(FREEZE_GATHER, () -> {
            Vector3f at = new Vector3f(MIDDLE_X + x, TOP + LAYER_STEP * 2.0F, MIDDLE_Z + z);
            view.spawnParticles(DiegesisParticles.DUST, at, new Vector3f(0.0F, 1.5F, 0.0F), 1.8F, 10, FROST);
            view.spawnParticles(DiegesisParticles.GLINT, at, new Vector3f(0.0F, 1.0F, 0.0F), 1.5F, 4, FROST);
            view.playSound(SoundEvents.GLASS_BREAK, "surface", 0.2F, 1.8F);
            for (int i = 1; i < count; i++) {
                view.node("layer_" + i).ifPresent(node -> node.setTint(FROST));
            }
        });
    }

    /**
     * Draws layer {@code index}: an egg cracked open, a liquid as a film over the layer under it, anything else as its icon.
     */
    private void dress(NodeView node, int index, ItemStack layer, List<ItemStack> layers) {
        if (index > 0 && index - 1 < layers.size() && KitchenLooks.isPoured(layer)) {
            ItemStack under = KitchenLooks.opened(layers.get(index - 1));
            Optional<ItemVoxels.Icon> shape = ItemVoxels.icon(under);
            Optional<KitchenLooks.Liquid> liquid = KitchenLooks.liquid(layer);
            if (shape.isPresent() && liquid.isPresent()) {
                float size = (index - 1 == 0 ? ITEM_SIZE : LAYER_SIZE) * FILM_SHARE;
                KitchenLooks.Liquid it = liquid.get();
                node.setShape(ItemVoxels.film(shape.get(), size, FILM_THICK, it.icon(), it.column(), it.row()), it.icon().sprite());
                node.setTint(0xFFFFFFFF);
                return;
            }
        }
        ItemStack look = KitchenLooks.opened(layer);
        node.setShape(shape(look, index == 0 ? ITEM_SIZE : LAYER_SIZE, List.of(), 0.0F), sprite(look));
        node.setTint(KitchenLooks.isCracked(layer) ? KitchenLooks.RAW_EGG : 0xFFFFFFFF);
    }

    /**
     * Layer {@code index} goes down: from the player or the hand it lands; an egg cracks over the stack first; a liquid is poured
     * from its container, which hangs over the stack, tips and goes back to the cook.
     */
    private void arrives(StationView view, NodeView node, int index, ItemStack layer, boolean fromPlayer) {
        float x = jitterX(index);
        float z = jitterZ(index);
        Vector3f at = new Vector3f(MIDDLE_X + x, TOP + index * LAYER_STEP + 0.6F, MIDDLE_Z + z);
        if (index > 0 && KitchenLooks.isPoured(layer)) {
            node.moveTo(x, 0.0F, z, 0.0F);
            node.scaleTo(0.2F, 0.0F);
            pourUntil = clock + POUR_TIP + POUR_FLOW;
            ghost(view, layer, LAYER_SIZE).ifPresent(container -> {
                container.scaleTo(POUR_SCALE, 0.0F);
                float y = index * LAYER_STEP + POUR_HEIGHT;
                Vector3f from = fromPlayer ? FromPlayer.offset(view, TOP + y, 0, 1) : fromHand(view, container, y);
                container.moveTo(x + from.x, y, z + from.z, 0.0F);
                container.moveTo(x + 1.5F, y, z, POUR_TIP);
                container.rotateTo(0.0F, 0.0F, POUR_TILT, POUR_TIP);
                after(POUR_TIP, () -> {
                    KitchenLooks.pour(view, "surface", layer, new Vector3f(MIDDLE_X + x + 0.8F, TOP + y - 0.5F, MIDDLE_Z + z), at);
                    node.scaleTo(1.0F, POUR_FLOW);
                });
                after(POUR_TIP + POUR_FLOW, () -> {
                    container.rotateTo(0.0F, 0.0F, 0.0F, LIFT_TIME);
                    container.moveTo(x + 1.5F, y + LIFT_HEIGHT, z, LIFT_TIME + AWAY_TIME);
                    container.scaleTo(0.0F, LIFT_TIME + AWAY_TIME);
                });
                after(POUR_TIP + POUR_FLOW + LIFT_TIME + AWAY_TIME, () -> container.setVisible(false));
            });
            return;
        }
        if (fromPlayer) {
            landFromPlayer(view, node, x, 0.0F, z);
            view.playSound(FarmersDelightContent.BOARD_PLACE, "surface", 0.7F, 1.15F + 0.05F * index);
        } else {
            land(view, node, x, 0.0F, z);
        }
        if (KitchenLooks.isCracked(layer)) {
            after(fromPlayer ? PLAYER_LAND_TIME : LAND_TIME, () -> KitchenLooks.crack(view, "surface", at));
        }
    }

    @Override
    public void tick(StationView view) {
        int dishes = pages.version(view);
        if (dishes != usableFor) {
            usableFor = dishes;
            usable.clear();
        }
        if (!Float.isNaN(melting)) {
            melting -= 0.05F;
            if (melting <= 0.0F) {
                melting = Float.NaN;
                for (int i = 0; i < CuttingBoardStation.MOST_LAYERS; i++) {
                    shownLayers[i] = ItemStack.EMPTY;
                    view.node("layer_" + i).ifPresent(node -> {
                        node.scaleTo(1.0F, 0.0F);
                        node.setVisible(false);
                    });
                }
            }
        }
        refresh(view);
        view.node("item_gone").filter(gone -> gone.isVisible() && !gone.isMoving()).ifPresent(gone -> gone.setVisible(false));
    }

    private static SoundEvent chopSound(Item tool) {
        if (tool.builtInRegistryHolder().is(net.minecraft.tags.ItemTags.AXES)) {
            return SoundEvents.AXE_STRIP.value();
        }
        if (tool.builtInRegistryHolder().is(net.minecraft.tags.ItemTags.SHOVELS)) {
            return SoundEvents.SHOVEL_FLATTEN.value();
        }
        return FarmersDelightContent.KNIFE_CUT;
    }
}
