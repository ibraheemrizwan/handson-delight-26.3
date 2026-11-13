package com.pockyl.handson_delight;

import com.pockyl.handson_delight.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * What a modpack can turn off, read once at startup from {@code config/handson_delight.properties} (the same file on every loader).
 * On a server it decides for everybody: a station turned off there opens on no client, its block keeps Farmer's Delight's own use.
 */
public final class HandsOnConfig {
    private static final String FILE = HandsOnDelight.MOD_ID + ".properties";
    private static final String HEADER = String.join("\n",
            "Hands-On Delight. Every value is true or false; restart the game or the server after a change.",
            "cooking_pot, cutting_board, skillet, stove: the station of that block. Off, the block works as in Farmer's Delight.",
            "remove_crafting_recipes: Farmer's Delight's food crafts (sandwiches, salads, rolls, pies, feasts) are made on the",
            "  cutting board only, not in a crafting grid.",
            "  Machines (Create, AE2, Refined Storage) craft them either way.",
            "stirring_required: food a cook puts into the pot waits for the spoon while the cook is at it. Off, it also cooks",
            "  unstirred, Farmer's Delight's way; stirring still cooks the whole pot at once and earns stars.",
            "recipe_book: the recipe book beside the stations. Each player's own: read on their client.");

    private static boolean cookingPot = true;
    private static boolean cuttingBoard = true;
    private static boolean skillet = true;
    private static boolean stove = true;
    private static boolean removeCraftingRecipes = true;
    private static boolean stirringRequired = true;
    private static boolean recipeBook = true;

    private HandsOnConfig() {
    }

    static void load() {
        Path path = Platform.INSTANCE.configDir().resolve(FILE);
        Properties values = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                values.load(reader);
            } catch (IOException | IllegalArgumentException exception) {
                HandsOnDelight.LOGGER.warn("Could not read {}, using the defaults", path, exception);
            }
        }
        cookingPot = read(values, "cooking_pot", cookingPot);
        cuttingBoard = read(values, "cutting_board", cuttingBoard);
        skillet = read(values, "skillet", skillet);
        stove = read(values, "stove", stove);
        removeCraftingRecipes = read(values, "remove_crafting_recipes", removeCraftingRecipes);
        stirringRequired = read(values, "stirring_required", stirringRequired);
        recipeBook = read(values, "recipe_book", recipeBook);
        // Written back every start, so a file from an older version gets the new keys.
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                values.store(writer, HEADER);
            }
        } catch (IOException exception) {
            HandsOnDelight.LOGGER.warn("Could not write {}", path, exception);
        }
    }

    private static boolean read(Properties values, String key, boolean fallback) {
        String value = values.getProperty(key, String.valueOf(fallback)).trim();
        boolean read = value.equalsIgnoreCase("true") || !value.equalsIgnoreCase("false") && fallback;
        values.setProperty(key, String.valueOf(read));
        return read;
    }

    public static boolean cookingPot() {
        return cookingPot;
    }

    public static boolean cuttingBoard() {
        return cuttingBoard;
    }

    public static boolean skillet() {
        return skillet;
    }

    public static boolean stove() {
        return stove;
    }

    /** Whether board dishes leave players' crafting grids; never without the board to make them. */
    public static boolean removeCraftingRecipes() {
        return removeCraftingRecipes && cuttingBoard;
    }

    public static boolean stirringRequired() {
        return stirringRequired && cookingPot;
    }

    /** Whether the recipe book hangs beside the stations; the client's own file decides (owner, 2026-11-08). */
    public static boolean recipeBook() {
        return recipeBook;
    }
}
