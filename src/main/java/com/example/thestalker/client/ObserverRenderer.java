package com.example.thestalker.client;

import com.example.thestalker.TheStalkerMod;
import com.example.thestalker.entity.ObserverEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;

public class ObserverRenderer extends MobEntityRenderer<ObserverEntity, StalkerModel<ObserverEntity>> {
    private static final Identifier TEXTURE = TheStalkerMod.id("textures/entity/observer.png");
    public ObserverRenderer(EntityRendererFactory.Context context) {
        super(context, new StalkerModel<>(context.getPart(StalkerModel.LAYER)), 0.0f);
    }
    @Override public Identifier getTexture(ObserverEntity entity) { return TEXTURE; }
    @Override protected void scale(ObserverEntity entity, MatrixStack matrices, float tickDelta) {
        float scale = Math.max(0.015f, 1.0f - entity.getDissolveProgress(tickDelta));
        matrices.scale(scale, scale, scale);
    }
}
