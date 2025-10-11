package net.arro.paxium.block.custom;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Collections;
import java.util.List;

public class PaxiumOre extends DropExperienceBlock {
    public PaxiumOre(IntProvider pXpRange, Properties pProperties) {
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

}
