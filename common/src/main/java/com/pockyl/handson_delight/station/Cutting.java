package com.pockyl.handson_delight.station;

import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;

import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.KitchenTags;
import com.pockyl.handson_delight.mixin.RecipeManagerAccessor;
import com.pockyl.handson_delight.platform.Platform;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Farmer's Delight's cutting recipes, read for the board's station: what an item is cut with and into how many strokes. */
public final class Cutting {
    /**
     * The most marks an item gets (owner, 2026-11-09: as many cuts as the pieces it falls into, "do everything"): a cake into seven is
     * cut three times, never more.
     */
    public static final int MOST_MARKS = 3;
    /** Strips of pasta: this many cuts side by side, whatever comes out. */
    private static final int STRIPS = 3;
    /**
     * Per recipe manager: the client's and the server's of a single-player game are two. Read again when the manager's map of
     * recipes is a new one (a reload, a server's recipes): looked up every frame at the board, so never built anew in between.
     */
    private static final Map<Object, Cached> CACHE = new WeakHashMap<>();

    private Cutting() {
    }

    /** Every cutting recipe, Farmer's Delight's and its addons', wood and flowers too. */
    public static List<CuttingBoardRecipe> everything(Level level) {
        return cached(level).everything();
    }

    /**
     * The cutting recipes that are cooking, the board's station: food goes in or comes out, or what comes out goes into the pot
     * (rice, mushrooms, pasta). Wood, flowers, baskets and the rest stay with Farmer's Delight's own board.
     */
    public static List<CuttingBoardRecipe> all(Level level) {
        return cached(level).cooking();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Cached cached(Level level) {
        var manager = KitchenRecipes.access(level);
        var recipes = manager.recipes();
        synchronized (CACHE) {
            Cached cached = CACHE.get(manager);
            if (cached == null || cached.from() != recipes) {
                List<CuttingBoardRecipe> everything = new ArrayList<>();
                Map<CuttingBoardRecipe, Identifier> ids = new IdentityHashMap<>();
                List<RecipeHolder<?>> found = List.copyOf(manager.getAllOfType((RecipeType) FarmersDelightContent.CUTTING.get()));
                for (RecipeHolder<?> holder : found) {
                    if (holder.value() instanceof CuttingBoardRecipe cutting && !cutting.getInput().isEmpty()) {
                        everything.add(cutting);
                        ids.put(cutting, holder.id().identifier());
                    }
                }
                List<CookingPotRecipe> pot = Platform.INSTANCE.cookingRecipes(level).stream().map(RecipeHolder::value).toList();
                cached = new Cached(recipes, List.copyOf(everything), everything.stream().filter(recipe -> isCooking(recipe, pot)).toList(), ids);
                CACHE.put(manager, cached);
            }
            return cached;
        }
    }

    private static boolean isCooking(CuttingBoardRecipe recipe, List<CookingPotRecipe> pot) {
        for (ItemStack input : KitchenRecipes.choices(recipe.getInput())) {
            if (input.has(DataComponents.FOOD)) {
                return true;
            }
        }
        for (ItemStack result : recipe.getResults().stream().map(net.minecraft.world.item.ItemStackTemplate::create).toList()) {
            if (result.has(DataComponents.FOOD) || pot.stream().anyMatch(cooking -> cooking.input().stream().anyMatch(in -> in.test(result)))) {
                return true;
            }
        }
        return false;
    }

    /** Whether the board's own use should take {@code item}: Farmer's Delight cuts it, and it is no cooking. */
    public static boolean isOthers(Level level, ItemStack item) {
        return !item.isEmpty() && recipesFor(level, item).isEmpty()
                && everything(level).stream().anyMatch(recipe -> recipe.getInput().test(item));
    }

    /** The cutting recipes read from a recipe manager's map of recipes, as long as the map is the same one. */
    private record Cached(Object from, List<CuttingBoardRecipe> everything, List<CuttingBoardRecipe> cooking,
                          Map<CuttingBoardRecipe, Identifier> ids) {
    }

    /** The id {@code recipe} is known by, now that a recipe holds none of its own. */
    public static Identifier id(Level level, CuttingBoardRecipe recipe) {
        return cached(level).ids().get(recipe);
    }

    public static List<CuttingBoardRecipe> recipesFor(Level level, ItemStack item) {
        List<CuttingBoardRecipe> found = new ArrayList<>();
        if (!item.isEmpty()) {
            all(level).stream().filter(recipe -> recipe.getInput().test(item)).forEach(found::add);
        }
        return found;
    }

    /** How the marks of an item lie on it. */
    public enum Style {
        /** Across its length, a little askew each time: meat, fish, a cabbage. */
        ACROSS,
        /** Square across its length, in even slices: a roll. */
        STRAIGHT,
        /** Through its middle, crossing each other: a pie, a cake. */
        WEDGES,
        /** Square across, close side by side: pasta from dough. */
        STRIPS
    }

    /** How {@code item} is cut: in how many marks, laid how. Both sides read the same recipes and tags, so they agree. */
    public record Plan(int marks, Style style) {
    }

    public static Plan plan(Level level, ItemStack item) {
        Style style = item.is(KitchenTags.CUT_IN_WEDGES) ? Style.WEDGES
                : item.is(KitchenTags.CUT_IN_STRIPS) ? Style.STRIPS
                : item.is(KitchenTags.SLICED_STRAIGHT) ? Style.STRAIGHT : Style.ACROSS;
        if (style == Style.STRIPS) {
            return new Plan(STRIPS, style);
        }
        int pieces = recipesFor(level, item).stream().findFirst().map(Cutting::pieces).orElse(2);
        // n cuts across make n + 1 pieces; n cuts through the middle make 2n wedges.
        int marks = style == Style.WEDGES ? (pieces + 1) / 2 : pieces - 1;
        return new Plan(Mth.clamp(marks, 1, MOST_MARKS), style);
    }

    /** What an item falls into: the main result as many as it gives, and one piece for each thing besides (the bone of a ham). */
    static int pieces(CuttingBoardRecipe recipe) {
        List<ItemStack> results = recipe.getResults().stream().map(net.minecraft.world.item.ItemStackTemplate::create).toList();
        if (results.isEmpty()) {
            return 2;
        }
        return results.get(0).getCount() + results.size() - 1;
    }

    /** The inventory slot whose tool cuts {@code item}: the one in hand first, then the hotbar and the rest; -1 without one. */
    public static int toolSlot(Level level, ItemStack item, Inventory inventory) {
        if (findWith(level, item, inventory.getSelectedItem()).isPresent()) {
            return inventory.getSelectedSlot();
        }
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (findWith(level, item, inventory.getItem(slot)).isPresent()) {
                return slot;
            }
        }
        return -1;
    }

    public static Optional<CuttingBoardRecipe> findWith(Level level, ItemStack item, ItemStack tool) {
        return tool.isEmpty() ? Optional.empty() : recipesFor(level, item).stream().filter(recipe -> recipe.getTool().test(tool)).findFirst();
    }
}
