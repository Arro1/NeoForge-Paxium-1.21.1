package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Paxium.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.RAW_PAXIUM.get());
        basicItem(ModItems.PAXIUM.get());

        basicItem(ModItems.PAXIUM_HELMET.get());
        basicItem(ModItems.PAXIUM_CHESTPLATE.get());
        basicItem(ModItems.PAXIUM_LEGGINGS.get());
        basicItem(ModItems.PAXIUM_BOOTS.get());

        handheldItem(ModItems.PAXIUM_SWORD.get());

        paxiumBow();

        basicItem(ModItems.PAXIUM_CAPACITY_UPGRADE_SMITHING_TEMPLATE.get());
        basicItem(ModItems.PAXIUM_RECHARGE_UPGRADE_SMITHING_TEMPLATE.get());
    }

    // Mirrors vanilla bow.json / bow_pulling_*.json: an "item/generated" base model carrying the
    // bow's own hand-pose transforms, with three draw-stage overrides swapped in via the vanilla
    // "pulling"/"pull" item properties while PaxiumBowItem's draw is held.
    private void paxiumBow() {
        ItemModelBuilder bow = getBuilder("paxium_bow")
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/paxium_bow"));

        bow.transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                    .rotation(-80, 260, -40).translation(-1, -2, 2.5F).scale(0.9F)
                    .end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                    .rotation(-80, -280, 40).translation(-1, -2, 2.5F).scale(0.9F)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    .rotation(0, -90, 25).translation(1.13F, 3.2F, 1.13F).scale(0.68F)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                    .rotation(0, 90, -25).translation(1.13F, 3.2F, 1.13F).scale(0.68F)
                    .end()
                .end();

        ItemModelBuilder pulling0 = getBuilder("paxium_bow_pulling_0")
                .parent(bow).texture("layer0", modLoc("item/paxium_bow_pulling_0"));
        ItemModelBuilder pulling1 = getBuilder("paxium_bow_pulling_1")
                .parent(bow).texture("layer0", modLoc("item/paxium_bow_pulling_1"));
        ItemModelBuilder pulling2 = getBuilder("paxium_bow_pulling_2")
                .parent(bow).texture("layer0", modLoc("item/paxium_bow_pulling_2"));

        bow.override()
                .predicate(ResourceLocation.withDefaultNamespace("pulling"), 1.0F)
                .model(pulling0).end();
        bow.override()
                .predicate(ResourceLocation.withDefaultNamespace("pulling"), 1.0F)
                .predicate(ResourceLocation.withDefaultNamespace("pull"), 0.65F)
                .model(pulling1).end();
        bow.override()
                .predicate(ResourceLocation.withDefaultNamespace("pulling"), 1.0F)
                .predicate(ResourceLocation.withDefaultNamespace("pull"), 0.9F)
                .model(pulling2).end();
    }
}