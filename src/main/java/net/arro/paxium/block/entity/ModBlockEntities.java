package net.arro.paxium.block.entity;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Paxium.MODID);

    public static final Supplier<BlockEntityType<StarforgeBlockEntity>> STARFORGE_BE =
            BLOCK_ENTITIES.register("starforge_be", () -> BlockEntityType.Builder.of(
                    StarforgeBlockEntity::new, ModBlocks.STARFORGE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}