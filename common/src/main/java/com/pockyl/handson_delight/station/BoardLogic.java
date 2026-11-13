package com.pockyl.handson_delight.station;

import com.pockyl.handson_delight.KitchenNbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;

import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotStorage;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationLogic;
import com.pockyl.handson_delight.assembly.Assembling;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.platform.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The board's rules. Cutting is Farmer's Delight's own, once the item has had its chops; what it makes goes to the cook instead of
 * onto the floor. A dish is put together from the bread up, and every layer must belong to some dish made that way. Also runs on
 * the clients around a board nobody works at, to show what lies on it; there it only reads.
 */
public final class BoardLogic implements StationLogic {
    /** Ticks between two chops the server takes: a click as fast as a hand can go, not a macro. */
    private static final int CHOP_GAP = 3;
    /** Where marks may lie, as fractions of the item's length from its far end: never at its very ends. */
    private static final float SPOT_FROM = 0.2F;
    private static final float SPOT_TO = 0.8F;
    /** How far a mark may stray from its even place, as a share of the room between two marks: pieces of a size one can see. */
    private static final float SPOT_JITTER = 0.2F;

    private final CuttingBoardBlockEntity board;
    /** How many of the marks the knife has come down on, in order. */
    private int marks;
    /** Where the marks of the item lying there are, see {@link CuttingBoardStation#SPOTS}. */
    private final float[] spots = new float[Cutting.MOST_MARKS];
    private final float[] angles = new float[Cutting.MOST_MARKS];
    /** How the item lying there is cut: how many marks, laid how. */
    private Cutting.Plan plan = new Cutting.Plan(1, Cutting.Style.ACROSS);
    private ItemStack cutting = ItemStack.EMPTY;
    private long lastChop = -CHOP_GAP;
    /** The dish the cook picked in the book: made at once when the layers make it. */
    private Identifier picked;

