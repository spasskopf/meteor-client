package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.modules.misc.AutoTrade;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TradeUtils {
    public static final List<ItemStack> TRADES;
    // public static final Map<VillagerProfession, Item> WORKSTATION = new HashMap<>();


    static {
        TRADES = new LinkedList<>();
        TRADES.addAll(getEnchantedBooks());
        /*
       WORKSTATION.put(VillagerProfession.ARMORER, Items.BLAST_FURNACE);
       WORKSTATION.put(VillagerProfession.BUTCHER, Items.SMOKER);
       WORKSTATION.put(VillagerProfession.CARTOGRAPHER, Items.CARTOGRAPHY_TABLE);
       WORKSTATION.put(VillagerProfession.CLERIC, Items.BREWING_STAND);
       WORKSTATION.put(VillagerProfession.FARMER, Items.COMPOSTER);
       WORKSTATION.put(VillagerProfession.FISHERMAN, Items.BARREL);
       WORKSTATION.put(VillagerProfession.FLETCHER, Items.FLETCHING_TABLE);
       WORKSTATION.put(VillagerProfession.LEATHERWORKER, Items.CAULDRON);
       WORKSTATION.put(VillagerProfession.LIBRARIAN, Items.LECTERN);
       WORKSTATION.put(VillagerProfession.SHEPHERD, Items.LOOM);
       WORKSTATION.put(VillagerProfession.MASON, Items.STONECUTTER);
       WORKSTATION.put(VillagerProfession.WEAPONSMITH, Items.GRINDSTONE);
       WORKSTATION.put(VillagerProfession.TOOLSMITH, Items.SMITHING_TABLE);
   */


    }


    public static List<ItemStack> getEnchantedBooks() {
        List<Enchantment> list = (List) Registry.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).collect(Collectors.toList());
        List<ItemStack> trades = new ArrayList<>();
        for (int randomEnchantment : random(list.size())) {
            Enchantment enchantment = (Enchantment) list.get(randomEnchantment);
            for (int i = 1; i < mathHelperNextInt(enchantment.getMinLevel(), enchantment.getMaxLevel()).length; i++) {
                ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, i));
                System.out.println("new EnchantmentLevelEntry(enchantment,i).enchantment.getName(i).asString() = " + enchantment.getName(1) + " | " + new EnchantmentLevelEntry(enchantment, i).enchantment.getName(i).getString());
                trades.add(itemStack);
            }
        }
        return trades;
    }

    private static int[] random(int bound) {
        return random(0, bound);
    }

    private static int[] random(int min, int max) {
        int[] values = new int[max - min + 1];
        for (int i = min; i < max; i++) {
            values[i] = i;
        }
        return values;
    }

    private static int[] mathHelperNextInt(int min, int max) {
        return random(min, max + 1);
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
}
