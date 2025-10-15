package net.arro.paxium.block;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.custom.PaxiumOre;
import net.arro.paxium.item.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Paxium.MODID);

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    public static final DeferredBlock<Block> DEEPSLATE_PAXIUM_ORE = registerBlock("deepslate_paxium_ore",
            () -> new PaxiumOre(UniformInt.of(10, 15),
                    BlockBehaviour.Properties.of()
                    .strength(40f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.COLOR_BLACK)
                    .lightLevel((state) -> 7)
                    .sound(SoundType.DEEPSLATE)));

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
