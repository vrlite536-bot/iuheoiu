package com.example.thestalker;

import com.example.thestalker.block.ModBlocks;
import com.example.thestalker.entity.AmbushManager;
import com.example.thestalker.entity.OfferingManager;
import com.example.thestalker.registry.ModEntities;
import com.example.thestalker.entity.ObserverEntity;
import com.example.thestalker.entity.StalkerEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.example.thestalker.network.JumpscarePayload;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheStalkerMod implements ModInitializer {
    public static final String MOD_ID = "thestalker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(JumpscarePayload.ID, JumpscarePayload.CODEC);
        ModBlocks.register();
        ModEntities.register();
        FabricDefaultAttributeRegistry.register(ModEntities.OBSERVER, ObserverEntity.createHostileAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.STALKER, StalkerEntity.createStalkerAttributes());
        ServerTickEvents.END_SERVER_TICK.register(OfferingManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(AmbushManager::tick);
        // Do not leave a delayed lethal cinematic attached to a player who disconnects.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            AmbushManager.cancel(handler.player);
            OfferingManager.clearAnimation(handler.player);
        });
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, ModEntities.STALKER, 2, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, ModEntities.OBSERVER, 5, 1, 1);
        LOGGER.info("The Stalker has entered the dark.");
    }
}
