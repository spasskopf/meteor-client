package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.TradeListSetting;
import minegame159.meteorclient.utils.entity.TradeUtils;

public class TradeListingScreen extends LeftRightListSettingScreen<String> {
    public TradeListingScreen(TradeListSetting setting) {
        super("Select Trades", setting, TradeUtils.TRADES_AS_STRING);
    }


    @Override
    protected WWidget getValueWidget(String value) {
        return new WLabel(value);
    }

    @Override
    protected String getValueName(String value) {
        return value;
    }


}
