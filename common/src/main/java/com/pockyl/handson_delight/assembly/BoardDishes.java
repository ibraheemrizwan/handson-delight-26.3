package com.pockyl.handson_delight.assembly;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.pockyl.handson_delight.HandsOnConfig;
import com.pockyl.handson_delight.HandsOnDelight;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The board's dishes on a client, for recipe viewers: the server sends them as a player joins and after each reload, before the
 * recipes and tags, so a viewer that starts on those finds them here. A server without this mod sends nothing, and its sandwiches
 * stay crafting recipes.
 */
public final class BoardDishes {
    public static final Identifier CHANNEL = HandsOnDelight.id("board_dishes");

    /** The server's config keeps these dishes in the crafting grid as well: the viewers show both. */
    private static final String CRAFTED_TOO = "crafted_too";
    private static volatile ListTag received = new ListTag();
    private static volatile boolean craftedToo;
    private static ListTag readFrom;
    private static List<AssemblyRecipes.Assembly> dishes = List.of();

    private BoardDishes() {
    }

    /** The packet: the dishes as the server has them. A client without this mod never gets it. */
    public record Payload(CompoundTag book) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(CHANNEL);
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeNbt(payload.book()),
                buffer -> new Payload(Objects.requireNonNullElseGet(buffer.readNbt(), CompoundTag::new)));

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    /** The packet, on the server. */
    public static Payload payload() {
        CompoundTag book = new CompoundTag();
        // Without the board's station nothing is made there: the viewers show the crafting recipes alone.
        book.put("dishes", HandsOnConfig.cuttingBoard() ? AssemblyRecipes.book() : new ListTag());
        book.putBoolean(CRAFTED_TOO, !HandsOnConfig.removeCraftingRecipes());
        return new Payload(book);
    }

    /** Called on the network thread: the dishes are only kept here, and read once the tags are bound. */
    public static void receive(Payload packet) {
        CompoundTag payload = packet.book();
        craftedToo = payload.getBooleanOr(CRAFTED_TOO, false);
        received = payload.getListOrEmpty("dishes");
    }

    public static void clear() {
        received = new ListTag();
        craftedToo = false;
    }

    public static synchronized List<AssemblyRecipes.Assembly> all() {
        ListTag book = received;
        if (book != readFrom) {
            dishes = AssemblyRecipes.read(book);
            readFrom = book;
        }
        return dishes;
    }

    /** The crafting recipes a player does not make: the viewers hide them. */
    public static Set<Identifier> ids() {
        if (craftedToo) {
            return Set.of();
        }
        return all().stream().map(AssemblyRecipes.Assembly::id).collect(Collectors.toUnmodifiableSet());
    }
}
