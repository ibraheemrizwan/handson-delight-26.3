package com.pockyl.handson_delight.station;

import com.pockyl.handson_delight.KitchenNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity;

import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotStorage;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationLogic;
import com.pockyl.handson_delight.platform.Platform;

/** The stove's actions at the station: food on, a piece turned, a piece taken. The grilling itself is {@link StoveCooking}'s. */
public final class StoveLogic implements StationLogic {
    /** Doneness is told to the clients in steps this fine: the look moves smoothly between them. */
    private static final float STEP = 1.0F / 32.0F;

    private final AbstractStoveBlockEntity stove;

    public StoveLogic(StationContext station) {
        stove = (AbstractStoveBlockEntity) station.blockEntity();
        station.state().bindSlots(StoveStation.GRILL, new SlotStorage() {
            @Override
            public int size() {
                return Grill.PLACES;
            }

            @Override
            public ItemStack get(int index) {
                return Platform.INSTANCE.getItem(stove, index);
            }

            @Override
            public void set(int index, ItemStack stack) {
                Platform.INSTANCE.setItem(stove, index, stack);
            }
        });
    }

    /** Nothing goes on by the library's own moves: every stack comes through {@code place}, the hand's slot stays empty. */
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
        int place = data.contains(StoveStation.PLACE) ? data.getIntOr(StoveStation.PLACE, 0) : -1;
        switch (action) {
            case StoveStation.ACTION_PLACE -> place(station, player, data.getBooleanOr(StoveStation.ONE, false) ? place : -1,
                    data.contains(StoveStation.FROM) ? data.getIntOr(StoveStation.FROM, 0) : -1);
            case StoveStation.ACTION_TURN -> turn(station, place);
            case StoveStation.ACTION_TAKE -> take(station, player, place);
            default -> {
            }
        }
        publish(station);
    }

    /** The stack on the cursor, or the one in inventory slot {@code from}, goes onto the grill: everywhere free, or onto {@code place}. */
    private void place(StationContext station, Player player, int place, int from) {
        if (from >= Inventory.INVENTORY_SIZE) {
            return;
        }
        ItemStack carried = from >= 0 ? player.getInventory().getItem(from) : player.inventoryMenu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        int laid = StoveCooking.place(stove, carried, place);
        if (laid == 0) {
            CompoundTag data = new CompoundTag();
            data.put(StoveStation.STACK, KitchenNbt.save(carried.copyWithCount(1), station.level().registryAccess()));
            station.effect(StoveStation.EFFECT_REFUSED, "grill", data);
            return;
        }
        if (from >= 0) {
            player.getInventory().setItem(from, carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else {
            player.inventoryMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        }
        station.effect(StoveStation.EFFECT_PLACED, "grill", new CompoundTag());
    }

    private void turn(StationContext station, int place) {
        CompoundTag data = new CompoundTag();
        data.putInt(StoveStation.PLACE, place);
        station.effect(StoveCooking.turn(stove, place) ? StoveStation.EFFECT_TURNED : StoveStation.EFFECT_UNTURNED, "grill", data);
    }

    private void take(StationContext station, Player player, int place) {
        ItemStack taken = StoveCooking.take(stove, place);
        if (taken.isEmpty()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putInt(StoveStation.PLACE, place);
        data.put(StoveStation.STACK, KitchenNbt.save(taken, station.level().registryAccess()));
        station.effect(StoveStation.EFFECT_TAKEN, "grill", data);
        if (!player.getInventory().add(taken)) {
            player.drop(taken, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    /** How far each piece is, for the clients; sent only when it changes. */
    private void publish(StationContext station) {
        StationState state = station.state();
        Grill grill = StoveCooking.grill(stove);
        CompoundTag data = new CompoundTag();
        for (int place = 0; place < Grill.PLACES; place++) {
            if (!Platform.INSTANCE.getItem(stove, place).isEmpty()) {
                data.putFloat(StoveStation.DOWN + place, steps(grill.down(place)));
                data.putFloat(StoveStation.UP + place, steps(grill.up(place)));
                data.putBoolean(StoveStation.BY_HAND + place, grill.byHand(place));
            }
        }
        data.putBoolean(StoveStation.LIT, StoveCooking.isLit(stove));
        if (!data.equals(state.data())) {
            state.setData(data);
        }
    }

    private static float steps(float done) {
        return done >= 1.0F ? 1.0F : (float) Math.floor(done / STEP) * STEP;
    }
}
