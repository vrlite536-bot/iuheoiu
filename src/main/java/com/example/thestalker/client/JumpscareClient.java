package com.example.thestalker.client;

import com.example.thestalker.network.JumpscarePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** A short camera snap and face-flash; the server remains authoritative for the lethal hit. */
@Environment(EnvType.CLIENT)
public final class JumpscareClient {
    private static int ticksRemaining;
    private JumpscareClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(JumpscarePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().player != null) {
                        context.client().player.setPitch(8.0f);
                        ticksRemaining = 14;
                    }
                }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksRemaining > 0) ticksRemaining--;
        });
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderOverlay(drawContext));
    }

    private static void renderOverlay(DrawContext context) {
        if (ticksRemaining <= 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        int alpha = Math.min(235, 100 + (14 - ticksRemaining) * 12);
        context.fill(0, 0, w, h, (alpha << 24) | 0x080305);
        int cx = w / 2, cy = h / 2;
        float reach = (14 - ticksRemaining) / 14.0f;
        int handTop = h / 2 + (int) (h * 0.24f * (1.0f - reach));
        int handWidth = (int) (w * (0.08f + reach * 0.22f));
        context.fill(0, handTop, handWidth, h, 0xFF100F12);
        context.fill(w - handWidth, handTop, w, h, 0xFF100F12);
        for (int finger = 0; finger < 4; finger++) {
            int offset = finger * Math.max(3, handWidth / 5);
            context.fill(offset, handTop - 12 - finger * 2, offset + Math.max(4, handWidth / 7), handTop + 28, 0xFF171519);
            context.fill(w - offset - Math.max(4, handWidth / 7), handTop - 12 - finger * 2, w - offset, handTop + 28, 0xFF171519);
        }
        // A stark, graphic mask flashes into the player's view for a few frames.
        int faceW = Math.max(70, w / 3), faceH = Math.max(100, h * 2 / 3);
        context.fill(cx - faceW / 2, cy - faceH / 2, cx + faceW / 2, cy + faceH / 2, 0xFFE1DED8);
        context.fill(cx - faceW / 2, cy - faceH / 2, cx + faceW / 2, cy - faceH / 2 + faceH / 7, 0xFF111014);
        int eyeW = Math.max(14, faceW / 5), eyeH = Math.max(8, faceH / 13);
        context.fill(cx - faceW / 3, cy - faceH / 9, cx - faceW / 3 + eyeW, cy - faceH / 9 + eyeH, 0xFF9D1018);
        context.fill(cx + faceW / 3 - eyeW, cy - faceH / 9, cx + faceW / 3, cy - faceH / 9 + eyeH, 0xFF9D1018);
        context.fill(cx - faceW / 5, cy + faceH / 5, cx + faceW / 5, cy + faceH / 5 + Math.max(12, faceH / 8), 0xFF100D10);
    }
}
