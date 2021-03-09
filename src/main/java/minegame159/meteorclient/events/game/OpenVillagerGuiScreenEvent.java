package minegame159.meteorclient.events.game;

import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.Merchant;
import net.minecraft.village.MerchantInventory;

public class OpenVillagerGuiScreenEvent {

    private static final OpenVillagerGuiScreenEvent INSTANCE = new OpenVillagerGuiScreenEvent();

    public Merchant merchant;
    public MerchantInventory merchantInventory;
    public MerchantScreenHandler merchantScreenHandler;


    public static OpenVillagerGuiScreenEvent get(Merchant merchant, MerchantInventory merchantInventory, MerchantScreenHandler merchantScreenHandlerMixin) {
        INSTANCE.merchant = merchant;
        INSTANCE.merchantInventory = merchantInventory;
        INSTANCE.merchantScreenHandler = merchantScreenHandlerMixin;
        return INSTANCE;
    }
}
