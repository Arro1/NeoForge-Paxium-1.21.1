package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.glow.ModGlow;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
        glowItem(ModItems.RAW_PAXIUM.get(), "item/generated");
        glowItem(ModItems.PAXIUM.get(), "item/generated");

        glowItem(ModItems.PAXIUM_HELMET.get(), "item/generated");
        glowItem(ModItems.PAXIUM_CHESTPLATE.get(), "item/generated");
        glowItem(ModItems.PAXIUM_LEGGINGS.get(), "item/generated");
        glowItem(ModItems.PAXIUM_BOOTS.get(), "item/generated");

        glowItem(ModItems.PAXIUM_SWORD.get(), "item/handheld");

        paxiumBow();

        glowItem(ModItems.PAXIUM_CAPACITY_UPGRADE_SMITHING_TEMPLATE.get(), "item/generated");
        glowItem(ModItems.PAXIUM_RECHARGE_UPGRADE_SMITHING_TEMPLATE.get(), "item/generated");
        glowItem(ModItems.PAXIUM_BEAM_DAMAGE_UPGRADE_SMITHING_TEMPLATE.get(), "item/generated");
        glowItem(ModItems.PAXIUM_BLAST_UPGRADE_SMITHING_TEMPLATE.get(), "item/generated");

        glowItem(ModItems.PAXIUM_INFUSED_CRYSTAL.get(), "item/generated");
        glowItem(ModItems.UNSTABLE_PAXIUM_CHARGE.get(), "item/generated");
        glowItem(ModItems.REFINED_PAXIUM_CHARGE.get(), "item/generated");
    }

    // Builds an item model from its texture and, if the texture is registered in ModGlow, adds the animated
    // glow texture as a fullbright second layer. Use this for every item model so new glowing items just work.
    private ItemModelBuilder glowItem(Item item, String parent) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        ItemModelBuilder builder = getBuilder(key.getPath()).parent(new ModelFile.UncheckedModelFile(parent));
        return withGlow(builder, ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "item/" + key.getPath()));
    }

    private ItemModelBuilder withGlow(ItemModelBuilder builder, ResourceLocation baseTexture) {
        builder.texture("layer0", baseTexture);
        ModGlow.get(baseTexture).ifPresent(glow -> {
            builder.texture("layer1", glow.glow());
            builder.customLoader(GlowItemLayersBuilder::new).emissive(1).end();
        });
        return builder;
    }

    // Mirrors vanilla bow.json / bow_pulling_*.json: an "item/generated" base model carrying the
    // bow's own hand-pose transforms, with three draw-stage overrides swapped in via the vanilla
    // "pulling"/"pull" item properties while PaxiumBowItem's draw is held.
    private void paxiumBow() {
        ItemModelBuilder bow = getBuilder("paxium_bow")
                .parent(new ModelFile.UncheckedModelFile("item/generated"));
        withGlow(bow, modLoc("item/paxium_bow"));

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

        ItemModelBuilder pulling0 = withGlow(getBuilder("paxium_bow_pulling_0").parent(bow),
                modLoc("item/paxium_bow_pulling_0"));
        ItemModelBuilder pulling1 = withGlow(getBuilder("paxium_bow_pulling_1").parent(bow),
                modLoc("item/paxium_bow_pulling_1"));
        ItemModelBuilder pulling2 = withGlow(getBuilder("paxium_bow_pulling_2").parent(bow),
                modLoc("item/paxium_bow_pulling_2"));

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