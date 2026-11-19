package com.example.thestalker.block;

import com.example.thestalker.TheStalkerMod;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {
    public static final Block UNLIT_TORCH = Registry.register(Registries.BLOCK, TheStalkerMod.id("unlit_torch"),
            new UnlitTorchBlock(ParticleTypes.SMOKE, Block.Settings.copy(Blocks.TORCH).luminance(state -> 0)));
    public static final Block UNLIT_LANTERN = Registry.register(Registries.BLOCK, TheStalkerMod.id("unlit_lantern"),
            new UnlitLanternBlock(false, Block.Settings.copy(Blocks.LANTERN).luminance(state -> 0)));
    public static final Block UNLIT_SOUL_LANTERN = Registry.register(Registries.BLOCK, TheStalkerMod.id("unlit_soul_lantern"),
            new UnlitLanternBlock(true, Block.Settings.copy(Blocks.SOUL_LANTERN).luminance(state -> 0)));
    public static final Block UNLIT_WALL_TORCH = Registry.register(Registries.BLOCK, TheStalkerMod.id("unlit_wall_torch"),
            new UnlitWallTorchBlock(Block.Settings.copy(Blocks.WALL_TORCH).luminance(state -> 0)));
    public static final Item UNLIT_LANTERN_ITEM = Registry.register(Registries.ITEM, TheStalkerMod.id("unlit_lantern"), new BlockItem(UNLIT_LANTERN, new Item.Settings()));
    public static final Item UNLIT_SOUL_LANTERN_ITEM = Registry.register(Registries.ITEM, TheStalkerMod.id("unlit_soul_lantern"), new BlockItem(UNLIT_SOUL_LANTERN, new Item.Settings()));
    public static final Item UNLIT_TORCH_ITEM = Registry.register(Registries.ITEM, TheStalkerMod.id("unlit_torch"), new BlockItem(UNLIT_TORCH, new Item.Settings()));
    public static final Item UNLIT_WALL_TORCH_ITEM = Registry.register(Registries.ITEM, TheStalkerMod.id("unlit_wall_torch"), new BlockItem(UNLIT_WALL_TORCH, new Item.Settings()));
    private ModBlocks() {}
    public static void register() { TheStalkerMod.LOGGER.info("Registering Stalker blocks"); }
}
