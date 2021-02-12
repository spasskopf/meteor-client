package minegame159.meteorclient.modules.misc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalXZ;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.system.CallbackI;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AutoDiamond extends Module {
    public AutoDiamond() {
        super(Category.Misc, "auto-diamond", "Automatically trades with The World for Diamonds");
    }

    ArrayList<BlockPos> pos;
    int cooldown = 0;
    int index = 0;
    boolean exception = true;
    BlockPos current;

    @Override
    public void onActivate() {
        try {
            System.out.println("Activates");
            List<String> data = load("C:\\Users\\Martin\\Desktop\\Diamanten\\Markus-Server\\info.txt");
            pos = getPos(data.get(0));
            index = Integer.parseInt(data.get(1));
            exception = false;
        } catch (Exception e) {
            System.out.println("Damage ERROR");
            exception = true;
        }
    }

    @Override
    public void onDeactivate() {
        System.out.println("Deactivates");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!exception) {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal() == null) {
                System.out.println("Index: " + index + ", Pos: " + pos.get(index).toString());
                current = pos.get(index);
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("goto " + current.getX() + " " + current.getY() + " " + current.getZ());
                index++;
                if (index >= pos.size()) exception = true;
            }
        }
    }

    public ArrayList<BlockPos> getPos(String path) {
        List<String> in = load(path);
        ArrayList<BlockPos> out = new ArrayList<BlockPos>();
        String a[];
        for (int i = 0; i < in.size(); i++) {
            a = in.get(i).split(" ");
            out.add(new BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4].substring(0, a[4].length() - 1))));
        }
        return out;
    }

    public List<String> load(String path) {
        try {
            File file = new File(path);
            return Files.readAllLines(file.toPath());
        } catch (Exception e) {
        }
        return null;
    }
}
