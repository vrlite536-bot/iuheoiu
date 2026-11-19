package com.example.thestalker.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A cold lantern left behind after the Stalker snuffs its flame. */
public class UnlitLanternBlock extends LanternBlock {
    private final boolean soul;
    public UnlitLanternBlock(boolean soul, Settings settings) { super(settings); this.soul = soul; }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockState lit = (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).getDefaultState()
                    .with(HANGING, state.get(HANGING));
            world.setBlockState(pos, lit, 3);
            world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.5f, 0.9f);
        }
        return ActionResult.SUCCESS;
    }
}
