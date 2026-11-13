package com.pockyl.handson_delight.station;

import com.pockyl.handson_delight.KitchenNbt;
import com.pockyl.handson_delight.KitchenRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.block.SkilletBlock;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.handson_delight.KitchenTags;
import com.pockyl.handson_delight.platform.Platform;

import java.util.Optional;

/**
 * Frying by hand. Food put into the skillet at the station fries on the side that lies on the pan; once that side is done it waits
 * until the cook turns it with a toss of the pan, and when both sides are done it is ready and stays in the pan until the cook takes
 * it (owner, 2026-11-08: "I must take it out of the skillet myself once it is cooked"). Up to {@link #BATCH} fry together, a bigger
 * batch taking longer by the square root, as in the pot; the cook puts in no more than that. Food put in with Farmer's Delight's own right click (a
 * player without this mod, or sneaking) cooks Farmer's Delight's way, one after the other.
 */
public final class SkilletCooking {
    public static final int BATCH = 16;
    /** As much faster than Farmer's Delight's skillet as the pot is than its own. */
    private static final float PACE = 1.5F;
    /** Ticks between two tosses the server takes: the food is still in the air before that. */
    private static final int TURN_GAP = 8;

    private SkilletCooking() {
    }

    public static Optional<CampfireCookingRecipe> recipe(Level level, ItemStack stack) {
        return holder(level, stack).map(RecipeHolder::value);
    }

