package net.arro.paxium.item;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Paxium.MODID);

    public static final Supplier<CreativeModeTab> PAXIUM_TAB = CREATIVE_MODE_TAB.register("paxium_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.PAXIUM.get()))
                    .title(Component.translatable("creativetab.paxium.paxium"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.RAW_PAXIUM.get());
                        output.accept(ModItems.PAXIUM.get());
                        output.accept(ModBlocks.DEEPSLATE_PAXIUM_ORE.get());

                        output.accept(ModBlocks.STARFORGE.get());

                        output.accept(ModItems.PAXIUM_HELMET.get());
                        output.accept(ModItems.PAXIUM_CHESTPLATE.get());
                        output.accept(ModItems.PAXIUM_LEGGINGS.get());
                        output.accept(ModItems.PAXIUM_BOOTS.get());
                        output.accept(ModItems.PAXIUM_SWORD.get());
                        output.accept(ModItems.PAXIUM_BOW.get());

                        output.accept(ModItems.PAXIUM_CAPACITY_UPGRADE_SMITHING_TEMPLATE.get());
                        output.accept(ModItems.PAXIUM_RECHARGE_UPGRADE_SMITHING_TEMPLATE.get());
                        output.accept(ModItems.PAXIUM_BEAM_DAMAGE_UPGRADE_SMITHING_TEMPLATE.get());
                        output.accept(ModItems.PAXIUM_BLAST_UPGRADE_SMITHING_TEMPLATE.get());

                        output.accept(ModItems.PAXIUM_INFUSED_CRYSTAL.get());
                        output.accept(ModItems.UNSTABLE_PAXIUM_CHARGE.get());
                        output.accept(ModItems.REFINED_PAXIUM_CHARGE.get());
                        output.accept(ModBlocks.PAXIUM_BOMB.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
