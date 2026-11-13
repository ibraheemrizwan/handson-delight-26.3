package com.pockyl.handson_delight.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.assembly.AssemblyRecipes;

import java.util.ArrayList;
import java.util.List;

/**
 * How the recipe viewers draw a dish put together on the board, the same in JEI, EMI and REI: what goes down first lies on the board,
 * as the
 * food does in Farmer's Delight's own cutting category, the other layers stand beside it in slots, then the arrow and the dish. The
 * board, slots and arrow are Farmer's Delight's, read from its texture.
 */
public final class AssemblyLayout {
    public static final Identifier CATEGORY = HandsOnDelight.id("assembly");
    public static final Identifier TEXTURE = FarmersDelightContent.id("textures/gui/jei/cutting_board.png");
    public static final int WIDTH = 154;
    public static final int HEIGHT = 54;

    /** The board in the texture, and where it is drawn. */
    public static final int BOARD_U = 4;
    public static final int BOARD_V = 29;
    public static final int BOARD_WIDTH = 40;
    public static final int BOARD_HEIGHT = 22;
    public static final int BOARD_X = 4;
    public static final int BOARD_Y = 29;
    /** What goes down first, on the board: the item's corner. */
    public static final int BASE_X = 16;
    public static final int BASE_Y = 27;
    public static final int SLOT_U = 0;
    public static final int SLOT_V = 58;
    public static final int SLOT_SIZE = 18;
    public static final int ARROW_U = 48;
    public static final int ARROW_V = 21;
    public static final int ARROW_WIDTH = 22;
    public static final int ARROW_HEIGHT = 16;
    public static final int ARROW_X = 109;
    public static final int ARROW_Y = 19;
    /** The dish's slot frame. */
    public static final int RESULT_X = 136;
    public static final int RESULT_Y = 18;

    private static final int LAYERS_X = 50;
    private static final int COLUMNS = 3;

    private AssemblyLayout() {
    }

    public static Component title() {
        return Component.translatable("handson_delight.assembly");
    }

    public static Component goesDownFirst() {
        return Component.translatable("handson_delight.assembly.first");
    }

    public static ItemStack icon() {
        return new ItemStack(FarmersDelightContent.CUTTING_BOARD.get());
    }

    public static Ingredient base(AssemblyRecipes.Assembly dish) {
        return dish.ingredients().get(dish.base());
    }

    /** Everything that goes on it, in the recipe's order. */
    public static List<Ingredient> layers(AssemblyRecipes.Assembly dish) {
        List<Ingredient> layers = new ArrayList<>(dish.ingredients());
        layers.remove(dish.base());
        return layers;
    }

    public static ItemStack result(AssemblyRecipes.Assembly dish) {
        return dish.result();
    }

    /** The corner of the frame of layer {@code index} of {@code count}: three in a row, the rows centred. */
    public static int layerX(int index) {
        return LAYERS_X + index % COLUMNS * SLOT_SIZE;
    }

    public static int layerY(int index, int count) {
        int rows = (count + COLUMNS - 1) / COLUMNS;
        return (HEIGHT - rows * SLOT_SIZE) / 2 + index / COLUMNS * SLOT_SIZE;
    }
}
