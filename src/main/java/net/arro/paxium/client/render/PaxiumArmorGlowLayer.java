package net.arro.paxium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.arro.paxium.Paxium;
import net.arro.paxium.util.PaxiumArmor;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Re-renders the Paxium armor with an emissive, vein-only texture so the fire veins glow in the dark.
 * Each vein pulses (a different phase per armor piece) and a bright heat wave travels up the body.
 */
public class PaxiumArmorGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation OUTER_GLOW = glowTexture(1);
    private static final ResourceLocation INNER_GLOW = glowTexture(2);

    private static final EquipmentSlot[] SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // Vanilla armor deformations (inner = leggings, outer = everything else) plus a hair, so the glow shell sits
    // the same tiny distance in front of the base armor everywhere instead of z-fighting with it.
    private static final float GLOW_EXPANSION = 0.06F;
    private static final CubeDeformation INNER_DEFORMATION = new CubeDeformation(0.5F + GLOW_EXPANSION);
    private static final CubeDeformation OUTER_DEFORMATION = new CubeDeformation(1.0F + GLOW_EXPANSION);

    private final HumanoidModel<T> innerModel;
    private final HumanoidModel<T> outerModel;

    public PaxiumArmorGlowLayer(RenderLayerParent<T, M> parent, HumanoidModel<T> innerModel, HumanoidModel<T> outerModel) {
        super(parent);
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    /** Armor model for the glow shell of a standard humanoid (players, zombies, skeletons). */
    public static <E extends LivingEntity> HumanoidModel<E> humanoidModel(boolean inner) {
        return new HumanoidModel<>(LayerDefinition.create(
                HumanoidModel.createMesh(inner ? INNER_DEFORMATION : OUTER_DEFORMATION, 0.0F), 64, 32).bakeRoot());
    }

    public static ArmorStandArmorModel armorStandModel(boolean inner) {
        return new ArmorStandArmorModel(
                ArmorStandArmorModel.createBodyLayer(inner ? INNER_DEFORMATION : OUTER_DEFORMATION).bakeRoot());
    }

    private static ResourceLocation glowTexture(int layer) {
        return ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "textures/models/armor/paxium_layer_" + layer + "_glow.png");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean fullSet = PaxiumArmor.hasFullSet(entity);
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!PaxiumArmor.isPaxiumArmor(stack)) {
                continue;
            }
            boolean inner = slot == EquipmentSlot.LEGS;
            HumanoidModel<T> model = inner ? innerModel : outerModel;
            getParentModel().copyPropertiesTo(model);
            setPartVisibility(model, slot);

            VertexConsumer glow = new GlowVertexConsumer(
                    buffer.getBuffer(RenderType.eyes(inner ? INNER_GLOW : OUTER_GLOW)),
                    entity.tickCount + partialTicks, slot.getIndex() * 1.7F, fullSet);
            model.renderToBuffer(poseStack, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        }
    }

    private static void setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }

    /**
     * Modulates the vertex colour of the (already hot-coloured) glow texture. Animation is driven only by time and the
     * texture coordinates, so it looks identical in-world, in third person and in the inventory preview.
     */
    private static final class GlowVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float time;
        private final float phase;
        private final float strength;

        private int red = 255, green = 255, blue = 255, alpha = 255;

        GlowVertexConsumer(VertexConsumer delegate, float time, float phase, boolean fullSet) {
            this.delegate = delegate;
            this.time = time;
            this.phase = phase;
            this.strength = fullSet ? 1.0F : 0.85F;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            // v runs down the texture, so the wave sweeps along the armor pieces.
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.11F + phase);
            float wave = Math.max(0.0F, Mth.sin(v * 40.0F - time * 0.16F + u * 9.0F));
            wave = wave * wave * wave;
            float flicker = 0.03F * Mth.sin(time * 1.9F + (u + v) * 70.0F);

            // Veins stay red; only the wave peaks drift toward orange (and, at the very top, yellow).
            float heat = Mth.clamp((0.32F + 0.18F * pulse + 0.2F * wave + flicker) * strength, 0.2F, 0.8F);
            float shift = wave * strength;
            int r = (int) (red * heat);
            // Scale the tint by pixel brightness so dark vein edges stay dark and the shading survives.
            float lum = red / 255.0F;
            int g = (int) (green * heat + shift * 55.0F * lum * lum);
            int b = (int) (blue * heat + shift * shift * 8.0F * lum * lum);
            delegate.setColor(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255), alpha);
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
