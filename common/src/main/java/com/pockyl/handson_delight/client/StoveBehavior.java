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
import org.joml.Vector3f;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.NodeView;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.scene.Visual;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.station.Grill;
import com.pockyl.handson_delight.station.SkilletCooking;
import com.pockyl.handson_delight.station.StoveCooking;
import com.pockyl.handson_delight.station.StoveStation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The stove's top on the client: six pieces on the grill, each built from its icon and seared on its top as far as that side is done,
 * the cooked look once it is. A click on a piece turns it over (a hop and a half turn, the other side's look edge on), the other
 * button takes it back; done, either takes it. Heat shimmers over the lit grill, a piece that cooks steams and sizzles quietly, one
 * whose lower side is done smokes and sizzles sharper: it wants turning.
 */
public final class StoveBehavior implements StationBehavior {
    /** How big a piece is drawn: Farmer's Delight's own stove draws its items six pixels wide, a little apart. */
    private static final float SIZE = 5.0F;
    private static final float TOP = 16.02F;
    /** The colour a top goes towards while the side under it is done, before it shows the cooked look. */
    private static final int SEARED = 0xFFB8875C;
    private static final float SEAR = 0.7F;
    private static final float FLIGHT = 0.3F;
    private static final float RISE = 3.0F;
    private static final float LAND_TIME = 0.2F;
    private static final float LAND_FROM = 4.0F;
    private static final float PIECE_STAGGER = 0.14F;
    private static final float PLAYER_LAND_TIME = 0.32F;
    private static final float AWAY_TIME = 0.3F;
    private static final float SHIMMER_RATE = 1.6F;
    private static final float VAPOUR_RATE = 0.9F;
    private static final float SMOKE_RATE = 2.5F;
    private static final float SIZZLE_TIME = 1.6F;
    private static final float WAITING_SIZZLE_TIME = 1.1F;
    private static final int GHOSTS = 3;

    private final StoveBook pages = new StoveBook();
    private final RecipeBook book = new RecipeBook(pages);
    private final Map<Item, Boolean> usable = new HashMap<>();
    private final RandomSource random = RandomSource.create();
    private final ItemStack[] shown = new ItemStack[Grill.PLACES];
    /** The look each piece shows, by how far its top is done; held while it is in the air after a turn. */
    private final float[] shownUp = new float[Grill.PLACES];
    private final float[] heldUntil = new float[Grill.PLACES];
    private final float[] turnedAt = new float[Grill.PLACES];
    private final List<Step> steps = new ArrayList<>();
    private boolean quiet = true;
    private float clock;
    private float shimmer;
    private float vapour;
    private float smoke;
    private float sizzle;
    private int ghost;

    private record Step(float at, Runnable run) {
    }

    public StoveBehavior(StationView view) {
        Arrays.fill(shown, ItemStack.EMPTY);
        Arrays.fill(turnedAt, -10.0F);
    }

    @Override
    public void onSceneReady(StationView view) {
        Arrays.fill(shown, ItemStack.EMPTY);
        Arrays.fill(shownUp, -1.0F);
        steps.clear();
        quiet = true;
        view.node("book_open").ifPresent(node -> node.setPainter(book));
        refresh(view);
    }

    @Override
    public void onOpen(StationView view) {
        book.opened(view);
        usable.clear();
        view.setInventoryFilter(stack -> usable.computeIfAbsent(stack.getItem(),
                item -> StoveCooking.canGrill(view.level(), stack)));
        // A right click with food lays it on the grill, as Farmer's Delight's stove takes it: never slower than without the station.
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && view.openedByUse() && player.inventoryMenu.getCarried().isEmpty()) {
            onInventoryShiftClick(view, player.getInventory().getSelectedSlot(), player.getMainHandItem());
        }
    }

    @Override
    public void onClose(StationView view) {
        book.shut(view);
    }

    @Override
    public void onStateChanged(StationView view, StationState.Changes changes) {
        refresh(view);
    }

    @Override
    public void tick(StationView view) {
        refresh(view);
        for (int i = 0; i < GHOSTS; i++) {
            view.node("lift_" + i).filter(node -> node.isVisible() && !node.isMoving()).ifPresent(node -> node.setVisible(false));
        }
    }

    /** The pieces as the state says; what came on lands, several at once from the player one after the other. */
    private void refresh(StationView view) {
        StationState state = view.state();
        CompoundTag data = state.data();
        int arriving = 0;
        for (int place = 0; place < Grill.PLACES; place++) {
            ItemStack now = state.getItem(StoveStation.place(place));
            arriving += shown[place].isEmpty() && !now.isEmpty() ? 1 : 0;
        }
        boolean together = arriving > 1 && !quiet;
        int landing = 0;
        boolean lit = data.getBooleanOr(StoveStation.LIT, false);
        for (int place = 0; place < Grill.PLACES; place++) {
            ItemStack now = state.getItem(StoveStation.place(place));
            float up = clock < heldUntil[place] ? shownUp[place] : data.getFloatOr(StoveStation.UP + place, 0.0F);
            boolean arrives = shown[place].isEmpty() && !now.isEmpty();
            if (ItemStack.isSameItemSameComponents(now, shown[place]) && look(up) == look(shownUp[place])) {
                continue;
            }
            shown[place] = now.copy();
            shownUp[place] = up;
            int index = place;
            Optional<NodeView> node = view.node("piece_" + place);
            node.ifPresent(it -> draw(view, it, index));
            if (!arrives || quiet || node.isEmpty()) {
                continue;
            }
            float delay = together ? landing * PIECE_STAGGER : 0.0F;
            Vector3f from = together ? FromPlayer.offset(view, LAND_FROM, landing, arriving) : null;
            landing++;
            node.get().setVisible(delay <= 0.0F);
            Runnable lands = () -> {
                if (shown[index].isEmpty()) {
                    return;
                }
                node.get().setVisible(true);
                land(node.get(), from);
                if (lit) {
                    after(from == null ? LAND_TIME : PLAYER_LAND_TIME, () -> hitsTheGrill(view, index));
                }
            };
            if (delay > 0.0F) {
                after(delay, lands);
            } else {
                lands.run();
            }
        }
        quiet = false;
    }

    /** The look of a top {@code up} done, in steps: drawn again only when it changes by one. */
    private static int look(float up) {
        return up < 0.0F ? -1 : up >= 1.0F ? 100 : Mth.floor(up * 12.0F);
    }

    private void draw(StationView view, NodeView node, int place) {
        ItemStack stack = shown[place];
        node.setVisible(!stack.isEmpty());
        if (stack.isEmpty()) {
            return;
        }
        boolean done = shownUp[place] >= 1.0F;
        ItemStack look = done ? cooked(view, stack) : KitchenLooks.opened(stack);
        Optional<ItemVoxels.Icon> icon = ItemVoxels.icon(look);
        if (icon.isPresent()) {
            node.setVisual(null);
            node.setShape(ItemVoxels.build(icon.get(), SIZE, List.of(), 0.0F), icon.get().sprite());
        } else {
            node.setShape(null, null);
            node.setVisual(Visual.ofItem(look.getItem()).withAnchor(new Vector3f(8.0F, 8.0F, 8.0F)));
        }
        node.setTint(KitchenLooks.fryingTint(stack, cooked(view, stack), shownUp[place], SEARED, SEAR));
        if (!node.isMoving()) {
            node.moveTo(0.0F, 0.0F, 0.0F, 0.0F);
            node.rotateTo(0.0F, yaw(place), 0.0F, 0.0F);
        }
    }

    /** What the fire makes of {@code raw}: the look of a done top. */
    private static ItemStack cooked(StationView view, ItemStack raw) {
        return SkilletCooking.recipe(view.level(), raw).map(recipe -> KitchenRecipes.result(recipe))
                .filter(result -> !result.isEmpty()).orElse(raw);
    }

    /** Laid by hand: each piece a little turned, never two the same way. */
    private static float yaw(int place) {
        return ((place * 7919) % 41 - 20) * 1.5F;
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

    /** Comes down onto its place from a little above, or with {@code from} over from the player, with a hop. */
    private void land(NodeView node, Vector3f from) {
        if (from == null) {
            node.moveTo(0.0F, LAND_FROM, 0.0F, 0.0F);
            node.hopTo(0.0F, 0.0F, 0.0F, 0.8F, LAND_TIME);
        } else {
            node.moveTo(from.x, LAND_FROM, from.z, 0.0F);
            node.hopTo(0.0F, 0.0F, 0.0F, 2.0F, PLAYER_LAND_TIME);
        }
    }

    @Override
    public void frame(StationView view, float dt) {
        clock += dt;
        if (!steps.isEmpty()) {
            List<Step> due = steps.stream().filter(step -> step.at() <= clock).toList();
            steps.removeAll(due);
            due.forEach(step -> step.run().run());
        }
        heat(view, dt);
        book.frame(view, dt);
    }

    /**
     * The grill at work. Lit, the air over it shimmers; a piece that cooks steams a little and sizzles quietly; one whose lower side is
     * done smokes from under it and sizzles sharper, wanting to be turned. Done pieces only rest.
     */
    private void heat(StationView view, float dt) {
        CompoundTag data = view.state().data();
        if (!data.getBooleanOr(StoveStation.LIT, false)) {
            sizzle = 0.0F;
            return;
        }
        shimmer += dt * SHIMMER_RATE;
        while (shimmer >= 1.0F) {
            shimmer -= 1.0F;
            view.spawnParticles(HandsOnDelightClient.HEAT, new Vector3f(8.0F + randomly(5.5F), TOP + 2.0F + random.nextFloat() * 2.0F,
                    8.0F + randomly(5.5F)), new Vector3f(0.0F, 3.0F, 0.0F), 0.3F, 1, 0);
        }
        List<Integer> cooking = new ArrayList<>();
        List<Integer> waiting = new ArrayList<>();
        for (int place = 0; place < Grill.PLACES; place++) {
            if (shown[place].isEmpty() || StoveCooking.isDone(view.level(), shown[place]) || clock - turnedAt[place] < FLIGHT) {
                continue;
            }
            boolean waits = data.getBooleanOr(StoveStation.BY_HAND + place, false) && data.getFloatOr(StoveStation.DOWN + place, 0.0F) >= 1.0F
                    && data.getFloatOr(StoveStation.UP + place, 0.0F) < 1.0F;
            (waits ? waiting : cooking).add(place);
        }
        vapour += dt * VAPOUR_RATE * cooking.size();
        while (vapour >= 1.0F && !cooking.isEmpty()) {
            vapour -= 1.0F;
            view.spawnParticles(DiegesisParticles.STEAM, onPiece(cooking.get(random.nextInt(cooking.size()))),
                    new Vector3f(randomly(0.4F), 2.2F, randomly(0.4F)), 0.4F, 1, 0x40FFFFFF);
        }
        smoke += dt * SMOKE_RATE * waiting.size();
        while (smoke >= 1.0F && !waiting.isEmpty()) {
            smoke -= 1.0F;
            view.spawnParticles(HandsOnDelightClient.PAN_SMOKE, roundPiece(waiting.get(random.nextInt(waiting.size()))),
                    new Vector3f(randomly(0.5F), 4.0F, randomly(0.5F)), 0.3F, 1, 0);
        }
        if (cooking.isEmpty() && waiting.isEmpty()) {
            sizzle = 0.0F;
            return;
        }
        sizzle -= dt;
        if (sizzle <= 0.0F) {
            boolean wants = !waiting.isEmpty();
            sizzle = wants ? WAITING_SIZZLE_TIME : SIZZLE_TIME;
            view.playSound(FarmersDelightContent.SKILLET_SIZZLE, "grill", wants ? 0.3F : 0.15F,
                    (wants ? 1.15F : 1.0F) + random.nextFloat() * 0.1F);
        }
    }

    /** Food landing on the hot grill: it hisses and steams. */
    private void hitsTheGrill(StationView view, int place) {
        view.spawnParticles(DiegesisParticles.STEAM, onPiece(place), new Vector3f(0.0F, 3.0F, 0.0F), 1.2F, 2, 0x50FFFFFF);
        view.playSound(FarmersDelightContent.SKILLET_ADD_FOOD, "grill", 0.45F, 1.05F + random.nextFloat() * 0.1F);
    }

    /** Where piece {@code place} lies, scene pixels. */
    private static Vector3f placeOf(int place) {
        return new Vector3f(place % 3 == 0 ? 12.8F : place % 3 == 1 ? 8.0F : 3.2F, TOP, place < 3 ? 11.2F : 4.8F);
    }

    private Vector3f onPiece(int place) {
        return placeOf(place).add(randomly(1.2F), 0.6F, randomly(1.2F));
    }

    /** Just round the edge of a piece, on the grill: where the smoke of a side that waits curls up from. */
    private Vector3f roundPiece(int place) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        float reach = SIZE * 0.5F * (0.8F + 0.3F * random.nextFloat());
        return placeOf(place).add(Mth.cos(angle) * reach, 0.15F, Mth.sin(angle) * reach);
    }

    private float randomly(float most) {
        return (random.nextFloat() * 2.0F - 1.0F) * most;
    }

    private void after(float seconds, Runnable run) {
        steps.add(new Step(clock + seconds, run));
    }

    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (!(event instanceof SceneEvent.Click click)) {
            return;
        }
        if ("book_open".equals(click.node())) {
            book.click(view);
            return;
        }
        int place = placeOf(click.node());
        if (place < 0 || shown[place].isEmpty()) {
            return;
        }
        // The piece itself is what the cook clicks when it smokes: it turns. The other button takes it back, raw or half done, a way to
        // start over; done, either takes it.
        if (click.button() == 1 || StoveCooking.isDone(view.level(), shown[place])) {
            CompoundTag at = new CompoundTag();
            at.putInt(StoveStation.PLACE, place);
            view.sendAction(StoveStation.ACTION_TAKE, at);
        } else if (clock - turnedAt[place] > FLIGHT + 0.05F) {
            CompoundTag at = new CompoundTag();
            at.putInt(StoveStation.PLACE, place);
            view.sendAction(StoveStation.ACTION_TURN, at);
            turned(view, place);
        }
    }

    private static int placeOf(String node) {
        if (node == null || !node.startsWith("piece_")) {
            return -1;
        }
        try {
            int place = Integer.parseInt(node.substring("piece_".length()));
            return place >= 0 && place < Grill.PLACES ? place : -1;
        } catch (NumberFormatException notAPiece) {
            return -1;
        }
    }

    /** One piece over, as everybody sees it: up and half round, then, edge on, the other side's look, the rest of the way and down. */
    private void turned(StationView view, int place) {
        turnedAt[place] = clock;
        float newUp = view.state().data().getFloatOr(StoveStation.DOWN + place, 0.0F);
        heldUntil[place] = clock + FLIGHT / 2.0F;
        view.node("piece_" + place).ifPresent(node -> {
            float yaw = yaw(place);
            node.hopTo(0.0F, 0.0F, 0.0F, RISE, FLIGHT);
            node.rotateTo(90.0F, yaw, 0.0F, FLIGHT / 2.0F);
            after(FLIGHT / 2.0F, () -> {
                heldUntil[place] = 0.0F;
                shownUp[place] = newUp;
                draw(view, node, place);
                node.rotateTo(-90.0F, yaw, 0.0F, 0.0F);
                node.rotateTo(0.0F, yaw, 0.0F, FLIGHT / 2.0F);
            });
        });
        view.playSound(SoundEvents.LANTERN_STEP, "grill", 0.25F, 1.3F);
        after(FLIGHT, () -> {
            view.playSound(FarmersDelightContent.SKILLET_ADD_FOOD, "grill", 0.5F, 1.0F + random.nextFloat() * 0.15F);
            view.spawnParticles(DiegesisParticles.STEAM, onPiece(place), new Vector3f(0.0F, 3.0F, 0.0F), 0.8F, 2, 0x50FFFFFF);
        });
    }

    /**
     * Food let go over the grill goes on: one onto every free place, or with the right button one onto the place under the cursor. The
     * book takes its click with a stack in the hand.
     */
    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        if ("book_open".equals(node)) {
            book.click(view);
            return true;
        }
        CompoundTag how = new CompoundTag();
        if (button == 1) {
            how.putBoolean(StoveStation.ONE, true);
            how.putInt(StoveStation.PLACE, nearestFree(view));
        }
        view.sendAction(StoveStation.ACTION_PLACE, how);
        return true;
    }

    /** The free place nearest the cursor on the grill; -1 for none. */
    private int nearestFree(StationView view) {
        Optional<Vector3f> cursor = view.cursorOn(TOP);
        int best = -1;
        float nearest = Float.MAX_VALUE;
        for (int place = 0; place < Grill.PLACES; place++) {
            if (!view.state().getItem(StoveStation.place(place)).isEmpty()) {
                continue;
            }
            Vector3f at = placeOf(place);
            float distance = cursor.map(point -> point.distanceSquared(at)).orElse((float) place);
            if (distance < nearest) {
                nearest = distance;
                best = place;
            }
        }
        return best;
    }

    /** Shift + click on food in the inventory lays the stack on the grill, as at the board and the pan. */
    @Override
    public boolean onInventoryShiftClick(StationView view, int slot, ItemStack stack) {
        if (stack.isEmpty() || SkilletCooking.recipe(view.level(), stack).isEmpty()) {
            return false;
        }
        CompoundTag how = new CompoundTag();
        how.putInt(StoveStation.FROM, slot);
        view.sendAction(StoveStation.ACTION_PLACE, how);
        return true;
    }

    @Override
    public void onEffect(StationView view, Identifier effect, String node, CompoundTag data) {
        int place = data.getIntOr(StoveStation.PLACE, 0);
        if (StoveStation.EFFECT_TURNED.equals(effect)) {
            // The cook's own turn is on show already.
            if (place >= 0 && place < Grill.PLACES && (!view.isFocused() || clock - turnedAt[place] > 0.6F)) {
                turned(view, place);
            }
        } else if (StoveStation.EFFECT_UNTURNED.equals(effect)) {
            if (place >= 0 && place < Grill.PLACES) {
                heldUntil[place] = 0.0F;
                shownUp[place] = -1.0F;
                refresh(view);
            }
        } else if (StoveStation.EFFECT_PLACED.equals(effect)) {
            view.playSound(SoundEvents.LANTERN_PLACE, "grill", 0.4F, 1.1F);
        } else if (StoveStation.EFFECT_REFUSED.equals(effect)) {
            ItemStack refused = KitchenNbt.parse(view.level().registryAccess(), data.getCompoundOrEmpty(StoveStation.STACK));
            ghost(view, refused).ifPresent(ghost -> {
                ghost.moveTo(0.0F, LAND_FROM + 2.0F, 0.0F, 0.0F);
                ghost.moveTo(0.0F, 1.5F, 0.0F, 0.09F);
                after(0.09F, () -> ghost.hopTo(0.0F, LAND_FROM + 2.0F, -6.0F, 1.2F, 0.22F));
                after(0.31F, () -> ghost.setVisible(false));
            });
            view.playSound(SoundEvents.LANTERN_HIT, "grill", 0.4F, 1.3F);
        } else if (StoveStation.EFFECT_TAKEN.equals(effect)) {
            taken(view, place, KitchenNbt.parse(view.level().registryAccess(), data.getCompoundOrEmpty(StoveStation.STACK)));
        } else if (StoveStation.EFFECT_GRILLED.equals(effect) && place >= 0 && place < Grill.PLACES) {
            view.spawnParticles(DiegesisParticles.STEAM, onPiece(place), new Vector3f(0.0F, 3.0F, 0.0F), 1.5F, 3, 0x60FFFFFF);
            view.spawnParticles(DiegesisParticles.GLINT, onPiece(place), new Vector3f(0.0F, 2.0F, 0.0F), 1.5F, 3, 0xFFFFE9A0);
            view.playSound(SoundEvents.ITEM_PICKUP, "grill", 0.3F, 1.3F);
        }
    }

    /** A piece leaves the grill for the cook's inventory: it lifts and melts away where it was, the drawer shows it arriving. */
    private void taken(StationView view, int place, ItemStack stack) {
        if (place < 0 || place >= Grill.PLACES || stack.isEmpty()) {
            return;
        }
        Vector3f at = placeOf(place).sub(8.0F, TOP, 8.0F);
        ghost(view, stack).ifPresent(ghost -> {
            ghost.moveTo(at.x, 0.0F, at.z, 0.0F);
            ghost.rotateTo(0.0F, yaw(place), 0.0F, 0.0F);
            ghost.hopTo(at.x, 2.5F, at.z, 1.5F, AWAY_TIME);
            ghost.scaleTo(0.0F, AWAY_TIME);
            after(AWAY_TIME, () -> ghost.setVisible(false));
        });
        // The real one goes at once; the state that says so may come a moment later.
        view.node("piece_" + place).ifPresent(node -> node.setVisible(false));
        shown[place] = ItemStack.EMPTY;
        view.playSound(SoundEvents.ITEM_PICKUP, "grill", 0.3F, 1.5F);
    }

    private Optional<NodeView> ghost(StationView view, ItemStack stack) {
        ghost = (ghost + 1) % GHOSTS;
        Optional<NodeView> node = view.node("lift_" + ghost);
        node.ifPresent(it -> {
            Optional<ItemVoxels.Icon> icon = ItemVoxels.icon(stack);
            if (icon.isPresent()) {
                it.setVisual(null);
                it.setShape(ItemVoxels.build(icon.get(), SIZE, List.of(), 0.0F), icon.get().sprite());
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
