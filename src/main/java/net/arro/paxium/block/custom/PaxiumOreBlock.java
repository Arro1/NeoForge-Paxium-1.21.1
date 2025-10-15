package net.arro.paxium.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Collections;
import java.util.List;

public class PaxiumOreBlock extends DropExperienceBlock {
    public PaxiumOreBlock(IntProvider pXpRange, Properties pProperties) {
        super(pXpRange, pProperties);
    }

    private boolean isCorrectTool(ItemStack toolStack) {
        if (toolStack != null && toolStack.getItem() instanceof TieredItem tieredItem) {
            return tieredItem.getTier() == Tiers.NETHERITE;
        }
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pParams) {
        ItemStack tool = pParams.getOptionalParameter(LootContextParams.TOOL);
        if (isCorrectTool(tool)) {
                return super.getDrops(pState, pParams);
        }
        return Collections.emptyList();
    }

    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!pLevel.isClientSide) {
            return;
        }

        double d0 = pPos.getX() + pRandom.nextDouble();
        double d1 = pPos.getY() + 1.0;
        double d2 = pPos.getZ() + pRandom.nextDouble();

        if (pRandom.nextInt(3) == 0) {
            pLevel.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0, 0.0, 0.0);
        }
        if (pRandom.nextInt(5) == 0) {
            pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }
}
