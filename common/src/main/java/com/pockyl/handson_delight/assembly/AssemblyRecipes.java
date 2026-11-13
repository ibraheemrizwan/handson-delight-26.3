package com.pockyl.handson_delight.assembly;

import com.pockyl.handson_delight.KitchenRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import vectorwing.farmersdelight.common.block.FeastBlock;
import vectorwing.farmersdelight.common.block.PieBlock;

import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnDelight;
import com.pockyl.handson_delight.KitchenTags;
import com.pockyl.handson_delight.platform.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Food that Farmer's Delight and its addons make in the crafting table and that is put together on the cutting board instead. The
 * recipes are found when the server loads them and kept here as they were, with the same ingredients and result, so that an addon's
 * dish needs nothing from us. They stay among the server's recipes for automation (Create's crafters, AE2, Refined Storage): only
 * a player's own crafting grid does not make them, and no player learns them for the recipe book.
 *
 * <p>The rule: a crafting recipe of Farmer's Delight or of a mod that needs it, made of more than one kind of thing, whose result is
 * food, a drink made of food, a feast, a pie or a cake. Slices back into a whole pie, crates opened and leaves pressed back into a
 * cabbage are one kind of thing and stay in the crafting table. Item tags {@link #ASSEMBLED} and {@link #NOT_ASSEMBLED} on the
 * result correct the rule.
 */
public final class AssemblyRecipes {
    /** Results whose recipes are put together on the board although the rule would leave them in the crafting table. */
    public static final TagKey<Item> ASSEMBLED = TagKey.create(Registries.ITEM, HandsOnDelight.id("assembled"));
    /** Results whose recipes stay in the crafting table although the rule would take them. */
    public static final TagKey<Item> NOT_ASSEMBLED = TagKey.create(Registries.ITEM, HandsOnDelight.id("not_assembled"));

    /** Farmer's Delight's dough from water is a recipe type of its own, with nothing to read: the board knows what it takes. */
    private static final String DOUGH = FarmersDelightContent.id("dough").toString();
    private static final String SHAPELESS = "minecraft:crafting_shapeless";
    private static final String SHAPED = "minecraft:crafting_shaped";
    private static final int GRID = 3;

    /** Every recipe that may be put together, as the recipe manager loaded it; the tags choose among them once they are bound. */
    private static volatile List<Assembly> candidates = List.of();
    private static volatile Chosen chosen;

    private AssemblyRecipes() {
    }

    /** How a dish is finished once its layers make it: what the cook's hands would do. */
    public enum Finish {
        /** Pressed together from the top: a pie into its crust, a juice, a cake. */
        PRESS,
        /** Tossed in the bowl: a salad. */
        TOSS,
        /** Rolled up: a kelp roll, a wrap round its filling. */
        ROLL,
        /** Pushed along the stick, from its tip down. */
        SKEWER,
        /** Kneaded. */
        KNEAD,
        /** Laid in the bowl, each piece in its place: a plated meal, a feast. */
        ARRANGE,
        /** The bread is split and its top half closes the filling: a sandwich, a burger. */
        SANDWICH,
        /** Rice pressed into a block, the slices laid along its top: nigiri. */
        SHAPE,
        /** Fruit frozen together round the top of the stick: a popsicle. */
        FREEZE
    }

    /**
     * What goes down first, found by name among the ingredients in this order: it says what kind of dish it is. A recipe with none
     * of them starts with its first ingredient.
     */
    public enum Base {
        BOWL("bowl", Finish.TOSS),
        BOTTLE("glass_bottle", Finish.PRESS),
        BREAD("bread", Finish.PRESS),
        CRUST("pie_crust", Finish.PRESS),
        STICK("stick", Finish.SKEWER),
        KELP("kelp", Finish.ROLL),
        RICE("rice", Finish.ROLL),
        WHEAT("wheat", Finish.KNEAD),
        FIRST(null, Finish.PRESS);

        private final String word;
        private final Finish finish;

        Base(String word, Finish finish) {
            this.word = word;
            this.finish = finish;
        }

        public Finish finish() {
            return finish;
        }
    }

    /**
     * A dish put together on the board: everything lies on ingredient {@code base}, which goes down first. {@code ingredients} are the
     * recipe's own, without the empty places of a shaped one; {@code slots}: where each lies in a 3 x 3 crafting grid, so the recipe
     * itself can make the dish and say what is left (a bucket, a bottle). {@code json}: the recipe as the clients read it.
     */
    public record Assembly(Identifier id, CraftingRecipe recipe, List<Ingredient> ingredients, int[] slots, int base, Base kind,
            ItemStack result, JsonObject json) {
        /**
         * What the cook's hands do: a bowl of plants is tossed and anything else in a bowl arranged; a pie or a cake is pressed
         * whatever goes down first; a stick with ice freezes, bread closes over the filling or is rolled round it, rice is shaped.
         */
        public Finish finish() {
            if (kind == Base.BOWL) {
                return isTossed() ? Finish.TOSS : Finish.ARRANGE;
            }
            if (isBlock()) {
                return Finish.PRESS;
            }
            return switch (kind) {
                case STICK -> result.is(KitchenTags.FROZEN) || ingredients.stream().anyMatch(in -> in.test(new ItemStack(Items.ICE)))
                        ? Finish.FREEZE : Finish.SKEWER;
                case BREAD -> result.is(KitchenTags.WRAPPED) ? Finish.ROLL : Finish.SANDWICH;
                case RICE -> Finish.SHAPE;
                default -> kind.finish();
            };
        }

        /** A salad: tagged so, or, untagged, a bowl of nothing but plants. Meat, fish and eggs are laid in their places. */
        private boolean isTossed() {
            if (result.is(KitchenTags.TOSSED)) {
                return true;
            }
            if (result.is(KitchenTags.ARRANGED) || isBlock()) {
                return false;
            }
            for (int i = 0; i < ingredients.size(); i++) {
                ItemStack[] choices = KitchenRecipes.choices(ingredients.get(i));
                if (i == base || choices.length == 0) {
                    continue;
                }
                ItemStack food = choices[0];
                boolean meat = food.is(ItemTags.MEAT);
                String name = BuiltInRegistries.ITEM.getKey(food.getItem()).getPath();
                if (meat || food.is(ItemTags.FISHES) || food.is(KitchenTags.CRACKED) || name.contains("egg") || name.contains("bacon")) {
                    return false;
                }
            }
            return true;
        }

        /** Whether the dish is a block put down whole: a feast, a pie, a cake. */
        public boolean isBlock() {
            return result.getItem() instanceof BlockItem;
        }
    }

    /** The recipes chosen among the candidates, and the list the clients get. */
    private record Chosen(List<Assembly> all, Set<Identifier> ids, ListTag book) {
    }

    public static List<Assembly> all() {
        return chosen().all();
    }

    /** Every recipe as its id and JSON, for {@link #read}. */
    public static ListTag book() {
        return chosen().book();
    }

    /**
     * Whether the recipe {@code id} is made on the board: a player's crafting grid does not make it, shaped, shapeless or of a type
     * of its own ({@code CraftingMenuMixin}).
     */
    public static boolean isMadeOnTheBoard(Identifier id) {
        return chosen().ids().contains(id);
    }

    /**
     * Chosen when first asked after the recipes or the tags changed: while the recipes load, the item tags are not bound yet. The
     * server asks first when a player joins, or when it sends the reloaded recipes, after the tags.
     */
    private static Chosen chosen() {
        Chosen now = chosen;
        if (now == null) {
            synchronized (AssemblyRecipes.class) {
                now = chosen;
                if (now == null) {
                    now = choose(candidates);
                    chosen = now;
                }
            }
        }
        return now;
    }

    /** The tags may have changed: the recipes are chosen again when next asked. */
    public static void tagsChanged() {
        chosen = null;
    }

    private static Chosen choose(List<Assembly> from) {
        List<Assembly> all = new ArrayList<>();
        ListTag book = new ListTag();
        for (Assembly assembly : from) {
            ItemStack result = assembly.result();
            boolean taken = result.is(ASSEMBLED) || !result.is(NOT_ASSEMBLED) && (isDish(result) || isDrinkOfFood(assembly));
            if (!taken) {
                continue;
            }
            all.add(assembly);
            CompoundTag entry = new CompoundTag();
            entry.putString("id", assembly.id().toString());
            entry.putString("json", assembly.json().toString());
            book.add(entry);
        }
        return new Chosen(List.copyOf(all), Set.copyOf(all.stream().map(Assembly::id).toList()), book);
    }

    /** Food, or a block of food put down whole. */
    private static boolean isDish(ItemStack result) {
        if (result.has(DataComponents.FOOD)) {
            return true;
        }
        return result.getItem() instanceof BlockItem item && isWholeFood(item.getBlock());
    }

    /** A drink made of food (melon juice); milk poured from a bucket into bottles is no dish. */
    private static boolean isDrinkOfFood(Assembly assembly) {
        return assembly.result().getUseAnimation() == ItemUseAnimation.DRINK && assembly.ingredients().stream()
                .anyMatch(ingredient -> Arrays.stream(KitchenRecipes.choices(ingredient)).anyMatch(item -> item.has(DataComponents.FOOD)));
    }

    private static boolean isWholeFood(Block block) {
        return block instanceof FeastBlock || block instanceof PieBlock || block instanceof CakeBlock;
    }

    /**
     * The recipes {@link #book} lists, read where they are needed: on a client its tags are bound and its items known. Their
     * ingredients and results name items and tags only, which the built-in registries know.
     */
    public static List<Assembly> read(ListTag list) {
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        List<Assembly> read = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            Identifier id = Identifier.tryParse(entry.getStringOr("id", ""));
            try {
                JsonObject json = JsonParser.parseString(entry.getStringOr("json", "")).getAsJsonObject();
                if (id != null) {
                    parse(id, json, registries).ifPresent(read::add);
                }
            } catch (RuntimeException broken) {
                HandsOnDelight.LOGGER.warn("Could not read the board's recipe {}", id, broken);
            }
        }
        return read;
    }

    /**
     * Finds the recipes of {@code json} that the board may make; all of them go on to the recipe manager. Their ingredients are only
     * read as JSON here: item tags are not bound yet while recipes load.
     */
    public static void collect(Map<Identifier, JsonElement> json, Map<Identifier, JsonObject> found) {
        json.forEach((id, element) -> {
            if (element.isJsonObject()) {
                candidate(id, element.getAsJsonObject()).ifPresent(recipe -> found.put(id, recipe));
            }
        });
    }

    /** Reads the recipes {@link #collect} found, after the recipe manager has read its own: only those it {@code loaded}. */
    public static void load(Map<Identifier, JsonObject> taken, Predicate<Identifier> loaded,
            HolderLookup.Provider registries) {
        List<Assembly> read = new ArrayList<>();
        taken.forEach((id, json) -> {
            if (!loaded.test(id)) {
                return;
            }
            try {
                parse(id, json, registries).ifPresent(read::add);
            } catch (RuntimeException broken) {
                HandsOnDelight.LOGGER.error("Could not read the recipe {} for the cutting board", id, broken);
            }
        });
        candidates = List.copyOf(read);
        chosen = null;
        HandsOnDelight.LOGGER.info("{} crafting recipes of Farmer's Delight may be put together on the cutting board", read.size());
    }

    /** The recipe as the board reads it, when it is one the board could make: else empty. */
    private static Optional<JsonObject> candidate(Identifier id, JsonObject json) {
        String type = string(json, "type");
        if (!id.getNamespace().equals(FarmersDelightContent.MOD_ID) && !Platform.INSTANCE.isFarmersDelightAddon(id.getNamespace())) {
            return Optional.empty();
        }
        JsonObject recipe = json;
        if (DOUGH.equals(type)) {
            // One wheat and a water bucket, which stays: the recipe's own matches and remainders say so, the board only lays them.
            recipe = json.deepCopy();
            JsonArray ingredients = new JsonArray();
            ingredients.add("minecraft:wheat");
            ingredients.add("minecraft:water_bucket");
            recipe.add("ingredients", ingredients);
            JsonObject result = new JsonObject();
            result.addProperty("id", FarmersDelightContent.id("wheat_dough").toString());
            recipe.add("result", result);
        } else if (!SHAPELESS.equals(type) && !SHAPED.equals(type)) {
            return Optional.empty();
        }
        List<JsonElement> named = named(recipe);
        if (named == null || named.size() < 2 || named.size() > GRID * GRID || named.stream().allMatch(named.get(0)::equals)) {
            return Optional.empty();
        }
        JsonElement result = recipe.get("result");
        String name = result == null ? null : result.isJsonObject() ? resultName(result.getAsJsonObject())
                : result.isJsonPrimitive() ? result.getAsString() : null;
        Identifier item = name == null ? null : Identifier.tryParse(name);
        return item != null && BuiltInRegistries.ITEM.containsKey(item) ? Optional.of(recipe) : Optional.empty();
    }

    /** A result names its item as {@code id}, as 1.21 does, or as {@code item}, as some addons still write it. */
    private static String resultName(JsonObject result) {
        String id = string(result, "id");
        return id != null ? id : string(result, "item");
    }

    /** What {@code RecipeManager.fromJson} does, which is not public, and what the board needs of the recipe. */
    private static Optional<Assembly> parse(Identifier id, JsonObject json, HolderLookup.Provider registries) {
        DynamicOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Recipe<?> recipe = Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow(JsonParseException::new);
        List<JsonElement> named = named(json);
        if (!(recipe instanceof CraftingRecipe crafting) || named == null || named.isEmpty()) {
            return Optional.empty();
        }
        List<Ingredient> ingredients = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        if (crafting instanceof ShapedRecipe shaped) {
            List<Optional<Ingredient>> all = shaped.getIngredients();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).isPresent()) {
                    ingredients.add(all.get(i).orElseThrow());
                    slots.add(i / shaped.getWidth() * GRID + i % shaped.getWidth());
                }
            }
        } else if (!crafting.placementInfo().ingredients().isEmpty()) {
            ingredients.addAll(crafting.placementInfo().ingredients());
        } else {
            // A recipe of its own type lists nothing: the ingredients the board knows for it.
            named.forEach(ingredient -> ingredients.add(Ingredient.CODEC.parse(ops, ingredient).getOrThrow(JsonParseException::new)));
        }
        while (slots.size() < ingredients.size()) {
            slots.add(slots.size());
        }
        if (ingredients.size() != named.size() || ingredients.size() > GRID * GRID) {
            return Optional.empty();
        }
        ItemStack result = KitchenRecipes.result(crafting);
        if (result.isEmpty() && json.has("result") && json.get("result").isJsonObject()) {
            result = ItemStack.CODEC.parse(ops, json.get("result")).result().orElse(ItemStack.EMPTY);
        }
        if (result.isEmpty()) {
            return Optional.empty();
        }
        int base = 0;
        Base kind = Base.FIRST;
        search:
        for (Base candidate : Base.values()) {
            for (int i = 0; candidate.word != null && i < named.size(); i++) {
                if (names(named.get(i), candidate.word)) {
                    base = i;
                    kind = candidate;
                    break search;
                }
            }
        }
        return Optional.of(new Assembly(id, crafting, List.copyOf(ingredients), slots.stream().mapToInt(Integer::intValue).toArray(),
                base, kind, result.copy(), json));
    }

    /** The ingredients of a shapeless or shaped recipe's JSON, a shaped one's row by row as the recipe lists them; null if broken. */
    private static List<JsonElement> named(JsonObject json) {
        List<JsonElement> named = new ArrayList<>();
        if (json.has("ingredients") && json.get("ingredients").isJsonArray()) {
            json.getAsJsonArray("ingredients").forEach(named::add);
            return named;
        }
        if (!json.has("pattern") || !json.get("pattern").isJsonArray() || !json.has("key") || !json.get("key").isJsonObject()) {
            return null;
        }
        JsonObject key = json.getAsJsonObject("key");
        for (JsonElement row : json.getAsJsonArray("pattern")) {
            for (char symbol : row.getAsString().toCharArray()) {
                if (symbol != ' ') {
                    JsonElement ingredient = key.get(String.valueOf(symbol));
                    if (ingredient == null) {
                        return null;
                    }
                    named.add(ingredient);
                }
            }
        }
        return named;
    }

    private static JsonObject item(String id) {
        JsonObject item = new JsonObject();
        item.addProperty("item", id);
        return item;
    }

    /** Whether an ingredient, or one of its choices, is an item or a tag with {@code word} in its path. */
    private static boolean names(JsonElement ingredient, String word) {
        if (ingredient.isJsonArray()) {
            for (JsonElement choice : ingredient.getAsJsonArray()) {
                if (names(choice, word)) {
                    return true;
                }
            }
            return false;
        }
        if (ingredient.isJsonPrimitive()) {
            String value = ingredient.getAsString();
            Identifier id = Identifier.tryParse(value.startsWith("#") ? value.substring(1) : value);
            return id != null && id.getPath().contains(word);
        }
        if (!ingredient.isJsonObject()) {
            return false;
        }
        JsonObject object = ingredient.getAsJsonObject();
        String name = object.has("item") ? string(object, "item") : string(object, "tag");
        Identifier id = name == null ? null : Identifier.tryParse(name);
        return id != null && id.getPath().contains(word);
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    /** Whether {@code item} is what a dish of the board makes: lying there, it is a dish made, not food to be cut. */
    public static boolean isDish(Item item) {
        return all().stream().anyMatch(dish -> dish.result().is(item));
    }

    /** Whether {@code item} starts a dish: it is what goes down first in one. */
    public static boolean isBase(ItemStack item) {
        for (Assembly assembly : all()) {
            if (assembly.ingredients().get(assembly.base()).test(item)) {
                return true;
            }
        }
        return false;
    }

    /** Whether what lies on the board, the base first, is all part of some dish. */
    public static boolean fits(List<ItemStack> layers) {
        for (Assembly assembly : all()) {
            if (assign(assembly, layers, false) != null) {
                return true;
            }
        }
        return false;
    }

    /** Whether more could still go on the layers for a bigger dish: then a finished one waits for the cook's word. */
    public static boolean canGrow(List<ItemStack> layers) {
        for (Assembly assembly : all()) {
            if (assembly.ingredients().size() > layers.size() && assign(assembly, layers, false) != null) {
                return true;
            }
        }
        return false;
    }

    /** The dish that what lies on the board makes, all of it and nothing more. */
    public static Optional<Assembly> complete(List<ItemStack> layers) {
        for (Assembly assembly : all()) {
            if (assign(assembly, layers, true) != null) {
                return Optional.of(assembly);
            }
        }
        return Optional.empty();
    }

    /**
     * Which ingredient each layer takes, the first one the base; {@code all}: every ingredient is taken. Null when the layers do not
     * go into the dish.
     */
    public static int[] assign(Assembly assembly, List<ItemStack> layers, boolean all) {
        List<Ingredient> ingredients = assembly.ingredients();
        if (layers.isEmpty() || layers.size() > ingredients.size() || all && layers.size() != ingredients.size()) {
            return null;
        }
        if (!ingredients.get(assembly.base()).test(layers.get(0))) {
            return null;
        }
        int[] taken = new int[layers.size()];
        taken[0] = assembly.base();
        boolean[] used = new boolean[ingredients.size()];
        used[assembly.base()] = true;
        return assign(ingredients, layers, 1, used, taken) ? taken : null;
    }

    private static boolean assign(List<Ingredient> ingredients, List<ItemStack> layers, int next, boolean[] used, int[] taken) {
        if (next == layers.size()) {
            return true;
        }
        for (int i = 0; i < ingredients.size(); i++) {
            if (!used[i] && ingredients.get(i).test(layers.get(next))) {
                used[i] = true;
                taken[next] = i;
                if (assign(ingredients, layers, next + 1, used, taken)) {
                    return true;
                }
                used[i] = false;
            }
        }
        return false;
    }
}
