package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenNbt;
import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import vectorwing.farmersdelight.common.block.SkilletBlock;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.NodeView;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.scene.Visual;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.station.SkilletCooking;
import com.pockyl.handson_delight.station.SkilletStation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the skillet shows, to the cook and to everybody around: the food in the pan, built from its icon, its top turning from raw to
 * cooked as it is turned over; the pan itself while somebody works at it, tossed by its handle, the food flying up and coming down
 * the other side up.
 */
public final class SkilletBehavior implements StationBehavior {
    /** At most this many pieces lie on top, with as many under them for a fuller pan: a batch of sixteen shows eight. */
    private static final int PIECES = 4;
    /** Pieces lying under the top ones lift these by this much, pixels. */
    private static final float PILE = 0.6F;
    /** The colour the raw top takes on as it fries, at most, multiplied in: a seared, not a burnt, look. */
    private static final int SEARED = 0xFFB8875C;
    private static final float SEAR = 0.7F;
    /**
     * The toss (owner of the idea: the plan's "turning"): the far lip of the pan comes up and the pan jerks towards the cook, the food
     * leaves it, turns over in the air and comes down. Degrees, pixels, seconds.
     */
    private static final float TILT = 11.0F;
    private static final float JERK_UP = 1.2F;
    private static final float JERK_BACK = 0.9F;
    private static final float JERK_TIME = 0.07F;
    private static final float SETTLE_TIME = 0.2F;
    private static final float FLIGHT = 0.32F;
    private static final float FLIGHT_STRONG = 0.12F;
    private static final float HEIGHT = 4.5F;
    private static final float HEIGHT_STRONG = 6.0F;
    private static final float STAGGER = 0.03F;
    /** A press on the handle shorter than this is a click, and tosses; a quick pull upwards tosses at once. Seconds, pixels/s. */
    private static final float CLICK = 0.35F;
    private static final float FLICK = 30.0F;
    private static final float HARDEST = 110.0F;
    /** The pan's copy outlives the station by this long: the block's own model needs a moment to be built again. Seconds. */
    private static final float BLOCK_BACK = 0.3F;
    /** Putting in and taking out, as at the board. */
    private static final float LAND_TIME = 0.2F;
    private static final float LAND_FROM = 4.0F;
    /** Seconds between pieces that come in together. */
    private static final float PIECE_STAGGER = 0.14F;
    private static final float PLAYER_LAND_TIME = 0.32F;
    private static final float AWAY_TIME = 0.3F;
    private static final float AWAY_Z = 12.0F;
    /**
     * The heat, per second: oil seething in small bubbles round the food, fat spitting off it, wisps of steam while a side fries, smoke
     * curling up from under food whose lower side is done, and a shimmer over a hot pan, more of it while the pan is empty.
     */
    private static final float SEETHE_RATE = 14.0F;
    private static final float SPIT_RATE = 5.0F;
    private static final float VAPOUR_RATE = 1.6F;
    private static final float SMOKE_RATE = 6.0F;
    private static final float SHIMMER_RATE = 1.2F;
    private static final float SHIMMER_EMPTY_RATE = 2.4F;
    /** The pan's floor, scene pixels, and how far round a piece its oil seethes, times its size. */
    private static final float FLOOR = 1.05F;
    private static final float EDGE = 0.5F;
    private static final float WAITING_SIZZLE = 1.3F;
    /**
     * How the pan tells the cook where the food is (owner: "it is unclear when to turn it and when it is done; make it answer with
     * sound"): the frying sizzle grows louder and higher as the side on the pan browns, seconds apart; when that side is done the fat
     * pops and the food gives a little jump, and soft pops go on while it waits; a done batch settles with a deep sizzle and a
     * crackle. Pieces already in the pan move over to their new places this fast when more come.
     */
    private static final float FRY_VOICE = 1.0F;
    private static final float WAITING_POP = 1.4F;
    private static final float POP_HOP = 0.6F;
    private static final float POP_TIME = 0.18F;
    private static final float GLIDE = 0.16F;

    private final SkilletBook pages = new SkilletBook();
    private final RecipeBook book = new RecipeBook(pages);
    private final Map<Item, Boolean> usable = new HashMap<>();
    private final RandomSource random = RandomSource.create();
    private final PanOil oil = new PanOil();

