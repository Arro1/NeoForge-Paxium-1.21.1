package net.arro.paxium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.block.entity.ModBlockEntities;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
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

        ModArmorMaterials.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        ModEvents.register(NeoForge.EVENT_BUS);

        ModBlockEntities.register(modEventBus);

        ModAttachmentTypes.register(modEventBus);

        ModMenuTypes.register(modEventBus);

        ModRecipeTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);

        if (FMLEnvironment.dist.isClient()) {
            modEventBus.register(ClientModEvents.class);
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
        private static final ResourceLocation FLIGHT_FUEL_BAR_BACKGROUND_SPRITE =
                ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
        private static final ResourceLocation FLIGHT_FUEL_BAR_PROGRESS_SPRITE =
                ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

        // How far to push the XP bar / health / armor / food / air / etc. up, to make
        // room for the flight fuel bar directly above the hotbar.
        private static final float HUD_SHIFT_PX = 9.0F;

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.STARFORGE_MENU.get(), StarforgeScreen::new);
        }

        // The vanilla SPEAR use-pose (trident wind-up) translates the item wildly upward/back and looks
        // broken on a sword - replace the first-person hand pose entirely with a gentle forward tilt.
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
                    // Tilt the tip forward/down instead of SPEAR's exaggerated pull-back.
                    poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
                    return true;
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
                    ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "flight_fuel_bar"),
                    (guiGraphics, deltaTracker) -> renderFlightFuelBar(guiGraphics));
        }

        private static LayeredDraw.Layer shiftedUp(LayeredDraw.Layer original) {
            return (guiGraphics, deltaTracker) -> {
                if (isFlightFuelBarShown()) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0.0F, -HUD_SHIFT_PX, 0.0F);
                    original.render(guiGraphics, deltaTracker);
                    guiGraphics.pose().popPose();
                } else {
                    original.render(guiGraphics, deltaTracker);
                }
            };
        }

        private static boolean isFlightFuelBarShown() {
            Player player = Minecraft.getInstance().player;
            return player != null && PaxiumArmor.hasFullSet(player);
        }

        private static void renderFlightFuelBar(GuiGraphics guiGraphics) {
            if (!isFlightFuelBarShown()) {
                return;
            }

            Player player = Minecraft.getInstance().player;
            int fuel = player.getData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get());
            int filledWidth = Math.round((float) fuel / ModAttachmentTypes.MAX_FLIGHT_FUEL_TICKS * 182.0F);

            int x = guiGraphics.guiWidth() / 2 - 91;
            int y = guiGraphics.guiHeight() - 30;

            guiGraphics.setColor(1.0F, 0.5F, 0.0F, 1.0F);
            guiGraphics.blitSprite(FLIGHT_FUEL_BAR_BACKGROUND_SPRITE, x, y, 182, 5);
            if (filledWidth > 0) {
                guiGraphics.setColor(1.0F, 0.65F, 0.05F, 1.0F);
                guiGraphics.blitSprite(FLIGHT_FUEL_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, x, y, filledWidth, 5);
            }
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
