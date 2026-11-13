package com.pockyl.handson_delight.client;

import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;

import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;
import com.pockyl.handson_delight.station.BoardLogic;
import com.pockyl.handson_delight.station.Cutting;
import com.pockyl.handson_delight.station.CuttingBoardStation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The board's book: what can be cut, listed by what comes off (a cabbage leaf: a cabbage and a knife, with everything else the cut
 * gives at the foot), and the dishes put together on it, what goes down first first. The dishes come from the station's data: a
 * dedicated server
 * sends their recipes nowhere else.
 */
final class BoardBook implements RecipeBook.Source {
    /** The dishes' recipes as the server last sent them ({@code CuttingBoardStation.EFFECT_BOOK}): the same for every board. */
    private static ListTag received = new ListTag();
    private static final String DISHES = "dishes";
    private static final String KNIFE = "knife";
    private ListTag readFrom;
    private List<RecipeBook.Page> dishes = List.of();
    /** Goes up each time the dishes are read anew: what the drawer shows depends on them. */
    private int version;

    @Override
    public List<RecipeBook.Page> pages(StationView view) {
        Level level = view.level();
        List<RecipeBook.Page> pages = new ArrayList<>(dishes(view));
        for (CuttingBoardRecipe recipe : Cutting.all(level)) {
            List<ItemStack> results = recipe.getResults().stream().map(net.minecraft.world.item.ItemStackTemplate::create).toList();
            if (results.isEmpty()) {
                continue;
            }
            List<Ingredient> ingredients = List.of(recipe.getInput(), recipe.getTool());
            List<ItemStack> foot = results.size() > 1 ? results : List.of();
            pages.add(new RecipeBook.Page(Cutting.id(level, recipe), results.get(0).copy(), ingredients, foot, null, false,
                    toolKind(recipe)));
        }
        return pages;
    }

    /** The tabs: everything, the dishes, then a tab for each tool the board cuts with, the knife first. */
    @Override
    public List<RecipeBook.Tab> tabs(StationView view) {
        List<RecipeBook.Tab> tabs = new ArrayList<>();
        // Everything under a compass, as in vanilla's recipe book.
        tabs.add(new RecipeBook.Tab(null, new ItemStack(Items.COMPASS)));
        if (!dishes(view).isEmpty()) {
            tabs.add(new RecipeBook.Tab(DISHES, new ItemStack(BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id("hamburger")))));
        }
        Set<String> tools = new LinkedHashSet<>();
        Cutting.all(view.level()).stream().map(BoardBook::toolKind).sorted(Comparator.comparing(kind -> !kind.equals(KNIFE)))
                .forEach(tools::add);
        for (String tool : tools) {
            ItemStack icon = tool.equals(KNIFE) ? new ItemStack(BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id("iron_knife")))
                    : new ItemStack(BuiltInRegistries.ITEM.get(Identifier.parse(tool)).orElseThrow().value());
            tabs.add(new RecipeBook.Tab(tool, icon));
        }
        return tabs;
    }

    @Override
    public String memory() {
        return CuttingBoardStation.ID.toString();
    }

    /** What a cut is made with: the knife, or the first item of its tool (an axe), by id. */
    private static String toolKind(CuttingBoardRecipe recipe) {
        ItemStack knife = new ItemStack(BuiltInRegistries.ITEM.getValue(FarmersDelightContent.id("iron_knife")));
        if (recipe.getTool().test(knife)) {
            return KNIFE;
        }
        ItemStack[] tools = KitchenRecipes.choices(recipe.getTool());
        return tools.length == 0 ? KNIFE : BuiltInRegistries.ITEM.getKey(tools[0].getItem()).toString();
    }

    static void received(ListTag book) {
        received = book;
    }

    private List<RecipeBook.Page> dishes(StationView view) {
        ListTag book = received;
        if (book != readFrom) {
            readFrom = book;
            List<RecipeBook.Page> read = new ArrayList<>();
            for (AssemblyRecipes.Assembly dish : AssemblyRecipes.read(book)) {
                List<Ingredient> ingredients = new ArrayList<>(dish.ingredients());
                // What goes down first is listed first.
                ingredients.add(0, ingredients.remove(dish.base()));
                ItemStack result = dish.result().copy();
                read.add(new RecipeBook.Page(dish.id(), result, ingredients, List.of(), null, false, DISHES));
            }
            dishes = read;
            version++;
        }
        return dishes;
    }

    /** The dishes as last read, a new number each time they change. */
    int version(StationView view) {
        dishes(view);
        return version;
    }

    /** Whether {@code stack} goes into one of the dishes put together on the board. */
    boolean goesIntoADish(StationView view, ItemStack stack) {
        return dishes(view).stream().anyMatch(page -> page.ingredients().stream().anyMatch(ingredient -> ingredient.test(stack)));
    }

    /**
     * A dish picked in the book is the one the board makes as soon as the layers make it, even if they could grow into more; what it
     * takes is laid from the inventory, one set or, with shift, a batch.
     */
    @Override
    public void picked(StationView view, RecipeBook.Page page) {
        // By its recipe: the dishes are read anew when the server sends them, the page may be of the list before.
        if (dishes(view).stream().anyMatch(dish -> dish.key().equals(page.key()))) {
            CompoundTag pick = new CompoundTag();
            pick.putString(CuttingBoardStation.DISH, page.key().toString());
            pick.putBoolean(CuttingBoardStation.ALL, net.minecraft.client.Minecraft.getInstance().hasShiftDown());
            view.sendAction(CuttingBoardStation.ACTION_PICK, pick);
        }
    }

    /** The dish the layers make, else what lies on the board to be cut. */
    @Override
    public Object current(StationView view) {
        String dish = view.state().data().getStringOr(CuttingBoardStation.DISH, "");
        if (!dish.isEmpty()) {
            for (RecipeBook.Page page : dishes(view)) {
                if (BuiltInRegistries.ITEM.getKey(page.dish().getItem()).toString().equals(dish)) {
                    return page.key();
                }
            }
        }
        ItemStack onBoard = view.state().getItem(CuttingBoardStation.BOARD_SLOT);
        return Cutting.recipesFor(view.level(), onBoard).stream().findFirst().map(recipe -> Cutting.id(view.level(), recipe)).orElse(null);
    }

    @Override
    public List<ItemStack> inStation(StationView view) {
        List<ItemStack> there = new ArrayList<>(BoardLogic.layers(view.state()));
        there.add(view.state().getItem(CuttingBoardStation.BOARD_SLOT));
        return there;
    }
}
