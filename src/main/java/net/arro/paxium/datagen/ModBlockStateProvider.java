package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.block.custom.StarforgeBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Paxium.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.DEEPSLATE_PAXIUM_ORE);
        starforgeBlock();
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void starforgeBlock() {
        ModelFile starforgeOn = models().orientable("starforge_on",
                modLoc("block/starforge_side"),
                modLoc("block/starforge_front_on"),
                modLoc("block/starforge_top"));

        ModelFile starforgeOff = models().orientable("starforge",
                modLoc("block/starforge_side"),
                modLoc("block/starforge_front"),
                modLoc("block/starforge_top"));

        getVariantBuilder(ModBlocks.STARFORGE.get())
                .forAllStates(state -> {
                    Direction dir = state.getValue(StarforgeBlock.FACING);
                    boolean isLit = state.getValue(StarforgeBlock.LIT);

                    return ConfiguredModel.builder()
                            .modelFile(isLit ? starforgeOn : starforgeOff)
                            .rotationY((int) dir.toYRot())
                            .build();
                });

        simpleBlockItem(ModBlocks.STARFORGE.get(), starforgeOff);
    }
}