package com.example.thestalker.client;

import com.example.thestalker.block.ModBlocks;
import com.example.thestalker.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

public class TheStalkerClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(StalkerModel.LAYER, StalkerModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);
        EntityRendererRegistry.register(ModEntities.OBSERVER, ObserverRenderer::new);
        // Torches and lanterns have see-through pixels; without the cutout layer they render with black boxes.
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                ModBlocks.UNLIT_TORCH, ModBlocks.UNLIT_WALL_TORCH, ModBlocks.UNLIT_LANTERN, ModBlocks.UNLIT_SOUL_LANTERN);
        JumpscareClient.register();
    }
}
