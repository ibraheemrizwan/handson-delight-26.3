package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.Floater;
import com.pockyl.diegesis.api.client.MeshShape;
import com.pockyl.diegesis.api.client.NodeView;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.scene.Visual;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.KitchenTags;
import com.pockyl.handson_delight.platform.Platform;
import com.pockyl.handson_delight.station.CookingPotStation;
import com.pockyl.handson_delight.station.PotLogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * What the pot shows, to the player working at it and to everybody around. Everything real comes from the state; the rest is
 * reading it: the water, what swims in it, how it colours and simmers, the spoon going round. The pot itself is the block.
 */
public final class PotBehavior implements StationBehavior {
    private static final int SLOTS = CookingPotBlockEntity.MEAL_DISPLAY_SLOT;
    private static final String[] ITEM_NODES = nodes("item_");
    private static final String[] MELT_NODES = nodes("melt_");
    /** A press on the spoon shorter than this is a click: the spoon stays in the hand, stirring as the mouse goes, until the next click. */
    private static final float SPOON_CLICK = 0.3F;
    @SuppressWarnings("removal")
    private static final Identifier BADGE_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "block/white_concrete");
    /**
     * Where the liquid starts and how deep a full pot is, in pixels; slots stand at {@link #ITEM_REST}. A full pot stays 1.5 px under
     * the rim (10): at 9.5 what floats, the foam at the walls and the waves showed as a thin line over the rim from the side
     * (owner's screenshot, 2026-11-08). The scene's {@code broth} tank ends at the same height.
     */
    private static final float BROTH_FLOOR = 2.0F;
    private static final float BROTH_DEPTH = 6.5F;
    private static final float ITEM_REST = 8.0F - 0.25F;
    /** How far the ingredients drift from the middle of the pot, in pixels. */
    private static final float DRIFT_RADIUS = 2.2F;
    /**
     * The badge is drawn in the pixels of the dish's icon, which is 16 of them wide and centred on the badge: a dark plate with cut
     * corners, three stars over the icon for how well the pot is stirred, a bar under it for how far the batch is.
     */
    private static final float PIXEL = 0.125F;
    static final String[] STAR = {"..#..", ".###.", "#####", ".###.", ".#.#."};
    static final int STAR_SIZE = 5;
    /** The most stars, the pixel between two of them, the top of a star; of three, the middle one stands a pixel higher. */
    private static final int STARS = 3;
    private static final int STAR_GAP = 1;
    private static final int STAR_TOP = -14;
    private static final int BAR_LEFT = -7;
    private static final int BAR_TOP = 10;
    private static final int BAR_LENGTH = 14;
    /** After a batch, its stars light up after the first, stay lit until the second, seconds, and flash one after the other this far apart. */
    private static final float GRADE_WAIT = 0.3F;
    private static final float GRADE_SHOWN = 2.8F;
    private static final float GRADE_STAGGER = 0.18F;
    private static final float[] GRADE_PITCH = {1.0F, 1.26F, 1.5F};
    /** The servings the stars added, shown as a stack count is, over the icon's lower right corner: digits three pixels by five. */
    private static final String[] PLUS = {"...", ".#.", "###", ".#.", "..."};
    private static final String[][] DIGITS = {
            {"###", "#.#", "#.#", "#.#", "###"}, {".#.", "##.", ".#.", ".#.", "###"}, {"###", "..#", "###", "#..", "###"},
            {"###", "..#", ".##", "..#", "###"}, {"#.#", "#.#", "###", "..#", "..#"}, {"###", "#..", "###", "..#", "###"},
            {"###", "#..", "###", "#.#", "###"}, {"###", "..#", ".#.", ".#.", ".#."}, {"###", "#.#", "###", "#.#", "###"},
            {"###", "#.#", "###", "..#", "###"}};
    private static final int COUNT_RIGHT = 9;
    /** A pot with no fire under it shows an unlit flame where the bar would run, as Farmer's Delight's menu does: nothing cooks yet. */
    private static final String[] FLAME = {"..#..", ".##.#", "#####", "#####", ".###."};
    private static final int FLAME_LEFT = -2;
    private static final int FLAME_TOP = 8;
    private static final int COUNT_TOP = 4;
    private static final Vector3f ITEM_MIDDLE = new Vector3f(8.0F, 8.0F, 8.0F);
    private static final Vector3f RISE = new Vector3f(0.0F, 3.5F, 0.0F);
    private static final Vector3f STEAM_RISE = new Vector3f(0.0F, 3.2F, 0.0F);
    /** The pot's steam: whiter than the library's, a pot on the fire steams plainly; it still leaves the soup in sight. */
    private static final int STEAM_COLOR = 0x6CFFFFFF;
    private static final Vector3f SPLASH_UP = new Vector3f(0.0F, 7.0F, 0.0F);
    private static final int DIAL_COLOR = 0xFFFFCB6B;
    private static final int TRACK_COLOR = 0x50FFF2DC;
    private static final int PLATE_COLOR = 0x8C1E140C;
    /** How much floats in a soup for the servings in the pot. */
    private static final int PIECES_BASE = 14;
    private static final int PIECES_PER_SERVING = 5;
    private static final int PIECES_MOST = 32;
    /** The end of cooking: an ingredient swells for this long, then shrinks away until {@link #MELT_TIME}, one after the other. */
    private static final float SWELL_TIME = 0.16F;
    private static final float SWELL = 1.4F;
    private static final float MELT_TIME = 0.55F;
    private static final float MELT_STAGGER = 0.07F;
    /** How long after the last ingredient the pieces start to come up. */
    private static final float PIECES_WAIT = 0.25F;
    private static final Vector3f PUFF_UP = new Vector3f(0.0F, 4.0F, 0.0F);
    private static final int GLINT_COLOR = 0xFFFFE9A0;
    /** Water takes this alpha, a finished soup the other: it thickens as it cooks. */
    private static final int WATER_ALPHA = 0xA0;
    private static final int SOUP_ALPHA = 0xF0;
    /** How much of the food's colour the water has before any cooking. */
    private static final float RAW_TINT = 0.3F;

    /** How far the spoon's end goes from the pot's middle, in pixels. */
    private static final float SPOON_REACH = 2.3F;
    /** The height of the spoon's end, where it pivots in the model. */
    private static final float SPOON_END = 3.0F;
    /** How much the spoon leans at rest (Farmer's Delight's own angle) and in the hand, degrees. */
    private static final float REST_LEAN = 22.5F;
    private static final float STIR_LEAN = 7.0F;
    /** The spoon's half width; how far from it an item's middle stays; how far from the pot's middle a wall lets an item go. */
    private static final float SPOON_RADIUS = 1.05F;
    /** The spoon going back to its rest by itself: how long it takes to get moving (seconds), how fast it then goes and rises out of the soup. */
    private static final float SPOON_HOME_EASE = 0.6F;
    private static final float SPOON_HOME_RATE = 1.8F;
    private static final float SPOON_HOME_GRIP = 2.0F;
    private static final float ITEM_CLEARANCE = 2.1F;
    private static final float ITEM_ROOM = 2.9F;
    private static final float ITEM_RADIUS = 1.15F;
    private static final float POT_HALF = 5.0F;
    /** A somersault of an item in the soup: from how strong a swirl, how often at the strongest (per second), how long, how high. */
    private static final float TUMBLE_FROM = 0.3F;
    private static final float TUMBLE_RATE = 0.35F;
    private static final float TUMBLE_TIME = 0.7F;
    private static final float TUMBLE_HOP = 0.7F;
    /** How fast an item pushed aside drifts back to its way. */
    private static final float PUSH_HALF_LIFE = 0.35F;
    /** For how long after the local hand let go the spoon is still its own, seconds: longer than a stir keeps the pot going. */
    private static final float HAND_MEMORY = 2.5F;
    /** Seconds between what the hand tells the server, and how far it must have moved by then, in pixels. */
    private static final float SEND_INTERVAL = 0.12F;
    private static final float SEND_TRAVEL = 0.2F;
    private static final float RIPPLE_INTERVAL = 0.09F;
    private static final float RIPPLE_STRENGTH = 0.17F;
    /** How loud the layers of the pot's sound are; a piece coming up, and how far apart those come at most, seconds. */
    private static final float BODY_RESTING = 0.3F;
    private static final float BODY_COOKING = 0.7F;
    private static final float STIR_VOLUME = 0.7F;
    private static final float BLUB_VOLUME = 0.32F;
    private static final float SIZZLE_VOLUME = 0.22F;
    private static final float ADD_FOOD_VOLUME = 0.16F;
    /** Farmer's Delight's boiling comes in clips with soft ends: laid over each other this often (seconds) they make one sound. */
    private static final float BOIL_EVERY = 2.6F;
    private static final float BOIL_EVERY_COOKING = 1.7F;
    private static final float SIZZLE_EVERY = 3.0F;
    private static final float BLUB_GAP = 0.11F;
    private static final int MOST_BLUBS = 9;
    /** How long the soup keeps turning after the spoon stops, seconds, and how many radians a second it turns at most. */
    private static final float SWIRL_HALF_LIFE = 0.9F;
    /** A whole item's trail: how clear at its clearest, how wide (pixels), from what speed and how much more it takes to be full (pixels per second). */
    private static final float ITEM_WAKE = 0.7F;
    /** Trails on plain water and on a soup that is done: how long they stay (seconds), how clearly they stand out. */
    private static final float WATER_WAKE_LIFE = 1.2F;
    private static final float SOUP_WAKE_LIFE = 9.0F;
    private static final float WATER_WAKE_CONTRAST = 0.25F;
    private static final float SOUP_WAKE_CONTRAST = 1.0F;
    private static final float ITEM_WAKE_WIDTH = 1.4F;
    private static final float ITEM_WAKE_FROM = 0.4F;
    private static final float ITEM_WAKE_RANGE = 1.6F;
    private static final float SWIRL_SPEED = 1.5F;
    private static final float CALM_DRIFT = 0.05F;
    /** Bubbles along the walls are small beside the ones that rise from the bottom. */
    private static final float EDGE_BUBBLE_SIZE = 0.5F;
    /**
     * Several things put in at once (a dish picked in the book, a stack shared out) jump in one after the other (owner: "cozy"),
     * from the player at the bottom of the screen ({@link FromPlayer}): seconds between them, seconds in the air, how high they
     * start over the soup and how high the arc goes, pixels.
     */
    private static final float ARRIVE_STAGGER = 0.16F;
    private static final float ARRIVE_TIME = 0.4F;
    private static final float ARRIVE_FROM = 9.0F;
    private static final float ARRIVE_ARC = 4.0F;
    /**
     * A bowl from the inventory offered over a finished soup to an empty hand (owner: "offer to scoop it if there is a bowl in the
     * inventory"): how see-through, how high over the soup it hangs (pixels), how long it stays away after a scoop (seconds).
     */
    private static final int OFFERED = 0x8CFFFFFF;
    private static final float OFFER_LIFT = 1.5F;
    private static final float OFFER_AWAY = 0.35F;

    private final RecipeBook book = new RecipeBook(new PotBook());
    /** Whether an item goes into some dish or serves one: only those show in the inventory at the pot. */
    private final UsableCache usable = new UsableCache();
    private final ItemStack[] shown = new ItemStack[SLOTS];
    private ItemStack preview = ItemStack.EMPTY;
    private float shownProgress;
    private float shownStars;
    /** How long the stars the last batch got have been shown. */
    private float gradeAge = Float.NaN;
    private float time;
    private float bubbles;
    private float edgeBubbles;
    private float steam;
    private final float[] places = new float[SLOTS];
    private float level;
    private float turned;
    private int drawnDial = -1;
    private int drawnTrack = -1;
    private int bubbleColor = 0xFFFFFFFF;
    private Item forMeal;
    private List<Floater> mealPieces = List.of();
    private int servings = -1;
    /** The ghosts of ingredients that just cooked away: how long they have been going (negative: waiting), where they lie. */
    private final float[] meltAge = new float[SLOTS];
    private final float[] meltX = new float[SLOTS];
    private final float[] meltY = new float[SLOTS];
    private final float[] meltZ = new float[SLOTS];
    /** Items jumping in: how long they have been in the air (negative: waiting for their turn, NaN: floating), where from. */
    private final float[] arriveAge = new float[SLOTS];
    private final float[] arriveX = new float[SLOTS];
    private final float[] arriveZ = new float[SLOTS];
    /**
     * What does not float whole (a liquid poured, an egg cracked, sugar dissolving): how long since it went in, NaN while it floats or
     * before it is in; once gone it stays gone for as long as the slot holds it.
     */
    private final float[] sinkAge = new float[SLOTS];
    private final boolean[] sunk = new boolean[SLOTS];
    private float piecesWait;
    private int brothColor;

    /** A liquid's container hangs this high over the soup, tips over this long, pours, and is gone. */
    private static final float POUR_HOVER = 3.0F;
    private static final float POUR_TIP = 0.15F;
    private static final float POUR_FLOW = 0.4F;
    private static final float POUR_TILT = 110.0F;
    /** How long a dissolving or cracked thing takes to be gone. */
    private static final float SINK_TIME = 0.6F;
    private static final float CRACK_TIME = 0.15F;

    /** How much of the water a liquid poured into it takes over. */
    private static final float POURED_SHARE = 0.6F;
    /** A block container waits this small, this high over the soup. */
    private static final float FILLED_SCALE = 0.45F;
    private static final float FILLED_LIFT = 5.0F;

    private float shownTurn = Float.NaN;
    private float shownFacing = Float.NaN;
    /** The local player's hand is on the spoon. */
    private boolean stroking;
    /** Where the spoon's end is, in the pot's own pixels from its middle. */
    private float tipX;
    private float tipZ;
    /** 0: leaning at rest, 1: in the soup. */
    private float grip;
    private float homing;
    private float swirl;
    private float handLeft;
    /** Where the spoon goes through the surface, and where the items float, in pixels from the pot's middle in the scene. */
    private float spoonX;
    private float spoonZ;
    private final float[] itemX = new float[SLOTS];
    private final float[] itemZ = new float[SLOTS];
    private final float[] lastItemX = new float[SLOTS];
    private final float[] lastItemZ = new float[SLOTS];
    private final boolean[] itemFloating = new boolean[SLOTS];
    private final float[] pushX = new float[SLOTS];
    private final float[] tumbling = new float[SLOTS];
    private final float[] pushZ = new float[SLOTS];
    private float swirlSign = 1.0F;
    /** When the hand took the spoon; whether a click did, so the next click puts it down. */
    private float strokeStart;
    /** The container offered over the soup, empty while none is; when the last scoop with it went. */
    private ItemStack offered = ItemStack.EMPTY;
    private Item offeredItem;
    private float scoopedAt = -10.0F;
    private boolean heldByClick;
    /** Kept between frames, not made anew in each. */
    private final Matrix4f potInverse = new Matrix4f();
    private final Quaternionf lean = new Quaternionf();
    private final Vector3f restAxis = new Vector3f();
    private final Vector3f leanAxis = new Vector3f();
    private final Vector3f poseAngles = new Vector3f();
    private final Vector3f spoonEnd = new Vector3f();
    private final Vector3f spoonThrough = new Vector3f();
    private final float[][] obstacles = new float[SLOTS + 1][];
    /** The ingredients the dish was last looked up for, and the dish. */
    private final ItemStack[] cookedFor = new ItemStack[SLOTS];
    private ItemStack cookedMade = ItemStack.EMPTY;
    private float sendClock;
    private float sendTravel;
    private float rippleClock;
    private float boilClock;
    private float sizzleClock;
    private int blubs;
    private float blubClock;
    private int piecesShown;

    public PotBehavior(StationView view) {
        java.util.Arrays.fill(shown, ItemStack.EMPTY);
        java.util.Arrays.fill(meltAge, Float.NaN);
        java.util.Arrays.fill(arriveAge, Float.NaN);
        java.util.Arrays.fill(sinkAge, Float.NaN);
        java.util.Arrays.fill(sunk, false);
    }

    @Override
    public void onSceneReady(StationView view) {
        drawnDial = -1;
        drawnTrack = -1;
        shownTurn = Float.NaN;
        view.node("dial").ifPresent(dial -> dial.setTint(DIAL_COLOR));
        view.node("dial_track").ifPresent(track -> track.setTint(TRACK_COLOR));
        view.node("badge").ifPresent(plate -> {
            plate.setTint(PLATE_COLOR);
            MeshShape.Builder shape = MeshShape.builder();
            pixels(shape, -10, -16, 20, 28);
            pixels(shape, -9, -17, 18, 1);
            pixels(shape, -9, 12, 18, 1);
            plate.setShape(shape.build(), BADGE_TEXTURE, true);
        });
        view.node("book_open").ifPresent(node -> node.setPainter(book));
        refresh(view, true);
    }

    @Override
    public void onOpen(StationView view) {
        usable.clear();
        // Asked for every slot every frame, so remembered by item; a stack with its own data may match differently, it is asked each time.
        view.setInventoryFilter(stack -> usable.test(stack, it -> usable(view, it)));
        book.opened(view);
        refresh(view, true);
    }

    @Override
    public void onClose(StationView view) {
        stroking = false;
        heldByClick = false;
        book.shut(view);
        refresh(view, true);
    }

    @Override
    public void onStateChanged(StationView view, StationState.Changes changes) {
        refresh(view, false);
    }

    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (event instanceof SceneEvent.Stroke stroke) {
            switch (stroke.phase()) {
                case START -> {
                    stroking = true;
                    if (!heldByClick) {
                        strokeStart = time;
                    }
                }
                case END -> {
                    stroking = false;
                    if (heldByClick) {
                        // The next click puts the spoon down.
                        heldByClick = false;
                    } else if (time - strokeStart < SPOON_CLICK) {
                        // A click takes the spoon in hand: it stirs as the mouse goes, with no button to hold.
                        heldByClick = true;
                        view.holdStroke();
                    }
                }
                default -> {
                }
            }
        }
        if (event instanceof SceneEvent.Click click && click.node().equals("book_open")) {
            book.click(view);
        } else if (event instanceof SceneEvent.Click click && click.node().equals("soup") && click.button() == 0 && !offered.isEmpty()) {
            CompoundTag how = new CompoundTag();
            how.putBoolean(CookingPotStation.FROM_INVENTORY, true);
            how.putBoolean(CookingPotStation.SCOOP_ALL, net.minecraft.client.Minecraft.getInstance().hasShiftDown());
            view.sendAction(CookingPotStation.ACTION_SCOOP, how);
            scoopedAt = time;
        }
    }

    /** A bowl dipped into the soup fills; the book takes its click with a stack in the hand too; the rest goes on to the pot as usual. */
    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        if ("book_open".equals(node)) {
            book.click(view);
            return true;
        }
        if ("preview".equals(node) || !canScoop(view, stack)) {
            return false;
        }
        CompoundTag how = new CompoundTag();
        how.putBoolean(CookingPotStation.SCOOP_ALL, shift);
        view.sendAction(CookingPotStation.ACTION_SCOOP, how);
        return true;
    }

    private static boolean usable(StationView view, ItemStack stack) {
        for (RecipeHolder<CookingPotRecipe> holder : Platform.INSTANCE.cookingRecipes(view.level())) {
            CookingPotRecipe recipe = holder.value();
            if (ItemStack.isSameItem(KitchenRecipes.container(recipe), stack) || recipe.input().stream().anyMatch(ingredient -> ingredient.test(stack))) {
                return true;
            }
        }
        return false;
    }

    private static boolean canScoop(StationView view, ItemStack stack) {
        return view.level().getBlockEntity(view.pos()) instanceof CookingPotBlockEntity pot && !pot.getMeal().isEmpty() && pot.isContainerValid(stack);
    }

    /**
     * With an empty hand over a finished soup, a container from the inventory hangs see-through where the hand is, ready to dip: a
     * click scoops with it, Shift as many as there are, and the hand stays empty. Not over the spoon, nor while it stirs.
     */
    private void offerBowl(StationView view) {
        offered = ItemStack.EMPTY;
        LocalPlayer player = Minecraft.getInstance().player;
        NodeView node = view.node("scoop").orElse(null);
        boolean spoon = stroking || heldByClick || view.node("ladle").map(NodeView::isHovered).orElse(false);
        if (node == null || player == null || !view.isFocused() || spoon || !player.inventoryMenu.getCarried().isEmpty()
                || time - scoopedAt < OFFER_AWAY || !(view.level().getBlockEntity(view.pos()) instanceof CookingPotBlockEntity pot)) {
            if (node != null) {
                node.setVisible(false);
            }
            view.node("soup").ifPresent(soup -> soup.setEnabled(false));
            return;
        }
        int slot = PotLogic.containerSlot(pot, player.getInventory());
        float surface = BROTH_FLOOR + BROTH_DEPTH * level;
        Optional<Vector3f> over = slot < 0 ? Optional.empty() : view.cursorOn(surface + OFFER_LIFT)
                .filter(point -> Math.abs(point.x - 8.0F) < POT_HALF && Math.abs(point.z - 8.0F) < POT_HALF);
        node.setVisible(over.isPresent());
        view.node("soup").ifPresent(soup -> soup.setEnabled(over.isPresent()));
        if (over.isEmpty()) {
            return;
        }
        offered = player.getInventory().getItem(slot);
        if (offeredItem != offered.getItem()) {
            offeredItem = offered.getItem();
            node.setVisual(Visual.ofItem(offeredItem).withAnchor(ITEM_MIDDLE));
            node.setTint(OFFERED);
            // A pumpkin to be stuffed is not dipped like a bowl: it waits over the rim, small, and the soup is ladled into it.
            node.scaleTo(isFilledNotDipped(offered) ? FILLED_SCALE : 1.0F, 0.0F);
        }
        float lift = isFilledNotDipped(offered) ? FILLED_LIFT : OFFER_LIFT;
        node.moveTo(over.get().x - 8.0F, surface + lift - 8.0F, over.get().z - 8.0F, 0.0F);
    }

    /** A container that is a block (a pumpkin for a stuffed pumpkin): filled from the pot, never dipped into it. */
    private static boolean isFilledNotDipped(ItemStack container) {
        return container.getItem() instanceof net.minecraft.world.item.BlockItem;
    }

    /** Whether the hand holds a bowl over the soup: it fills on a click, and the pot says so with its outline. */
    private boolean bowlOverSoup(StationView view) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !view.isFocused() || !canScoop(view, player.inventoryMenu.getCarried())) {
            return false;
        }
        return view.cursorOn(BROTH_FLOOR + BROTH_DEPTH * level)
                .filter(point -> Math.abs(point.x - 8.0F) < POT_HALF && Math.abs(point.z - 8.0F) < POT_HALF).isPresent();
    }

    @Override
    public void onEffect(StationView view, Identifier effect, String node, CompoundTag data) {
        if (CookingPotStation.EFFECT_SCOOPED.equals(effect)) {
            book.statsChanged();
            view.node("broth").ifPresent(broth -> broth.ripple(8.0F, 8.0F, 0.5F));
            view.spawnOnLiquid(DiegesisParticles.SPLASH, "broth", 4, 0xFF000000 | brothColor, SPLASH_UP);
            if (isFilledNotDipped(offered)) {
                // Ladled up into the pumpkin over the rim.
                view.node("scoop").map(NodeView::scenePosition).ifPresent(into -> view.spawnParticles(DiegesisParticles.DROPLET,
                        new Vector3f(8.0F, BROTH_FLOOR + BROTH_DEPTH * level, 8.0F), new Vector3f(into).sub(8.0F, BROTH_FLOOR + BROTH_DEPTH * level, 8.0F),
                        0.3F, 8, 0xFF000000 | brothColor));
            }
            view.playSound(PotSounds.SCOOP, "broth", 0.55F, 1.0F);
        }
    }

    /** Reads the state again; {@code quiet} shows it as it is, without the splashes of things arriving. */
    private void refresh(StationView view, boolean quiet) {
        StationState state = view.state();
        ItemStack[] items = state.getItems(CookingPotStation.INGREDIENTS);
        ItemStack meal = state.getItem(CookingPotStation.MEAL_SLOT);
        if (!quiet && servings >= 0 && meal.getCount() > servings) {
            cookedThrough(view, items);
            graded(view);
        }
        servings = meal.getCount();
        List<Integer> arrivals = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack now = items[i];
            if (!now.isEmpty() && shown[i].isEmpty() && !quiet) {
                arrivals.add(i);
            }
            if (now.isEmpty() || quiet) {
                arriveAge[i] = Float.NaN;
            }
            if (!ItemStack.isSameItem(now, shown[i])) {
                // Another thing in this slot: it floats until it goes in; one found there when the pot is first seen is in already.
                sinkAge[i] = Float.NaN;
                sunk[i] = quiet && sinks(now);
                int index = i;
                view.node(ITEM_NODES[i]).ifPresent(node -> node.scaleTo(sunk[index] ? 0.0F : 1.0F, 0.0F));
            }
            shown[i] = now.copy();
        }
        if (arrivals.size() > 1) {
            float height = BROTH_FLOOR + BROTH_DEPTH * level + ARRIVE_FROM;
            for (int k = 0; k < arrivals.size(); k++) {
                int index = arrivals.get(k);
                Vector3f from = FromPlayer.point(view, height, k, arrivals.size()).orElse(new Vector3f(8.0F, height, 8.0F));
                arriveAge[index] = -k * ARRIVE_STAGGER;
                arriveX[index] = from.x - 8.0F;
                arriveZ[index] = from.z - 8.0F;
            }
        } else {
            arrivals.forEach(index -> splashIn(view, index));
        }
        for (int i = 0; i < SLOTS; i++) {
            boolean there = !shown[i].isEmpty() && !(arriveAge[i] < 0.0F) && !sunk[i];
            view.node(ITEM_NODES[i]).ifPresent(node -> node.setEnabled(there));
        }
        view.node("served").ifPresent(node -> node.setEnabled(!state.getItem(CookingPotStation.SERVED_SLOT).isEmpty()));
        preview = meal.isEmpty() ? cooked(view, items) : meal;
        boolean focused = view.isFocused();
        view.node("preview").ifPresent(node -> {
            node.setVisible(focused && !preview.isEmpty());
            if (!preview.isEmpty()) {
                node.setVisual(Visual.ofItem(preview.getItem()).withAnchor(ITEM_MIDDLE));
            }
        });
        badge(view);
        tooltip(view, meal);
        colour(view, meal, items);
        pieces(view, meal);
    }

    /** An item comes into the soup: rings, drops, a plop, and a hiss on a hot pot. */
    private void splashIn(StationView view, int index) {
        if (sinks(shown[index])) {
            goesIn(view, index);
            return;
        }
        view.node("broth").ifPresent(broth -> broth.ripple(8.0F, 8.0F, 0.5F));
        view.spawnParticles(DiegesisParticles.SPLASH, ITEM_NODES[index], 5, 0);
        view.playSound(PotSounds.PLOP, ITEM_NODES[index], 0.5F, 0.92F + 0.03F * index);
        if (view.state().getControl(CookingPotStation.HEATED) > 0.5F) {
            view.playSound(FarmersDelightContent.SKILLET_ADD_FOOD, ITEM_NODES[index], ADD_FOOD_VOLUME, 0.8F);
        }
    }

    /**
     * Whether {@code stack} goes into the soup without floating in it whole: a liquid is poured out of its container, an egg is
     * cracked, sugar and the like dissolve.
     */
    private static boolean sinks(ItemStack stack) {
        return KitchenLooks.isPoured(stack) || KitchenLooks.isCracked(stack) || KitchenTags.is(stack, KitchenTags.DISSOLVES);
    }

    /** Something that does not float whole went in: it starts its way out of sight. */
    private void goesIn(StationView view, int index) {
        sinkAge[index] = 0.0F;
        ItemStack stack = shown[index];
        Vector3f at = view.node(ITEM_NODES[index]).map(NodeView::scenePosition).orElse(new Vector3f(8.0F, 8.0F, 8.0F));
        if (KitchenLooks.isCracked(stack)) {
            KitchenLooks.crack(view, ITEM_NODES[index], at);
            view.node("broth").ifPresent(broth -> broth.ripple(8.0F, 8.0F, 0.3F));
        } else if (!KitchenLooks.isPoured(stack)) {
            view.spawnInLiquid(DiegesisParticles.BUBBLE, "broth", 6, bubbleColor, RISE);
            view.playSound(PotSounds.PLOP, ITEM_NODES[index], 0.3F, 1.3F);
        }
    }

    /**
     * Where something that does not float whole is on its way out of sight: a container hangs over the soup and tips it out, an egg's
     * shell is gone at once, what dissolves sinks and shrinks. Moves its node; once gone, the slot shows nothing.
     */
    private void sinking(StationView view, int index, float dt, float surface) {
        sinkAge[index] += dt;
        float age = sinkAge[index];
        ItemStack stack = shown[index];
        NodeView node = view.node(ITEM_NODES[index]).orElse(null);
        if (node == null) {
            return;
        }
        float x = itemX[index];
        float z = itemZ[index];
        float gone;
        if (KitchenLooks.isPoured(stack)) {
            float tip = Math.min(1.0F, age / POUR_TIP);
            node.moveTo(x, surface + POUR_HOVER * tip, z, 0.0F);
            node.rotateTo(0.0F, 0.0F, POUR_TILT * tip, 0.0F);
            if (age - dt < POUR_TIP && age >= POUR_TIP) {
                Vector3f from = node.scenePosition();
                KitchenLooks.pour(view, ITEM_NODES[index], stack, from, new Vector3f(from.x, from.y - POUR_HOVER - 1.0F, from.z));
                view.node("broth").ifPresent(broth -> broth.ripple(x + 8.0F, z + 8.0F, 0.4F));
            }
            gone = POUR_TIP + POUR_FLOW;
            if (age > gone - 0.2F) {
                node.scaleTo(0.0F, 0.2F);
            }
        } else if (KitchenLooks.isCracked(stack)) {
            gone = CRACK_TIME;
            node.scaleTo(0.0F, CRACK_TIME);
        } else {
            gone = SINK_TIME;
            node.moveTo(x, surface - 3.0F * age / SINK_TIME, z, 0.0F);
            node.scaleTo(0.0F, SINK_TIME);
        }
        if (age >= gone) {
            sinkAge[index] = Float.NaN;
            sunk[index] = true;
            node.setEnabled(false);
        }
    }

    /** Items waiting for their turn come out one by one; what lands splashes. */
    private void arrivals(StationView view, float dt) {
        for (int i = 0; i < SLOTS; i++) {
            if (Float.isNaN(arriveAge[i])) {
                continue;
            }
            float before = arriveAge[i];
            arriveAge[i] += dt;
            if (before < 0.0F && arriveAge[i] >= 0.0F) {
                view.node(ITEM_NODES[i]).ifPresent(node -> node.setEnabled(true));
            }
            if (arriveAge[i] >= ARRIVE_TIME) {
                arriveAge[i] = Float.NaN;
                splashIn(view, i);
            }
        }
    }

    /** The badge shows while there is a dish to cook or serve; its stars and bar while it cooks, or while a batch shows its stars. */
    private void badge(StationView view) {
        boolean shown = view.isFocused() && !preview.isEmpty();
        boolean running = shown && (view.state().getItem(CookingPotStation.MEAL_SLOT).isEmpty() || hasIngredients(view) || !Float.isNaN(gradeAge));
        view.node("badge").ifPresent(plate -> plate.setVisible(shown));
        view.node("dial_track").ifPresent(track -> track.setVisible(running));
        view.node("dial").ifPresent(dial -> dial.setVisible(running));
    }

    /** A batch is done: the stars it got light up one after the other and stay lit for a moment. The server's word may come a tick later. */
    private void graded(StationView view) {
        gradeAge = view.isFocused() ? 0.0F : Float.NaN;
        book.statsChanged();
        badge(view);
    }

    private static int grade(StationView view) {
        return Mth.clamp(view.state().data().getIntOr(CookingPotStation.GRADE, 0), 0, STARS);
    }

    private static int bonus(StationView view) {
        return Math.max(0, view.state().data().getIntOr(CookingPotStation.BONUS, 0));
    }

    /** The stars the cook has opened for this dish: one the first time, all three once they know it. */
    private static int openStars(StationView view) {
        return Mth.clamp(view.state().data().getIntOr(CookingPotStation.OPEN_STARS, 0), 1, STARS);
    }

    /** Where the left edge of a star stands, the open ones centred over the icon; five pixels wide, they sit between its pixels. */
    private static float starLeft(int open, int star) {
        return -(open * (STAR_SIZE + STAR_GAP) - STAR_GAP) / 2.0F + star * (STAR_SIZE + STAR_GAP);
    }

    private static int starTop(int open, int star) {
        return open == STARS && star == 1 ? STAR_TOP - 1 : STAR_TOP;
    }

    private void showGrade(StationView view, float dt) {
        if (Float.isNaN(gradeAge)) {
            return;
        }
        float before = gradeAge;
        gradeAge += dt;
        NodeView plate = view.node("badge").orElse(null);
        for (int star = 0; star < grade(view) && plate != null; star++) {
            float at = GRADE_WAIT + star * GRADE_STAGGER;
            if (before <= at && gradeAge > at) {
                float x = -(starLeft(openStars(view), star) + STAR_SIZE / 2.0F) * PIXEL;
                float z = -(starTop(openStars(view), star) + STAR_SIZE / 2.0F) * PIXEL;
                Vector3f middle = plate.sceneMatrix().transformPosition(new Vector3f(x, 0.0F, z));
                view.spawnParticles(DiegesisParticles.GLINT, middle, PUFF_UP, 0.4F, 3, GLINT_COLOR);
                if (bonus(view) > 0) {
                    // The chime is for stars that pay: one star lights up quietly, so the first ones teach what the stars are for.
                    view.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), "badge", 0.35F, GRADE_PITCH[star]);
                }
            }
        }
        if (gradeAge >= GRADE_SHOWN) {
            gradeAge = Float.NaN;
            badge(view);
        }
    }

    /**
     * The moment it is done, the cartoon way: what went in swells and bursts one after the other, and only then the pieces come up.
     * Farmer's Delight has taken the items by now, so what bursts are copies where they floated; a stack with more left goes on floating.
     */
    private void cookedThrough(StationView view, ItemStack[] items) {
        float surface = BROTH_FLOOR + BROTH_DEPTH * level - ITEM_REST;
        int melting = 0;
        for (int i = 0; i < SLOTS; i++) {
            int index = i;
            if (shown[i].isEmpty() || !floats(i)) {
                continue;
            }
            if (!items[i].isEmpty()) {
                continue;
            }
            meltX[i] = itemX[i];
            meltY[i] = surface;
            meltZ[i] = itemZ[i];
            meltAge[i] = -melting++ * MELT_STAGGER;
            view.node(MELT_NODES[i]).ifPresent(node -> {
                node.setVisual(Visual.ofItem(shown[index].getItem()).withAnchor(ITEM_MIDDLE));
                node.moveTo(meltX[index], meltY[index], meltZ[index], 0.0F);
                node.scaleTo(1.0F, 0.0F);
                node.setVisible(true);
            });
        }
        if (melting > 0) {
            piecesWait = melting * MELT_STAGGER + MELT_TIME + PIECES_WAIT;
            view.spawnInLiquid(DiegesisParticles.BUBBLE, "broth", 10, bubbleColor, RISE);
            view.node("broth").ifPresent(broth -> broth.ripple(8.0F, 8.0F, 0.3F));
        }
    }

    /** Swells, shrinks away and is gone with a puff. */
    private void melt(StationView view, float dt) {
        for (int i = 0; i < SLOTS; i++) {
            if (Float.isNaN(meltAge[i])) {
                continue;
            }
            meltAge[i] += dt;
            float age = meltAge[i];
            if (age < 0.0F) {
                continue;
            }
            NodeView node = view.node(MELT_NODES[i]).orElse(null);
            if (node == null) {
                meltAge[i] = Float.NaN;
            } else if (age >= MELT_TIME) {
                meltAge[i] = Float.NaN;
                node.setVisible(false);
                burst(view, i);
            } else {
                float left = 1.0F - (age - SWELL_TIME) / (MELT_TIME - SWELL_TIME);
                node.scaleTo(age < SWELL_TIME ? Mth.lerp(age / SWELL_TIME, 1.0F, SWELL) : SWELL * left * left, 0.0F);
                node.moveTo(meltX[i], meltY[i] - age * 0.7F, meltZ[i], 0.0F);
            }
        }
        if (piecesWait > 0.0F) {
            piecesWait -= dt;
            if (piecesWait <= 0.0F) {
                pieces(view, view.state().getItem(CookingPotStation.MEAL_SLOT));
            }
        }
    }

    private void burst(StationView view, int slot) {
        Vector3f at = new Vector3f(8.0F + meltX[slot], 8.0F + meltY[slot], 8.0F + meltZ[slot]);
        view.spawnParticles(DiegesisParticles.STEAM, at, PUFF_UP, 0.5F, 4, 0);
        view.spawnParticles(DiegesisParticles.GLINT, at, PUFF_UP, 0.9F, 3, GLINT_COLOR);
        view.node("broth").ifPresent(broth -> broth.ripple(at.x, at.z, 0.35F));
        view.playSound(PotSounds.BURST, "broth", 0.55F, 0.95F + 0.05F * slot);
    }

    /** The soup holds what it is made of, and thins out serving by serving. */
    private void pieces(StationView view, ItemStack meal) {
        if (piecesWait > 0.0F) {
            return;
        }
        int count = meal.isEmpty() ? 0 : Math.min(PIECES_MOST, PIECES_BASE + PIECES_PER_SERVING * meal.getCount());
        if (preview.getItem() != forMeal) {
            forMeal = preview.isEmpty() ? null : preview.getItem();
            mealPieces = preview.isEmpty() ? List.of() : CookedPieces.of(view.level(), preview, FoodColors.broth(view.level(), preview));
        }
        int shownCount = count;
        view.node("broth").ifPresent(broth -> broth.setFloating(mealPieces, shownCount));
        // What comes up can be heard coming up, one after the other.
        blubs = Math.min(MOST_BLUBS, blubs + Math.max(0, count - piecesShown));
        piecesShown = count;
    }

    private boolean hasIngredients(StationView view) {
        return java.util.Arrays.stream(view.state().getItems(CookingPotStation.INGREDIENTS)).anyMatch(stack -> !stack.isEmpty());
    }

    /** Clear water, tinted by what is in it, deepening to the colour of the food as it cooks. */
    private void colour(StationView view, ItemStack meal, ItemStack[] items) {
        int water = WATER_ALPHA << 24 | BiomeColors.getAverageWaterColor((net.minecraft.client.multiplayer.ClientLevel)view.level(), view.pos()) & 0xFFFFFF;
        if (meal.isEmpty()) {
            // A liquid poured in colours the water at once: milk makes it milky before anything cooks.
            for (ItemStack item : items) {
                if (KitchenLooks.isPoured(item)) {
                    water = blend(water, SOUP_ALPHA << 24 | KitchenLooks.liquidColor(item) & 0xFFFFFF, POURED_SHARE);
                }
            }
        }
        List<ItemStack> food = new ArrayList<>();
        if (!meal.isEmpty()) {
            food.add(meal);
        } else {
            food.addAll(List.of(items));
            food.add(view.state().getItem(CookingPotStation.SERVED_SLOT));
        }
        food.removeIf(ItemStack::isEmpty);
        int colour = water;
        float uneven = 0.0F;
        if (!food.isEmpty()) {
            float cooked = meal.isEmpty() ? RAW_TINT + (1.0F - RAW_TINT) * view.state().getControl(CookingPotStation.PROGRESS) : 1.0F;
            int soup = preview.isEmpty() ? FoodColors.mix(food) : FoodColors.broth(view.level(), preview);
            colour = blend(water, SOUP_ALPHA << 24 | soup, cooked);
            uneven = cooked;
        }
        float mottle = uneven;
        view.node("broth").ifPresent(broth -> {
            broth.setMottle(mottle);
            broth.setWakeLook(Mth.lerp(mottle, WATER_WAKE_LIFE, SOUP_WAKE_LIFE), Mth.lerp(mottle, WATER_WAKE_CONTRAST, SOUP_WAKE_CONTRAST));
        });
        int shownColor = colour;
        brothColor = colour & 0xFFFFFF;
        bubbleColor = 0xE0000000 | blend(colour, 0xFFFFFFFF, 0.6F) & 0xFFFFFF;
        view.node("broth").ifPresent(broth -> broth.setLiquidColor(shownColor));
    }

    private static int blend(int from, int to, float amount) {
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            result |= Math.round(Mth.lerp(amount, from >>> shift & 0xFF, to >>> shift & 0xFF)) << shift;
        }
        return result;
    }

    private void tooltip(StationView view, ItemStack meal) {
        view.node("preview").ifPresent(node -> {
            if (preview.isEmpty()) {
                return;
            }
            StationState state = view.state();
            if (!meal.isEmpty()) {
                node.setTooltip(Component.translatable("handson_delight.pot.ready", meal.getCount(), meal.getHoverName()));
            } else if (state.getControl(CookingPotStation.HEATED) < 0.5F || state.getControl(CookingPotStation.STIRRING) < 0.5F) {
                node.setTooltip(preview.getHoverName());
            } else {
                int seconds = Mth.ceil(state.data().getIntOr(CookingPotStation.COOK_LEFT, 0) / 20.0F);
                node.setTooltip(Component.translatable("handson_delight.pot.cooking", preview.getHoverName(), seconds));
            }
        });
    }

    /** What the ingredients make; recipes are on the client too, so the player sees it before anything cooks. */
    private ItemStack cooked(StationView view, ItemStack[] items) {
        boolean same = true;
        for (int i = 0; i < SLOTS && same; i++) {
            same = cookedFor[i] != null && ItemStack.matches(cookedFor[i], items[i]);
        }
        if (!same) {
            for (int i = 0; i < SLOTS; i++) {
                cookedFor[i] = items[i].copy();
            }
            cookedMade = Platform.INSTANCE.findRecipe(view.level(), Arrays.asList(items))
                    .map(found -> KitchenRecipes.result(found.value())).orElse(ItemStack.EMPTY);
        }
        return cookedMade;
    }

    private static String[] nodes(String prefix) {
        String[] names = new String[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            names[i] = prefix + i;
        }
        return names;
    }

    @Override
    public void frame(StationView view, float dt) {
        time += dt;
        StationState state = view.state();
        float progress = state.getControl(CookingPotStation.PROGRESS);
        shownProgress = Mth.lerp(Math.min(1.0F, dt * 6.0F), shownProgress, progress);
        shownStars = Mth.lerp(Math.min(1.0F, dt * 6.0F), shownStars, state.getControl(CookingPotStation.STARS));
        boolean heated = state.getControl(CookingPotStation.HEATED) > 0.5F;
        boolean stirred = state.getControl(CookingPotStation.STIRRING) > 0.5F;
        boolean meal = !state.getItem(CookingPotStation.MEAL_SLOT).isEmpty();
        boolean cooking = heated && stirred && !preview.isEmpty() && !meal;
        boolean soup = !preview.isEmpty() || meal;

        turnToBlock(view);
        level = Mth.lerp(Math.min(1.0F, dt * 3.0F), level, state.getTank(CookingPotStation.BROTH).amount() / 100.0F);
        moveSpoon(view, dt, stirred);
        arrivals(view, dt);
        drift(view, dt);
        melt(view, dt);
        book.frame(view, dt);
        offerBowl(view);
        boolean offering = view.node("mouth").map(NodeView::isHovered).orElse(false) || bowlOverSoup(view) || !offered.isEmpty();
        view.node("pot").ifPresent(pot -> pot.setOutlined(offering));
        view.node("broth").ifPresent(broth -> {
            broth.setAgitation(!heated ? 0.0F : cooking ? 0.12F : 0.03F);
            // A pot starts to boil along its walls.
            broth.setEdgeBoil(!heated ? 0.0F : cooking ? 0.9F : soup ? 0.55F : 0.3F);
        });

        // Hot water only breathes; what cooks simmers all over, and a soup left alone goes on bubbling slowly.
        bubbles += dt * (!heated ? 0.0F : cooking ? 7.0F : soup ? 2.2F : 0.8F);
        if (bubbles >= 1.0F) {
            view.spawnInLiquid(DiegesisParticles.BUBBLE, "broth", (int) bubbles, bubbleColor, RISE);
            bubbles -= (int) bubbles;
        }
        steam += dt * (!heated ? 0.0F : cooking ? 6.5F : soup ? 3.2F : 1.6F);
        if (steam >= 1.0F) {
            view.spawnOnLiquid(DiegesisParticles.STEAM, "broth", (int) steam, STEAM_COLOR, STEAM_RISE);
            steam -= (int) steam;
        }
        edgeBubbles += dt * (!heated ? 0.0F : cooking ? 16.0F : soup ? 8.0F : 4.0F);
        if (edgeBubbles >= 1.0F) {
            view.spawnAtLiquidEdge(DiegesisParticles.BUBBLE_POP, "broth", (int) edgeBubbles, bubbleColor, EDGE_BUBBLE_SIZE);
            edgeBubbles -= (int) edgeBubbles;
        }
        sounds(view, dt, heated, cooking, soup || hasFood());
        showGrade(view, dt);
        drawDial(view);
    }

    /**
     * What the pot sounds like: one layer for what it is doing, crossing over to the next, and the spoon on top, as fast as it goes round.
     * All of it runs for everybody who hears the pot.
     */
    private boolean hasFood() {
        for (ItemStack stack : shown) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** With nothing in it the pot keeps Farmer's Delight's own sound: {@code CookingPotBlockMixin} lets it through. */
    private void sounds(StationView view, float dt, boolean heated, boolean cooking, boolean soup) {
        view.loopSound("body", PotSounds.BODY, !heated || !soup ? 0.0F : cooking ? BODY_COOKING : BODY_RESTING, 1.0F);
        view.loopSound("stir", PotSounds.STIR, swirl * STIR_VOLUME, 0.9F + 0.15F * swirl);
        boilClock -= dt;
        if (heated && soup && boilClock <= 0.0F) {
            boilClock = cooking ? BOIL_EVERY_COOKING : BOIL_EVERY;
            view.playSound(FarmersDelightContent.BOIL_SOUP, "broth", 1.0F, 0.95F + 0.1F * view.level().getRandom().nextFloat());
        }
        sizzleClock -= dt;
        if (cooking && sizzleClock <= 0.0F) {
            sizzleClock = SIZZLE_EVERY;
            view.playSound(FarmersDelightContent.SKILLET_SIZZLE, "broth", SIZZLE_VOLUME, 0.9F);
        }
        blubClock -= dt;
        if (blubs > 0 && blubClock <= 0.0F) {
            blubs--;
            blubClock = BLUB_GAP;
            view.playSound(PotSounds.BLUB, "broth", BLUB_VOLUME, 0.9F + 0.04F * blubs);
        }
    }

    /**
     * The block keeps its place whichever side the scene faces. Scenes turn to the player who opens the pot, so what stands in the
     * block turns back, and what floats keeps its place in the world.
     */
    private void turnToBlock(StationView view) {
        BlockState state = view.level().getBlockState(view.pos());
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }
        Direction block = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        float facing = view.facing().toYRot();
        float turn = facing - block.toYRot();
        if (turn != shownTurn) {
            shownTurn = turn;
            view.node("pot").ifPresent(node -> node.rotateTo(0.0F, turn, 0.0F, 0.0F));
        }
        if (facing != shownFacing) {
            if (!Float.isNaN(shownFacing)) {
                turned -= (facing - shownFacing) * Mth.DEG_TO_RAD;
            }
            shownFacing = facing;
        }
    }

    /** The spoon's end follows the hand round the pot; for everybody else it follows what the server was told. */
    private void moveSpoon(StationView view, float dt, boolean stirred) {
        NodeView spoon = view.node("ladle").orElse(null);
        NodeView pot = view.node("pot").orElse(null);
        if (spoon == null || pot == null) {
            return;
        }
        float wantX = 0.0F;
        float wantZ = 0.0F;
        boolean hand = stroking && view.isFocused();
        if (hand) {
            Optional<Vector3f> cursor = view.cursorOn(BROTH_FLOOR + BROTH_DEPTH * level);
            if (cursor.isPresent()) {
                Vector3f inPot = potInverse.set(pot.sceneMatrix()).invert().transformPosition(cursor.get());
                float distance = (float) Math.hypot(inPot.x, inPot.z);
                float shorter = distance > SPOON_REACH ? SPOON_REACH / distance : 1.0F;
                wantX = inPot.x * shorter;
                wantZ = inPot.z * shorter;
            }
            handLeft = HAND_MEMORY;
        } else if (stirred && handLeft > 0.0F) {
            // Let go, it stands where the hand left it: what the server last heard lies a little behind.
            wantX = tipX;
            wantZ = tipZ;
        } else if (stirred) {
            wantX = view.state().getControl(CookingPotStation.SPOON_X) * SPOON_REACH;
            wantZ = view.state().getControl(CookingPotStation.SPOON_Z) * SPOON_REACH;
        }
        handLeft -= dt;
        // Nobody holds it any more: it sets off softly and takes its time, the rest of its moves answer a hand.
        boolean goingHome = !hand && !stirred;
        homing = goingHome ? Math.min(1.0F, homing + dt / SPOON_HOME_EASE) : 0.0F;
        grip = Mth.lerp(Math.min(1.0F, dt * (goingHome ? SPOON_HOME_GRIP * homing : 7.0F)), grip, goingHome ? 0.0F : 1.0F);
        float follow = 1.0F - (float) Math.exp(-dt * (hand ? 18.0F : goingHome ? SPOON_HOME_RATE * homing : 9.0F));
        float stepX = (wantX - tipX) * follow;
        float stepZ = (wantZ - tipZ) * follow;
        // Round which way the hand goes: the sign of the turn about the middle.
        float across = tipX * stepZ - tipZ * stepX;
        tipX += stepX;
        tipZ += stepZ;
        float moved = (float) Math.hypot(stepX, stepZ);
        boolean inSoup = grip > 0.5F;
        if (inSoup && dt > 0.0F) {
            // Water takes up a stir slowly: what floats speeds up and turns round smoothly, whatever the hand does from frame to frame.
            if (moved > 0.02F && Math.abs(across) > 1.0e-3F) {
                swirlSign = Mth.lerp(Math.min(1.0F, dt * 1.5F), swirlSign, Math.signum(across));
            }
            float wanted = Math.min(1.0F, moved / dt / 7.0F);
            if (wanted > swirl) {
                swirl = Mth.lerp(Math.min(1.0F, dt * 2.5F), swirl, wanted);
            }
            stirEffects(view, pot, moved, dt, hand);
        }
        swirl *= (float) Math.pow(0.5F, dt / SWIRL_HALF_LIFE);
        pose(spoon, pot);
    }

    /** What a moving spoon does: rings from its end, a low sound, and word to the server that the pot is being stirred. */
    private void stirEffects(StationView view, NodeView pot, float moved, float dt, boolean hand) {
        if (moved > 0.005F) {
            view.node("broth").ifPresent(broth -> broth.leaveWake(spoonX + 8.0F, spoonZ + 8.0F));
        }
        rippleClock -= dt;
        if (moved > 0.01F && rippleClock <= 0.0F) {
            rippleClock = RIPPLE_INTERVAL;
            Vector3f at = pot.sceneMatrix().transformPosition(new Vector3f(tipX, 0.0F, tipZ));
            view.node("broth").ifPresent(broth -> broth.ripple(at.x, at.z, RIPPLE_STRENGTH));
        }
        if (hand) {
            sendClock -= dt;
            sendTravel += moved;
            if (sendClock <= 0.0F && sendTravel >= SEND_TRAVEL) {
                sendClock = SEND_INTERVAL;
                sendTravel = 0.0F;
                CompoundTag where = new CompoundTag();
                where.putFloat(CookingPotStation.STIR_X, tipX / SPOON_REACH);
                where.putFloat(CookingPotStation.STIR_Z, tipZ / SPOON_REACH);
                where.putFloat(CookingPotStation.STIR_AT, time * 20.0F);
                view.sendAction(CookingPotStation.ACTION_STIR, where);
            }
        }
    }

    /** Leans the spoon from its rest towards the hand's upright, its end staying where the hand has it. */
    private void pose(NodeView spoon, NodeView pot) {
        // The lean grows with the distance from the middle, so it goes through the middle without a jump.
        float rest = (float) Math.tan(REST_LEAN * Mth.DEG_TO_RAD);
        float held = (float) Math.tan(STIR_LEAN * Mth.DEG_TO_RAD) / SPOON_REACH;
        float leanX = Mth.lerp(grip, rest, tipX * held);
        float leanZ = Mth.lerp(grip, 0.0F, tipZ * held);
        // The shortest way from how the model leans by itself: the handle tips, it never twists about itself.
        Quaternionf turn = lean.rotationTo(restAxis.set(rest, 1.0F, 0.0F).normalize(), leanAxis.set(leanX, 1.0F, leanZ).normalize());
        Vector3f angles = turn.getEulerAnglesYXZ(poseAngles).mul(Mth.RAD_TO_DEG);
        Vector3f end = turn.transform(spoonEnd.set(0.0F, SPOON_END, 0.0F));
        spoon.moveTo(tipX - end.x, SPOON_END - end.y, tipZ - end.z, 0.0F);
        spoon.rotateTo(angles.x, angles.y, angles.z, 0.0F);
        // Where the handle goes through the surface, for what floats there to go round it.
        float height = BROTH_FLOOR + BROTH_DEPTH * level - SPOON_END;
        Vector3f through = pot.sceneMatrix().transformPosition(spoonThrough.set(tipX + leanX * height, 0.0F, tipZ + leanZ * height));
        spoonX = through.x - 8.0F;
        spoonZ = through.z - 8.0F;
    }

    /** The ingredients lie nearly still on calm water and go round with the soup, each at its own place on the circle. */
    private void drift(StationView view, float dt) {
        float current = (CALM_DRIFT + swirl * SWIRL_SPEED) * swirlSign;
        turned += dt * current;
        view.node("broth").ifPresent(broth -> broth.setCurrent(current));
        int present = 0;
        for (int i = 0; i < SLOTS; i++) {
            present += floats(i) ? 1 : 0;
        }
        float surface = BROTH_FLOOR + BROTH_DEPTH * level - ITEM_REST;
        int rank = 0;
        for (int i = 0; i < SLOTS; i++) {
            // What floats shares the circle evenly, and makes room when something new drops in.
            if (floats(i)) {
                places[i] = Mth.lerp(Math.min(1.0F, dt * 2.5F), places[i], rank++ * Mth.TWO_PI / Math.max(present, 1));
                float angle = turned + places[i];
                place(i, Mth.cos(angle) * DRIFT_RADIUS, Mth.sin(angle) * DRIFT_RADIUS, dt);
            }
        }
        shoulder();
        itemWakes(view, dt);
        for (int i = 0; i < SLOTS; i++) {
            int index = i;
            if (!Float.isNaN(sinkAge[i])) {
                sinking(view, i, dt, surface);
                continue;
            }
            if (sunk[i]) {
                continue;
            }
            view.node(ITEM_NODES[i]).ifPresent(node -> {
                float angle = turned + places[index];
                float bob = Mth.sin(time * 0.9F + index * 1.3F) * 0.07F;
                float roll = tumble(view, index, dt);
                float hop = Mth.sin(roll * Mth.PI) * TUMBLE_HOP;
                float over = roll * roll * (3.0F - 2.0F * roll) * 360.0F;
                float x = itemX[index];
                float y = surface + bob + hop;
                float z = itemZ[index];
                float flight = arriveAge[index];
                if (flight >= 0.0F) {
                    // On its way in from the hand: an arc that comes down onto its place on the circle.
                    float t = Math.min(1.0F, flight / ARRIVE_TIME);
                    float eased = t * t * (3.0F - 2.0F * t);
                    x = Mth.lerp(eased, arriveX[index], x);
                    z = Mth.lerp(eased, arriveZ[index], z);
                    y = Mth.lerp(t, y + ARRIVE_FROM, y) + ARRIVE_ARC * Mth.sin(t * Mth.PI);
                    over += (1.0F - eased) * 180.0F;
                }
                node.moveTo(x, y, z, 0.0F);
                node.rotateTo(Mth.sin(time * 0.5F + index) * 3.0F + over, -angle * Mth.RAD_TO_DEG, Mth.cos(time * 0.4F + index) * 3.0F, 0.0F);
            });
        }
        // What floats in pieces goes round the spoon and the whole things.
        if (obstacles[present] == null) {
            obstacles[present] = new float[3 + present * 3];
        }
        float[] standing = obstacles[present];
        standing[0] = spoonX + 8.0F;
        standing[1] = spoonZ + 8.0F;
        standing[2] = SPOON_RADIUS;
        int at = 3;
        for (int i = 0; i < SLOTS; i++) {
            if (floats(i)) {
                standing[at++] = itemX[i] + 8.0F;
                standing[at++] = itemZ[i] + 8.0F;
                standing[at++] = ITEM_RADIUS;
            }
        }
        view.node("broth").ifPresent(broth -> broth.setFloatingObstacles(standing));
        view.node("served").ifPresent(node -> node.moveTo(0.0F, surface, 0.0F, 0.0F));
    }

    /** Whole items draw a trail through the soup, the faster the clearer; on calm water they leave none. */
    private void itemWakes(StationView view, float dt) {
        view.node("broth").ifPresent(broth -> {
            for (int i = 0; i < SLOTS; i++) {
                if (!floats(i) || !Float.isNaN(arriveAge[i])) {
                    itemFloating[i] = false;
                    continue;
                }
                if (itemFloating[i] && dt > 0.0F) {
                    float speed = (float) Math.hypot(itemX[i] - lastItemX[i], itemZ[i] - lastItemZ[i]) / dt;
                    float strength = ITEM_WAKE * Math.min(1.0F, (speed - ITEM_WAKE_FROM) / ITEM_WAKE_RANGE);
                    if (strength > 0.0F) {
                        broth.leaveWake(itemX[i] + 8.0F, itemZ[i] + 8.0F, ITEM_WAKE_WIDTH, strength);
                    }
                }
                lastItemX[i] = itemX[i];
                lastItemZ[i] = itemZ[i];
                itemFloating[i] = true;
            }
        });
    }

    /** Whether the thing in slot {@code i} floats whole in the soup now. */
    private boolean floats(int i) {
        return !shown[i].isEmpty() && !sunk[i] && Float.isNaN(sinkAge[i]);
    }

    /** Items in the pot do not lie on each other: they push apart, and neither the spoon nor a wall lets them through. */
    private void shoulder() {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < SLOTS; i++) {
                for (int j = i + 1; j < SLOTS; j++) {
                    if (!floats(i) || !floats(j)) {
                        continue;
                    }
                    float dx = itemX[j] - itemX[i];
                    float dz = itemZ[j] - itemZ[i];
                    float apart = (float) Math.hypot(dx, dz);
                    if (apart < ITEM_RADIUS * 2.0F) {
                        float nx = apart > 0.001F ? dx / apart : Mth.cos(i + j);
                        float nz = apart > 0.001F ? dz / apart : Mth.sin(i + j);
                        float move = (ITEM_RADIUS * 2.0F - apart) / 2.0F;
                        shift(i, -nx * move, -nz * move);
                        shift(j, nx * move, nz * move);
                    }
                }
            }
        }
    }

    private void shift(int index, float x, float z) {
        float nx = Mth.clamp(itemX[index] + x, -ITEM_ROOM, ITEM_ROOM);
        float nz = Mth.clamp(itemZ[index] + z, -ITEM_ROOM, ITEM_ROOM);
        pushX[index] += nx - itemX[index];
        pushZ[index] += nz - itemZ[index];
        itemX[index] = nx;
        itemZ[index] = nz;
    }

    /** How far an item is through a somersault, 0 to 1: a good stir turns one over now and then. */
    private float tumble(StationView view, int index, float dt) {
        if (tumbling[index] > 0.0F) {
            tumbling[index] += dt / TUMBLE_TIME;
            if (tumbling[index] >= 1.0F) {
                tumbling[index] = 0.0F;
            }
        } else if (swirl > TUMBLE_FROM && view.level().getRandom().nextFloat() < dt * TUMBLE_RATE * swirl) {
            tumbling[index] = 0.001F;
        }
        return tumbling[index];
    }

    /** Where an item floats: on its way round, unless the spoon pushed it aside; it never goes through the spoon or a wall. */
    private void place(int index, float x, float z, float dt) {
        float settle = (float) Math.pow(0.5F, dt / PUSH_HALF_LIFE);
        pushX[index] *= settle;
        pushZ[index] *= settle;
        float px = x + pushX[index];
        float pz = z + pushZ[index];
        for (int pass = 0; pass < 2; pass++) {
            float dx = px - spoonX;
            float dz = pz - spoonZ;
            float away = (float) Math.hypot(dx, dz);
            if (away < ITEM_CLEARANCE) {
                float out = ITEM_CLEARANCE / Math.max(away, 0.01F);
                px = spoonX + dx * out;
                pz = spoonZ + dz * out;
            }
            px = Mth.clamp(px, -ITEM_ROOM, ITEM_ROOM);
            pz = Mth.clamp(pz, -ITEM_ROOM, ITEM_ROOM);
        }
        pushX[index] = px - x;
        pushZ[index] = pz - z;
        itemX[index] = px;
        itemZ[index] = pz;
    }

    /** The lit part of the badge: the bar as far as the batch is, the stars as far as they are earned, filling from the bottom row. */
    private void drawDial(StationView view) {
        boolean holding = !Float.isNaN(gradeAge);
        boolean cold = view.state().getControl(CookingPotStation.HEATED) < 0.5F;
        int bar = holding ? BAR_LENGTH : cold ? 0 : Math.round(shownProgress * BAR_LENGTH);
        int lit = holding ? Mth.clamp(Mth.floor((gradeAge - GRADE_WAIT) / GRADE_STAGGER) + 1, 0, grade(view)) : 0;
        int open = openStars(view);
        int rows = Math.min(open * STAR.length, holding ? lit * STAR.length : Math.round(shownStars * STAR.length));
        // The added servings show once the last star is lit.
        int added = holding && lit == grade(view) && gradeAge > GRADE_WAIT + grade(view) * GRADE_STAGGER ? Math.min(bonus(view), 99) : 0;
        int key = ((added * 4 + open) * 100 + bar) * 100 + rows;
        if (key == drawnDial) {
            return;
        }
        int track = open * 2 + (cold ? 1 : 0);
        if (track != drawnTrack) {
            // Only the stars the cook has opened are there at all.
            drawnTrack = track;
            MeshShape shape = cold ? flame(open) : dial(open, BAR_LENGTH, open * STAR.length, 0);
            view.node("dial_track").ifPresent(node -> node.setShape(shape, BADGE_TEXTURE, true));
        }
        drawnDial = key;
        view.node("dial").ifPresent(dial -> dial.setShape(bar == 0 && rows == 0 ? null : dial(open, bar, rows, added), BADGE_TEXTURE, true));
    }

    /** The unlit stars and, instead of the bar, the flame a fire would light. */
    private static MeshShape flame(int open) {
        MeshShape.Builder shape = MeshShape.builder();
        for (int star = 0; star < open; star++) {
            glyph(shape, STAR, starLeft(open, star), starTop(open, star));
        }
        glyph(shape, FLAME, FLAME_LEFT, FLAME_TOP);
        return shape.build();
    }

    private static void glyph(MeshShape.Builder shape, String[] rows, float left, float top) {
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length(); column++) {
                if (rows[row].charAt(column) == '#') {
                    pixels(shape, left + column, top + row, 1, 1);
                }
            }
        }
    }

    /** "+N" right-aligned over the icon's lower right corner, where a stack shows its count. */
    private static void count(MeshShape.Builder shape, int added) {
        String digits = "+" + added;
        int x = COUNT_RIGHT - digits.length() * 4 + 1;
        for (char c : digits.toCharArray()) {
            String[] glyph = c == '+' ? PLUS : DIGITS[c - '0'];
            for (int row = 0; row < glyph.length; row++) {
                for (int column = 0; column < 3; column++) {
                    if (glyph[row].charAt(column) == '#') {
                        pixels(shape, x + column, COUNT_TOP + row, 1, 1);
                    }
                }
            }
            x += 4;
        }
    }

    /** {@code bar} pixels of the bar and {@code rows} rows of the {@code open} stars, counted from the bottom of the first star on. */
    private static MeshShape dial(int open, int bar, int rows, int added) {
        MeshShape.Builder shape = MeshShape.builder();
        if (added > 0) {
            count(shape, added);
        }
        if (bar > 0) {
            pixels(shape, BAR_LEFT, BAR_TOP, bar, 1);
        }
        for (int star = 0; star < open; star++) {
            int lit = Mth.clamp(rows - star * STAR.length, 0, STAR.length);
            for (int row = STAR.length - lit; row < STAR.length; row++) {
                for (int column = 0; column < STAR_SIZE; column++) {
                    if (STAR[row].charAt(column) == '#') {
                        pixels(shape, starLeft(open, star) + column, starTop(open, star) + row, 1, 1);
                    }
                }
            }
        }
        return shape.build();
    }

    /** A flat rectangle of the badge, in pixels of the icon: x to the right and z down as the player sees it, the node's own axes the other way. */
    private static void pixels(MeshShape.Builder shape, float x, float z, float width, float height) {
        float left = -(x + width) * PIXEL;
        float right = -x * PIXEL;
        float top = -z * PIXEL;
        float bottom = -(z + height) * PIXEL;
        shape.corner(left, 0.0F, top, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F)
                .corner(right, 0.0F, top, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F)
                .corner(right, 0.0F, bottom, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F)
                .corner(left, 0.0F, bottom, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
    }
}
