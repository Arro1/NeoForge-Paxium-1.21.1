package net.arro.paxium.item.custom;

import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.util.PaxiumArmor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class PaxiumSwordItem extends SwordItem {
    // Public: shared with ModEvents' fire-beam ignite-on-hit.
    public static final float IGNITE_SECONDS = 5.0F;

    // Applied every time the beam channel ends, for any reason, so it can't be restarted in a rapid burst.
    private static final int FIRE_BEAM_COOLDOWN_TICKS = 20;

    public PaxiumSwordItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        target.igniteForSeconds(IGNITE_SECONDS);
        return result;
    }

    // Starts the fire-beam channel; ModEvents#updateFireBeam owns the per-tick drain/damage/stop logic.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!PaxiumArmor.hasFullSet(player)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!player.isCreative()) {
            float meter = player.getData(ModAttachmentTypes.FIRE_METER.get());
            if (meter <= 0.0F) {
                return InteractionResultHolder.fail(stack);
            }
        }

        // player, not null: client plays it locally only for the shooter, server broadcasts to
        // everyone else - together exactly one play, no double-up between the two logical sides.
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    // NONE, not SPEAR: PlayerRenderer maps SPEAR straight to the overhead THROW_SPEAR pose before it
    // ever asks IClientItemExtensions#getArmPose, which would hide PaxiumArmPoses.FIRE_BEAM. First
    // person doesn't depend on this - applyForgeHandTransform replaces that pose entirely.
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // Fires exactly once whenever the channel ends, for any reason: releasing right-click, the Fire Meter
    // running out, or losing the full set mid-beam. Puts the sword on a short cooldown either way, so
    // hitting an empty meter while still holding the button can't immediately restart the beam for a burst of
    // tiny re-triggers.
    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, FIRE_BEAM_COOLDOWN_TICKS);
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
