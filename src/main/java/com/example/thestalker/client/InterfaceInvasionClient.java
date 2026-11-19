package com.example.thestalker.client;

import com.example.thestalker.entity.StalkerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Frequent, purely visual Stalker interface interference for testing. */
@Environment(EnvType.CLIENT)
public final class InterfaceInvasionClient {
    private static int countdown = 80;
    private static int activeTicks;
    private static int eyeAnim;
    private InterfaceInvasionClient() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) { activeTicks = 0; return; }
            eyeAnim++;
            boolean nearby = !client.world.getEntitiesByClass(StalkerEntity.class,
                    client.player.getBoundingBox().expand(36.0), entity -> entity.isAlive()).isEmpty();
            if (!nearby) { activeTicks = 0; countdown = Math.max(countdown, 60); return; }
            if (activeTicks > 0) activeTicks--;
            else if (--countdown <= 0) {
                activeTicks = 65 + client.player.getRandom().nextInt(55);
                countdown = 90 + client.player.getRandom().nextInt(170);
            }
        });
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderHud(drawContext));
        ScreenEvents.AFTER_RENDER.register((screen, drawContext, mouseX, mouseY, tickDelta) -> renderScreen(drawContext));
    }

    private static void renderHud(DrawContext context) {
        if (activeTicks <= 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int w = client.getWindow().getScaledWidth(), h = client.getWindow().getScaledHeight();
        int pulse = (int)(Math.sin(eyeAnim * 0.45) * 35) + 100;
        context.fill(0, 0, w, 2, (pulse << 24) | 0x650008);
        context.fill(0, h - 2, w, h, (pulse << 24) | 0x650008);
        // The red eye temporarily overlays one hotbar slot (slot 4) without changing inventory data.
        int slotX = w / 2 - 90 + 4 * 18;
        int slotY = h - 22;
        drawEye(context, slotX + 1, slotY + 1, 16, 16, eyeAnim);
    }

    private static void renderScreen(DrawContext context) {
        if (activeTicks <= 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return;
        int w = client.getWindow().getScaledWidth(), h = client.getWindow().getScaledHeight();
        int pulse = (int)(Math.sin(eyeAnim * 0.4) * 18) + 34;
        // Mild screen-edge interference only; buttons and menu controls remain usable.
        context.fill(0, 0, w, 3, (pulse << 24) | 0x750008);
        context.fill(0, h - 3, w, h, (pulse << 24) | 0x750008);
        int x = w - 30, y = 24;
        drawEye(context, x, y, 24, 18, eyeAnim);
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen
                || client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>) {
            int slotX = w / 2 - 90 + 4 * 18;
            drawEye(context, slotX + 1, h - 22, 16, 16, eyeAnim);
        }
    }

    /** Pixel-art animated eye made from simple rectangles, avoiding extra texture assets. */
    private static void drawEye(DrawContext ctx, int x, int y, int width, int height, int phase) {
        int blink = Math.floorMod(phase, 48);
        int iris = (int)(Math.sin(phase * 0.24) * 2.0);
        ctx.fill(x, y + height / 3, x + width, y + height * 2 / 3, 0xFF26070A);
        ctx.fill(x + width / 5, y + height / 5, x + width * 4 / 5, y + height * 4 / 5, 0xFF9C111C);
        ctx.fill(x + width / 3, y + height / 4, x + width * 2 / 3, y + height * 3 / 4, 0xFF180308);
        if (blink < 3) {
            ctx.fill(x, y + height / 2 - 1, x + width, y + height / 2 + 2, 0xFF31060B);
        } else {
            ctx.fill(x + width / 2 - width / 8 + iris, y + height / 3, x + width / 2 + width / 8 + iris,
                    y + height * 2 / 3, 0xFFFF2635);
            ctx.fill(x + width / 2 - 1 + iris, y + height / 3, x + width / 2 + 1 + iris, y + height * 2 / 3, 0xFF050204);
        }
    }
}
