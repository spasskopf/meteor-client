package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.EntityDestroyEvent;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import minegame159.meteorclient.events.entity.player.BreakBlockEvent;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.render.Tracers;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.entity.TradeUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoTrade extends Module {

    private static final String PREFIX;


    static {
        PREFIX = "AutoTrade";
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private VillagerEntity villager;
    private State state = State.WAITING_FOR_VILLAGER_IN_RANGE;
    private BlockPos.Mutable workStation = null;

    private boolean didStateChange = false;
    private boolean isBreakingWorkStation = false;

    /**
     * A List of all Villagers, who are known to have the wrong profession
     */
    private final List<VillagerEntity> excludedVillagers = new ArrayList<>();

    //<editor-fold desc="Settings">
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the villager can be to interact with it.")
            .defaultValue(3)
            .min(0)
            .max(6)
            .sliderMax(6)
            .build()
    );
    private final Setting<Double> workstationRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("workstation-range")
            .description("The maximum range placed workstation can be away before placing a new one.")
            .defaultValue(3)
            .min(0)
            .max(10)
            .sliderMax(10)
            .build()
    );
    private final Setting<TradeUtils.Professions> targetProfession = sgGeneral.add(new EnumSetting.Builder<TradeUtils.Professions>()
            .name("targetProfession")
            .description("What type of villager profession you want.")
            .defaultValue(TradeUtils.Professions.LIBRARIAN)
            .build()
    );
    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
            .name("tracers")
            .description("Draws lines to targeted villager and their workstation.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SettingColor> tracersColor = sgGeneral.add(new ColorSetting.Builder()
            .name("tracers-color")
            .description("The color of the tracers.")
            .defaultValue(new SettingColor(225, 225, 225))
            .build()
    );

    private final Setting<Boolean> continueWorking = sgGeneral.add(new BoolSetting.Builder()
            .name("continue")
            .description("Continues even if an exception occurred (might spam your chat / log with exception messages...)")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<String>> trades = sgGeneral.add(new TradeListSetting.Builder()
            .name("trades")
            .description("Trades you want to get")
            //.defaultValue(TradeUtils.TRADES_AS_STRING)
            .build()
    );
    //</editor-fold>


    public AutoTrade() {
        super(Category.Misc, "auto-trade", "Automatically trades with Villagers");
    }


    //<editor-fold desc="Activate / Deactivate">
    @Override
    public void onActivate() {
        state = State.WAITING_FOR_VILLAGER_IN_RANGE;
        villager = null;
        workStation = null;
        excludedVillagers.clear();
    }

    @Override
    public void onDeactivate() {
        state = State.WAITING_FOR_VILLAGER_IN_RANGE;
        villager = null;
        workStation = null;
        excludedVillagers.clear();
    }
    //</editor-fold>


    private void removeWorkStation() {
        mine(workStation.toImmutable());
    }

    private void scanForVillagerInRange() {
        if (didStateChange) {
            ChatUtils.info(PREFIX, Text.of("Checking for villagers in range..."));
            didStateChange = false;
        }

        if (mc.player.isDead() || !mc.player.isAlive()) {
            villager = null;
            return;
        }

        villager = (VillagerEntity) EntityUtils.get(entity -> {
            if (entity == mc.player || entity == mc.cameraEntity) {
                return false;
            }
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) {
                return false;
            }
            if (entity.distanceTo(mc.player) > range.get()) {
                return false;
            }


            return entity instanceof VillagerEntity && !((VillagerEntity) entity).isBaby() && !excludedVillagers.contains(entity);
        }, SortPriority.LowestDistance);
        if (villager != null) {
            state = State.WAITING_FOR_JOB;
            ChatUtils.info("Waiting for job!");
        }
    }

    /**
     * @return true if the villager's trade is the right trade
     */
    private boolean checkTrade() {
         for (TradeOffer offer : villager.getOffers()) {
            if (trades.get().contains(TradeUtils.toString(offer.getMutableSellItem()))) {
                return true;
            }

        }
        return false;

    }

    private void scanForJob() {
        if (villager.getVillagerData().getProfession() == VillagerProfession.NONE) {
            if (workStation != null) {
                if (!workStation.isWithinDistance(mc.player.getPos(), workstationRange.get())) {
                    ChatUtils.warning("Old workstation is not in range! Placing a new one. (Location: %s)", workStation.toShortString());
                    workStation = new BlockPos.Mutable(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                } else {
                    return;
                }
            } else {
                workStation = new BlockPos.Mutable(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            }


            final Direction direction = getDirection();
            if (place(direction.getX(), 0, direction.getZ())) {
                ChatUtils.info("Placed Workstation!");
            } else {
                ChatUtils.error("Could not find / place Workstation");
                toggleIfNotContinueWorking();
            }
        } else if (villager.getVillagerData().getProfession() == targetProfession.get().getProfession()) {
            state = State.CHECKING_TRADE;
            ChatUtils.info("Found villager with correct profession!");
        } else {
            excludedVillagers.add(villager);
            state = State.WAITING_FOR_VILLAGER_IN_RANGE;
        }

    }


    //<editor-fold desc="Event Handler">
    @EventHandler
    public void onRender(RenderEvent event) {
        if (tracers.get()) {
            if (villager != null) {
                RenderUtils.drawTracerToEntity(event,
                        villager,
                        tracersColor.get(),
                        Modules.get().get(Tracers.class).target.get(),
                        Modules.get().get(Tracers.class).stem.get());

            }
            if (workStation != null) {
                RenderUtils.drawTracerToPos(workStation,
                        tracersColor.get(),
                        event);
            }
        }

    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) {
            return;
        }

        if (state == State.WAITING_FOR_VILLAGER_IN_RANGE || villager == null) {
            scanForVillagerInRange();
        } else if (state == State.WAITING_FOR_JOB) {
            scanForJob();
        } else if (state == State.CHECKING_TRADE) {
            if (checkTrade()) {
                state = State.FINISHED;
                toggle();
                ChatUtils.prefixInfo(PREFIX, "Got correct trade! Disabling!");
                mc.player.playSound(new SoundEvent(new Identifier("minecraft:block.bell.use")), SoundCategory.PLAYERS, 2, 1);
            } else {
                try {
                    if (!isBreakingWorkStation) {
                        ChatUtils.info("Removing Workstation!");
                        isBreakingWorkStation = true;
                        removeWorkStation();
                    } else {
                        ChatUtils.info("is breaking workstation!");
                    }
                } catch (Exception e) {
                    ChatUtils.info("Exception while trying to break block!");
                    state = State.WAITING_FOR_VILLAGER_IN_RANGE;
                    isBreakingWorkStation = false;
                    e.printStackTrace();
                }
            }
        }

    }

    @EventHandler
    public void onEntityDeath(EntityDestroyEvent event) {
        if (event.entity instanceof VillagerEntity) {
            if (event.entity.equals(villager)) {
                villager = null;
                state = State.WAITING_FOR_VILLAGER_IN_RANGE;
                ChatUtils.prefixInfo(PREFIX, "The villager just died");
            }
            //IntelliJ: Why is casting useless here, but without casting it's a "Suspicious call to 'List.remove'"?
            excludedVillagers.remove((VillagerEntity) event.entity);
        }
    }

    @EventHandler
    private void onVillagerProfessionUpdate(VillagerUpdateProfessionEvent event) {
      /*
       ChatUtils.info("Listener Called! Event Type: %s Profession: %s/%s",
               event.action, event.oldData.getProfession(), event.newData.getProfession()
       );
      */
        if (event.action == VillagerUpdateProfessionEvent.Action.LOST_JOB) {
            if (event.entity.equals(villager)) {
                state = State.WAITING_FOR_JOB;
                workStation = null;
            }
            excludedVillagers.remove(event.entity);
        }

    }

    @EventHandler
    public void onBlockBreak(BreakBlockEvent event) {
        if (event.blockPos.equals(workStation)) {
            isBreakingWorkStation = false;
            ChatUtils.info("Workstation has been removed");
            workStation = null;
            state = State.WAITING_FOR_JOB;
        }
    }
    //</editor-fold>

    private void toggleIfNotContinueWorking() {
        if (continueWorking.get()) {
            ChatUtils.prefixInfo(PREFIX, "Continued because continue working is on! Might cause issues!");
        } else {
            toggle();
        }
    }

    @Override
    public String getInfoString() {
        return state.toString();
    }

    public enum State {
        WAITING_FOR_VILLAGER_IN_RANGE,
        REMOVING_WORKSTATION,
        WAITING_FOR_JOB,
        CHECKING_TRADE,
        FINISHED;

        @Override
        public String toString() {
            final String name = this.name();
            return (name.charAt(0) +
                    name.toLowerCase()
                            .substring(1))
                    .replace("_", " ");
        }

    }


    public static String[] getEnchantments(ItemStack itemStack) {

        final Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
        final String[] enchs = new String[enchantments.size()];
        final int[] n = {0};
        enchantments.forEach((enchantment, integer) -> {
            enchs[n[0]] = enchantment.getName(integer).getString();
            n[0]++;
        });
        return enchs;
        /*
           ListTag listTag = itemStack.getEnchantments();

           ChatUtils.info("Zero Compund Size: %d", listTag.getCompound(0).getSize());
           for (int i = 0; i < listTag.size(); ++i) {
               enchs[i] = String.format("Enchantment %s, %d",
                       listTag.getCompound(i).getString("id"),
                       listTag.getCompound(i).getInt("lvl"));
           }
           return enchs;
         */
    }
    //<editor-fold desc="Stuff. was  private in other classes...">


    private boolean place(int x, int y, int z) {
        setBlockPos(x, y, z);
        BlockState blockState = mc.world.getBlockState(workStation);

        if (!blockState.getMaterial().isReplaceable()) {
            return true;
        }

        int slot = findSlot(targetProfession.get().getProfession());
        return BlockUtils.place(workStation, Hand.MAIN_HAND, slot, true, 100, true);
        //return BlockUtils.place(workStation, Hand.MAIN_HAND, slot, true, 100, false, true, true);
    }

    private void setBlockPos(int x, int y, int z) {
        workStation.set(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
    }

    private int findSlot(VillagerProfession profession) {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (!(item instanceof BlockItem)) {
                continue;
            }

            if (item == TradeUtils.Professions.get(profession).getWorkStation()) {
                return i;
            }
        }

        return -1;
    }

    protected Direction getDirection() {
        Direction dir;
        float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
        float pitch = mc.gameRenderer.getCamera().getPitch() % 360;

        if (yaw < 0) {
            yaw += 360;
        }

        if (yaw >= 337.5 || yaw < 22.5) {
            dir = Direction.South;
        } else if (yaw >= 22.5 && yaw < 67.5) {
            dir = Direction.SouthWest;
        } else if (yaw >= 67.5 && yaw < 112.5) {
            dir = Direction.West;
        } else if (yaw >= 112.5 && yaw < 157.5) {
            dir = Direction.NorthWest;
        } else if (yaw >= 157.5 && yaw < 202.5) {
            dir = Direction.North;
        } else if (yaw >= 202.5 && yaw < 247.5) {
            dir = Direction.NorthEast;
        } else if (yaw >= 247.5 && yaw < 292.5) {
            dir = Direction.East;
        } else if (yaw >= 292.5 && yaw < 337.5) {
            dir = Direction.SouthEast;
        } else {
            dir = Direction.NaN;
        }
        return dir;
    }

    private enum Direction {
        South("South", "Z+", 0, 1),
        SouthEast("South East", "Z+ X+", 1, 1),
        West("West", "X-", -1, 0),
        NorthWest("North West", "Z- X-", -1, -1),
        North("North", "Z-", 0, -1),
        NorthEast("North East", "Z- X+", -1, 1),
        East("East", "X+", 1, 0),
        SouthWest("South West", "Z+ X-", 1, -1),
        NaN("NaN", "NaN", 1, 1);

        public String name;
        private final int x;
        private final int z;
        public String axis;

        Direction(String name, String axis, int x, int z) {
            this.axis = axis;
            this.name = name;
            this.x = x;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }
    }

    private void mine(BlockPos blockPos) {
        if (blockPos == null) {
            ChatUtils.error("Why is blockPos null?");
            return;
        }
        ChatUtils.info("Sent first Packet");
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, net.minecraft.util.math.Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, net.minecraft.util.math.Direction.UP));
        ChatUtils.info("Sent second Packet");
    }
    //</editor-fold>
}