    public BoardLogic(StationContext station) {
        board = (CuttingBoardBlockEntity) station.blockEntity();
        station.state().bindSlots(CuttingBoardStation.BOARD, new SlotStorage() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public ItemStack get(int index) {
                return Platform.INSTANCE.getItem(board);
            }

            @Override
            public void set(int index, ItemStack stack) {
                Platform.INSTANCE.setItem(board, stack);
            }
        });
        BoardLayers layers = (BoardLayers) board;
        station.state().bindSlots(CuttingBoardStation.LAYERS, new SlotStorage() {
            @Override
            public int size() {
                return CuttingBoardStation.MOST_LAYERS;
            }

            @Override
            public ItemStack get(int index) {
                return layers.handson$layers().get(index);
            }

            @Override
            public void set(int index, ItemStack stack) {
                layers.handson$layers().set(index, stack);
                layers.handson$layersChanged();
            }
        });
        restart(station);
    }

    /** The layers of the dish being put together, bottom first. */
    public static List<ItemStack> layers(StationState state) {
        List<ItemStack> layers = new ArrayList<>();
        for (ItemStack layer : state.getItems(CuttingBoardStation.LAYERS)) {
            if (!layer.isEmpty()) {
                layers.add(layer);
            }
        }
        return layers;
    }

    /** Nothing goes in by the library's own moves: every stack comes through {@link #place}, the hand's slot stays empty. */
    @Override
    public boolean mayPlace(StationContext station, Player player, SlotRef slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayTake(StationContext station, Player player, SlotRef slot) {
        return slot.group().equals(CuttingBoardStation.BOARD);
    }

    @Override
    public void tick(StationContext station) {
        if (station.level().isClientSide()) {
            return;
        }
        ItemStack onBoard = Platform.INSTANCE.getItem(board);
        if (!ItemStack.isSameItemSameComponents(onBoard, cutting)) {
            // Something else lies there now: a hopper took the item, or a deployer cut it.
            cutting = onBoard.copyWithCount(1);
            restart(station);
        }
        publish(station);
    }

    @Override
    public void onOpen(StationContext station, Player player) {
        if (!AssemblyRecipes.book().isEmpty()) {
            CompoundTag book = new CompoundTag();
            book.put(CuttingBoardStation.BOOK, AssemblyRecipes.book());
            station.effect(CuttingBoardStation.EFFECT_BOOK, "surface", book);
        }
    }

    /** The marks and the dish picked start over when the last cook leaves; another one may still be cutting. */
    @Override
    public void onClose(StationContext station, Player player) {
        if (station.users().isEmpty()) {
            restart(station);
            picked = null;
        }
        publish(station);
    }

    @Override
    public void onCustomAction(StationContext station, Player player, String action, CompoundTag data) {
        switch (action) {
            case CuttingBoardStation.ACTION_PLACE -> place(station, player, data.getBooleanOr(CuttingBoardStation.ONE, false),
                    data.contains(CuttingBoardStation.FROM) ? data.getIntOr(CuttingBoardStation.FROM, 0) : -1);
            case CuttingBoardStation.ACTION_CHOP -> chop(station, player, data.getIntOr(CuttingBoardStation.MARK, 0), data.getFloatOr(CuttingBoardStation.AT, 0.0F));
            case CuttingBoardStation.ACTION_TAKE -> take(station, player);
            case CuttingBoardStation.ACTION_ASSEMBLE -> assemble(station, player);
            case CuttingBoardStation.ACTION_PICK -> {
                Identifier dish = Identifier.tryParse(data.getStringOr(CuttingBoardStation.DISH, ""));
                pick(station, player, dish, data.getBooleanOr(CuttingBoardStation.ALL, false));
            }
            default -> {
            }
        }
        publish(station);
    }

    /**
     * The stack on the cursor, or the one in inventory slot {@code from}, goes onto the board: on the dish when it belongs to one, else
     * to be cut.
     */
    private void place(StationContext station, Player player, boolean one, int from) {
        if (from >= Inventory.INVENTORY_SIZE) {
            return;
        }
        ItemStack carried = from >= 0 ? player.getInventory().getItem(from) : player.inventoryMenu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        StationState state = station.state();
        List<ItemStack> layers = layers(state);
        ItemStack onBoard = Platform.INSTANCE.getItem(board);
        if (!layers.isEmpty() || onBoard.isEmpty() && AssemblyRecipes.isBase(carried)) {
            List<ItemStack> next = new ArrayList<>(layers);
            next.add(carried.copyWithCount(1));
            if (layers.size() >= CuttingBoardStation.MOST_LAYERS || !AssemblyRecipes.fits(next)) {
                refuse(station, carried);
                return;
            }
            // A batch: the bread sets how many, every layer after it as many as the hand has, up to that.
            int batch = layers.isEmpty() ? CuttingBoardStation.BATCH : layers.get(0).getCount();
            state.setItem(SlotRef.of(CuttingBoardStation.LAYERS, layers.size()), carried.split(one ? 1 : Math.min(carried.getCount(), batch)));
            putBack(player, from, carried);
            CompoundTag where = new CompoundTag();
            where.putInt(CuttingBoardStation.AT, layers.size());
            station.effect(CuttingBoardStation.EFFECT_PLACED, "surface", where);
            finishIfWanted(station, player, next);
            return;
        }
        boolean fits = onBoard.isEmpty() ? !Cutting.recipesFor(station.level(), carried).isEmpty()
                : ItemStack.isSameItemSameComponents(onBoard, carried);
        int room = Math.min(carried.getMaxStackSize(), board.getMaxStackSize()) - onBoard.getCount();
        int moved = Math.min(one ? 1 : carried.getCount(), room);
        if (!fits || moved <= 0) {
            refuse(station, carried);
            return;
        }
        ItemStack put = onBoard.isEmpty() ? carried.copyWithCount(moved) : onBoard.copyWithCount(onBoard.getCount() + moved);
        carried.shrink(moved);
        putBack(player, from, carried);
        Platform.INSTANCE.setItem(board, put);
        if (onBoard.isEmpty()) {
            cutting = put.copyWithCount(1);
            restart(station);
        }
        station.effect(CuttingBoardStation.EFFECT_PLACED, "surface", new CompoundTag());
    }

    /**
     * The layers make a dish at once when it is the one picked in the book, or when they could become nothing bigger. A dish that is
     * also the start of a bigger one waits for a click on the stack or a layer more: nothing is made that the cook did not mean.
     */
    private void finishIfWanted(StationContext station, Player player, List<ItemStack> layers) {
        Optional<AssemblyRecipes.Assembly> dish = AssemblyRecipes.complete(layers);
        if (dish.isPresent() && (dish.get().id().equals(picked) || !AssemblyRecipes.canGrow(layers))) {
            assemble(station, player);
        }
    }

    /**
     * A dish picked in the book is the one the board makes, and what it takes comes from the cook's inventory: onto an empty board one
     * set, or as many as a batch with {@code all}; onto layers already there, the rest of it for as many as lie there.
     */
    private void pick(StationContext station, Player player, Identifier id, boolean all) {
        Optional<AssemblyRecipes.Assembly> dish = AssemblyRecipes.all().stream().filter(assembly -> assembly.id().equals(id)).findFirst();
        if (dish.isEmpty()) {
            return;
        }
        picked = id;
        fill(station, player, dish.get(), all);
        finishIfWanted(station, player, layers(station.state()));
    }

    private void fill(StationContext station, Player player, AssemblyRecipes.Assembly dish, boolean all) {
        StationState state = station.state();
        List<ItemStack> layers = layers(state);
        // A dish made before is taken off for the new one (a pie too, which the knife could slice); what lies there to be cut keeps
        // the board.
        ItemStack onBoard = Platform.INSTANCE.getItem(board);
        if (layers.isEmpty() && !onBoard.isEmpty() && !AssemblyRecipes.isDish(onBoard.getItem())
                && !Cutting.recipesFor(station.level(), onBoard).isEmpty()) {
            return;
        }
        int[] taken = layers.isEmpty() ? new int[0] : AssemblyRecipes.assign(dish, layers, false);
        if (taken == null) {
            return;
        }
        // What is still missing, the base first: it goes down first.
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < dish.ingredients().size(); i++) {
            int ingredient = i;
            if (Arrays.stream(taken).noneMatch(used -> used == ingredient)) {
                missing.add(ingredient);
            }
        }
        missing.sort(Comparator.comparing(ingredient -> ingredient != dish.base()));
        if (missing.isEmpty() || layers.size() + missing.size() > CuttingBoardStation.MOST_LAYERS) {
            return;
        }
        int want = layers.isEmpty() ? all ? CuttingBoardStation.BATCH : 1 : layers.get(0).getCount();
        Inventory inventory = player.getInventory();
        ItemStack[] left = new ItemStack[Inventory.INVENTORY_SIZE];
        for (int slot = 0; slot < left.length; slot++) {
            left[slot] = inventory.getItem(slot).copy();
        }
        // The item each new layer is made of, found with the first set; an item that does not stack (a bucket) goes down once.
        ItemStack[] kinds = new ItemStack[missing.size()];
        int[] counts = new int[missing.size()];
        List<int[]> removals = new ArrayList<>();
        int sets = 0;
        while (sets < want) {
            List<int[]> set = new ArrayList<>();
            boolean whole = true;
            for (int m = 0; m < missing.size() && whole; m++) {
                if (kinds[m] != null && kinds[m].getMaxStackSize() == 1) {
                    continue;
                }
                int slot = source(left, dish.ingredients().get(missing.get(m)), kinds[m]);
                if (slot < 0) {
                    whole = false;
                    break;
                }
                if (kinds[m] == null) {
                    kinds[m] = left[slot].copyWithCount(1);
                }
                left[slot].shrink(1);
                set.add(new int[] {slot, m});
            }
            if (!whole) {
                break;
            }
            for (int[] one : set) {
                counts[one[1]]++;
            }
            removals.addAll(set);
            sets++;
        }
        if (sets == 0) {
            return;
        }
        if (layers.isEmpty() && !onBoard.isEmpty()) {
            Platform.INSTANCE.setItem(board, ItemStack.EMPTY);
            taken(station, onBoard, -1);
            give(player, onBoard);
        }
        for (int[] removal : removals) {
            inventory.removeItem(removal[0], 1);
        }
        for (int m = 0; m < missing.size(); m++) {
            state.setItem(SlotRef.of(CuttingBoardStation.LAYERS, layers.size() + m), kinds[m].copyWithCount(counts[m]));
        }
        CompoundTag where = new CompoundTag();
        where.putInt(CuttingBoardStation.AT, layers.size() + missing.size() - 1);
        station.effect(CuttingBoardStation.EFFECT_PLACED, "surface", where);
    }

    /** The inventory slot that gives the next item for a layer: one the ingredient takes, the same as the layer's when it has one. */
    private static int source(ItemStack[] inventory, Ingredient ingredient, ItemStack kind) {
        for (int slot = 0; slot < inventory.length; slot++) {
            ItemStack stack = inventory[slot];
            if (!stack.isEmpty() && ingredient.test(stack) && (kind == null || ItemStack.isSameItemSameComponents(kind, stack))) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * The knife comes down on the item; with its last chop Farmer's Delight cuts a batch of the stack, one item after the other as
     * its own cut does (each wears the knife), and the pieces go to the cook.
     */
    private void chop(StationContext station, Player player, int mark, float at) {
        Level level = station.level();
        ItemStack onBoard = Platform.INSTANCE.getItem(board);
        long now = level.getGameTime();
        // Only the next mark is shown, so only it can be cut. The cook's client drew the cut at once: it hears that there was none.
        if (onBoard.isEmpty() || mark != marks || now - lastChop < CHOP_GAP) {
            refuse(station);
            return;
        }
        lastChop = now;
        // The knife is the one the cook took on the cursor; with an empty hand, one from the inventory (owner: "I need not take
        // the knife from its slot to cut").
        ItemStack tool = player.inventoryMenu.getCarried();
        int toolSlot = -1;
        if (Cutting.findWith(level, onBoard, tool).isEmpty()) {
            toolSlot = tool.isEmpty() ? Cutting.toolSlot(level, onBoard, player.getInventory()) : -1;
            if (toolSlot < 0) {
                refuse(station);
                return;
            }
            tool = player.getInventory().getItem(toolSlot);
        }
        ItemStack knife = tool;
        CompoundTag chopped = new CompoundTag();
        chopped.putFloat(CuttingBoardStation.AT, Mth.clamp(at, 0.0F, 16.0F));
        chopped.putFloat(CuttingBoardStation.SPOT, spots[mark]);
        chopped.putFloat(CuttingBoardStation.ANGLE, angles[mark]);
        marks++;
        chopped.putString(CuttingBoardStation.TOOL, BuiltInRegistries.ITEM.getKey(tool.getItem()).toString());
        if (marks < plan.marks()) {
            station.effect(CuttingBoardStation.EFFECT_CHOPPED, "item", chopped);
            return;
        }
        // The next item of the stack comes with marks of its own.
        restart(station);
        int done = 0;
        while (done < CuttingBoardStation.BATCH && !Platform.INSTANCE.getItem(board).isEmpty() && !tool.isEmpty()) {
            List<ItemStack> results = BoardResults.catching(done > 0, () -> board.processStoredItemUsingTool(knife, player));
            if (results == null) {
                break;
            }
            results.forEach(result -> give(player, result));
            done++;
        }
        if (toolSlot < 0) {
            player.inventoryMenu.setCarried(tool.isEmpty() ? ItemStack.EMPTY : tool);
        } else {
            player.getInventory().setItem(toolSlot, tool.isEmpty() ? ItemStack.EMPTY : tool);
        }
        if (done == 0) {
            refuse(station);
            return;
        }
        chopped.putInt(CuttingBoardStation.COUNT, done);
        chopped.putBoolean(CuttingBoardStation.DONE, true);
        station.effect(CuttingBoardStation.EFFECT_CHOPPED, "item", chopped);
    }

    /** The top layer comes off the dish; with none, what lies to be cut goes back. */
    private void take(StationContext station, Player player) {
        StationState state = station.state();
        List<ItemStack> layers = layers(state);
        if (!layers.isEmpty()) {
            SlotRef top = SlotRef.of(CuttingBoardStation.LAYERS, layers.size() - 1);
            ItemStack taken = state.getItem(top);
            taken(station, taken, layers.size() - 1);
            give(player, taken);
            state.setItem(top, ItemStack.EMPTY);
            return;
        }
        ItemStack onBoard = Platform.INSTANCE.getItem(board);
        if (!onBoard.isEmpty()) {
            Platform.INSTANCE.setItem(board, ItemStack.EMPTY);
            restart(station);
            taken(station, onBoard, -1);
            give(player, onBoard);
        }
    }

    /**
     * The layers become their dish, round after round as the crafting table makes it, as many times as the layers allow; it lies on
     * the board (Farmer's Delight's slot, so a hopper can take it as well as a click). What is left of the layers, and what the recipe
     * leaves (a bucket, a bottle), goes to the cook.
     */
    private void assemble(StationContext station, Player player) {
        StationState state = station.state();
        List<ItemStack> layers = layers(state);
        Optional<AssemblyRecipes.Assembly> dish = AssemblyRecipes.complete(layers);
        Assembling.Made made = dish.map(it -> Assembling.make(it, layers, station.level(), CuttingBoardStation.BATCH)).orElse(null);
        if (made == null || made.rounds() == 0) {
            refuse(station);
            return;
        }
        for (int i = 0; i < CuttingBoardStation.MOST_LAYERS; i++) {
            state.setItem(SlotRef.of(CuttingBoardStation.LAYERS, i), ItemStack.EMPTY);
        }
        made.layers().forEach(left -> give(player, left));
        made.leftovers().forEach(left -> give(player, left));
        for (ItemStack result : made.dishes()) {
            result.onCraftedBy(player, result.getCount());
            ItemStack onBoard = Platform.INSTANCE.getItem(board);
            if (onBoard.isEmpty() || ItemStack.isSameItemSameComponents(onBoard, result)) {
                int room = Math.min(result.getMaxStackSize(), board.getMaxStackSize()) - onBoard.getCount();
                int there = Math.max(0, Math.min(room, result.getCount()));
                if (there > 0) {
                    Platform.INSTANCE.setItem(board, result.copyWithCount(onBoard.getCount() + there));
                    result.shrink(there);
                }
            }
            give(player, result);
        }
        CompoundTag how = new CompoundTag();
        ListTag from = new ListTag();
        layers.forEach(layer -> from.add(KitchenNbt.save(layer.copyWithCount(1), station.level().registryAccess())));
        how.put(CuttingBoardStation.LAYERS, from);
        how.putString(CuttingBoardStation.FINISH, dish.get().finish().name());
        how.putBoolean(CuttingBoardStation.BLOCK, dish.get().isBlock());
        station.effect(CuttingBoardStation.EFFECT_ASSEMBLED, "item", how);
    }

    private static void refuse(StationContext station) {
        refuse(station, ItemStack.EMPTY);
    }

    /** The board will not have it: with a {@code stack}, that stack bounces back to the cook's hand. */
    private static void refuse(StationContext station, ItemStack stack) {
        CompoundTag data = new CompoundTag();
        if (!stack.isEmpty()) {
            data.put(CuttingBoardStation.STACK, KitchenNbt.save(stack.copyWithCount(1), station.level().registryAccess()));
        }
        station.effect(CuttingBoardStation.EFFECT_REFUSED, "surface", data);
    }

    /** What came off the board, from which layer (-1: what lay to be cut, or the dish), for the clients to lift it off. */
    private static void taken(StationContext station, ItemStack stack, int layer) {
        CompoundTag data = new CompoundTag();
        data.put(CuttingBoardStation.STACK, KitchenNbt.save(stack, station.level().registryAccess()));
        data.putInt(CuttingBoardStation.AT, layer);
        station.effect(CuttingBoardStation.EFFECT_TAKEN, "surface", data);
    }

    /**
     * No cuts on the item yet, and new marks for it, as many as the pieces it falls into ask (one cut for two pieces), at even places
     * a little askew. A spot is a fraction of the item's length from its far end; an angle is the client's to turn into a line on the
     * icon (see {@link Cutting.Style}): across and wedges get a share from -1 to 1 of how far they may lean, a roll and strips none;
     * wedges get their turn about the middle in radians.
     */
    private void restart(StationContext station) {
        marks = 0;
        ItemStack onBoard = Platform.INSTANCE.getItem(board);
        plan = onBoard.isEmpty() ? new Cutting.Plan(1, Cutting.Style.ACROSS) : Cutting.plan(station.level(), onBoard);
        RandomSource random = station.level().getRandom();
        int count = plan.marks();
        // Each mark has an even share of the length and lies near the middle of it.
        float room = (SPOT_TO - SPOT_FROM) / count;
        float turn = random.nextFloat() * Mth.PI;
        for (int mark = 0; mark < count; mark++) {
            switch (plan.style()) {
                case WEDGES -> {
                    // Through the middle, evenly round it: two cuts make four wedges.
                    spots[mark] = 0.5F;
                    angles[mark] = turn + mark * Mth.PI / count;
                }
                case STRAIGHT, STRIPS -> {
                    // Even slices of a roll, close strips of pasta: square across, at even places.
                    float span = plan.style() == Cutting.Style.STRIPS ? 0.5F : 1.0F;
                    spots[mark] = 0.5F + ((mark + 1.0F) / (count + 1.0F) - 0.5F) * span;
                    angles[mark] = 0.0F;
                }
                case ACROSS -> {
                    float even = SPOT_FROM + (mark + 0.5F) * room;
                    float stray = count == 1 ? 0.15F : room * SPOT_JITTER;
                    spots[mark] = even + Mth.nextFloat(random, -stray, stray);
                    angles[mark] = Mth.nextFloat(random, -1.0F, 1.0F);
                }
            }
        }
        if (plan.style() == Cutting.Style.ACROSS) {
            // Cut in no fixed order from one end to the other; lean so two never meet on the item.
            for (int attempt = 0; attempt < 30 && crosses(count); attempt++) {
                for (int mark = 0; mark < count; mark++) {
                    angles[mark] = Mth.nextFloat(random, -1.0F, 1.0F);
                }
            }
            if (crosses(count)) {
                java.util.Arrays.fill(angles, 0, count, angles[0]);
            }
            for (int mark = count - 1; mark > 0; mark--) {
                int other = random.nextInt(mark + 1);
                float spot = spots[mark];
                spots[mark] = spots[other];
                spots[other] = spot;
                float angle = angles[mark];
                angles[mark] = angles[other];
                angles[other] = angle;
            }
        }
    }

    private boolean crosses(int count) {
        for (int a = 0; a < count; a++) {
            for (int b = a + 1; b < count; b++) {
                if (crossOnTheItem(spots[a], angles[a] * MOST_LEAN, spots[b], angles[b] * MOST_LEAN)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How far a mark across leans from square at most, radians: the client leans less on a long item, never more. */
    public static final float MOST_LEAN = 35.0F * Mth.DEG_TO_RAD;

    /**
     * Whether two marks meet on the item, taken as a square with its length from far to near: each runs through the middle of the
     * item's width at its spot, the angle from square across.
     */
    public static boolean crossOnTheItem(float spotA, float angleA, float spotB, float angleB) {
        float ax = Mth.cos(angleA);
        float az = Mth.sin(angleA);
        float bx = Mth.cos(angleB);
        float bz = Mth.sin(angleB);
        float across = ax * bz - az * bx;
        if (Math.abs(across) < 1.0e-4F) {
            return false;
        }
        // Where along mark A it meets mark B.
        float t = (0.0F * bz - (spotB - spotA) * bx) / across;
        float x = 0.5F + t * ax;
        float z = spotA + t * az;
        return x >= 0.0F && x <= 1.0F && z >= 0.0F && z <= 1.0F;
    }

    /** How the item lying there is cut. */
    public Cutting.Plan plan() {
        return plan;
    }

    /** Where the marks of the item lying there are now, fractions from its far end. */
    public float[] spots() {
        return spots.clone();
    }

    /** How the marks lie: see {@link #restart}. */
    public float[] angles() {
        return angles.clone();
    }

    /** What is left of the stack goes back where it came from: the cursor, or its inventory slot. */
    private static void putBack(Player player, int from, ItemStack stack) {
        if (from >= 0) {
            player.getInventory().setItem(from, stack.isEmpty() ? ItemStack.EMPTY : stack);
        } else {
            player.inventoryMenu.setCarried(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    /** Into the inventory, what does not fit at the cook's feet, in stacks no bigger than the item allows (eight cookies a round). */
    private static void give(Player player, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack part = stack.split(stack.getMaxStackSize());
            if (!player.getInventory().add(part)) {
                player.drop(part, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
    }

    /** The cuts and the dish the layers make, for the clients; sent only when they change. */
    private void publish(StationContext station) {
        StationState state = station.state();
        CompoundTag data = new CompoundTag();
        if (marks > 0) {
            data.putInt(CuttingBoardStation.MARKS, marks);
        }
        if (!Platform.INSTANCE.getItem(board).isEmpty() && layers(state).isEmpty()) {
            ListTag where = new ListTag();
            ListTag how = new ListTag();
            for (int mark = 0; mark < plan.marks(); mark++) {
                where.add(FloatTag.valueOf(spots[mark]));
                how.add(FloatTag.valueOf(angles[mark]));
            }
            data.put(CuttingBoardStation.SPOTS, where);
            data.put(CuttingBoardStation.ANGLES, how);
        }
        AssemblyRecipes.complete(layers(state)).ifPresent(dish -> data.putString(CuttingBoardStation.DISH,
                BuiltInRegistries.ITEM.getKey(dish.result().getItem()).toString()));
        if (!data.equals(state.data())) {
            state.setData(data);
        }
    }
}
