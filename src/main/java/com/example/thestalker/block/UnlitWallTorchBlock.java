package com.example.thestalker.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UnlitWallTorchBlock extends WallTorchBlock {
    public UnlitWallTorchBlock(Settings settings) { super(ParticleTypes.SMOKE, settings); }

    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            world.setBlockState(pos, Blocks.WALL_TORCH.getDefaultState().with(FACING, state.get(FACING)), 3);
            world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.45f, 1.25f);
            world.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 3, 0.02, 0.02, 0.02, 0.01);
        }
        return ActionResult.SUCCESS;
    }
}
