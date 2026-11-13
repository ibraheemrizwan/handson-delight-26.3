package com.pockyl.handson_delight.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.assembly.AssemblyRecipes.Assembly;
import com.pockyl.handson_delight.assembly.BoardDishes;
import com.pockyl.handson_delight.compat.AssemblyLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * EMI shows the sandwiches as put together on the cutting board, and no longer in the crafting table. Found by its annotation on
 * NeoForge, by the {@code emi} entry point on Fabric. The category has the id of the JEI one, so EMI does not show JEI's as well.
 */
@EmiEntrypoint
public final class HandsOnEmiPlugin implements EmiPlugin {
    private static final EmiStack BOARD = EmiStack.of(AssemblyLayout.icon());
    private static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(AssemblyLayout.CATEGORY, BOARD) {
        @Override
        public Component getName() {
            return AssemblyLayout.title();
        }
    };

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, BOARD);
        List<Assembly> dishes = BoardDishes.all();
        Set<Identifier> ids = BoardDishes.ids();
        registry.removeRecipes(recipe -> recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && ids.contains(recipe.getId()));
        for (Assembly dish : dishes) {
            registry.addRecipe(new AssemblyRecipe(dish));
        }
    }

    private static final class AssemblyRecipe extends BasicEmiRecipe {
        private final EmiIngredient base;
        private final List<EmiIngredient> layers = new ArrayList<>();

        AssemblyRecipe(Assembly dish) {
            // EMI wants ids of its own: the crafting recipe keeps the dish's.
            super(CATEGORY, HandsOnDelight.id("/" + dish.id().getNamespace() + "/" + dish.id().getPath()),
                    AssemblyLayout.WIDTH, AssemblyLayout.HEIGHT);
            base = EmiIngredient.of(AssemblyLayout.base(dish));
            for (Ingredient layer : AssemblyLayout.layers(dish)) {
                layers.add(EmiIngredient.of(layer));
            }
            inputs.add(base);
            inputs.addAll(layers);
            catalysts.add(BOARD);
            outputs.add(EmiStack.of(AssemblyLayout.result(dish)));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(AssemblyLayout.TEXTURE, AssemblyLayout.BOARD_X, AssemblyLayout.BOARD_Y, AssemblyLayout.BOARD_WIDTH,
                    AssemblyLayout.BOARD_HEIGHT, AssemblyLayout.BOARD_U, AssemblyLayout.BOARD_V);
            widgets.addTexture(AssemblyLayout.TEXTURE, AssemblyLayout.ARROW_X, AssemblyLayout.ARROW_Y, AssemblyLayout.ARROW_WIDTH,
                    AssemblyLayout.ARROW_HEIGHT, AssemblyLayout.ARROW_U, AssemblyLayout.ARROW_V);
            widgets.addSlot(base, AssemblyLayout.BASE_X - 1, AssemblyLayout.BASE_Y - 1).drawBack(false)
                    .appendTooltip(AssemblyLayout.goesDownFirst());
            for (int i = 0; i < layers.size(); i++) {
                widgets.addSlot(layers.get(i), AssemblyLayout.layerX(i), AssemblyLayout.layerY(i, layers.size()))
                        .backgroundTexture(AssemblyLayout.TEXTURE, AssemblyLayout.SLOT_U, AssemblyLayout.SLOT_V);
            }
            widgets.addSlot(outputs.get(0), AssemblyLayout.RESULT_X, AssemblyLayout.RESULT_Y)
                    .backgroundTexture(AssemblyLayout.TEXTURE, AssemblyLayout.SLOT_U, AssemblyLayout.SLOT_V).recipeContext(this);
        }
    }
}
