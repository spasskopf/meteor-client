package minegame159.meteorclient.systems.modules.misc;

import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class Area {
    ArrayList<BlockPos> pos = new ArrayList<>();
    Area[][] a;

    int x;
    int z;
    int size;

    private static final int MAX_BLOCKS_PER_AREA = 256;
    private static final int MIN_SIZE = 16;

    public Area(int x, int z, int size) {
        this.x = x;
        this.z = z;
        this.size = size;
    }

    public String sort() {
        if (a == null) {
            if (pos.size() > 0) {
                return BlockPosToString(areaSort());
            } else {
                return "";
            }
        } else {
            StringBuilder s = new StringBuilder();
            for (Area[] areas : a) {
                for (int j = 0; j < a[0].length; j++) {
                    s.append(areas[j].sort());
                }
            }
            return s.toString();
        }
    }

    public String BlockPosToString(ArrayList<BlockPos> b) {
        StringBuilder s = new StringBuilder();

        for (BlockPos blockPos : b) {
            s.append("[.b goto ").append(blockPos.getX()).append(" ").append(blockPos.getY()).append(" ").append(blockPos.getZ()).append("]\n");
        }

        return s.toString();
    }

    public ArrayList<BlockPos> areaSort() {
        ArrayList<BlockPos> sorted = new ArrayList<>();

        sorted.add(pos.get(0));
        pos.remove(0);

        int index;
        while (pos.size() > 0) {
            index = nearest(sorted.get(sorted.size() - 1), pos);
            sorted.add(pos.get(index));
            pos.remove(index);
        }

        return sorted;
    }

    public int nearest(BlockPos b, ArrayList<BlockPos> unsorted) {
        int min_distance = dist(b, unsorted.get(0));
        int index = 0;

        for (int i = 1; i < unsorted.size(); i++) {
            if (min_distance > dist(b, unsorted.get(i))) {
                index = i;
                min_distance = dist(b, unsorted.get(i));
            }
        }

        return index;
    }

    public int dist(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    public void split() {
        ChatUtils.info("Length: " + pos.size());
        //ChatUtils.info(".toStirng: " + pos.toString());
        if (pos.size() > MAX_BLOCKS_PER_AREA && size >= MIN_SIZE) {
            a = new Area[2][2];
            for (int i = 0; i < a.length; i++) {
                for (int j = 0; j < a[0].length; j++) {
                    a[i][j] = new Area(x + size * i / 2, z + size * j / 2, size / 2);
                }
            }

            for (Area[] areas : a) {
                for (int j = 0; j < a[0].length; j++) {
                    for (BlockPos po : pos) {
                        areas[j].add(po);
                    }
                    areas[j].split();
                }
            }

            pos = new ArrayList<>();
        }
    }

    public void add(BlockPos b) {
        //ChatUtils.info("size: " + pos.size() + ", " + b.toString());
        if ((b.getX() >= x && b.getX() < x + size) && (b.getZ() >= z && b.getZ() < z + size)) {
            pos.add(b);
        }
    }
}
