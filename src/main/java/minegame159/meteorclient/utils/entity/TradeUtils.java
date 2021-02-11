package minegame159.meteorclient.utils.entity;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TradeUtils {
    private static final List<ItemStack> TRADES;


    static {
        TRADES = new LinkedList<>();


    }


    public ItemStack[] enchantedBooks(Entity entity, Random random) {
        List<Enchantment> list = (List) Registry.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).collect(Collectors.toList());
        List<ItemStack> trades = new ArrayList<>();
        for (int randomEnchantment : random(list.size())) {


            Enchantment enchantment = (Enchantment) list.get(randomEnchantment);
            for (int i = 0; i < mathHelperNextInt(enchantment.getMinLevel(), enchantment.getMaxLevel()).length; i++) {

                ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, i));
                trades.add(itemStack);
            }
        }
        return trades.toArray(new ItemStack[]{});
    }

    private static int[] random(int bound) {
        return random(0, bound);
    }

    private static int[] random(int min, int max) {
        int[] values = new int[max - min];
        for (int i = min; i < max; i++) {
            values[i] = i;
        }
        return values;
    }

    private static int[] mathHelperNextInt(int min, int max) {
        return random(min, max + 1);
    }

}
