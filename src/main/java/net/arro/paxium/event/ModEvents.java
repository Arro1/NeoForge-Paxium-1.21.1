package net.arro.paxium.event;

import net.arro.paxium.item.ModArmorMaterials;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class ModEvents {
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        if (hasFullPaxiumSet(player)) {
            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModTags.Items.HOT_ITEMS)) {
                if (!player.fireImmune()) {
                    player.setRemainingFireTicks(20);
                }
                break;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTickParticles(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (!level.isClientSide) {
            return;
        }

        if (player.isSpectator() || !hasFullPaxiumSet(player)) {
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

        if (event.getSource().is(DamageTypeTags.IS_FIRE) && hasFullPaxiumSet(entity)) {
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
        }
    }

    private static boolean hasFullPaxiumSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PAXIUM_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PAXIUM_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PAXIUM_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PAXIUM_BOOTS.get());
    }

    public static void register(IEventBus eventBus) {
        eventBus.register(new ModEvents());
    }

}
