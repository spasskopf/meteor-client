package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.settings.TradeListingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class TradeListSetting extends Setting<List<String>> {

    public TradeListSetting(String name, String description, List<String> defaultValue, Consumer<List<String>> onChanged, Consumer<Setting<List<String>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        this.value = new ArrayList<>(defaultValue);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new TradeListingScreen(this));

    }

    @Override
    protected List<String> parseImpl(String str) {
        return new ArrayList<>(Arrays.asList(str.split(",")));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>();
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(List<String> value) {
        return true;
    }


    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (String item : get()) {
            valueTag.add(StringTag.of(item));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<String> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            get().add(tagI.asString());
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<String> defaultValue = new ArrayList<>();
        private Consumer<List<String>> onChanged;
        private Consumer<Setting<List<String>>> onModuleActivated;


        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<String> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<String>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<String>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }


        public TradeListSetting build() {
            return new TradeListSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }

}
