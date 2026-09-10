package net.arro.paxium.event;

import net.arro.paxium.Paxium;
import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.item.ModArmorMaterials;
import net.arro.paxium.util.ModTags;
import net.arro.paxium.util.PaxiumArmor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModEvents {
    private static final ResourceLocation FLIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "flight");
    private static final AttributeModifier FLIGHT_MODIFIER =
            new AttributeModifier(FLIGHT_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);

    // Players who flew and haven't touched the ground since - fuel stays frozen mid-fall until they land.
    private static final Set<UUID> AWAITING_LANDING = new HashSet<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        if (PaxiumArmor.hasFullSet(player)) {
            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
            updateFlight(player);
            return;
        }

        setFlightAllowed(player, false);

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModTags.Items.HOT_ITEMS)) {
                if (!player.fireImmune()) {
                    player.setRemainingFireTicks(20);
                }
                break;
            }
        }
    }

    private static void updateFlight(Player player) {
        int fuel = player.getData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get());
        int updatedFuel = fuel;

        UUID id = player.getUUID();

        if (player.getAbilities().flying) {
            updatedFuel = Math.max(0, fuel - 1);
            AWAITING_LANDING.add(id);
        } else if (AWAITING_LANDING.contains(id) && !player.onGround()) {
            // Still falling after flight ended - hold off on regenerating until they land.
        } else {
            AWAITING_LANDING.remove(id);
            updatedFuel = Math.min(ModAttachmentTypes.MAX_FLIGHT_FUEL_TICKS, fuel + 1);
        }

        if (updatedFuel != fuel) {
            player.setData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get(), updatedFuel);
        }

        setFlightAllowed(player, updatedFuel > 0);
    }

    private static void setFlightAllowed(Player player, boolean allowed) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null) {
            return;
        }

        if (allowed) {
            flight.addOrUpdateTransientModifier(FLIGHT_MODIFIER);
        } else {
            flight.removeModifier(FLIGHT_MODIFIER_ID);
        }
    }

    @SubscribeEvent
    public void onPlayerTickParticles(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (!level.isClientSide) {
            return;
        }

        if (player.isSpectator() || !PaxiumArmor.hasFullSet(player)) {
            return;
        }

        RandomSource random = level.random;
        AABB box = player.getBoundingBox();
        double x = box.minX + random.nextDouble() * (box.maxX - box.minX);
        double y = box.minY + random.nextDouble() * (box.maxY - box.minY);
        double z = box.minZ + random.nextDouble() * (box.maxZ - box.minZ);

        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        if (event.getSource().is(DamageTypeTags.IS_FIRE) && PaxiumArmor.hasFullSet(entity)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        // Get the item stack that the player is hovering over
        ItemStack stack = event.getItemStack();

        // Check if this item has our "hot_items" tag
        if (stack.is(ModTags.Items.HOT_ITEMS)) {
            // If it does, add a new line of text to its tooltip
            event.getToolTip().add(Component.literal("Dangerously Hot!")
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        }

        if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial() == ModArmorMaterials.PAXIUM) {
            event.getToolTip().add(Component.translatable("tooltip.paxium.fire_immunity_set_bonus")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            event.getToolTip().add(Component.translatable("tooltip.paxium.flight_set_bonus")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    public static void register(IEventBus eventBus) {
        eventBus.register(new ModEvents());
    }

}
