package com.example.thestalker.network;

import com.example.thestalker.TheStalkerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Empty server-to-client packet that starts the jumpscare overlay. */
public record JumpscarePayload() implements CustomPayload {
    public static final CustomPayload.Id<JumpscarePayload> ID = new CustomPayload.Id<>(TheStalkerMod.id("jumpscare"));
    public static final PacketCodec<RegistryByteBuf, JumpscarePayload> CODEC = PacketCodec.unit(new JumpscarePayload());

    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
