package com.example.thestalker.client;

import com.example.thestalker.TheStalkerMod;
import com.example.thestalker.entity.StalkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class StalkerRenderer extends MobEntityRenderer<StalkerEntity, StalkerModel<StalkerEntity>> {
    private static final Identifier TEXTURE = TheStalkerMod.id("textures/entity/stalker.png");
    public StalkerRenderer(EntityRendererFactory.Context context) {
        super(context, new StalkerModel<>(context.getPart(StalkerModel.LAYER)), 0.35f);
    }
    @Override public Identifier getTexture(StalkerEntity entity) { return TEXTURE; }
}
