package minegame159.meteorclient.settings;

import com.mojang.serialization.Lifecycle;
import minegame159.meteorclient.utils.entity.TradeUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.apache.logging.log4j.core.util.ObjectArrayIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class TradeListSetting extends Setting<List<String>> {

    public static final Registry<String> REGISTRY = new TradeRegistry();

    public TradeListSetting(String name, String description, List<String> defaultValue, Consumer<List<String>> onChanged, Consumer<Setting<List<String>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        this.value = new ArrayList<>(defaultValue);

    }

    @Override
    protected List<String> parseImpl(String str) {
        return new ArrayList<>(Arrays.asList(str.split(",")));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>();
        if (callbacks) {
            changed();
        }
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

    private static class TradeRegistry extends Registry<String> {

        public TradeRegistry() {
            super(RegistryKey.ofRegistry(new Identifier("meteor-client", "villager-trades")), Lifecycle.stable());
        }

        @Nullable
        @Override
        public Identifier getId(String entry) {
            return null;
        }

        @Override
        public Optional<RegistryKey<String>> getKey(String entry) {
            return Optional.empty();
        }

        @Override
        public int getRawId(@Nullable String entry) {
            return 0;
        }

        @Nullable
        @Override
        public String get(int index) {
            return null;
        }

        @Nullable
        @Override
        public String get(@Nullable RegistryKey<String> key) {
            return null;
        }

        @Nullable
        @Override
        public String get(@Nullable Identifier id) {
            return null;
        }

        @Override
        protected Lifecycle getEntryLifecycle(String object) {
            return null;
        }

        @Override
        public Lifecycle getLifecycle() {
            return null;
        }

        @Override
        public Set<Identifier> getIds() {
            return null;
        }

        @Override
        public Set<Map.Entry<RegistryKey<String>, String>> getEntries() {
            return null;
        }

        @Override
        public boolean containsId(Identifier id) {
            return false;
        }

        @NotNull
        @Override
        public Iterator<String> iterator() {
            return new ObjectArrayIterator<>(TradeUtils.TRADES_AS_STRING.toArray(new String[0]));
        }
    }
}
