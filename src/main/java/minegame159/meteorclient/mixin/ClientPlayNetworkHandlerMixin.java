/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.EntityDestroyEvent;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import minegame159.meteorclient.events.entity.player.PickItemsEvent;
import minegame159.meteorclient.events.game.GameJoinedEvent;
import minegame159.meteorclient.events.game.GameLeftEvent;
import minegame159.meteorclient.events.packets.ContainerSlotUpdateEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.events.world.ChunkDataEvent;
import minegame159.meteorclient.mixininterface.IExplosionS2CPacket;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.movement.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow private MinecraftClient client;

    @Shadow private ClientWorld world;

    private boolean worldNotNull;

    @Inject(at = @At("HEAD"), method = "onGameJoin")
    private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
        worldNotNull = world != null;
    }

    @Inject(at = @At("TAIL"), method = "onGameJoin")
    private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
        if (worldNotNull) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }

        MeteorClient.EVENT_BUS.post(GameJoinedEvent.get());
    }

    @Inject(at = @At("HEAD"), method = "sendPacket", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, CallbackInfo info) {
        PacketEvent.Send event = MeteorClient.EVENT_BUS.post(PacketEvent.Send.get(packet));

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "sendPacket", at = @At("TAIL"))
    private void onSendPacketTail(Packet<?> packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(PacketEvent.Sent.get(packet));
    }

    @Inject(at = @At("HEAD"), method = "onPlaySound")
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(PlaySoundPacketEvent.get(packet));
    }

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo info) {
        WorldChunk chunk = client.world.getChunk(packet.getX(), packet.getZ());
        MeteorClient.EVENT_BUS.post(ChunkDataEvent.get(chunk));
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
    private void onContainerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(ContainerSlotUpdateEvent.get(packet));
    }

    @Inject(method = "onEntitiesDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;removeEntity(I)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onEntityDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo info, int i, int j) {
        MeteorClient.EVENT_BUS.post(EntityDestroyEvent.get(client.world.getEntityById(j)));
    }

    @Inject(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void onExplosionVelocity(ExplosionS2CPacket packet, CallbackInfo ci) {
        Velocity velocity = Modules.get().get(Velocity.class); //Velocity for explosions
        if (!velocity.explosions.get()) return;

        ((IExplosionS2CPacket) packet).setVelocityX((float) (packet.getPlayerVelocityX() * velocity.getHorizontal()));
        ((IExplosionS2CPacket) packet).setVelocityY((float) (packet.getPlayerVelocityY() * velocity.getVertical()));
        ((IExplosionS2CPacket) packet).setVelocityZ((float) (packet.getPlayerVelocityZ() * velocity.getHorizontal()));
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;", ordinal = 0))
    private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo info) {
        Entity itemEntity = client.world.getEntityById(packet.getEntityId());
        Entity entity = client.world.getEntityById(packet.getCollectorEntityId());

        if (itemEntity instanceof ItemEntity && entity == client.player) {
            MeteorClient.EVENT_BUS.post(PickItemsEvent.get(((ItemEntity) itemEntity).getStack(), packet.getStackAmount()));
        }
    }


    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/data/DataTracker;writeUpdatedEntries(Ljava/util/List;)V"))
    public void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
        final Entity entity = this.world.getEntityById(packet.id());
        try {
            if (entity != null && packet.getTrackedValues() != null && entity instanceof VillagerEntity) {
                for (DataTracker.Entry<?> entry : packet.getTrackedValues()) {
                    if (entry.get() instanceof VillagerData) {

                        final VillagerData newData = new VillagerData(((VillagerData) entry.get()).getType(),
                                ((VillagerData) entry.get()).getProfession(),
                                ((VillagerData) entry.get()).getLevel());

                        //Spawning a new Villager has same Data i think
                        if (!((VillagerEntity) entity).getVillagerData().equals(newData)) {
                            MeteorClient.EVENT_BUS.post(VillagerUpdateProfessionEvent.get(
                                    (VillagerEntity) entity,
                                    ((VillagerEntity) entity).getVillagerData(),
                                    newData,
                                    newData.getProfession() == VillagerProfession.NONE
                                            ? VillagerUpdateProfessionEvent.Action.LOST_JOB
                                            : VillagerUpdateProfessionEvent.Action.GOT_JOB
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("network handler tracker: oof");
            e.printStackTrace();
        }

    }

}