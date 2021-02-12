package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.EntityDestroyEvent;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import minegame159.meteorclient.events.entity.player.BreakBlockEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.entity.TradeUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;

public class AutoTrade extends Module {

    private static final String PREFIX;


    static {
        PREFIX = "[AutoTrade]";
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private VillagerEntity villager;
    private State state = State.WAITING_FOR_VILLAGER_IN_RANGE;
    private BlockPos.Mutable workStation = null;

    private boolean didStateChange = false;
    private boolean isBreakingWorkStation = false;


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
    //</editor-fold>


    public AutoTrade() {
        super(Category.Misc, "auto-trade", "Automatically trades with Villagers");
        System.out.println("Hallo");
    }


    //<editor-fold desc="Activate / Deactivate">
    @Override
    public void onActivate() {
        state = State.WAITING_FOR_VILLAGER_IN_RANGE;
        villager = null;
        workStation = null;
    }

    @Override
    public void onDeactivate() {
        state = State.WAITING_FOR_VILLAGER_IN_RANGE;
        villager = null;
        workStation = null;
    }
    //</editor-fold>

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
                    }
                } catch (Exception e) {
                    ChatUtils.info("Exception while trying to break block!");
                    e.printStackTrace();
                }
            }
        }

    }

    private void removeWorkStation() {
        mine(workStation.toImmutable());
    }

    /**
     * @return true if the villager's trade is the right trade
     */
    private boolean checkTrade() {
        for (TradeOffer offer : villager.getOffers()) {
            if (offer.getMutableSellItem().getItem() == Items.ENCHANTED_BOOK) {
                ChatUtils.info("villager sells book!" + offer.getMutableSellItem().getItem().toString());
                for (String enchantment : getEnchantments(offer.getMutableSellItem())) {
                    ChatUtils.info("Enchantment: %s", enchantment);
                }
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
                this.toggle(false);
            }
        } else if (villager.getVillagerData().getProfession() == targetProfession.get().getProfession()) {
            state = State.CHECKING_TRADE;
            ChatUtils.info("Found villager with correct profession!");
        }
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

            return entity instanceof VillagerEntity;
        }, SortPriority.LowestDistance);
        if (villager != null) {
            state = State.WAITING_FOR_JOB;
            ChatUtils.info("Waiting for job!");
        }
    }


    //<editor-fold desc="Event Handler">
    @EventHandler
    public void onEntityDeath(EntityDestroyEvent event) {
        if (event.entity.equals(villager)) {
            villager = null;
            state = State.WAITING_FOR_VILLAGER_IN_RANGE;
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
            }
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

    //<editor-fold desc="Stuff. was  private in other classes...">
    public static String[] getEnchantments(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTag();
        ListTag listTag;

        // Get list tag
        if (!tag.contains("Enchantments", 9)) {
            listTag = new ListTag();
            tag.put("Enchantments", listTag);
        } else {
            listTag = tag.getList("Enchantments", 10);
        }
        String[] enchantments = new String[listTag.size()];
        for (int i = 0; i < listTag.size(); i++) {
            Tag _t = listTag.get(i);
            CompoundTag t = (CompoundTag) _t;


            try {
                enchantments[i] =
                        Enchantment.byRawId(Integer.parseInt(t.getString("id")))
                                + " "
                                + t.getString("lvl");
            } catch (NumberFormatException e) {
                enchantments[i] =
                        t.getString("id")
                                + " "
                                + t.getString("lvl");
            }


        }
        return enchantments;

    }


    private boolean place(int x, int y, int z) {
        setBlockPos(x, y, z);
        BlockState blockState = mc.world.getBlockState(workStation);

        if (!blockState.getMaterial().isReplaceable()) {
            return true;
        }

        int slot = findSlot(targetProfession.get().getProfession());
        return BlockUtils.place(workStation, Hand.MAIN_HAND, slot, true, 100);
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
        if (pitch < 0) {
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

