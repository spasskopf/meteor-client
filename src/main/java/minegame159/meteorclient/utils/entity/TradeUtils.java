package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.modules.misc.AutoTrade;
import minegame159.meteorclient.utils.misc.MathUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TradeUtils {
    public static final List<ItemStack> TRADES;
    public static final List<String> TRADES_AS_STRING;

    static {
        TRADES = new ArrayList<>();
        TRADES.add(asItemStack(Items.EMERALD));

        TRADES.addAll(getLibrarian());
        TRADES.addAll(getArmorer());
        TRADES.addAll(getButcher());
        TRADES.addAll(getCartographer());
        TRADES.addAll(getCleric());
        TRADES.addAll(getFarmer());
        TRADES.addAll(getFisherman());
        TRADES.addAll(getFletcher());
        TRADES.addAll(getLeatherworker());
        TRADES.addAll(getMason());
        TRADES.addAll(getShepherd());
        TRADES.addAll(getToolsmith());
        TRADES.addAll(getWeaponsmith());

        TRADES_AS_STRING = new ArrayList<>();
        for (ItemStack trade : TRADES) {
            TRADES_AS_STRING.add(toString(trade));
        }

    }


    public static List<ItemStack> getButcher() {
        return asList(asItemStack(Items.RABBIT_STEW));
    }

    public static List<ItemStack> getCartographer() {
        return asList(asItemStack(Items.MAP));
    }

    public static List<ItemStack> getCleric() {
        return asList(asItemStack(Items.REDSTONE));
    }

    public static List<ItemStack> getFarmer() {
        return asList(asItemStack(Items.BREAD));
    }

    public static List<ItemStack> getFisherman() {
        return asList(asItemStackArray(Items.COOKED_COD, Items.COD_BUCKET));
    }

    public static List<ItemStack> getFletcher() {
        return asList(asItemStackArray(Items.ARROW, Items.FLINT));
    }

    public static List<ItemStack> getLeatherworker() {
        //If somebody really wants all possible color combinations:
        //Use search, find flowers and dye armor.
        return asList(asItemStackArray(Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE));
    }

    public static List<ItemStack> getMason() {
        return asList(asItemStack(Items.BRICK));
    }

    public static List<ItemStack> getShepherd() {
        return asList(asItemStack(Items.SHEARS));
    }

    public static List<ItemStack> getToolsmith() {
        return asList(asItemStackArray(
                Items.STONE_AXE,
                Items.STONE_SHOVEL,
                Items.STONE_PICKAXE,
                Items.STONE_HOE));
    }

    public static List<ItemStack> getWeaponsmith() {
        final ArrayList<ItemStack> list = new ArrayList<>();
        list.add(asItemStack(Items.IRON_AXE));
        list.add(asItemStack(Items.IRON_SWORD));
        //I just hope no one wants to have iron swords...
        //Because Random#nextFloat is a bit too much to simulate...
        return list;
    }

    public static List<ItemStack> getArmorer() {
        return asList(
                asItemStackArray(Items.COAL,
                        Items.IRON_LEGGINGS,
                        Items.IRON_BOOTS,
                        Items.IRON_HELMET,
                        Items.IRON_CHESTPLATE)
        );
    }


    public static List<ItemStack> getLibrarian() {
        final List<Enchantment> list = (List) Registry.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).collect(Collectors.toList());
        final List<ItemStack> trades = new ArrayList<>();
        for (Enchantment enchantment : list) {
            for (int i : MathUtils.mathHelperNextInt(enchantment.getMinLevel(), enchantment.getMaxLevel())) {
                final ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, i));
                trades.add(itemStack);
            }
        }
        return trades;
    }

    private static ItemStack asItemStack(ItemConvertible item) {
        return new ItemStack(item);
    }

    private static ItemStack[] asItemStackArray(ItemConvertible... items) {
       final ItemStack[] r = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            r[i] = asItemStack(items[i]);
        }
        return r;
    }

    private static List<ItemStack> asList(ItemStack... array) {
        return new ArrayList<>(Arrays.asList(array));
    }

    public static String toString(VillagerData villagerData) {
        return String.format(
                "Type %s | Profession %s | lvl %d",
                villagerData.getType().toString(),
                villagerData.getProfession().toString(),
                villagerData.getLevel()
        );

    }


    public enum Professions {
        ARMORER(VillagerProfession.ARMORER, Items.BLAST_FURNACE),
        BUTCHER(VillagerProfession.BUTCHER, Items.SMOKER),
        CARTOGRAPHER(VillagerProfession.CARTOGRAPHER, Items.CARTOGRAPHY_TABLE),
        CLERIC(VillagerProfession.CLERIC, Items.BREWING_STAND),
        FARMER(VillagerProfession.FARMER, Items.COMPOSTER),
        FISHERMAN(VillagerProfession.FISHERMAN, Items.BARREL),
        FLETCHER(VillagerProfession.FLETCHER, Items.FLETCHING_TABLE),
        LEATHERWORKER(VillagerProfession.LEATHERWORKER, Items.CAULDRON),
        LIBRARIAN(VillagerProfession.LIBRARIAN, Items.LECTERN),
        SHEPHERD(VillagerProfession.SHEPHERD, Items.LOOM),
        MASON(VillagerProfession.MASON, Items.STONECUTTER),
        WEAPONSMITH(VillagerProfession.WEAPONSMITH, Items.GRINDSTONE),
        TOOLSMITH(VillagerProfession.TOOLSMITH, Items.SMITHING_TABLE),
        ;

        private final VillagerProfession profession;
        private final Item workStation;

        Professions(VillagerProfession profession, Item workStation) {

            this.profession = profession;
            this.workStation = workStation;
        }

        public static Professions valueOf(VillagerProfession profession) {
            return get(profession);
        }

        public static Professions get(VillagerProfession profession) {
            for (Professions value : Professions.values()) {
                if (value.profession == profession) {
                    return value;
                }
            }
            //Oof?
            return null;
        }

        public VillagerProfession getProfession() {
            return profession;
        }

        public Item getWorkStation() {
            return workStation;
        }

        @Override
        public String toString() {
            final String name = this.name();
            return (name.charAt(0) +
                    name.toLowerCase()
                            .substring(1))
                    .replace("_", " ");
        }
    }

    public static String toString(ItemStack itemStack) {
        final StringBuilder b = new StringBuilder();
        final String[] enchantments = AutoTrade.getEnchantments(itemStack);
        b.append(itemStack.getItem().toString().replace("_"," "));
        b.append(" ");
        if (enchantments.length > 0) {
            for (int i = 0; i < enchantments.length - 1; i++) {
                b.append(enchantments[i]);
                b.append(", ");
            }
            b.append(enchantments[enchantments.length - 1]);
        }
        return b.toString();
    }
}