    public static Optional<RecipeHolder<CampfireCookingRecipe>> holder(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return KitchenRecipes.find(level, RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack.copyWithCount(1)));
    }

    /** Ticks one side of {@code batch} takes, for a recipe Farmer's Delight's skillet cooks in {@code skilletTicks}. */
    public static int sideTicks(int skilletTicks, int batch) {
        return Math.max(1, Math.round(skilletTicks * (float) Math.sqrt(Math.max(1, batch)) / PACE / 2.0F));
    }

    /** What Farmer's Delight's skillet takes for one of {@code stack}, with the Fire Aspect it was placed with. */
    public static int skilletTicks(Level level, ItemStack stack, int fireAspect) {
        return recipe(level, stack).map(recipe -> SkilletBlock.getSkilletCookingTime(recipe.cookingTime(), fireAspect)).orElse(0);
    }

    public static Frying frying(SkilletBlockEntity skillet) {
        return ((FryingPan) skillet).handson$frying();
    }

    /**
     * Where Farmer's Delight would cook the stack this tick (the skillet is hot and holds food): returns true when it fries by hand,
     * and Farmer's Delight's own cooking stays out.
     */
    public static boolean fry(SkilletBlockEntity skillet, ItemStack stack) {
        Frying frying = frying(skillet);
        Level level = skillet.getLevel();
        if (!frying.byHand() || level == null) {
            return false;
        }
        Optional<CampfireCookingRecipe> recipe = recipe(level, stack);
        if (recipe.isPresent()) {
            frying.batch(Math.min(stack.getCount(), BATCH));
            int ticks = SkilletBlock.getSkilletCookingTime(recipe.get().cookingTime(), ((FryingPan) skillet).handson$fireAspect());
            if (isOneSided(stack)) {
                // An egg fries sunny side up: through from below in the time both sides take, no toss asked for.
                frying.fryThrough(0.5F / sideTicks(ticks, frying.batch()));
            } else {
                frying.fry(1.0F / sideTicks(ticks, frying.batch()));
            }
            if (frying.down() >= 1.0F && frying.up() >= 1.0F) {
                finish(skillet, frying, stack, recipe.get());
            }
        }
        send(skillet, false);
        return true;
    }

    /** Every tick of the skillet: an emptied pan (taken out, washed out by water) starts over. */
    public static void check(SkilletBlockEntity skillet) {
        Frying frying = frying(skillet);
        if (skillet.getStoredStack().isEmpty() && (frying.byHand() || frying.down() > 0.0F || frying.up() > 0.0F)) {
            frying.farmersDelightsWay();
            send(skillet, true);
        }
    }

    /** The stack on the cursor goes into the pan, one of it or as much as fits; false when the pan will not have it. */
    public static boolean place(SkilletBlockEntity skillet, Player player, ItemStack carried, boolean one) {
        Level level = skillet.getLevel();
        ItemStack inPan = skillet.getStoredStack();
        BlockState state = skillet.getBlockState();
        if (carried.isEmpty() || state.getValue(SkilletBlock.WATERLOGGED) || recipe(level, carried).isEmpty()
                || !inPan.isEmpty() && !ItemStack.isSameItemSameComponents(inPan, carried)) {
            return false;
        }
        int moved = Math.min(one ? 1 : carried.getCount(), Math.min(BATCH, carried.getMaxStackSize()) - inPan.getCount());
        if (moved <= 0) {
            return false;
        }
        frying(skillet).byHand(player.getUUID());
        Platform.INSTANCE.setItem(skillet, inPan.isEmpty() ? carried.copyWithCount(moved) : inPan.copyWithCount(inPan.getCount() + moved));
        carried.shrink(moved);
        send(skillet, true);
        return true;
    }

    /** The cook tosses the pan: the food turns over. False while the last toss is still in the air, or with nothing in the pan. */
    public static boolean turn(SkilletBlockEntity skillet, Player player) {
        Level level = skillet.getLevel();
        Frying frying = frying(skillet);
        if (level == null || skillet.getStoredStack().isEmpty() || level.getGameTime() - frying.turnedAt() < TURN_GAP
                || recipe(level, skillet.getStoredStack()).isEmpty()) {
            return false;
        }
        if (!frying.byHand()) {
            // Cooking Farmer's Delight's way, one after the other: from now on the cook's.
            frying.reset();
        }
        frying.byHand(player.getUUID());
        frying.turn(level.getGameTime());
        send(skillet, true);
        return true;
    }

    /** Everything in the pan goes back to the cook, raw or half done. */
    public static ItemStack take(SkilletBlockEntity skillet) {
        ItemStack inPan = skillet.getStoredStack().copy();
        Platform.INSTANCE.setItem(skillet, ItemStack.EMPTY);
        frying(skillet).farmersDelightsWay();
        send(skillet, true);
        return inPan;
    }

    /** Whether {@code stack} is fried on one side only and never turned (see {@link KitchenTags#ONE_SIDED}). */
    public static boolean isOneSided(ItemStack stack) {
        return stack.is(KitchenTags.ONE_SIDED);
    }

    /** Whether {@code stack} in the pan is done: nothing the campfire makes of it any more. */
    public static boolean isDone(Level level, ItemStack stack) {
        return !stack.isEmpty() && recipe(level, stack).isEmpty();
    }

    /**
     * Both sides are done: the batch becomes what the campfire makes of it and waits in the pan for the cook, who takes it. Only a
     * stack bigger than a batch (put in Farmer's Delight's way, then tossed) cannot keep the rest raw beside it: then the batch goes
     * to the cook at the pan, or with nobody there jumps out as it does from Farmer's Delight's skillet.
     */
    private static void finish(SkilletBlockEntity skillet, Frying frying, ItemStack stack, CampfireCookingRecipe recipe) {
        Level level = skillet.getLevel();
        int count = Math.min(frying.batch(), stack.getCount());
        ItemStack one = recipe.assemble(new SingleRecipeInput(stack.copyWithCount(1)));
        ItemStack done = one.copyWithCount(one.getCount() * count);
        ItemStack left = stack.copyWithCount(stack.getCount() - count);
        Optional<StationContext> station = level instanceof ServerLevel server ? Diegesis.activeStation(server, skillet.getBlockPos())
                : Optional.empty();
        if (left.isEmpty() && !done.isEmpty() && done.getCount() <= done.getMaxStackSize()) {
            // Still the cook's (byHand): Farmer's Delight's own cooking stays out of a pan of finished food.
            Platform.INSTANCE.setItem(skillet, done);
            frying.reset();
            CompoundTag ready = new CompoundTag();
            ready.put(SkilletStation.STACK, KitchenNbt.save(done, level.registryAccess()));
            ready.putBoolean(SkilletStation.IN_PAN, true);
            station.ifPresent(at -> at.effect(SkilletStation.EFFECT_FRIED, "pan", ready));
            return;
        }
        Platform.INSTANCE.setItem(skillet, left);
        frying.reset();
        if (left.isEmpty()) {
            frying.farmersDelightsWay();
        }
        if (done.isEmpty()) {
            return;
        }
        Optional<Player> cook = station.flatMap(at -> at.users().stream().<Player>map(player -> player)
                .min((a, b) -> Boolean.compare(!a.getUUID().equals(frying.cook()), !b.getUUID().equals(frying.cook()))));
        CompoundTag data = new CompoundTag();
        data.put(SkilletStation.STACK, KitchenNbt.save(done, level.registryAccess()));
        data.putBoolean(SkilletStation.TO_COOK, cook.isPresent());
        if (cook.isPresent()) {
            if (!cook.get().getInventory().add(done)) {
                cook.get().drop(done, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        } else {
            jumpOut(skillet, done);
        }
        station.ifPresent(at -> at.effect(SkilletStation.EFFECT_FRIED, "pan", data));
    }

    /** Out of the pan towards its side, as Farmer's Delight's skillet throws what it has cooked. */
    private static void jumpOut(SkilletBlockEntity skillet, ItemStack done) {
        Level level = skillet.getLevel();
        BlockPos pos = skillet.getBlockPos();
        Direction side = skillet.getBlockState().getValue(SkilletBlock.FACING).getClockWise();
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, done,
                side.getStepX() * 0.08, 0.25, side.getStepZ() * 0.08);
        level.addFreshEntity(item);
    }

    /** Saves the skillet and tells the clients around: always, or only when the food got a step further. */
    private static void send(SkilletBlockEntity skillet, boolean always) {
        Frying frying = frying(skillet);
        boolean due = frying.dueToSend();
        if (!always && !due) {
            return;
        }
        skillet.setChanged();
        Level level = skillet.getLevel();
        if (level != null) {
            level.sendBlockUpdated(skillet.getBlockPos(), skillet.getBlockState(), skillet.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
