package net.arro.paxium.event;

import net.arro.paxium.Paxium;
import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.client.PaxiumClientHelper;
import net.arro.paxium.component.FireMeterUpgrades;
import net.arro.paxium.component.PaxiumUpgradeStat;
import net.arro.paxium.entity.custom.PaxiumFireBurstEntity;
import net.arro.paxium.item.ModArmorMaterials;
import net.arro.paxium.item.custom.PaxiumBowItem;
import net.arro.paxium.item.custom.PaxiumSwordItem;
import net.arro.paxium.util.ModTags;
import net.arro.paxium.util.PaxiumArmor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
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

    // Players who flew and haven't touched the ground since - the Fire Meter stays frozen mid-fall until they land.
    private static final Set<UUID> AWAITING_LANDING = new HashSet<>();

    private static final double FIRE_BEAM_RANGE = 20.0;
    private static final float FIRE_BEAM_METER_PER_TICK = 2.0F;
    private static final float FIRE_BEAM_DAMAGE = 6.0F;
    // Per Beam Damage level on the sword; meter drain is unaffected.
    public static final float BEAM_DAMAGE_PER_LEVEL = 2.0F;

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
            // should fire indefinitely without touching the Fire Meter at all.
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
        float meter = player.getData(ModAttachmentTypes.FIRE_METER.get());
        int capacity = PaxiumArmor.getFireMeterCapacity(player);
        // Clamp first so swapping in a lower-capacity piece trims an overfull meter.
        float updatedMeter = Math.min(meter, capacity);

        UUID id = player.getUUID();

        if (player.getAbilities().flying) {
            updatedMeter = Math.max(0.0F, updatedMeter - 1.0F);
            AWAITING_LANDING.add(id);
        } else if (AWAITING_LANDING.contains(id) && !player.onGround()) {
            // Still falling after flight ended - hold off on regenerating until they land.
        } else {
            AWAITING_LANDING.remove(id);
            // Don't passively regenerate while the beam is actively draining the same pool this
            // tick - otherwise the two fight each other and the meter never settles at 0.
            if (!channelingBeam) {
                updatedMeter = Math.min(capacity, updatedMeter + PaxiumArmor.getFireMeterRechargeRate(player));
            }
        }

        if (updatedMeter != meter) {
            player.setData(ModAttachmentTypes.FIRE_METER.get(), updatedMeter);
        }

        setFlightAllowed(player, updatedMeter > 0.0F);
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

        // No Fire Meter read/write at all - indefinite beam in creative.
        fireBeamTick(player);
    }

    private static void updateFireBeam(Player player, boolean channeling) {
        if (!channeling) {
            return;
        }

        float meter = player.getData(ModAttachmentTypes.FIRE_METER.get());
        if (meter <= 0.0F) {
            player.stopUsingItem();
            return;
        }

        player.setData(ModAttachmentTypes.FIRE_METER.get(), Math.max(0.0F, meter - FIRE_BEAM_METER_PER_TICK));
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

        float damage = FIRE_BEAM_DAMAGE
                + BEAM_DAMAGE_PER_LEVEL * PaxiumUpgradeStat.BEAM_DAMAGE.getLevel(player.getUseItem());

        AABB sweep = new AABB(start, end).inflate(1.0);
        for (Entity candidate : level.getEntities(player, sweep, e -> e instanceof LivingEntity living && living.isAlive())) {
            if (candidate.getBoundingBox().clip(start, end).isPresent()) {
                LivingEntity target = (LivingEntity) candidate;
                boolean damaged = target.hurt(level.damageSources().playerAttack(player), damage);
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
        double height = box.maxY - box.minY;

        // Your own camera sits inside the bounding box in first person, so anything spawned there
        // ends up in your face: keep it to small flames around the feet, and no rising smoke.
        if (PaxiumClientHelper.isLocalFirstPerson(player)) {
            if (random.nextInt(4) == 0) {
                Vec3 point = randomPerimeterPoint(box, random, 0.1, box.minY + random.nextDouble() * height * 0.35);
                level.addParticle(ParticleTypes.SMALL_FLAME, point.x, point.y, point.z, 0.0, 0.0, 0.0);
            }
            return;
        }

        // Seen from outside: a full-body aura on the edge of the body, smoke only from the lower half.
        if (random.nextInt(3) == 0) {
            Vec3 point = randomPerimeterPoint(box, random, 0.05, box.minY + random.nextDouble() * height);
            level.addParticle(ParticleTypes.FLAME, point.x, point.y, point.z, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(5) == 0) {
            Vec3 point = randomPerimeterPoint(box, random, 0.05, box.minY + random.nextDouble() * height * 0.5);
            level.addParticle(ParticleTypes.SMOKE, point.x, point.y, point.z, 0.0, 0.0, 0.0);
        }
    }

    // A point on one of the box's four vertical faces at height y, pushed `outset` outward.
    private static Vec3 randomPerimeterPoint(AABB box, RandomSource random, double outset, double y) {
        double t = random.nextDouble();
        return switch (random.nextInt(4)) {
            case 0 -> new Vec3(box.minX - outset, y, Mth.lerp(t, box.minZ, box.maxZ));
            case 1 -> new Vec3(box.maxX + outset, y, Mth.lerp(t, box.minZ, box.maxZ));
            case 2 -> new Vec3(Mth.lerp(t, box.minX, box.maxX), y, box.minZ - outset);
            default -> new Vec3(Mth.lerp(t, box.minX, box.maxX), y, box.maxZ + outset);
        };
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

        if (stack.is(ModItems.PAXIUM_INFUSED_CRYSTAL.get())) {
            event.getToolTip().add(Component.translatable("tooltip.paxium.infused_crystal_refine")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        if (stack.is(ModItems.UNSTABLE_PAXIUM_CHARGE.get()) || stack.is(ModItems.REFINED_PAXIUM_CHARGE.get())) {
            int stage = stack.is(ModItems.UNSTABLE_PAXIUM_CHARGE.get()) ? 1 : 2;
            event.getToolTip().add(Component.translatable("tooltip.paxium.charge_stage",
                            Component.translatable("enchantment.level." + stage), Component.translatable("enchantment.level.3"))
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.paxium.charge_refine")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        if (stack.is(ModBlocks.PAXIUM_BOMB.get().asItem())) {
            event.getToolTip().add(Component.translatable("tooltip.paxium.bomb_ignite")
                    .withStyle(ChatFormatting.RED));
            event.getToolTip().add(Component.translatable("tooltip.paxium.bomb_warning")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }

        if (stack.getItem() instanceof PaxiumSwordItem) {
            event.getToolTip().add(Component.translatable("tooltip.paxium.sword_ignite")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            event.getToolTip().add(Component.translatable("tooltip.paxium.sword_fire_beam")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));

            int level = PaxiumUpgradeStat.BEAM_DAMAGE.getLevel(stack);
            event.getToolTip().add(Component.translatable("tooltip.paxium.weapon_upgrades").withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(upgradeLine("tooltip.paxium.beam_damage", level,
                    Component.translatable("tooltip.paxium.beam_damage_bonus", Math.round(level * BEAM_DAMAGE_PER_LEVEL))));
        }

        if (stack.getItem() instanceof PaxiumBowItem) {
            event.getToolTip().add(Component.translatable("tooltip.paxium.bow_fire_burst")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));

            int level = PaxiumUpgradeStat.BLAST.getLevel(stack);
            event.getToolTip().add(Component.translatable("tooltip.paxium.weapon_upgrades").withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(upgradeLine("tooltip.paxium.blast", level,
                    Component.translatable("tooltip.paxium.blast_bonus",
                            Math.round(level * PaxiumFireBurstEntity.DAMAGE_PER_LEVEL),
                            Math.round(level * PaxiumFireBurstEntity.POWER_PER_LEVEL / PaxiumFireBurstEntity.BASE_EXPLOSION_POWER * 100))));
        }

        if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial() == ModArmorMaterials.PAXIUM) {
            FireMeterUpgrades upgrades = PaxiumArmor.getUpgrades(stack);
            event.getToolTip().add(Component.translatable("tooltip.paxium.fire_meter_upgrades")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(upgradeLine("tooltip.paxium.fire_meter_capacity", upgrades.capacity(),
                    Component.translatable("tooltip.paxium.fire_meter_capacity_bonus",
                            upgrades.capacity() * ModAttachmentTypes.CAPACITY_PER_LEVEL)));
            event.getToolTip().add(upgradeLine("tooltip.paxium.fire_meter_recharge", upgrades.recharge(),
                    Component.translatable("tooltip.paxium.fire_meter_recharge_bonus",
                            Math.round(upgrades.recharge() * ModAttachmentTypes.RECHARGE_BONUS_PER_LEVEL * 100))));

            event.getToolTip().add(Component.translatable("tooltip.paxium.fire_immunity_set_bonus")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            event.getToolTip().add(Component.translatable("tooltip.paxium.fire_meter_set_bonus")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    // "  Capacity  ■■□  II  +50" - filled pips gold, empty pips dark gray, bonus only once upgraded.
    private static Component upgradeLine(String labelKey, int level, Component bonus) {
        MutableComponent line = Component.literal("  ")
                .append(Component.translatable(labelKey).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  "))
                .append(Component.literal("■".repeat(level)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("□".repeat(PaxiumUpgradeStat.MAX_LEVEL - level)).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("  "));

        if (level == 0) {
            return line.append(Component.literal("-").withStyle(ChatFormatting.DARK_GRAY));
        }

        return line.append(Component.translatable("enchantment.level." + level).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("  "))
                .append(bonus.copy().withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void register(IEventBus eventBus) {
        eventBus.register(new ModEvents());
    }

}
