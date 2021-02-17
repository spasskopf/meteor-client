package minegame159.meteorclient.utils.misc;

/**
 * Some Method used in the {@link net.minecraft.village.TradeOffers} class.
 * Used by {@link minegame159.meteorclient.utils.entity.TradeUtils}
 */
@SuppressWarnings("unused")
public class MathUtils {
    public static int[] random(int bound) {
        return random(0, bound);
    }

    public static int[] random(int min, int max) {
        int[] values = new int[max - min];
        for (int i = min; i < max; i++) {
            values[i - min] = i;
        }
        return values;
    }

    public static boolean[] nextBoolean() {
        return new boolean[]{false, true};
    }


    public static int[] mathHelperNextInt(int min, int max) {
        return random(min, max + 1);
    }

}