    private ItemStack shown = ItemStack.EMPTY;
    private int shownPieces;
    private int shownUnders;
    /** The top's look as drawn: raw (doneness up to 1) or cooked (1). */
    private float shownUp = -1.0F;
    /** After a toss and until the server says so, the side that was down is the top. NaN: the server's word. */
    private float tossedUp = Float.NaN;
    /** The top keeps its look until the food has turned edge on in the air, whatever the server says meanwhile. */
    private float lookHeldUntil;
    private boolean quiet = true;
    private boolean inUse;
    private float copyLeft;
    private float clock;
    private float tossedAt = -10.0F;
    private float pressedAt = Float.NaN;
    private float lastY;
    private float lastMove;
    private boolean tossedThisPress;
    private float spit;
    private float seethe;
    private float vapour;
    private float shimmer;
    private float smoke;
    private float sizzle;
    private float fryVoice;
    private float waitingPop;
    private boolean wasWaiting;
    /**
     * Pieces coming into the pan (piece_i at i, under_i at PIECES + i): seconds in the air (NaN: lying), where from, how long, how
     * high. Where to is asked every frame, so a piece still landing when more come lands on its new place.
     */
    private final float[] landAge = new float[PIECES * 2];
    private final float[] landX = new float[PIECES * 2];
    private final float[] landY = new float[PIECES * 2];
    private final float[] landZ = new float[PIECES * 2];
    private final float[] landTime = new float[PIECES * 2];
    private final float[] landHop = new float[PIECES * 2];
    /** Pieces that wait for their turn to come in: hidden until then, whatever the state says meanwhile. */
    private final boolean[] waitingIn = new boolean[PIECES * 2];
    private final List<Step> steps = new ArrayList<>();

    private record Step(float at, Runnable run) {
    }

    public SkilletBehavior(StationView view) {
        java.util.Arrays.fill(landAge, Float.NaN);
    }

    @Override
    public void onSceneReady(StationView view) {
        shown = ItemStack.EMPTY;
        shownPieces = 0;
        shownUnders = 0;
        shownUp = -1.0F;
        steps.clear();
        quiet = true;
        inUse = false;
        copyLeft = 0.0F;
        showCopy(view, false);
        view.node("book_open").ifPresent(node -> node.setPainter(book));
        view.node("oil").ifPresent(node -> node.setPainter(oil));
        refresh(view);
    }

