package com.pockyl.handson_delight.fabric.rei;

import dev.architectury.event.EventResult;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.pockyl.handson_delight.assembly.AssemblyRecipes.Assembly;
import com.pockyl.handson_delight.assembly.BoardDishes;
import com.pockyl.handson_delight.compat.AssemblyLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * REI shows the sandwiches as put together on the cutting board, and no longer in the crafting table. Fabric only: on NeoForge, REI
 * reads the JEI plugin.
 */
public final class HandsOnReiPlugin implements REIClientPlugin {
    private static final CategoryIdentifier<AssemblyDisplay> CATEGORY = CategoryIdentifier.of(AssemblyLayout.CATEGORY);
    private static final CategoryIdentifier<?> CRAFTING = CategoryIdentifier.of("minecraft", "plugins/crafting");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new AssemblyCategory());
        registry.addWorkstations(CATEGORY, EntryStacks.of(AssemblyLayout.icon()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (Assembly dish : BoardDishes.all()) {
            registry.add(new AssemblyDisplay(dish));
        }
        Set<Identifier> ids = BoardDishes.ids();
        registry.registerVisibilityPredicate((category, display) -> category.getCategoryIdentifier().equals(CRAFTING)
                && display.getDisplayLocation().filter(ids::contains).isPresent() ? EventResult.interruptFalse() : EventResult.pass());
    }

    private static final class AssemblyDisplay extends BasicDisplay {
        private final int layers;
        @Override public me.shedaniel.rei.api.common.display.DisplaySerializer<AssemblyDisplay> getSerializer() { return null; }

        AssemblyDisplay(Assembly dish) {
            super(inputs(dish), List.of(EntryIngredients.of(AssemblyLayout.result(dish))));
            layers = dish.ingredients().size() - 1;
        }

        private static List<EntryIngredient> inputs(Assembly dish) {
            List<EntryIngredient> inputs = new ArrayList<>();
            inputs.add(EntryIngredients.ofIngredient(AssemblyLayout.base(dish)));
            inputs.addAll(EntryIngredients.ofIngredients(AssemblyLayout.layers(dish)));
            return inputs;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return CATEGORY;
        }
    }

    private static final class AssemblyCategory implements DisplayCategory<AssemblyDisplay> {
        /** REI's frame around a recipe. */
        private static final int BORDER = 4;

        @Override
        public CategoryIdentifier<? extends AssemblyDisplay> getCategoryIdentifier() {
            return CATEGORY;
        }

        @Override
        public Component getTitle() {
            return AssemblyLayout.title();
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(AssemblyLayout.icon());
        }

        @Override
        public int getDisplayWidth(AssemblyDisplay display) {
            return AssemblyLayout.WIDTH + 2 * BORDER;
        }

        @Override
        public int getDisplayHeight() {
            return AssemblyLayout.HEIGHT + 2 * BORDER;
        }

        @Override
        public List<Widget> setupDisplay(AssemblyDisplay display, Rectangle bounds) {
            int x = bounds.x + BORDER;
            int y = bounds.y + BORDER;
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createTexturedWidget(AssemblyLayout.TEXTURE, x + AssemblyLayout.BOARD_X, y + AssemblyLayout.BOARD_Y,
                    AssemblyLayout.BOARD_U, AssemblyLayout.BOARD_V, AssemblyLayout.BOARD_WIDTH, AssemblyLayout.BOARD_HEIGHT));
            widgets.add(Widgets.createTexturedWidget(AssemblyLayout.TEXTURE, x + AssemblyLayout.ARROW_X, y + AssemblyLayout.ARROW_Y,
                    AssemblyLayout.ARROW_U, AssemblyLayout.ARROW_V, AssemblyLayout.ARROW_WIDTH, AssemblyLayout.ARROW_HEIGHT));
            List<EntryIngredient> inputs = display.getInputEntries();
            widgets.add(Widgets.createSlot(new Point(x + AssemblyLayout.BASE_X, y + AssemblyLayout.BASE_Y)).disableBackground().markInput()
                    .entries(inputs.get(0).stream().map(stack -> stack.copy().tooltip(AssemblyLayout.goesDownFirst())).toList()));
            for (int i = 0; i < display.layers; i++) {
                widgets.add(Widgets.createSlot(new Point(x + AssemblyLayout.layerX(i) + 1, y + AssemblyLayout.layerY(i, display.layers) + 1))
                        .markInput().entries(inputs.get(i + 1)));
            }
            widgets.add(Widgets.createSlot(new Point(x + AssemblyLayout.RESULT_X + 1, y + AssemblyLayout.RESULT_Y + 1)).markOutput()
                    .entries(display.getOutputEntries().get(0)));
            return widgets;
        }
    }
}
