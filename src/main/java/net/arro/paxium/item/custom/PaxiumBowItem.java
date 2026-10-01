package net.arro.paxium.item.custom;

import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.entity.custom.PaxiumFireBurstEntity;
import net.arro.paxium.util.PaxiumArmor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Draws and fires like a vanilla bow, but needs no arrows: releasing a full draw spends half of
// the shared Paxium fire-charge meter (see ModAttachmentTypes.FLIGHT_FUEL_TICKS) on a single
// explosive PaxiumFireBurstEntity instead.
public class PaxiumBowItem extends BowItem {
    private static final int FIRE_BURST_FUEL_COST = ModAttachmentTypes.MAX_FLIGHT_FUEL_TICKS / 2;
    private static final int FIRE_COOLDOWN_TICKS = 40;
    private static final float PROJECTILE_VELOCITY = 2.5F;
    private static final float MIN_POWER_TO_FIRE = 0.1F;

    public PaxiumBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!PaxiumArmor.hasFullSet(player)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!player.isCreative() && player.getData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get()) < FIRE_BURST_FUEL_COST) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // Vanilla draw-and-release: a quick tap (power below the usual bow threshold) cancels
    // silently, same as a vanilla bow release too early to loose an arrow.
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        float power = getPowerForTime(getUseDuration(stack, entity) - timeLeft);
        if (power < MIN_POWER_TO_FIRE || !PaxiumArmor.hasFullSet(player)) {
            return;
        }

        if (!player.isCreative()) {
            int fuel = player.getData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get());
            if (fuel < FIRE_BURST_FUEL_COST) {
                return;
            }
            player.setData(ModAttachmentTypes.FLIGHT_FUEL_TICKS.get(), fuel - FIRE_BURST_FUEL_COST);
        }

        if (level instanceof ServerLevel serverLevel) {
            PaxiumFireBurstEntity burst = new PaxiumFireBurstEntity(player, serverLevel,
                    player.getX(), player.getEyeY(), player.getZ());
            burst.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, PROJECTILE_VELOCITY, 1.0F);
            serverLevel.addFreshEntity(burst);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.getCooldowns().addCooldown(this, FIRE_COOLDOWN_TICKS);
    }
}
