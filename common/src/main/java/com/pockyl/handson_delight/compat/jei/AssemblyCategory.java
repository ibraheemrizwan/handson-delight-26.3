package com.pockyl.handson_delight.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.handson_delight.assembly.AssemblyRecipes.Assembly;
import com.pockyl.handson_delight.compat.AssemblyLayout;

import java.util.List;

import static com.pockyl.handson_delight.compat.AssemblyLayout.ARROW_HEIGHT;
import static com.pockyl.handson_delight.compat.AssemblyLayout.ARROW_U;
import static com.pockyl.handson_delight.compat.AssemblyLayout.ARROW_V;
import static com.pockyl.handson_delight.compat.AssemblyLayout.ARROW_WIDTH;
import static com.pockyl.handson_delight.compat.AssemblyLayout.ARROW_X;
import static com.pockyl.handson_delight.compat.AssemblyLayout.ARROW_Y;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BASE_X;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BASE_Y;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BOARD_HEIGHT;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BOARD_U;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BOARD_V;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BOARD_WIDTH;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BOARD_X;
import static com.pockyl.handson_delight.compat.AssemblyLayout.BOARD_Y;
import static com.pockyl.handson_delight.compat.AssemblyLayout.CATEGORY;
import static com.pockyl.handson_delight.compat.AssemblyLayout.HEIGHT;
import static com.pockyl.handson_delight.compat.AssemblyLayout.RESULT_X;
import static com.pockyl.handson_delight.compat.AssemblyLayout.RESULT_Y;
import static com.pockyl.handson_delight.compat.AssemblyLayout.SLOT_SIZE;
import static com.pockyl.handson_delight.compat.AssemblyLayout.SLOT_U;
import static com.pockyl.handson_delight.compat.AssemblyLayout.SLOT_V;
import static com.pockyl.handson_delight.compat.AssemblyLayout.TEXTURE;
import static com.pockyl.handson_delight.compat.AssemblyLayout.WIDTH;
import static com.pockyl.handson_delight.compat.AssemblyLayout.base;
import static com.pockyl.handson_delight.compat.AssemblyLayout.goesDownFirst;
import static com.pockyl.handson_delight.compat.AssemblyLayout.layerX;
import static com.pockyl.handson_delight.compat.AssemblyLayout.layerY;
import static com.pockyl.handson_delight.compat.AssemblyLayout.layers;
import static com.pockyl.handson_delight.compat.AssemblyLayout.result;
import static com.pockyl.handson_delight.compat.AssemblyLayout.title;

/** A dish put together on the cutting board, laid out by {@link AssemblyLayout}. */
final class AssemblyCategory implements IRecipeCategory<Assembly> {
    static final RecipeType<Assembly> TYPE = new RecipeType<>(CATEGORY, Assembly.class);

    private final IDrawable board;
    private final IDrawable slot;
    private final IDrawable arrow;
    private final IDrawable icon;

    AssemblyCategory(IGuiHelper gui) {
        board = gui.createDrawable(TEXTURE, BOARD_U, BOARD_V, BOARD_WIDTH, BOARD_HEIGHT);
        slot = gui.createDrawable(TEXTURE, SLOT_U, SLOT_V, SLOT_SIZE, SLOT_SIZE);
        arrow = gui.createDrawable(TEXTURE, ARROW_U, ARROW_V, ARROW_WIDTH, ARROW_HEIGHT);
        icon = gui.createDrawableItemStack(AssemblyLayout.icon());
    }

    @Override
    public RecipeType<Assembly> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return title();
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Assembly dish, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, BASE_X, BASE_Y).addIngredients(base(dish))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(goesDownFirst()));
        List<Ingredient> layers = layers(dish);
        for (int i = 0; i < layers.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, layerX(i) + 1, layerY(i, layers.size()) + 1).addIngredients(layers.get(i))
                    .setBackground(slot, -1, -1);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X + 1, RESULT_Y + 1)
                .addIngredient(VanillaTypes.ITEM_STACK, result(dish)).setBackground(slot, -1, -1);
    }

    @Override
    public void draw(Assembly dish, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        board.draw(graphics, BOARD_X, BOARD_Y);
        arrow.draw(graphics, ARROW_X, ARROW_Y);
    }
}
