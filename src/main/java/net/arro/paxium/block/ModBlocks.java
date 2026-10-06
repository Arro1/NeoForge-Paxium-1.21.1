package net.arro.paxium.block;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.custom.PaxiumBombBlock;
import net.arro.paxium.block.custom.PaxiumOreBlock;
import net.arro.paxium.block.custom.StarforgeBlock;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.item.custom.StarforgeBlockItem;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Paxium.MODID);

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Supplier<T> block, Function<DeferredHolder<Block, T>, Item> item) {
        DeferredHolder<Block, T> registeredBlock = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> item.apply(registeredBlock));
        return registeredBlock;
    }

    public static final DeferredBlock<Block> DEEPSLATE_PAXIUM_ORE = registerBlock("deepslate_paxium_ore",
            () -> new PaxiumOreBlock(UniformInt.of(10, 15),
                    BlockBehaviour.Properties.of()
                    .strength(40f)
                    .requiresCorrectToolForDrops()
                    .mapColor(MapColor.COLOR_BLACK)
                    .lightLevel((state) -> 7)
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredHolder<Block, Block> STARFORGE = registerBlock("starforge",
            () -> new StarforgeBlock(BlockBehaviour.Properties.of()
                    .strength(30f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ANVIL)
                    .lightLevel((state) -> state.getValue(StarforgeBlock.LIT) ? 15 : 0)),
            (block) -> new StarforgeBlockItem(block.get(), new Item.Properties())
    );

    // Third and final Starforge refinement of an End Crystal. Armed by flint and steel or redstone.
    public static final DeferredHolder<Block, PaxiumBombBlock> PAXIUM_BOMB = registerBlock("paxium_bomb",
            () -> new PaxiumBombBlock(BlockBehaviour.Properties.of()
                    .strength(5f, 1200f)
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel((state) -> 7)),
            (block) -> new BlockItem(block.get(), new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant())
    );

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().fireResistant()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
