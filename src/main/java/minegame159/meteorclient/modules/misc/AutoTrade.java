package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.EntityDestroyEvent;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import minegame159.meteorclient.events.entity.player.BreakBlockEvent;
import minegame159.meteorclient.events.game.OpenVillagerGuiScreenEvent;
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
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoTrade extends Module {

    private static final String PREFIX = "AutoTrade";

    //<editor-fold desc="Settings">
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the villager can be to interact with it.")
            .defaultValue(3)
            .min(0)
            .max(6)
            .sliderMax(6)
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
            .description("Draws lines to targeted villagers and their workstation.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> blacklistTracers = sgGeneral.add(new BoolSetting.Builder()
            .name("blacklist-tracers")
            .description("Draws lines to blacklisted villagers.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SettingColor> tracersColor = sgGeneral.add(new ColorSetting.Builder()
            .name("tracers-color")
            .description("The color of the tracers.")
            .defaultValue(new SettingColor(225, 225, 225))
            .build()
    );

    private final Setting<SettingColor> blackListTracersColor = sgGeneral.add(new ColorSetting.Builder()
            .name("blacklist-tracers-color")
            .description("The color of the blacklist tracers.")
            .defaultValue(new SettingColor(225, 0, 0))
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

    private VillagerEntity villager;
    private State state = State.WAITING_FOR_VILLAGER_IN_RANGE;
    private BlockPos workStation = null;


    /**
     * A List of all Villagers, who are known to have the wrong profession
     */
    private final List<VillagerEntity> blacklistedVillagers = new ArrayList<>();


    public AutoTrade() {
        super(Category.Misc, "auto-trade", "Automatically trades with Villagers");
    }


    //<editor-fold desc="Activate / Deactivate">
    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }
    //</editor-fold>


    /**
     * @return true if the villager's trade is the right trade
     */
    private boolean checkTrade(TradeOfferList offers) {
        for (TradeOffer offer : offers) {
            if (trades.get().contains(TradeUtils.toString(offer.getMutableSellItem()))) {
                ChatUtils.prefixInfo(PREFIX, "Correct Trade! %s", TradeUtils.toString(offer.getMutableSellItem()));
                return true;
            } else {
                ChatUtils.prefixInfo(PREFIX, "Wrong Trade! %s", TradeUtils.toString(offer.getMutableSellItem()));
            }
        }
        return false;
    }


    private void reset() {
        villager = null;
        workStation = null;
        state = State.WAITING_FOR_VILLAGER_IN_RANGE;
        blacklistedVillagers.clear();
    }


    private void mineWorkStation() {
        state = State.REMOVING_WORKSTATION;

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, workStation, net.minecraft.util.math.Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, workStation, net.minecraft.util.math.Direction.UP));
    }

    private void scanForVillagerInRange() {
        if (villager != null || state != State.WAITING_FOR_VILLAGER_IN_RANGE) {
            return;
        }

        final Entity nearestVillager = EntityUtils.get(entity -> {
            if (!entity.isAlive()) {
                return false;
            }
            if (mc.player.distanceTo(entity) >= range.get()) {
                return false;
            }

            if (entity == mc.player || entity.getType() != EntityType.VILLAGER) {
                return false;
            }
            //Ah yes: redundant cast but suspicious call without the cast...
            if (blacklistedVillagers.contains((VillagerEntity) entity)) {
                return false;
            }

            if (((VillagerEntity) entity).getVillagerData().getProfession() != VillagerProfession.NONE && ((VillagerEntity) entity).getVillagerData().getProfession() != targetProfession.get().getProfession()) {
                blacklistedVillagers.add((VillagerEntity) entity);
                ChatUtils.prefixInfo(PREFIX, "Added Villager at %s to Blacklist", entity.getBlockPos().toShortString());
                return false;
            }
            return !((VillagerEntity) entity).isBaby();
        }, SortPriority.LowestDistance);

        if (nearestVillager != null) {
            villager = (VillagerEntity) nearestVillager;
            state = State.WAITING_FOR_JOB;
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
        if (blacklistTracers.get()) {
            for (VillagerEntity villagerEntity : blacklistedVillagers) {
                RenderUtils.drawTracerToEntity(event,
                        villagerEntity,
                        blackListTracersColor.get(),
                        Modules.get().get(Tracers.class).target.get(),
                        Modules.get().get(Tracers.class).stem.get());
            }
        }

    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        switch (state) {
            case WAITING_FOR_VILLAGER_IN_RANGE:
                scanForVillagerInRange();
                break;
            case WAITING_FOR_JOB:
                placeWorkStation();
                break;
        }
    }

    private void placeWorkStation() {
        if (workStation == null) {
            workStation = new BlockPos.Mutable(mc.player.getPos().getX(),
                    mc.player.getPos().getY(),
                    mc.player.getPos().getZ()).add(getDirection().x, 0, getDirection().z);

            final int slot = InvUtils.findItemInHotbar(targetProfession.get().getWorkStation());
            if (slot == -1) {
                ChatUtils.prefixError(PREFIX, "Could not find Item in hotbar! Looking for \"%s\"", targetProfession.get().getWorkStation().toString());
                toggleIfNotContinueWorking();
                return;
            }
            final BlockPos.Mutable center = new BlockPos.Mutable(mc.player.getPos().getX(),
                    mc.player.getPos().getY(),
                    mc.player.getPos().getZ());
            //Try to place the block in every direction, beginning with the block in front of the player
            if (BlockUtils.place(workStation, Hand.MAIN_HAND, slot, true, 50, true)) {
                ChatUtils.prefixInfo(PREFIX, "Placed workstation at %s", workStation.toShortString());
                state = State.WAITING_FOR_JOB;
            } else if (BlockUtils.place(center.north(), Hand.MAIN_HAND, slot, true, 50, true)) {
                ChatUtils.prefixInfo(PREFIX, "Placed workstation at %s", center.north().toShortString());
                workStation = center.north();
                state = State.WAITING_FOR_JOB;
            } else if (BlockUtils.place(center.east(), Hand.MAIN_HAND, slot, true, 50, true)) {
                ChatUtils.prefixInfo(PREFIX, "Placed workstation at %s", center.east().toShortString());
                workStation = center.east();
                state = State.WAITING_FOR_JOB;
            } else if (BlockUtils.place(center.south(), Hand.MAIN_HAND, slot, true, 50, true)) {
                ChatUtils.prefixInfo(PREFIX, "Placed workstation at %s", center.south().toShortString());
                workStation = center.south();
                state = State.WAITING_FOR_JOB;
            } else if (BlockUtils.place(center.west(), Hand.MAIN_HAND, slot, true, 50, true)) {
                ChatUtils.prefixInfo(PREFIX, "Placed workstation at %s", center.west().toShortString());
                workStation = center.west();
                state = State.WAITING_FOR_JOB;
            } else {
                ChatUtils.prefixError(PREFIX, "Could not place workstation!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BreakBlockEvent event) {
        if (state != State.REMOVING_WORKSTATION) {
            return;
        }
        if (workStation == null) {
            return;
        }

        if (event.blockPos.equals(workStation)) {
            reset();
            ChatUtils.prefixInfo(PREFIX, "Mined workstation");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDestroyEvent event) {
        if (villager != null) {
            if (villager.equals(event.entity)) {
                reset();
                ChatUtils.prefixInfo(PREFIX, "Villager died");
            }
        }
    }

    @EventHandler
    public void onVillagerGuiOpen(OpenVillagerGuiScreenEvent event) {
        if (state != State.CHECKING_TRADE) {
            return;
        }
        ChatUtils.prefixInfo(PREFIX, "Opened Villager Gui! requesting close!");
        mc.openScreen(null);
        if (checkTrade(event.merchant.getOffers())) {
            state = State.FINISHED;
            mc.player.playSound(new SoundEvent(new Identifier("minecraft:block.bell.use")), SoundCategory.MASTER, 2, 1);
            ChatUtils.prefixInfo(PREFIX, "Got correct villager trade! Disabling AutoTrade!");
            toggle();
        } else {
            mineWorkStation();
        }

    }

    @EventHandler
    public void onVillagerUpdateProfession(VillagerUpdateProfessionEvent event) {
        if (event.action == VillagerUpdateProfessionEvent.Action.GOT_JOB) {
            if (villager != null && workStation != null) {
                if (villager.equals(event.entity)) {
                    if (event.newData.getProfession() == targetProfession.get().getProfession()) {
                        ChatUtils.prefixInfo(PREFIX, "Villager got correct profession");

                        Rotations.rotate(Rotations.getYaw(villager), Rotations.getPitch(villager), 60, () -> {
                            state = State.CHECKING_TRADE;
                            mc.interactionManager.interactEntity(mc.player, villager, Hand.MAIN_HAND);
                            mc.player.swingHand(Hand.MAIN_HAND);
                            ChatUtils.prefixInfo(PREFIX, "Request Interaction!");

                        });
                    } else {
                        reset();
                        ChatUtils.prefixInfo(PREFIX, "Reset because villager got wrong profession");
                    }
                } else {
                    ChatUtils.warning("Wrong Villager!");

                }
            }
        } else {
            blacklistedVillagers.remove(event.entity);

            if (villager != null && workStation != null) {
                if (villager.equals(event.entity)) {
                    reset();
                    ChatUtils.prefixInfo(PREFIX, "Reset because villager lost their profession");
                }
            }
        }
    }


    //</editor-fold>

    private void toggleIfNotContinueWorking() {
        if (continueWorking.get()) {
            ChatUtils.prefixInfo(PREFIX, "Continued because continue working is on! Might cause issues!");
            reset();
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
        WAITING_FOR_JOB,
        CHECKING_TRADE,
        REMOVING_WORKSTATION,
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
        workStation = new BlockPos(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
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

    //</editor-fold>
}