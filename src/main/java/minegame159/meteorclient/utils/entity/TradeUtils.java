package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.utils.misc.MathUtils;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TradeUtils {
    public static final List<ItemStack> TRADES;

    static {
        TRADES = new LinkedList<>();
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
     /*
       final HashSet<ItemStack> set = new HashSet<>();
       for (ItemStack itemStack2 : asItemStackArray(Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE)) {
           for (int firstColor : MathUtils.random(16)) {
               for (boolean firstBoolean : MathUtils.nextBoolean()) {
                   for (boolean secondBoolean : MathUtils.nextBoolean()) {
                       if (secondBoolean) {
                           for (int secondColor : MathUtils.random(16)) {
                               for (int thirdColor : MathUtils.random(16)) {
                                   List<DyeItem> dyeItemList = Lists.newArrayList();
                                   dyeItemList.add(DyeItem.byColor(DyeColor.byId(firstColor)));
                                   if (firstBoolean) {
                                       dyeItemList.add(DyeItem.byColor(DyeColor.byId(secondColor)));
                                   }
                                   if (secondBoolean) {
                                       dyeItemList.add(DyeItem.byColor(DyeColor.byId(thirdColor)));
                                   }
                                   set.add(DyeableItem.blendAndSetColor(itemStack2, dyeItemList));
                               }
                           }
                       }
                   }
               }
           }
       }
        return asList(set.toArray(new ItemStack[]{}));
     */
        //If somebody really wants all possible color combinations:
        //Use search, find flowers and dye armor.
        return asList(asItemStackArray(Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE));
    }

    public static List<ItemStack> getMason() {
        return asList(asItemStack(Items.BRICK));
    }

    public static List<ItemStack> getShepherd() {
        return asList(asItemStackArray(
                Blocks.WHITE_WOOL,
                Blocks.BROWN_WOOL,
                Blocks.BLACK_WOOL,
                Blocks.GRAY_WOOL));
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
        /*
           for (int level: MathUtils.random(15)) {
               //from random generating (int i = 5 + random.nextInt(15))
               level += 5;
               List<EnchantmentLevelEntry> enchantmentLevelEntries = Lists.newArrayList();
               Item item = stack.getItem();
               int i = item.getEnchantability();



               level += 1 + random.nextInt(i / 4 + 1) + random.nextInt(i / 4 + 1);
               float f = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;

               level = MathHelper.clamp(Math.round((float) level + (float) level * f), 1, 2147483647);
               List<EnchantmentLevelEntry> list2 = EnchantmentHelper.getPossibleEntries(level, stack, false);
           }
         */
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
        List<Enchantment> list = (List) Registry.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).collect(Collectors.toList());
        List<ItemStack> trades = new ArrayList<>();
        for (Enchantment enchantment : list) {
            for (int i : MathUtils.mathHelperNextInt(enchantment.getMinLevel(),enchantment.getMaxLevel())) {
                ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, i));
                System.out.println("new EnchantmentLevelEntry(enchantment,i).enchantment.getName(i).asString() = " + enchantment.getName(1) + " | " + new EnchantmentLevelEntry(enchantment, i).enchantment.getName(i).getString());
                trades.add(itemStack);
            }
        }
        return trades;
    }

    private static ItemStack asItemStack(ItemConvertible item) {
        return new ItemStack(item);
    }

    private static ItemStack[] asItemStackArray(ItemConvertible... items) {
        ItemStack[] r = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            r[i] = asItemStack(items[i]);
        }
        return r;
    }

    private static List<ItemStack> asList(ItemStack... array) {
        return new ArrayList<ItemStack>(Arrays.asList(array));
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
