package com.pockyl.handson_delight.station;

import com.pockyl.handson_delight.KitchenNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vectorwing.farmersdelight.common.block.entity.SkilletBlockEntity;

import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotStorage;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationLogic;
import com.pockyl.handson_delight.platform.Platform;

/**
 * The skillet's station: puts food in, tosses the pan, takes the food back, and tells the clients how far it is done. The frying
 * itself goes on in the skillet's own tick ({@link SkilletCooking}), also with nobody at it. Runs on the clients around an idle
 * skillet too, to show what lies in it; there it only reads.
 */
public final class SkilletLogic implements StationLogic {
    /** Doneness is told to the clients in steps this fine: the look moves smoothly between them. */
    private static final float STEP = 1.0F / 32.0F;

    private final SkilletBlockEntity skillet;

    public SkilletLogic(StationContext station) {
        skillet = (SkilletBlockEntity) station.blockEntity();
        station.state().bindSlots(SkilletStation.PAN, new SlotStorage() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public ItemStack get(int index) {
                return skillet.getStoredStack();
            }

            @Override
            public void set(int index, ItemStack stack) {
                Platform.INSTANCE.setItem(skillet, stack);
            }
        });
    }

    /** Nothing goes in by the library's own moves: every stack comes through {@code place}, the hand's slot stays empty. */
    @Override
    public boolean mayPlace(StationContext station, Player player, SlotRef slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayTake(StationContext station, Player player, SlotRef slot) {
        return false;
    }

    @Override
    public void tick(StationContext station) {
        publish(station);
    }

    @Override
    public void onCustomAction(StationContext station, Player player, String action, CompoundTag data) {
        switch (action) {
            case SkilletStation.ACTION_PLACE -> place(station, player, data.getBooleanOr(SkilletStation.ONE, false),
                    data.contains(SkilletStation.FROM) ? data.getIntOr(SkilletStation.FROM, 0) : -1);
            case SkilletStation.ACTION_TOSS -> toss(station, player, data.getFloatOr(SkilletStation.STRENGTH, 0.0F));
            case SkilletStation.ACTION_TAKE -> take(station, player);
            default -> {
            }
        }
        publish(station);
    }

    /** The stack on the cursor, or the one in inventory slot {@code from}, goes into the pan. */
    private void place(StationContext station, Player player, boolean one, int from) {
        if (from >= Inventory.INVENTORY_SIZE) {
            return;
        }
        ItemStack carried = from >= 0 ? player.getInventory().getItem(from) : player.inventoryMenu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        boolean wasEmpty = skillet.getStoredStack().isEmpty();
        if (!SkilletCooking.place(skillet, player, carried, one)) {
            CompoundTag data = new CompoundTag();
            data.put(SkilletStation.STACK, KitchenNbt.save(carried.copyWithCount(1), station.level().registryAccess()));
            station.effect(SkilletStation.EFFECT_REFUSED, "pan", data);
            return;
        }
        if (from >= 0) {
            player.getInventory().setItem(from, carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else {
            player.inventoryMenu.setCarried(carried);
        }
        CompoundTag data = new CompoundTag();
        data.putBoolean(SkilletStation.ONE, !wasEmpty);
        station.effect(SkilletStation.EFFECT_PLACED, "pan", data);
    }

    private void toss(StationContext station, Player player, float strength) {
        if (SkilletCooking.turn(skillet, player)) {
            CompoundTag data = new CompoundTag();
            data.putFloat(SkilletStation.STRENGTH, Math.max(0.0F, Math.min(1.0F, strength)));
            station.effect(SkilletStation.EFFECT_TOSSED, "pan", data);
        } else if (!skillet.getStoredStack().isEmpty()) {
            station.effect(SkilletStation.EFFECT_UNTOSSED, "pan", new CompoundTag());
        }
    }

    private void take(StationContext station, Player player) {
        ItemStack taken = SkilletCooking.take(skillet);
        if (taken.isEmpty()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.put(SkilletStation.STACK, KitchenNbt.save(taken, station.level().registryAccess()));
        station.effect(SkilletStation.EFFECT_TAKEN, "pan", data);
        if (!player.getInventory().add(taken)) {
            player.drop(taken, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    /** How far the food is, for the clients; sent only when it changes. */
    private void publish(StationContext station) {
        StationState state = station.state();
        Frying frying = SkilletCooking.frying(skillet);
        CompoundTag data = new CompoundTag();
        if (!skillet.getStoredStack().isEmpty()) {
            data.putFloat(SkilletStation.DOWN, steps(frying.down()));
            data.putFloat(SkilletStation.UP, steps(frying.up()));
            data.putInt(SkilletStation.BATCH, Math.max(1, Math.min(skillet.getStoredStack().getCount(), SkilletCooking.BATCH)));
            data.putBoolean(SkilletStation.BY_HAND, frying.byHand());
        }
        data.putBoolean(SkilletStation.HEATED, skillet.isHeated());
        if (!data.equals(state.data())) {
            state.setData(data);
        }
    }

    private static float steps(float done) {
        return done >= 1.0F ? 1.0F : (float) Math.floor(done / STEP) * STEP;
    }
}
