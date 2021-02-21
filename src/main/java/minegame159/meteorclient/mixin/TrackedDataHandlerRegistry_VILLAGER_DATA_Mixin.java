package minegame159.meteorclient.mixin;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for
 * {@link net.minecraft.entity.data.TrackedDataHandlerRegistry#VILLAGER_DATA}
 */
@Mixin(targets = "net/minecraft/entity/data/TrackedDataHandlerRegistry$9")
public abstract class TrackedDataHandlerRegistry_VILLAGER_DATA_Mixin {

    @Inject(method = "read", at = @At("HEAD"))
    private void readPacket(PacketByteBuf packetByteBuf, CallbackInfoReturnable<VillagerData> cir) {
        try {
            final PacketByteBuf packetByteBuf1 = new PacketByteBuf(packetByteBuf.copy());
            final VillagerData villagerData = new VillagerData((VillagerType) Registry.VILLAGER_TYPE.get(packetByteBuf1.readVarInt()),
                    (VillagerProfession) Registry.VILLAGER_PROFESSION.get(packetByteBuf1.readVarInt()),
                    packetByteBuf1.readVarInt());
            //  System.out.printf("read | %s%n",
//
            //          TradeUtils.toString(
            //                  villagerData)
            //  );

           // MeteorClient.EVENT_BUS.post(VillagerUpdateEvent.get(villagerData));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Inject(method = "copy", at = @At("HEAD"))
    //Minecraft Development Plugin might say no, but VillagerData villagerData, CallbackInfoReturnable<VillagerData> cir
    //is correct (McDev thinks $9 is something else)
    private void copyPacket(VillagerData villagerData, CallbackInfoReturnable<VillagerData> cir) {

        //   System.out.printf("copy | %s%n",
        //           TradeUtils.toString(villagerData));
        // MeteorClient.EVENT_BUS.post(VillagerUpdateEvent.get(villagerData));
    }


}
