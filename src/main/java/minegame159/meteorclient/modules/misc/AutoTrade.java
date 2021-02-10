package minegame159.meteorclient.modules.misc;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.village.TradeOffers;

public class AutoTrade extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    public AutoTrade() {
        super(Category.Misc, "auto-trade", "Automatically trades with Villagers");
    }

    @Override
    public void onActivate() {
        TradeOffers.PROFESSION_TO_LEVELED_TRADE.forEach((villagerProfession, int2ObjectMap) -> {
            ChatUtils.info("Profession: %s", villagerProfession.toString());
            for (Int2ObjectMap.Entry<TradeOffers.Factory[]> entry : int2ObjectMap.int2ObjectEntrySet()) {
                for (TradeOffers.Factory factory : entry.getValue()) {
                    ChatUtils.info("   Factory: %s", factory.getClass().getSimpleName());
                }
            }

        });
    }

    @EventHandler
    private void onVillagerProfessionUpdate(VillagerUpdateProfessionEvent event) {
        ChatUtils.info("Listener Called! Event Type: %s Profession: %s/%s",
                event.action, event.oldData.getProfession(), event.newData.getProfession()
        );

    }


}
