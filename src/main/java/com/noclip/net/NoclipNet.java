package com.noclip.net;

import com.noclip.Noclip;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class NoclipNet {
    private NoclipNet() {}

    /** Current sanity, 0..100. */
    public record SanityPayload(float sanity) implements CustomPacketPayload {
        public static final Type<SanityPayload> TYPE = new Type<>(Noclip.id("sanity"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SanityPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.FLOAT, SanityPayload::sanity, SanityPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The player just arrived in a level (empty string = left the Backrooms). */
    public record EnterPayload(String level, boolean firstVisit, int discovered) implements CustomPacketPayload {
        public static final Type<EnterPayload> TYPE = new Type<>(Noclip.id("enter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EnterPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, EnterPayload::level,
                ByteBufCodecs.BOOL, EnterPayload::firstVisit,
                ByteBufCodecs.VAR_INT, EnterPayload::discovered,
                EnterPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A scripted scare or effect for the client to play. */
    public record EventPayload(String event, float strength) implements CustomPacketPayload {
        public static final Type<EventPayload> TYPE = new Type<>(Noclip.id("event"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EventPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, EventPayload::event,
                ByteBufCodecs.FLOAT, EventPayload::strength,
                EventPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Full list of discovered levels, for the journal. */
    public record JournalPayload(String visited) implements CustomPacketPayload {
        public static final Type<JournalPayload> TYPE = new Type<>(Noclip.id("journal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, JournalPayload::visited, JournalPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: I have been pushing into a wall for this many ticks. */
    public record WallPushPayload(int ticks) implements CustomPacketPayload {
        public static final Type<WallPushPayload> TYPE = new Type<>(Noclip.id("wall_push"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WallPushPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, WallPushPayload::ticks, WallPushPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(WallPushPayload.TYPE, WallPushPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SanityPayload.TYPE, SanityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnterPayload.TYPE, EnterPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EventPayload.TYPE, EventPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JournalPayload.TYPE, JournalPayload.CODEC);
    }
}
