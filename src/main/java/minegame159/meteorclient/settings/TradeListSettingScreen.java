package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.screens.settings.LeftRightListSettingScreen;
import minegame159.meteorclient.gui.widgets.WWidget;

public class TradeListSettingScreen extends LeftRightListSettingScreen<String> {
    public TradeListSettingScreen(GuiTheme theme, TradeListSetting setting) {
        super(theme, "Select Trades", setting, TradeListSetting.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(String value) {
        return theme.label(value);
    }

    @Override
    protected String getValueName(String value) {
        return value;
    }
}
