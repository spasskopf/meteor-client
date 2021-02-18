package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import net.minecraft.entity.ai.brain.task.LoseJobOnSiteLossTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoseJobOnSiteLossTask.class)
public class LoseJobOnSiteLossTaskMixin {

    @Inject(method = "run", at = @At("HEAD"))
    public void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l, CallbackInfo ci){
        MeteorClient.EVENT_BUS.post(VillagerUpdateProfessionEvent.get(
                villagerEntity,
                villagerEntity.getVillagerData(),
                villagerEntity.getVillagerData().withProfession(VillagerProfession.NONE),
                VillagerUpdateProfessionEvent.Action.LOST_JOB
        ));
    }
}
