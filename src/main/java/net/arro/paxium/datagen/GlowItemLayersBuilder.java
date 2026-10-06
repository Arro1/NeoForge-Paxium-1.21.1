package net.arro.paxium.datagen;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge's {@code neoforge:item_layers} loader with fullbright (emissive) layers, so a glow layer ignores light level.
 */
public class GlowItemLayersBuilder extends CustomLoaderBuilder<ItemModelBuilder> {
    private final List<Integer> emissiveLayers = new ArrayList<>();

    public GlowItemLayersBuilder(ItemModelBuilder parent, ExistingFileHelper existingFileHelper) {
        super(ResourceLocation.fromNamespaceAndPath("neoforge", "item_layers"), parent, existingFileHelper, false);
    }

    public GlowItemLayersBuilder emissive(int layer) {
        emissiveLayers.add(layer);
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        super.toJson(json);
        JsonObject layers = new JsonObject();
        for (int layer : emissiveLayers) {
            JsonObject data = new JsonObject();
            data.addProperty("block_light", 15);
            data.addProperty("sky_light", 15);
            data.addProperty("ambient_occlusion", false);
            layers.add(Integer.toString(layer), data);
        }
        JsonObject neoforgeData = new JsonObject();
        neoforgeData.add("layers", layers);
        json.add("neoforge_data", neoforgeData);
        return json;
    }
}
