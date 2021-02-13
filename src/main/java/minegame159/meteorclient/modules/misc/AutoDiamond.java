package minegame159.meteorclient.modules.misc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoDiamond extends Module {
    public AutoDiamond() {
        super(Category.Misc, "auto-diamond", "Automatically farms diamond, which have been saved to a file");
    }

    ArrayList<BlockPos> pos;
    int index = 0;
    boolean exception = true;
    BlockPos current;
    private int playerX;
    private int playerY;
    private int playerZ;
    private State state;

    //<editor-fold desc="Settings">
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMining = settings.createGroup("Mine Diamonds");
    private final SettingGroup sgFinding = settings.createGroup("Find Diamonds");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Search for blocks or mine blocks you found before.")
            .defaultValue(Mode.SEARCH_FOR_BLOCKS)
            .build()
    );

    private final Setting<String> path = sgMining.add(new StringSetting.Builder()
            .name("path")
            .description("the path to the file.")
            .defaultValue(new File(MeteorClient.FOLDER, "info.txt").getAbsolutePath())
            .build()
    );

    public final Setting<Boolean> autoWalkHome = sgMining.add(new BoolSetting.Builder()
            .name("walk-home")
            .description("Will walk 'home' when you mined everything.")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> autoLogOut = sgMining.add(new BoolSetting.Builder()
            .name("log-out-on")
            .description("Logs out when you mined everything.")
            .defaultValue(false)
            .build());

    public final Setting<Integer> minHeight = sgFinding.add(new IntSetting.Builder()
            .name("min-height")
            .description("Minimum Height for searching")
            .defaultValue(6)
            .min(0)
            .max(256)
            .sliderMax(256)
            .build());

    public final Setting<Integer> maxHeight = sgFinding.add(new IntSetting.Builder()
            .name("max-height")
            .description("Maximum Height for searching")
            .defaultValue(6)
            .min(0)
            .max(256)
            .sliderMax(256)
            .build()
    );

    public final Setting<Integer> radius = sgFinding.add(new IntSetting.Builder()
            .name("radius")
            .description("Radius for searching")
            .defaultValue(128)
            .min(0)
            .max(512)
            .sliderMax(512)
            .build()
    );

    private final Setting<List<Block>> blocks = sgFinding.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to search for.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );
    private final Setting<Boolean> smart = sgFinding.add(new BoolSetting.Builder()
            .name("smart")
            .description("Finds ore-clusters and mines cluster after cluster. Makes mining faster but searching takes longer (true = search for clusters)")
            .defaultValue(false)
            .build()
    );

    public final Setting<Integer> maxClusterSize = sgFinding.add(new IntSetting.Builder()
            .name("max-cluster-size")
            .description("Maximum size for a single cluster. Requires smart")
            .defaultValue(10)
            .min(1)
            .max(100)
            .sliderMax(100)
            .build()
    );
    private final Setting<String> output = sgFinding.add(new StringSetting.Builder()
            .name("output")
            .description("Location of the saved file.")
            .defaultValue(new File(MeteorClient.FOLDER, "info.txt").getAbsolutePath())
            .build()
    );
    //</editor-fold>


    @Override
    public void onActivate() {
        if (mode.get() == Mode.BREAK_BLOCKS) {
            if (mc.player != null) {
                playerX = (int) mc.player.getX();
                playerY = (int) mc.player.getY();
                playerZ = (int) mc.player.getZ();
            }
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
        } else {
            try {
                startSearching();
            } catch (Exception e) {
                ChatUtils.error("An unexpected error occurred!");
                ChatUtils.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDeactivate() {
        ChatUtils.info("Deactivated Auto-Diamond :(");
        if (mode.get() == Mode.BREAK_BLOCKS) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("cancel");
            state = State.IDLE;
            index = 0;
            exception = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (isActive() && mode.get() == Mode.BREAK_BLOCKS) {
            if (!exception) {

                if (BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal() == null) {
                    if (state == State.GOING_HOME) {
                        stop();
                        return;
                    }
                    state = State.MINING;
                    System.out.printf("Index: %d, Pos: %s%n", index, pos.get(index).toString());
                    current = pos.get(index);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(String.format("goto %d %d %d", current.getX(), current.getY(), current.getZ()));
                    index++;
                    if (index >= pos.size()) {
                        ChatUtils.info("Finished!");
                        exception = true;
                        stop();
                    }
                }
            }
        }

    }

    int found = 0;
    int currentBlock = 0;
    int blocksToScan = 0;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void startSearching() {
        MeteorExecutor.execute(() -> {
            try {
                if (mc.player == null) {
                    ChatUtils.error("Player is null. Disabling!");
                    return;
                }
                if (mc.player.world == null) {
                    ChatUtils.error("Player-World is null. Disabling!");
                    return;
                }
                if (maxHeight.get() < minHeight.get()) {
                    ChatUtils.error("Max height must not be less than Min height!");
                    return;
                }
                long startTime = System.currentTimeMillis();
                File file = new File(MeteorClient.FOLDER, "AutoDiamond_" + new SimpleDateFormat("yy_MM_dd_hh_mm_ss").format(new Date()));
                File infoFile = new File(output.get());
                if (!file.exists()) {
                    file.createNewFile();
                }
                if (!infoFile.exists()) {
                    infoFile.createNewFile();
                }
                FileWriter infoWriter = new FileWriter(infoFile);
                infoWriter.write(file.getAbsolutePath());
                infoWriter.write("\n0");
                infoWriter.flush();
                infoWriter.close();
                ChatUtils.info("File has been saved to %s (use this file's path as  AutoDiamond input)!", infoFile.getAbsolutePath());

                FileWriter writer = new FileWriter(file);

                final int x = (int) mc.player.getPos().getX();
                final int z = (int) mc.player.getPos().getZ();
                final StringBuilder blocksFound = new StringBuilder();
                if (!smart.get()) {
                    searchDumb(x, z, blocksFound);
                } else {
                    searchMoreOrLessSmart(x, z, blocksFound);
                }
                writer.write(blocksFound.toString());
                writer.flush();
                writer.close();
                ChatUtils.info("Finished Searching! File with all blocks: " + file.getAbsolutePath());
                ChatUtils.info("Took %d Seconds!", TimeUnit.SECONDS.convert(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS));
            } catch (IOException e) {
                ChatUtils.error("An unexpected error occurred!");
                ChatUtils.error(e.getMessage());
                e.printStackTrace();
            } finally {
                toggle(false);
            }
        });
    }

    private void searchMoreOrLessSmart(int x, int z, StringBuilder blocksFound) {
        blocksToScan = radius.get() * radius.get() * (maxHeight.get() - minHeight.get());
        found = 0;
        currentBlock = 0;
        //TODO: initial capacity maybe makes it faster!
        List<BlockPos> unScannedBlocks = new ArrayList<>();
        for (int xPos = x - radius.get(); xPos < x + radius.get(); xPos++) {
            for (int zPos = z - radius.get(); zPos < z + radius.get(); zPos++) {
                for (int height = minHeight.get(); height < maxHeight.get(); height++) {
                    unScannedBlocks.add(new BlockPos(xPos, height, zPos));
                }
            }
        }

        while (unScannedBlocks.size() > 0) {
            if (unScannedBlocks.size() % 100 == 0) {
                ChatUtils.info("Unscanned Blocks: %d", unScannedBlocks.size());
            }
            scanForCluster(unScannedBlocks, blocksFound, unScannedBlocks.get(0), 0);
        }

    }

    private void scanForCluster(List<BlockPos> unScanned, StringBuilder string, BlockPos start, int amountFound) {
        //if scanForCluster has been called from the search method
        //not recursively (otherwise amountFound wouldn't be 0)
        if (amountFound == 0) {
            if (blocks.get().contains(mc.player.world.getBlockState(start).getBlock())) {
                appendBlockPos(string, start);
            } else {
                unScanned.remove(start);
                return;
            }
        }
        unScanned.remove(start);
        if (amountFound >= maxClusterSize.get()) {
            appendBlockPos(string, start);
            return;
        }
        for (Direction direction : Direction.values()) {
            final BlockPos next = start.add(direction.getVector());
            if (unScanned.contains(next)) {
                currentBlock++;
                if (blocks.get().contains(mc.player.world.getBlockState(next).getBlock())) {
                    amountFound++;
                    found++;
                    appendBlockPos(string, next);
                    ChatUtils.info("Found Cluster of %s! Position: %s ", mc.player.world.getBlockState(next).toString(), next.toShortString());
                    scanForCluster(unScanned, string, next, amountFound);
                } else {
                    unScanned.remove(next);
                }
            }
        }
    }

    private void searchDumb(int x, int z, StringBuilder blocksFound) {
        blocksToScan = radius.get() * radius.get() * (maxHeight.get() - minHeight.get());
        found = 0;
        currentBlock = 0;
        for (int xPos = x - radius.get(); xPos < x + radius.get(); xPos++) {
            for (int zPos = z - radius.get(); zPos < z + radius.get(); zPos++) {
                for (int height = minHeight.get(); height < maxHeight.get(); height++) {
                    BlockPos blockPos = new BlockPos(xPos, height, zPos);
                    currentBlock++;
                    if (blocks.get().contains(mc.player.world.getBlockState(blockPos).getBlock())) {
                        found++;
                        appendBlockPos(blocksFound, blockPos);
                    }
                }
            }
        }
    }

    private void appendBlockPos(StringBuilder blocksFound, BlockPos blockPos) {
        blocksFound.append("[.b goto ")
                .append(blockPos.getX())
                .append(" ")
                .append(blockPos.getY())
                .append(" ")
                .append(blockPos.getZ())
                .append("]\n");
    }

    private void stop() {
        if (autoWalkHome.get()) {
            if (state == State.MINING) {
                state = State.GOING_HOME;
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(playerX, playerY, playerZ));
                return;
            } else if (state == State.GOING_HOME) {
                if (autoLogOut.get()) {
                    if (mc.player != null) {
                        toggle(false);
                        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("Auto Diamond: finished and went home")));
                    }
                }
            }
        }
        if (autoLogOut.get()) {
            if (mc.player != null) {
                toggle(false);
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("Auto Diamond: finished")));
            }
        }
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

    @Override
    public String getInfoString() {
        if (mode.get() == Mode.BREAK_BLOCKS) {

            if (pos == null || index >= pos.size()) {
                return "Idle";
            }
            return String.format("%s, %d%%", pos.get(index).toShortString(), index * 100 / pos.size());
        } else {
            return String.format("Searching %d/%d (%s%%), Found: %d (%s%%)",
                    currentBlock,
                    blocksToScan,
                    blocksToScan == 0 ? "NaN" : (currentBlock * 100) / ((float) blocksToScan),
                    found,
                    currentBlock == 0 ? "NaN" : (found * 100) / ((float) currentBlock)
            );
        }
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

    public enum State {
        IDLE,
        MINING,
        GOING_HOME;

        @Override
        public String toString() {
            final String name = this.name();
            return (name.charAt(0) +
                    name.toLowerCase()
                            .substring(1))
                    .replace("_", " ");
        }
    }

    public enum Mode {
        SEARCH_FOR_BLOCKS, BREAK_BLOCKS;

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