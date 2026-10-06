package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.block.custom.StarforgeBlock;
import net.arro.paxium.glow.ModGlow;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Paxium.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.DEEPSLATE_PAXIUM_ORE.get(),
                glowCube("deepslate_paxium_ore", "deepslate_paxium_ore", "deepslate_paxium_ore", "deepslate_paxium_ore"));
        starforgeBlock();

        simpleBlockWithItem(ModBlocks.PAXIUM_BOMB.get(), glowCube("paxium_bomb",
                "paxium_bomb_side", "paxium_bomb_bottom", "paxium_bomb_top"));
    }

    // A full cube with a texture per face group. Faces whose texture is registered in ModGlow get a fullbright,
    // animated overlay element on top. Use this for every cube model so new glowing blocks just work.
    private BlockModelBuilder glowCube(String name, String side, String bottom, String top) {
        // block/block carries the display transforms (inventory angle, hand scale) that cube_all etc. inherit.
        BlockModelBuilder model = models().getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", modLoc("block/" + side))
                .texture("side", modLoc("block/" + side))
                .texture("bottom", modLoc("block/" + bottom))
                .texture("top", modLoc("block/" + top));

        Function<Direction, String> variable = dir -> switch (dir) {
            case DOWN -> "bottom";
            case UP -> "top";
            default -> "side";
        };

        model.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((dir, face) -> face.texture("#" + variable.apply(dir)).cullface(dir));

        Set<String> glowing = new HashSet<>();
        for (String var : new String[]{"side", "bottom", "top"}) {
            ModGlow.Entry glow = ModGlow.get(modLoc("block/" + switch (var) {
                case "bottom" -> bottom;
                case "top" -> top;
                default -> side;
            })).orElse(null);
            if (glow != null) {
                model.texture(var + "_glow", glow.glow());
                glowing.add(var);
            }
        }
        if (!glowing.isEmpty()) {
            // Same cube again, but only on faces that glow; identical coordinates are drawn on top (like grass overlays).
            ModelBuilder<BlockModelBuilder>.ElementBuilder overlay = model.element()
                    .from(0, 0, 0).to(16, 16, 16).shade(false).emissivity(15, 15).ao(false);
            for (Direction dir : Direction.values()) {
                String var = variable.apply(dir);
                if (glowing.contains(var)) {
                    overlay.face(dir).texture("#" + var + "_glow").cullface(dir).end();
                }
            }
            model.renderType("cutout");
        }
        return model;
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