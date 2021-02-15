package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.RotationUtils;
import minegame159.meteorclient.utils.world.BlockIterator;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

public class AutoFire extends Module {
    private static final StatusEffect FIRE_RESISTANCE = Registry.STATUS_EFFECT.get(new Identifier("fire_resistance"));

    public AutoFire() {
        super(Category.Misc, "auto-fire", "Automatically extinguishes fire around you");
    }

    //TODO: Onground, Center, Rotation (?), freecam crash bei OnDamage

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> extinguish = sgGeneral.add(new BoolSetting.Builder()
            .name("extinguish")
            .description("Automatically extinguishes fire around you.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for fire.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for fire.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Boolean> waterBucket = sgGeneral.add(new BoolSetting.Builder()
            .name("water")
            .description("Automatically places water when you are on fire (and don't have fire resistance).")
            .defaultValue(false)
            .build()
    );


    private boolean hasPlacedWater = false;
    private BlockPos blockPos = null;
    private boolean doesWaterBucketWork = true;


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world.getDimension().isRespawnAnchorWorking()) {
            if (doesWaterBucketWork) {
                ChatUtils.warning("Water Buckets don't work in this dimension!");
                doesWaterBucketWork = false;

            }
        } else {
            if (!doesWaterBucketWork) {
                ChatUtils.warning("Enabled Water Buckets!");
                doesWaterBucketWork = true;

            }
        }
        if (waterBucket.get() && doesWaterBucketWork) {
            if (hasPlacedWater) {
                final int slot = findSlot(Items.BUCKET);
                blockPos = mc.player.getBlockPos();
                place(slot);
                hasPlacedWater = false;

            } else if (!mc.player.hasStatusEffect(FIRE_RESISTANCE) && mc.player.isOnFire()) {
                blockPos = mc.player.getBlockPos();
                final int slot = findSlot(Items.WATER_BUCKET);
                if (mc.world.getBlockState(blockPos).getBlock() == Blocks.FIRE) {
                    float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
                    float pitch = mc.gameRenderer.getCamera().getPitch() % 360;

                    RotationUtils.packetRotate(90, 90);
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));

                    RotationUtils.packetRotate(yaw, pitch);
                }
                place(slot);
                hasPlacedWater = true;
            }
        }

        if (extinguish.get()) {
            BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
                if (blockState.getBlock() == Blocks.FIRE) {
                    extinguishFire(blockPos);
                }
            });
        }
    }

    private void place(int slot) {
        if (slot != -1) {
            final int preSlot = mc.player.inventory.selectedSlot;
            ChatUtils.info("PreSlot %s", preSlot);
            InvUtils.swap(slot);
            float yaw = mc.gameRenderer.getCamera().getYaw() % 360;
            float pitch = mc.gameRenderer.getCamera().getPitch() % 360;

            RotationUtils.packetRotate(90, 90);
            mc.interactionManager.interactItem(mc.player, mc.player.world, Hand.MAIN_HAND);
            InvUtils.swap(preSlot);

            RotationUtils.packetRotate(yaw, pitch);

        }
    }

    private void extinguishFire(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, net.minecraft.util.math.Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, net.minecraft.util.math.Direction.UP));
    }

    private int findSlot(Item item) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack block = mc.player.inventory.getStack(i);
            if (block.getItem() == item) {
                slot = i;
                break;
            }
        }

        return slot;
    }

}
