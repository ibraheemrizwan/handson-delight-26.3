package com.pockyl.handson_delight.station;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.ControlSpec;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotRules;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.state.TankSpec;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_delight.FarmersDelightContent;
import com.pockyl.handson_delight.HandsOnDelight;

/**
 * The cooking pot as a station. Its slots are windows on the pot's own inventory: hoppers reach it and it keeps its items whoever is
 * at it. The block is the pot, open in the world too (the mod's blockstate): the scene only adds what is in it, for everybody around.
 */
public final class CookingPotStation {
    public static final Identifier ID = HandsOnDelight.id("cooking_pot");

    public static final String INGREDIENTS = "ingredients";
    public static final String MEAL = "meal";
    public static final String SERVED = "served";
    public static final String BROTH = "broth";
    public static final String PROGRESS = "progress";
    public static final String HEATED = "heated";
    /** On while the pot counts as stirred; where the spoon's end is, -1 to 1 of its reach in the pot's own axes. */
    public static final String STIRRING = "stirring";
    public static final String SPOON_X = "spoon_x";
    public static final String SPOON_Z = "spoon_z";
    /** How well the pot is stirred, 0 to 3 stars; what it is when a batch is done brings bonus servings. */
    public static final String STARS = "stars";
    /** What the logic tells the clients in the station's data: how long cooking still takes, in ticks. */
    public static final String COOK_LEFT = "cook_left";
    /** And the stars the last batch was done with, and how many the cook at the pot has opened for this dish. */
    public static final String GRADE = "grade";
    public static final String OPEN_STARS = "open_stars";
    /** And how many servings the stars of the last batch added. */
    public static final String BONUS = "bonus";

    public static final SlotRef MEAL_SLOT = SlotRef.of(MEAL, 0);
    public static final SlotRef SERVED_SLOT = SlotRef.of(SERVED, 0);

    /** A custom action: the container on the player's cursor takes a serving of the soup. */
    public static final String ACTION_SCOOP = "scoop";
    /** A custom action: the spoon moved; data: where its end is now, {@link #STIR_X} and {@link #STIR_Z}. */
    public static final String ACTION_STIR = "stir";
    /**
     * A custom action: a dish picked in the recipe book; its ingredients go from the inventory into the pot, one set or, with
     * {@link #SCOOP_ALL}, as many as fit (owner's backlog: "as vanilla's book does"). Data: {@link #DISH}, the recipe's id.
     */
    public static final String ACTION_FILL = "fill";
    public static final String DISH = "dish";
    /** How many sets one fill puts in at most: as many as one batch cooks without taking longer. */
    public static final int FILL_MOST = 16;
    /** Data of {@link #ACTION_SCOOP}: the whole stack on the cursor, not one container. */
    public static final String SCOOP_ALL = "all";
    /**
     * Data of {@link #ACTION_SCOOP}: the hand is empty, the containers come from the inventory (owner: "with an empty hand over
     * the soup, offer to scoop it if there is a bowl in the inventory").
     */
    public static final String FROM_INVENTORY = "from_inventory";
    public static final String STIR_X = "x";
    /** Data of {@link #ACTION_STIR}: the cook's own clock, in ticks, so that the server judges the pace the hand had. */
    public static final String STIR_AT = "at";
    public static final String STIR_Z = "z";
    /** Played when a serving leaves the pot, for everybody who sees it. */
    public static final Identifier EFFECT_SCOOPED = HandsOnDelight.id("scooped");

    public static final StationSchema SCHEMA = StationSchema.builder()
            .slots(INGREDIENTS, CookingPotBlockEntity.MEAL_DISPLAY_SLOT, SlotRules.DEFAULT.kept().accepts(stack -> !isDish(stack)))
            .slots(MEAL, 1, SlotRules.DEFAULT.kept().accepts(stack -> false))
            .slots(SERVED, 1, SlotRules.DEFAULT.kept().accepts(stack -> false))
            .tank(BROTH, TankSpec.of(100))
            .control(PROGRESS, ControlSpec.stepped(0.0F, 1.0F, 100, 0.0F).locked())
            .control(HEATED, ControlSpec.toggle(false).locked())
            .control(STIRRING, ControlSpec.toggle(false).locked())
            .control(SPOON_X, ControlSpec.stepped(-1.0F, 1.0F, 64, 0.0F).locked())
            .control(SPOON_Z, ControlSpec.stepped(-1.0F, 1.0F, 64, 0.0F).locked())
            .control(STARS, ControlSpec.stepped(0.0F, StirSkill.MOST, 60, 0.0F).locked())
            .build();

    public static final StationType TYPE = Diegesis.station(ID)
            .block(FarmersDelightContent.COOKING_POT)
            .schema(SCHEMA)
            .logic(PotLogic::new)
            .orientation(StationType.Orientation.PLAYER)
            .openWhen(CookingPotStation::opens)
            .idleLogic()
            .build();

    private CookingPotStation() {
    }

    public static void register() {
        Diegesis.register(TYPE);
    }

    /** Dishes are for scooping, they do not go into the water. Modded ones are found in the recipes, by {@code PotLogic.mayPlace}. */
    private static boolean isDish(ItemStack stack) {
        return stack.is(Items.BOWL) || stack.is(Items.GLASS_BOTTLE);
    }

    /** Sneaking keeps the pot's handle toggle; a bowl in the hand keeps its quick serving. */
    private static boolean opens(Player player, BlockPos pos, BlockState state) {
        if (player.isSecondaryUseActive()) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        return !(player.level().getBlockEntity(pos) instanceof CookingPotBlockEntity pot) || pot.getMeal().isEmpty() || !pot.isContainerValid(held);
    }
}
