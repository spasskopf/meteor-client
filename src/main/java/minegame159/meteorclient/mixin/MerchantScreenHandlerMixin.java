package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.Merchant;
import net.minecraft.village.MerchantInventory;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreenHandler.class)
public class MerchantScreenHandlerMixin {
    @Shadow
    @Final
    private Merchant merchant;

    @Shadow
    @Final
    private MerchantInventory merchantInventory;

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/village/Merchant;)V", at = @At("RETURN"))
    public void onInit(int syncId, PlayerInventory playerInventory, Merchant merchant, CallbackInfo ci) {
        ChatUtils.info("Villager GUI Opened!");
        ChatUtils.info("Number of Offers:%d", merchant.getOffers().size());
        for (TradeOffer offer : merchant.getOffers()) {
            ChatUtils.info(tradeOfferToString(offer), new Object[0]);
        }

    }


    private String tradeOfferToString(TradeOffer offer) {
        StringBuilder b = new StringBuilder();
        b.append("Input: ");
        if (offer.getAdjustedFirstBuyItem() != null) {
            b.append("Adjusted: ");
            b.append(offer.getAdjustedFirstBuyItem().getCount());
            b.append(" of ");
            b.append(offer.getAdjustedFirstBuyItem().getItem().toString());
            b.append(" | ");
        } else if (offer.getOriginalFirstBuyItem() != null) {
            b.append("First: ");
            b.append(offer.getOriginalFirstBuyItem().getCount());
            b.append(" of ");
            b.append(offer.getOriginalFirstBuyItem().getItem().toString());
            b.append(" | ");
        }
        if (offer.getSecondBuyItem() != null) {
            b.append("Second: ");
            b.append(offer.getSecondBuyItem().getCount());
            b.append(" of ");
            b.append(offer.getSecondBuyItem().getItem().toString());
            b.append(" | ");
        }
        b.append("Output");
        if (offer.getMutableSellItem() != null) {
            b.append("Get (mutable): ");
            b.append(offer.getSellItem().getCount());
            b.append(" of ");
            b.append(offer.getSellItem().getItem().toString());
            b.append(" | ");
        } else if (offer.getSecondBuyItem() != null) {
            b.append("Get: ");
            b.append(offer.getSellItem().getCount());
            b.append(" of ");
            b.append(offer.getSellItem().getItem().toString());
            b.append(" | ");
        }
        return b.toString();
    }
}