    @Override
    public void onOpen(StationView view) {
        book.opened(view);
        usable.clear();
        view.setInventoryFilter(stack -> usable.computeIfAbsent(stack.getItem(),
                item -> SkilletCooking.recipe(view.level(), stack).isPresent()));
        // A right click with food puts it in the pan, as Farmer's Delight's skillet takes it: never slower than without the station.
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && view.openedByUse() && player.inventoryMenu.getCarried().isEmpty()) {
            ItemStack inPan = view.state().getItem(SkilletStation.PAN_SLOT);
            ItemStack held = player.getMainHandItem();
            if (inPan.isEmpty() || ItemStack.isSameItemSameComponents(inPan, held)) {
                onInventoryShiftClick(view, player.getInventory().getSelectedSlot(), held);
            }
        }
    }

    @Override
    public void onClose(StationView view) {
        book.shut(view);
        pressedAt = Float.NaN;
    }

    @Override
    public void onStateChanged(StationView view, StationState.Changes changes) {
        tossedUp = Float.NaN;
        refresh(view);
    }

    @Override
    public void tick(StationView view) {
        refresh(view);
        for (int i = 0; i < PIECES; i++) {
            view.node("lift_" + i).filter(node -> node.isVisible() && !node.isMoving()).ifPresent(node -> node.setVisible(false));
        }
    }

    /** The food in the pan as the state says: how many pieces, which look on top. */
    private void refresh(StationView view) {
        StationState state = view.state();
        ItemStack inPan = state.getItem(SkilletStation.PAN_SLOT);
        int batch = inPan.isEmpty() ? 0 : Math.max(1, Math.min(inPan.getCount(), SkilletCooking.BATCH));
        int pieces = Math.min(batch, PIECES);
        int unders = Math.min(Math.max(0, batch - PIECES), PIECES);
        float up = clock < lookHeldUntil ? shownUp : Float.isNaN(tossedUp) ? state.data().getFloatOr(SkilletStation.UP, 0.0F) : tossedUp;
        // Food that became what the fire makes of it is the same food: it stays where it lay, nothing lands (owner, 2026-11-09:
        // a potato done in the pan landed again as if put in).
        boolean sameFood = ItemStack.isSameItemSameComponents(inPan, shown)
                || !shown.isEmpty() && !inPan.isEmpty() && ItemStack.isSameItemSameComponents(cooked(view, shown), inPan);
        if (sameFood && pieces == shownPieces && unders == shownUnders && look(up) == look(shownUp)) {
            return;
        }
        int before = sameFood ? shownPieces : 0;
        shown = inPan.copy();
        shownPieces = pieces;
        shownUnders = unders;
        shownUp = up;
        float[][] places = new float[pieces][];
        for (int i = 0; i < pieces; i++) {
            places[i] = place(i, pieces);
        }
        if (pieces > 0) {
            oil.lay(places, size(pieces));
        }
        for (int i = 0; i < PIECES; i++) {
            int index = i;
            view.node("piece_" + i).ifPresent(node -> drawPiece(view, node, index, index < pieces, false));
            view.node("under_" + i).ifPresent(node -> drawPiece(view, node, index, index < unders, true));
        }
        if (!quiet) {
            // What came in lands; several at once (a stack, the book) come from the player one after the other (owner: "cozy",
            // "they must fly from the player"). What was there stays.
            boolean hot = state.data().getBooleanOr(SkilletStation.HEATED, false);
            boolean together = pieces - before > 1;
            for (int i = before; i < pieces; i++) {
                int index = i;
                float delay = (i - before) * PIECE_STAGGER;
                Vector3f from = together ? FromPlayer.offset(view, LAND_FROM, i - before, pieces - before) : null;
                Runnable lands = () -> {
                    waitingIn[index] = false;
                    waitingIn[PIECES + index] = false;
                    if (index >= shownPieces) {
                        return;
                    }
                    view.node("piece_" + index).ifPresent(node -> {
                        node.setVisible(true);
                        land(node, index, false, from);
                    });
                    view.node("under_" + index).filter(node -> index < shownUnders).ifPresent(node -> {
                        node.setVisible(true);
                        land(node, index, true, from);
                    });
                    if (hot) {
                        after(from == null ? LAND_TIME : PLAYER_LAND_TIME, () -> hitsTheFat(view));
                    }
                };
                if (delay > 0.0F) {
                    waitingIn[i] = true;
                    waitingIn[PIECES + i] = true;
                    view.node("piece_" + i).ifPresent(node -> node.setVisible(false));
                    view.node("under_" + i).ifPresent(node -> node.setVisible(false));
                    after(delay, lands);
                } else {
                    lands.run();
                }
            }
        }
        quiet = false;
    }

    /** The look of a top {@code up} done, in steps: drawn again only when it changes by one. */
    private static int look(float up) {
        return up < 0.0F ? -1 : up >= 1.0F ? 100 : Mth.floor(up * 12.0F);
    }

    private void drawPiece(StationView view, NodeView node, int index, boolean visible, boolean under) {
        node.setVisible(visible && !waitingIn[under ? PIECES + index : index]);
        if (!visible) {
            return;
        }
        ItemStack look = shownUp >= 1.0F ? cooked(view, shown) : KitchenLooks.opened(shown);
        float size = size(shownPieces);
        Optional<ItemVoxels.Icon> icon = ItemVoxels.icon(look);
        if (icon.isPresent()) {
            node.setVisual(null);
            node.setShape(ItemVoxels.build(icon.get(), size, List.of(), 0.0F), icon.get().sprite());
        } else {
            node.setShape(null, null);
            node.setVisual(Visual.ofItem(look.getItem()).withAnchor(new Vector3f(8.0F, 8.0F, 8.0F)));
        }
        node.setTint(KitchenLooks.fryingTint(shown, cooked(view, shown), shownUp, SEARED, SEAR));
        // What lies there already moves over to its place in the new layout; what is landing or in the air of a toss is on its way.
        if (!landing(index, under) && !(tossing() && node.isMoving())) {
            float[] at = place(index, shownPieces);
            node.moveTo(at[0], height(under, look), at[1], visible && quiet ? 0.0F : GLIDE);
            node.rotateTo(0.0F, yaw(index, under), 0.0F, 0.0F);
        }
    }

    private boolean tossing() {
        return clock - tossedAt < JERK_TIME + FLIGHT + FLIGHT_STRONG + STAGGER * PIECES * 2;
    }

    /** How high a piece lies: on the pan, on another piece, or as a block-like item standing on its middle. */
    private float height(boolean under, ItemStack look) {
        float lift = !under && shownUnders > 0 ? PILE : 0.0F;
        return lift + (ItemVoxels.icon(look).isPresent() ? 0.0F : 4.0F);
    }

    /** What the campfire makes of {@code raw}: the look of a done top. */
    private static ItemStack cooked(StationView view, ItemStack raw) {
        return SkilletCooking.recipe(view.level(), raw).map(recipe -> KitchenRecipes.result(recipe))
                .filter(result -> !result.isEmpty()).orElse(raw);
    }

    private static float size(int pieces) {
        return pieces <= 1 ? 9.0F : pieces == 2 ? 7.5F : 6.5F;
    }

    /** Where piece {@code index} of {@code count} lies, pixels from the middle of the pan: spread as a hand lays them. */
    private static float[] place(int index, int count) {
        return switch (count) {
            case 1 -> new float[] {0.0F, 0.0F};
            case 2 -> index == 0 ? new float[] {2.6F, 0.4F} : new float[] {-2.6F, -0.4F};
            case 3 -> new float[][] {{2.8F, 2.2F}, {-2.8F, 2.0F}, {0.2F, -2.8F}}[index];
            default -> new float[][] {{2.9F, 2.9F}, {-2.9F, 2.7F}, {2.7F, -2.9F}, {-2.8F, -2.8F}}[index];
        };
    }

    private static float yaw(int index, boolean under) {
        return ((index * 7919) % 41 - 20) * 1.2F + (under ? 35.0F : 0.0F);
    }

    private static int mix(int from, int to, float amount) {
        int color = 0xFF000000;
        for (int shift = 0; shift < 24; shift += 8) {
            int a = from >> shift & 0xFF;
            int b = to >> shift & 0xFF;
            color |= Math.round(a + (b - a) * amount) << shift;
        }
        return color;
    }

    /** Comes down into the pan from a little above, or with {@code from} over from the player, with a hop. */
    private void land(NodeView node, int index, boolean under, Vector3f from) {
        float[] at = place(index, shownPieces);
        float y = height(under, shown);
        int k = under ? PIECES + index : index;
        landAge[k] = 0.0F;
        landX[k] = at[0] + (from == null ? 0.0F : from.x);
        landY[k] = y + LAND_FROM;
        landZ[k] = at[1] + (from == null ? 0.0F : from.z);
        landTime[k] = from == null ? LAND_TIME : PLAYER_LAND_TIME;
        landHop[k] = from == null ? 0.8F : 2.0F;
        node.moveTo(landX[k], landY[k], landZ[k], 0.0F);
    }

    /** Pieces on their way in come down onto where they belong now, however many lie there by then. */
    private void landings(StationView view, float dt) {
        for (int k = 0; k < landAge.length; k++) {
            if (Float.isNaN(landAge[k])) {
                continue;
            }
            int index = k % PIECES;
            boolean under = k >= PIECES;
            landAge[k] += dt;
            float t = Math.min(1.0F, landAge[k] / landTime[k]);
            float[] at = place(index, Math.max(1, shownPieces));
            float y = height(under, shown);
            float eased = t * (2.0F - t);
            float x = Mth.lerp(eased, landX[k], at[0]);
            float z = Mth.lerp(eased, landZ[k], at[1]);
            float height = Mth.lerp(t, landY[k], y) + landHop[k] * Mth.sin(t * Mth.PI);
            int which = k;
            view.node((under ? "under_" : "piece_") + index).ifPresent(node -> node.moveTo(x, height, z, 0.0F));
            if (t >= 1.0F) {
                landAge[which] = Float.NaN;
            }
        }
    }

    private boolean landing(int index, boolean under) {
        return !Float.isNaN(landAge[under ? PIECES + index : index]);
    }

    @Override
    public void frame(StationView view, float dt) {
        clock += dt;
        if (!steps.isEmpty()) {
            List<Step> due = steps.stream().filter(step -> step.at() <= clock).toList();
            steps.removeAll(due);
            due.forEach(step -> step.run().run());
        }
        boolean nowInUse = view.isInUse();
        if (nowInUse) {
            copyLeft = BLOCK_BACK;
        } else {
            copyLeft = Math.max(0.0F, copyLeft - dt);
        }
        boolean copy = nowInUse || copyLeft > 0.0F;
        if (copy != inUse) {
            inUse = copy;
            showCopy(view, copy);
        }
        landings(view, dt);
        frying(view, dt);
        book.frame(view, dt);
    }

    /** The scene's own skillet, shown exactly while the block's is hidden; the grate and legs only on a campfire. */
    private void showCopy(StationView view, boolean shown) {
        BlockState state = view.level().getBlockState(view.pos());
        boolean stand = state.hasProperty(SkilletBlock.SUPPORT) && state.getValue(SkilletBlock.SUPPORT);
        view.node("pan_body").ifPresent(node -> node.setVisible(shown));
        view.node("handle").ifPresent(node -> node.setVisible(shown));
        view.node("stand").ifPresent(node -> node.setVisible(shown && stand));
    }

    /**
     * The pan at work. Hot: the air over it shimmers. Frying: fat runs out under the food and seethes round it in small bubbles, spits
     * off it in drops and steams; once the side on the pan is done, smoke curls up from under it and the sizzle sharpens: it wants
     * turning.
     */
    private void frying(StationView view, float dt) {
        CompoundTag data = view.state().data();
        boolean hot = data.getBooleanOr(SkilletStation.HEATED, false);
        boolean food = !shown.isEmpty();
        boolean inAir = clock - tossedAt < JERK_TIME + FLIGHT + FLIGHT_STRONG;
        oil.frame(dt, hot && food);
        if (!hot) {
            sizzle = 0.0F;
            calm(view);
            return;
        }
        shimmer += dt * (food ? SHIMMER_RATE : SHIMMER_EMPTY_RATE);
        while (shimmer >= 1.0F) {
            shimmer -= 1.0F;
            view.spawnParticles(HandsOnDelightClient.HEAT, new Vector3f(8.0F + randomly(4.5F), FLOOR + 2.5F + random.nextFloat() * 2.0F,
                    8.0F + randomly(4.5F)), new Vector3f(0.0F, 3.0F, 0.0F), 0.3F, 1, 0);
        }
        if (!food) {
            sizzle = 0.0F;
            calm(view);
            return;
        }
        // Fuller pans seethe and spit more, up to a full layer of four pieces. Done food only simmers in its fat while it waits.
        boolean done = SkilletCooking.isDone(view.level(), shown);
        float fullness = (0.5F + 0.5F * Math.min(1.0F, shownPieces / 4.0F)) * (done ? 0.3F : 1.0F);
        if (!inAir) {
            seethe += dt * SEETHE_RATE * fullness;
            while (seethe >= 1.0F) {
                seethe -= 1.0F;
                view.spawnParticles(HandsOnDelightClient.OIL_POP, roundFood(), new Vector3f(0.0F, 0.6F, 0.0F), 0.1F, 1, 0);
            }
        }
        if (done) {
            sizzle = 0.0F;
            calm(view);
            return;
        }
        spit += dt * SPIT_RATE * fullness;
        while (spit >= 1.0F) {
            spit -= 1.0F;
            float angle = random.nextFloat() * Mth.TWO_PI;
            float out = 2.5F + random.nextFloat() * 3.5F;
            view.spawnParticles(HandsOnDelightClient.FAT, roundFood(), new Vector3f(Mth.cos(angle) * out, 7.0F + random.nextFloat() * 6.0F,
                    Mth.sin(angle) * out), 0.1F, 1, 0);
        }
        boolean waits = data.getBooleanOr(SkilletStation.BY_HAND, false) && data.getFloatOr(SkilletStation.DOWN, 0.0F) >= 1.0F
                && data.getFloatOr(SkilletStation.UP, 0.0F) < 1.0F && Float.isNaN(tossedUp);
        if (waits && !wasWaiting && !inAir) {
            wantsTurning(view);
        }
        wasWaiting = waits;
        outlineHandle(view, waits && !inAir);
        if (!waits) {
            sizzle = 0.0F;
            // The sizzle grows as the side on the pan browns: the ear follows the frying without looking.
            float down = Mth.clamp(data.getFloatOr(SkilletStation.DOWN, 0.0F), 0.0F, 1.0F);
            fryVoice -= dt;
            if (fryVoice <= 0.0F && !inAir) {
                fryVoice = FRY_VOICE * (1.0F - 0.3F * down);
                view.playSound(FarmersDelightContent.SKILLET_SIZZLE, "pan", 0.12F + 0.28F * down, 0.85F + 0.3F * down + random.nextFloat() * 0.05F);
            }
            vapour += dt * VAPOUR_RATE * fullness;
            while (vapour >= 1.0F) {
                vapour -= 1.0F;
                view.spawnParticles(DiegesisParticles.STEAM, onFood(), new Vector3f(randomly(0.4F), 2.2F, randomly(0.4F)), 0.4F, 1,
                        0x40FFFFFF);
            }
            return;
        }
        smoke += dt * SMOKE_RATE * fullness;
        while (smoke >= 1.0F) {
            smoke -= 1.0F;
            view.spawnParticles(HandsOnDelightClient.PAN_SMOKE, roundFood(), new Vector3f(randomly(0.5F), 4.0F, randomly(0.5F)), 0.3F, 1, 0);
        }
        sizzle -= dt;
        if (sizzle <= 0.0F) {
            sizzle = WAITING_SIZZLE;
            view.playSound(FarmersDelightContent.SKILLET_SIZZLE, "pan", 0.3F, 1.15F + random.nextFloat() * 0.1F);
        }
        waitingPop -= dt;
        if (waitingPop <= 0.0F) {
            waitingPop = WAITING_POP * (0.8F + 0.4F * random.nextFloat());
            view.playSound(SoundEvents.LAVA_POP, "pan", 0.18F, 1.4F + random.nextFloat() * 0.3F);
        }
    }

    /**
     * The side on the pan is done: the fat pops twice and the food gives a little jump, once. Soft pops go on while it waits
     * ({@link #frying}); the handle stays outlined: a toss turns it.
     */
    private void wantsTurning(StationView view) {
        waitingPop = WAITING_POP;
        view.playSound(SoundEvents.LAVA_POP, "pan", 0.4F, 1.3F);
        after(0.09F, () -> view.playSound(SoundEvents.LAVA_POP, "pan", 0.35F, 1.55F));
        for (int i = 0; i < 8; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float out = 3.0F + random.nextFloat() * 3.0F;
            view.spawnParticles(HandsOnDelightClient.FAT, roundFood(), new Vector3f(Mth.cos(angle) * out, 8.0F + random.nextFloat() * 5.0F,
                    Mth.sin(angle) * out), 0.1F, 1, 0);
        }
        for (int i = 0; i < shownPieces; i++) {
            float[] at = place(i, shownPieces);
            for (boolean under : new boolean[] {false, true}) {
                int index = i;
                if (landing(index, under) || under && index >= shownUnders) {
                    continue;
                }
                float y = height(under, shown);
                view.node((under ? "under_" : "piece_") + i).filter(node -> !node.isMoving())
                        .ifPresent(node -> after(index * 0.04F, () -> node.hopTo(at[0], y, at[1], POP_HOP, POP_TIME)));
            }
        }
    }

    /** Nothing waits to be turned: no outline but the cursor's, and the next wait is new. */
    private void calm(StationView view) {
        wasWaiting = false;
        outlineHandle(view, false);
    }

    /** The handle is outlined under the cursor, and while food waits to be turned: that is what turns it. */
    private void outlineHandle(StationView view, boolean waits) {
        view.node("handle").ifPresent(handle -> handle.setOutlined(handle.isHovered() || waits && view.isFocused()));
    }

    /** Food landing in a hot pan: the fat seethes up round it, spits and steams. */
    private void hitsTheFat(StationView view) {
        if (shown.isEmpty()) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            view.spawnParticles(HandsOnDelightClient.OIL_POP, roundFood(), new Vector3f(0.0F, 0.6F, 0.0F), 0.1F, 1, 0);
        }
        for (int i = 0; i < 6; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float out = 3.0F + random.nextFloat() * 3.0F;
            view.spawnParticles(HandsOnDelightClient.FAT, roundFood(), new Vector3f(Mth.cos(angle) * out, 8.0F + random.nextFloat() * 5.0F,
                    Mth.sin(angle) * out), 0.1F, 1, 0);
        }
        view.spawnParticles(DiegesisParticles.STEAM, onFood(), new Vector3f(0.0F, 3.0F, 0.0F), 1.5F, 3, 0x50FFFFFF);
    }

    /** A point on the pan's floor just round the edge of a piece of food, scene pixels: where the oil seethes and the fat spits from. */
    private Vector3f roundFood() {
        int count = Math.max(1, shownPieces);
        float[] at = place(random.nextInt(count), count);
        float angle = random.nextFloat() * Mth.TWO_PI;
        float reach = size(shownPieces) * EDGE * (0.85F + 0.3F * random.nextFloat());
        return new Vector3f(8.0F + at[0] + Mth.cos(angle) * reach, FLOOR + 0.15F, 8.0F + at[1] + Mth.sin(angle) * reach);
    }

    /** A point on the food in the pan, scene pixels. */
    private Vector3f onFood() {
        float[] at = place(random.nextInt(Math.max(1, shownPieces)), Math.max(1, shownPieces));
        float spread = size(shownPieces) * 0.35F;
        return new Vector3f(8.0F + at[0] + randomly(spread), 1.6F, 8.0F + at[1] + randomly(spread));
    }

    private float randomly(float most) {
        return (random.nextFloat() * 2.0F - 1.0F) * most;
    }

    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (event instanceof SceneEvent.Stroke stroke && "handle".equals(stroke.node())) {
            stroked(view, stroke);
        } else if (event instanceof SceneEvent.Click click && "book_open".equals(click.node())) {
            book.click(view);
        } else if (event instanceof SceneEvent.Click click && "food".equals(click.node()) && !shown.isEmpty()) {
            // The food itself is what a player clicks when it smokes for turning: it turns, as by the handle. The other button takes
            // it back, raw or half done: a way to start over. Done food is taken by either button.
            if (click.button() == 1 || SkilletCooking.isDone(view.level(), shown)) {
                view.sendAction(SkilletStation.ACTION_TAKE, new CompoundTag());
            } else {
                toss(view, 0.4F);
            }
        }
    }

    /**
     * The handle held: the pan lifts a little in the hand. A quick pull upwards tosses it at once, as hard as the pull; a click on the
     * handle tosses too, so nobody has to find the gesture.
     */
    private void stroked(StationView view, SceneEvent.Stroke stroke) {
        float y = stroke.point().y();
        switch (stroke.phase()) {
            case START -> {
                pressedAt = clock;
                lastY = y;
                lastMove = clock;
                tossedThisPress = false;
                view.node("pan").ifPresent(pan -> pan.moveTo(0.0F, 0.5F, 0.3F, 0.08F));
            }
            case MOVE -> {
                float dt = clock - lastMove;
                if (dt > 0.0F && !tossedThisPress && !Float.isNaN(pressedAt)) {
                    float speed = (y - lastY) / dt;
                    if (speed > FLICK) {
                        tossedThisPress = toss(view, Mth.clamp((speed - FLICK) / (HARDEST - FLICK), 0.0F, 1.0F));
                    }
                }
                if (dt > 0.0F) {
                    lastY = y;
                    lastMove = clock;
                }
            }
            case END -> {
                if (!tossedThisPress && !Float.isNaN(pressedAt) && clock - pressedAt < CLICK) {
                    toss(view, 0.4F);
                } else if (!tossedThisPress) {
                    view.node("pan").ifPresent(pan -> pan.moveTo(0.0F, 0.0F, 0.0F, 0.12F));
                }
                pressedAt = Float.NaN;
            }
            default -> {
            }
        }
    }

    /** The local cook tosses: shown at once, the server told; false when the food is still in the air or there is none. */
    private boolean toss(StationView view, float strength) {
        if (shown.isEmpty() || clock - tossedAt < 0.42F) {
            return false;
        }
        CompoundTag how = new CompoundTag();
        how.putFloat(SkilletStation.STRENGTH, strength);
        view.sendAction(SkilletStation.ACTION_TOSS, how);
        tossed(view, strength);
        return true;
    }

    /** The toss as everybody sees it: the pan jerks, the food flies up, turns over at the top and comes down the other side up. */
    private void tossed(StationView view, float strength) {
        tossedAt = clock;
        float newUp = view.state().data().getFloatOr(SkilletStation.DOWN, 0.0F);
        view.node("pan").ifPresent(pan -> {
            pan.rotateTo(TILT * (0.7F + 0.3F * strength), 0.0F, 0.0F, JERK_TIME);
            pan.moveTo(0.0F, JERK_UP, JERK_BACK, JERK_TIME);
            after(JERK_TIME, () -> {
                pan.rotateTo(0.0F, 0.0F, 0.0F, SETTLE_TIME);
                pan.moveTo(0.0F, 0.0F, 0.0F, SETTLE_TIME);
            });
        });
        float flight = FLIGHT + FLIGHT_STRONG * strength;
        float height = HEIGHT + HEIGHT_STRONG * strength;
        tossedUp = newUp;
        lookHeldUntil = clock + JERK_TIME * 0.6F + flight / 2.0F;
        for (int i = 0; i < PIECES; i++) {
            for (boolean under : new boolean[] {false, true}) {
                int index = i;
                Optional<NodeView> node = view.node((under ? "under_" : "piece_") + i).filter(NodeView::isVisible);
                if (node.isEmpty()) {
                    continue;
                }
                float delay = JERK_TIME * 0.6F + STAGGER * (i + (under ? 2 : 0));
                float rise = height * (0.85F + 0.3F * random.nextFloat()) * (under ? 0.8F : 1.0F);
                after(delay, () -> flip(view, node.get(), index, under, flight, rise, newUp));
            }
        }
        view.playSound(SoundEvents.LANTERN_STEP, "pan", 0.35F, 0.8F + 0.2F * strength);
        after(JERK_TIME * 0.6F + flight, () -> {
            view.playSound(FarmersDelightContent.SKILLET_ADD_FOOD, "pan", 0.55F, 0.95F + random.nextFloat() * 0.15F);
            for (int i = 0; i < 10; i++) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                float out = 4.0F + random.nextFloat() * 4.0F;
                view.spawnParticles(HandsOnDelightClient.FAT, roundFood(), new Vector3f(Mth.cos(angle) * out, 8.0F + random.nextFloat() * 6.0F,
                        Mth.sin(angle) * out), 0.1F, 1, 0);
            }
            for (int i = 0; i < 8; i++) {
                view.spawnParticles(HandsOnDelightClient.OIL_POP, roundFood(), new Vector3f(0.0F, 0.6F, 0.0F), 0.1F, 1, 0);
            }
        });
    }

    /** One piece over: up and half round, then, edge on, the other side's look, the rest of the way round and down. */
    private void flip(StationView view, NodeView node, int index, boolean under, float flight, float rise, float newUp) {
        float[] at = place(index, shownPieces);
        float y = height(under, shown);
        float yaw = yaw(index, under);
        float spin = randomly(25.0F);
        node.hopTo(at[0], y, at[1], rise, flight);
        node.rotateTo(90.0F, yaw + spin * 0.5F, 0.0F, flight / 2.0F);
        after(flight / 2.0F, () -> {
            if (!under && index == 0) {
                lookHeldUntil = 0.0F;
                refresh(view);
            }
            node.rotateTo(-90.0F, yaw + spin * 0.5F, 0.0F, 0.0F);
            node.rotateTo(0.0F, yaw, 0.0F, flight / 2.0F);
        });
    }

    private void after(float seconds, Runnable run) {
        steps.add(new Step(clock + seconds, run));
    }

    /** Food let go over the pan goes in: all of it, or one with the right button. The book takes its click with a stack in the hand. */
    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        if ("book_open".equals(node)) {
            book.click(view);
            return true;
        }
        CompoundTag how = new CompoundTag();
        how.putBoolean(SkilletStation.ONE, button == 1);
        view.sendAction(SkilletStation.ACTION_PLACE, how);
        return true;
    }

    /** Shift + click on food in the inventory puts the whole stack into the pan, as at the board. */
    @Override
    public boolean onInventoryShiftClick(StationView view, int slot, ItemStack stack) {
        if (stack.isEmpty() || SkilletCooking.recipe(view.level(), stack).isEmpty()) {
            return false;
        }
        CompoundTag how = new CompoundTag();
        how.putInt(SkilletStation.FROM, slot);
        view.sendAction(SkilletStation.ACTION_PLACE, how);
        return true;
    }

    @Override
    public void onEffect(StationView view, Identifier effect, String node, CompoundTag data) {
        if (SkilletStation.EFFECT_TOSSED.equals(effect)) {
            // The cook's own toss is on show already.
            if (!view.isFocused() || clock - tossedAt > 0.6F) {
                tossed(view, data.getFloatOr(SkilletStation.STRENGTH, 0.0F));
            }
        } else if (SkilletStation.EFFECT_UNTOSSED.equals(effect)) {
            // The server did not take the cook's toss: the food lies the way it did.
            tossedUp = Float.NaN;
            lookHeldUntil = 0.0F;
            refresh(view);
        } else if (SkilletStation.EFFECT_PLACED.equals(effect)) {
            if (view.state().data().getBooleanOr(SkilletStation.HEATED, false)) {
                view.playSound(FarmersDelightContent.SKILLET_ADD_FOOD, "pan", 0.8F, 1.0F);
            }
            // An egg is cracked on the pan's rim and goes in without its shell.
            ItemStack placed = view.state().getItem(SkilletStation.PAN_SLOT);
            if (KitchenLooks.isCracked(placed)) {
                KitchenLooks.crack(view, "pan", roundFood());
            }
            view.playSound(SoundEvents.LANTERN_PLACE, "pan", 0.5F, 1.0F);
        } else if (SkilletStation.EFFECT_REFUSED.equals(effect)) {
            ItemStack refused = KitchenNbt.parse(view.level().registryAccess(), data.getCompoundOrEmpty(SkilletStation.STACK));
            ghost(view, refused, 0).ifPresent(ghost -> {
                ghost.moveTo(0.0F, LAND_FROM + 2.0F, 0.0F, 0.0F);
                ghost.moveTo(0.0F, 1.5F, 0.0F, 0.09F);
                after(0.09F, () -> ghost.hopTo(0.0F, LAND_FROM + 2.0F, AWAY_Z * 0.5F, 1.2F, 0.22F));
                after(0.31F, () -> ghost.setVisible(false));
            });
            view.playSound(SoundEvents.LANTERN_HIT, "pan", 0.4F, 1.3F);
        } else if (SkilletStation.EFFECT_TAKEN.equals(effect)) {
            leaves(view, KitchenNbt.parse(view.level().registryAccess(), data.getCompoundOrEmpty(SkilletStation.STACK)), true);
            view.playSound(SoundEvents.ITEM_PICKUP, "pan", 0.3F, 1.5F);
        } else if (SkilletStation.EFFECT_FRIED.equals(effect)) {
            if (data.getBooleanOr(SkilletStation.IN_PAN, false)) {
                ready(view);
            } else {
                leaves(view, KitchenNbt.parse(view.level().registryAccess(), data.getCompoundOrEmpty(SkilletStation.STACK)), data.getBooleanOr(SkilletStation.TO_COOK, false));
            }
            view.playSound(SoundEvents.ITEM_PICKUP, "pan", 0.35F, 1.3F);
        }
    }

    /** What lay in the pan leaves it: lifted off for the cook's inventory, or thrown out over the side. */
    /** The batch is done and waits in the pan: a puff of steam and a glint over it, the fat calms down. */
    private void ready(StationView view) {
        view.spawnParticles(DiegesisParticles.STEAM, onFood(), new Vector3f(0.0F, 3.0F, 0.0F), 2.0F, 4, 0x60FFFFFF);
        view.spawnParticles(DiegesisParticles.GLINT, onFood(), new Vector3f(0.0F, 2.0F, 0.0F), 2.5F, 4, 0xFFFFE9A0);
        // Done: one last deep sizzle as the food settles, then only the fire crackles under it (owner: a chime "does not fit").
        view.playSound(FarmersDelightContent.SKILLET_ADD_FOOD, "pan", 0.4F, 0.75F);
        after(0.35F, () -> view.playSound(SoundEvents.CAMPFIRE_CRACKLE, "pan", 0.6F, 0.9F));
    }

    private void leaves(StationView view, ItemStack stack, boolean toCook) {
        if (stack.isEmpty()) {
            return;
        }
        int pieces = shownPieces;
        float size = size(pieces);
        for (int i = 0; i < Math.max(1, pieces); i++) {
            float[] at = place(i, Math.max(1, pieces));
            float yaw = yaw(i, false);
            float delay = i * 0.04F;
            ghost(view, stack, i, size).ifPresent(ghost -> {
                ghost.moveTo(at[0], 0.0F, at[1], 0.0F);
                ghost.rotateTo(0.0F, yaw, 0.0F, 0.0F);
                after(delay, () -> {
                    if (toCook) {
                        // It lifts and melts away where it is: the drawer shows it arriving (Diegesis' gains).
                        ghost.hopTo(at[0], 2.5F, at[1], 1.5F, AWAY_TIME);
                        ghost.scaleTo(0.0F, AWAY_TIME);
                    } else {
                        ghost.hopTo(at[0] + AWAY_Z, 2.0F, at[1], 5.0F, AWAY_TIME);
                    }
                });
                after(delay + AWAY_TIME, () -> ghost.setVisible(false));
            });
        }
        // The real ones go at once; the state that says so may come a moment later.
        for (int i = 0; i < PIECES; i++) {
            view.node("piece_" + i).ifPresent(node -> node.setVisible(false));
            view.node("under_" + i).ifPresent(node -> node.setVisible(false));
        }
        shownPieces = 0;
        shownUnders = 0;
        shown = ItemStack.EMPTY;
    }

    private Optional<NodeView> ghost(StationView view, ItemStack stack, int index) {
        return ghost(view, stack, index, size(1));
    }

    /** Ghost {@code index}, shaped as {@code stack}. */
    private Optional<NodeView> ghost(StationView view, ItemStack stack, int index, float size) {
        Optional<NodeView> node = view.node("lift_" + index % PIECES);
        node.ifPresent(it -> {
            Optional<ItemVoxels.Icon> icon = ItemVoxels.icon(stack);
            if (icon.isPresent()) {
                it.setVisual(null);
                it.setShape(ItemVoxels.build(icon.get(), size, List.of(), 0.0F), icon.get().sprite());
            } else {
                it.setShape(null, null);
                it.setVisual(Visual.ofItem(stack.getItem()).withAnchor(new Vector3f(8.0F, 8.0F, 8.0F)));
            }
            it.setTint(0xFFFFFFFF);
            it.rotateTo(0.0F, 0.0F, 0.0F, 0.0F);
            it.scaleTo(1.0F, 0.0F);
            it.moveTo(0.0F, 0.0F, 0.0F, 0.0F);
            it.setVisible(true);
        });
        return node;
    }
}
