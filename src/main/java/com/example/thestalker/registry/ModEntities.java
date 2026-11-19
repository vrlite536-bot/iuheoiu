package com.example.thestalker.registry;

import com.example.thestalker.TheStalkerMod;
import com.example.thestalker.entity.ObserverEntity;
import com.example.thestalker.entity.StalkerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.world.Heightmap;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<ObserverEntity> OBSERVER = Registry.register(
            Registries.ENTITY_TYPE, TheStalkerMod.id("observer"),
            FabricEntityType.Builder.createMob(ObserverEntity::new, SpawnGroup.MONSTER,
                    builder -> builder.spawnRestriction(net.minecraft.entity.SpawnLocationTypes.ON_GROUND,
                            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ObserverEntity::canSpawn))
                    .dimensions(0.65f, 2.35f).maxTrackingRange(96).build("observer")
    );
    public static final EntityType<StalkerEntity> STALKER = Registry.register(
            Registries.ENTITY_TYPE, TheStalkerMod.id("stalker"),
            FabricEntityType.Builder.createMob(StalkerEntity::new, SpawnGroup.MONSTER,
                    builder -> builder.spawnRestriction(net.minecraft.entity.SpawnLocationTypes.ON_GROUND,
                            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, StalkerEntity::canSpawn))
                    .dimensions(0.7f, 2.4f).maxTrackingRange(96).build("stalker")
    );
    private ModEntities() {}
    public static void register() { TheStalkerMod.LOGGER.info("Registering Stalker entities"); }
}
