package minegame159.meteorclient.modules.misc;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AutoDiamond extends Module {
    public AutoDiamond() {
        super(Category.Misc, "auto-diamond", "Automatically farms diamond, which have been saved to a file");
    }

    ArrayList<BlockPos> pos;
    int index = 0;
    boolean exception = true;
    BlockPos current;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<String> path = sgGeneral.add(new StringSetting.Builder()
            .name("path")
            .description("the path to the file.")
            .defaultValue("info.txt")
            .build()
    );

    @Override
    public void onActivate() {
        try {
            List<String> data = load(path.get());
            pos = getPos(data.get(0));
            index = Integer.parseInt(data.get(1));
            exception = false;
        } catch (Exception e) {
            e.printStackTrace();
            toggle(false);
            exception = true;
        }
    }

    @Override
    public void onDeactivate() {
        ChatUtils.info("Deactivated Auto-Diamond :(");
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("cancel");

    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isActive() && !exception) {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal() == null) {
                System.out.printf("Index: %d, Pos: %s%n", index, pos.get(index).toString());
                current = pos.get(index);
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(String.format("goto %d %d %d", current.getX(), current.getY(), current.getZ()));
                index++;
                if (index >= pos.size()) {
                    toggle(false);
                    ChatUtils.info("Finished mining!");
                    exception = true;
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        if (pos == null || index >= pos.size()) {
            return "Idle";
        }
        return String.format("%s, %d%%", pos.get(index).toShortString(), index * 100 / pos.size());
    }

    public ArrayList<BlockPos> getPos(String path) {
        List<String> in = load(path);
        ArrayList<BlockPos> out = new ArrayList<>();
        String[] a;
        for (String s : in) {
            a = s.split(" ");
            out.add(new BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4].substring(0, a[4].length() - 1))));
        }
        return out;
    }

    public List<String> load(String path) {
        try {
            File file = new File(path);
            return Files.readAllLines(file.toPath());
        } catch (Exception e) {
            ChatUtils.error("Could not load file! Stopping!");
            ChatUtils.error(e.getMessage());
            e.printStackTrace();
            toggle(false);
        }
        return null;
    }
}
