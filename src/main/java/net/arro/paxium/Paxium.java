package net.arro.paxium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.block.entity.ModBlockEntities;
import net.arro.paxium.block.entity.renderer.StarforgeBlockEntityRenderer;
import net.arro.paxium.client.PaxiumArmPoses;
import net.arro.paxium.client.render.PaxiumArmorGlowLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.arro.paxium.component.ModDataComponents;
import net.arro.paxium.entity.ModEntities;
import net.arro.paxium.client.PaxiumClientHelper;
import net.arro.paxium.entity.client.PaxiumBlastRenderer;
import net.arro.paxium.entity.client.PaxiumFireBurstRenderer;
import net.arro.paxium.entity.client.PrimedPaxiumBombRenderer;
import net.arro.paxium.event.ModEvents;
import net.arro.paxium.item.ModArmorMaterials;
import net.arro.paxium.item.ModCreativeModeTabs;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.item.custom.PaxiumSwordItem;
import net.arro.paxium.recipe.ModRecipeSerializers;
import net.arro.paxium.recipe.ModRecipeTypes;
import net.arro.paxium.screen.ModMenuTypes;
import net.arro.paxium.screen.custom.StarforgeMenu;
import net.arro.paxium.screen.custom.StarforgeScreen;
import net.arro.paxium.util.PaxiumArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Paxium.MODID)
public class Paxium {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "paxium";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Paxium(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModDataComponents.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        ModEvents.register(NeoForge.EVENT_BUS);

        ModBlockEntities.register(modEventBus);

        ModEntities.register(modEventBus);

        ModAttachmentTypes.register(modEventBus);

        ModMenuTypes.register(modEventBus);

        ModRecipeTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.register(ClientModEvents.class);
            // Paxium Bomb camera shake.
            NeoForge.EVENT_BUS.addListener(PaxiumClientHelper::onComputeCameraAngles);
        }

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    public static class ClientModEvents {
        private static final ResourceLocation FIRE_METER_BAR_BACKGROUND_SPRITE =
                ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
        private static final ResourceLocation FIRE_METER_BAR_PROGRESS_SPRITE =
                ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

        // How far to push the XP bar / health / armor / food / air / etc. up, to make
        // room for the Fire Meter bar directly above the hotbar.
        private static final float HUD_SHIFT_PX = 9.0F;

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.STARFORGE_MENU.get(), StarforgeScreen::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.PAXIUM_FIRE_BURST.get(), PaxiumFireBurstRenderer::new);
            event.registerEntityRenderer(ModEntities.PRIMED_PAXIUM_BOMB.get(), PrimedPaxiumBombRenderer::new);
            event.registerEntityRenderer(ModEntities.PAXIUM_BLAST.get(), PaxiumBlastRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.STARFORGE_BE.get(), StarforgeBlockEntityRenderer::new);
        }

        // Glowing, animated fire veins on worn Paxium armor (players, armor stands, armored zombies/skeletons).
        @SubscribeEvent
        public static void addArmorGlowLayers(EntityRenderersEvent.AddLayers event) {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    addGlowLayer(renderer);
                }
            }
            if (event.getRenderer(EntityType.ARMOR_STAND) instanceof ArmorStandRenderer renderer) {
                renderer.addLayer(new PaxiumArmorGlowLayer<>(renderer,
                        PaxiumArmorGlowLayer.armorStandModel(true), PaxiumArmorGlowLayer.armorStandModel(false)));
            }
            if (event.getRenderer(EntityType.ZOMBIE) instanceof ZombieRenderer renderer) {
                addGlowLayer(renderer);
            }
            if (event.getRenderer(EntityType.SKELETON) instanceof SkeletonRenderer renderer) {
                addGlowLayer(renderer);
            }
        }

        private static <T extends LivingEntity, M extends HumanoidModel<T>> void addGlowLayer(LivingEntityRenderer<T, M> renderer) {
            renderer.addLayer(new PaxiumArmorGlowLayer<>(renderer,
                    PaxiumArmorGlowLayer.<T>humanoidModel(true), PaxiumArmorGlowLayer.<T>humanoidModel(false)));
        }

        // Vanilla only wires the "pulling"/"pull" item-property functions to the literal Items.BOW
        // instance (see ItemProperties.register calls in vanilla), not to every BowItem subclass -
        // without this, the paxium_bow.json overrides never fire and the draw animation never swaps.
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemProperties.register(ModItems.PAXIUM_BOW.get(), ResourceLocation.withDefaultNamespace("pull"),
                        (stack, level, entity, seed) -> {
                            if (entity == null) {
                                return 0.0F;
                            }
                            return entity.getUseItem() != stack ? 0.0F
                                    : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                        });
                ItemProperties.register(ModItems.PAXIUM_BOW.get(), ResourceLocation.withDefaultNamespace("pulling"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack
                                ? 1.0F : 0.0F);
            });
        }

        // Fire-beam channel pose. First person: replace the hand pose entirely with a gentle forward tilt.
        // Third person: a custom arm pose (the sword's UseAnim is NONE so PlayerRenderer reaches getArmPose).
        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            event.registerItem(new IClientItemExtensions() {
                @Override
                public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                                         ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
                    if (!(player.isUsingItem() && player.getUseItem().getItem() instanceof PaxiumSwordItem)) {
                        return false;
                    }

                    int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                    // Same base "held in hand" offset vanilla applies before any use-animation extras.
                    poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
                    // Tilt the tip slightly forward/down while channeling.
                    poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
                    return true;
                }

                // Third person (others, or F5): arm raised forward as if aiming the beam.
                @Override
                public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                    return entity.isUsingItem() && entity.getUseItem() == stack ? PaxiumArmPoses.FIRE_BEAM.getValue() : null;
                }
            }, ModItems.PAXIUM_SWORD.get());
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.wrapLayer(VanillaGuiLayers.EXPERIENCE_BAR, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.JUMP_METER, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.PLAYER_HEALTH, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.ARMOR_LEVEL, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.FOOD_LEVEL, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.VEHICLE_HEALTH, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.AIR_LEVEL, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.SELECTED_ITEM_NAME, ClientModEvents::shiftedUp);
            event.wrapLayer(VanillaGuiLayers.EXPERIENCE_LEVEL, ClientModEvents::shiftedUp);

            event.registerAbove(
                    VanillaGuiLayers.AIR_LEVEL,
                    ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "fire_meter_bar"),
                    (guiGraphics, deltaTracker) -> renderFireMeterBar(guiGraphics));
        }

        private static LayeredDraw.Layer shiftedUp(LayeredDraw.Layer original) {
            return (guiGraphics, deltaTracker) -> {
                if (isFireMeterBarShown()) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0.0F, -HUD_SHIFT_PX, 0.0F);
                    original.render(guiGraphics, deltaTracker);
                    guiGraphics.pose().popPose();
                } else {
                    original.render(guiGraphics, deltaTracker);
                }
            };
        }

        private static boolean isFireMeterBarShown() {
            Player player = Minecraft.getInstance().player;
            return player != null && !player.isSpectator() && PaxiumArmor.hasFullSet(player);
        }

        private static void renderFireMeterBar(GuiGraphics guiGraphics) {
            if (!isFireMeterBarShown()) {
                return;
            }

            Player player = Minecraft.getInstance().player;
            float meter = player.getData(ModAttachmentTypes.FIRE_METER.get());
            int capacity = PaxiumArmor.getFireMeterCapacity(player);
            int filledWidth = Math.round(Math.min(1.0F, meter / capacity) * 182.0F);

            int x = guiGraphics.guiWidth() / 2 - 91;
            int y = guiGraphics.guiHeight() - 30;

            guiGraphics.setColor(1.0F, 0.5F, 0.0F, 1.0F);
            guiGraphics.blitSprite(FIRE_METER_BAR_BACKGROUND_SPRITE, x, y, 182, 5);
            if (filledWidth > 0) {
                guiGraphics.setColor(1.0F, 0.65F, 0.05F, 1.0F);
                guiGraphics.blitSprite(FIRE_METER_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, x, y, filledWidth, 5);
            }
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
