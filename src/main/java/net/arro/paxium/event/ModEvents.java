package net.arro.paxium.event;

import net.arro.paxium.item.ModItems;
import net.arro.paxium.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
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
    public void onItemTooltip(ItemTooltipEvent event) {
        // Get the item stack that the player is hovering over
        ItemStack stack = event.getItemStack();

        // Check if this item has our "hot_items" tag
        if (stack.is(ModTags.Items.HOT_ITEMS)) {
            // If it does, add a new line of text to its tooltip
            event.getToolTip().add(Component.literal("Dangerously Hot!")
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        }
    }

    public static void register(IEventBus eventBus) {
        eventBus.register(new ModEvents());
    }

}
