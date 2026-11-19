package com.example.thestalker.client;

import com.example.thestalker.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class TheStalkerClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(StalkerModel.LAYER, StalkerModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);
        EntityRendererRegistry.register(ModEntities.OBSERVER, ObserverRenderer::new);
        JumpscareClient.register();
    }
}
