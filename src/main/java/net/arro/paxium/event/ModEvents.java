package net.arro.paxium.event;

import net.arro.paxium.Paxium;
import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.item.ModArmorMaterials;
import net.arro.paxium.item.custom.PaxiumBowItem;
import net.arro.paxium.item.custom.PaxiumSwordItem;
import net.arro.paxium.util.ModTags;
import net.arro.paxium.util.PaxiumArmor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

    private static final double FIRE_BEAM_RANGE = 20.0;
    private static final int FIRE_BEAM_FUEL_PER_TICK = 2;
    private static final float FIRE_BEAM_DAMAGE = 6.0F;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        if (player.isCreative()) {
            // Creative already has free flight/immortality; only the beam matters here, and it
            // should fire indefinitely without touching the fuel meter at all.
            updateFireBeamCreative(player);
            return;
        }

        if (PaxiumArmor.hasFullSet(player)) {
            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
            boolean channelingBeam = isChannelingFireBeam(player);
            updateFlight(player, channelingBeam);
            updateFireBeam(player, channelingBeam);
            return;
        }

        setFlightAllowed(player, false);
        stopFireBeamIfChanneling(player);

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModTags.Items.HOT_ITEMS)) {
                if (!player.fireImmune()) {
                    player.setRemainingFireTicks(20);
                }
                break;
            }
        }
    }

    private static void updateFlight(Player player, boolean channelingBeam) {
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
            // Don't passively regenerate while the beam is actively draining the same pool this
            // tick - otherwise the two fight each other and the meter never settles at 0.
            if (!channelingBeam) {
                updatedFuel = Math.min(ModAttachmentTypes.MAX_FLIGHT_FUEL_TICKS, fuel + 1);
            }
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

    private static boolean isChannelingFireBeam(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof PaxiumSwordItem;
    }

    private static void stopFireBeamIfChanneling(Player player) {
        if (isChannelingFireBeam(player)) {
            player.stopUsingItem();
        }
    }

    private static void updateFireBeamCreative(Player player) {
        if (!isChannelingFireBeam(player)) {
            return;
        }

        if (!PaxiumArmor.hasFullSet(player)) {
            player.stopUsingItem();
            return;
        }

        // No fuel read/write at all - indefinite beam in creative.
        fireBeamTick(player);
    }

    private static void updateFireBeam(Player player, boolean channeling) {
        if (!channeling) {
            return;
        }

        int fuel = player.getData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get());
        if (fuel <= 0) {
            player.stopUsingItem();
            return;
        }

        player.setData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get(), Math.max(0, fuel - FIRE_BEAM_FUEL_PER_TICK));
        fireBeamTick(player);
    }

    private static void fireBeamTick(Player player) {
        ServerLevel level = (ServerLevel) player.level();

        Vec3 start = player.getEyePosition();
        Vec3 aim = player.getViewVector(1.0F);
        Vec3 end = start.add(aim.scale(FIRE_BEAM_RANGE));

        BlockHitResult blockHit = level.clip(
                new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
        }

        AABB sweep = new AABB(start, end).inflate(1.0);
        for (Entity candidate : level.getEntities(player, sweep, e -> e instanceof LivingEntity living && living.isAlive())) {
            if (candidate.getBoundingBox().clip(start, end).isPresent()) {
                LivingEntity target = (LivingEntity) candidate;
                boolean damaged = target.hurt(level.damageSources().playerAttack(player), FIRE_BEAM_DAMAGE);
                target.igniteForSeconds(PaxiumSwordItem.IGNITE_SECONDS);
                if (damaged) {
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.GENERIC_BURN, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }

        spawnBeamParticles(level, start, aim, end);
    }

    private static void spawnBeamParticles(ServerLevel level, Vec3 start, Vec3 aim, Vec3 end) {
        RandomSource random = level.random;

        Vec3 trailStart = start.add(aim.scale(1.0));
        double length = trailStart.distanceTo(end);
        int steps = Math.max(1, (int) (length / 0.5));

        for (int i = 0; i <= steps; i++) {
            Vec3 point = trailStart.lerp(end, (double) i / steps);
            double jitter = 0.08;
            double x = point.x + (random.nextDouble() - 0.5) * jitter;
            double y = point.y + (random.nextDouble() - 0.5) * jitter;
            double z = point.z + (random.nextDouble() - 0.5) * jitter;

            level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if (random.nextInt(4) == 0) {
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        level.sendParticles(ParticleTypes.LARGE_SMOKE, end.x, end.y, end.z, 2, 0.15, 0.15, 0.15, 0.01);
        level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
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

        if (stack.getItem() instanceof PaxiumSwordItem) {
            event.getToolTip().add(Component.translatable("tooltip.paxium.sword_ignite")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            event.getToolTip().add(Component.translatable("tooltip.paxium.sword_fire_beam")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }

        if (stack.getItem() instanceof PaxiumBowItem) {
            event.getToolTip().add(Component.translatable("tooltip.paxium.bow_fire_burst")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
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
