package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import minegame159.meteorclient.utils.player.ChatUtils;
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
        ChatUtils.info("Villager Update! Old Profession: %s | new Profession %s",
                villagerEntity.getVillagerData().getProfession().toString(),
                VillagerProfession.NONE.toString());


        MeteorClient.EVENT_BUS.post(VillagerUpdateProfessionEvent.get(
                villagerEntity,
                villagerEntity.getVillagerData(),
                villagerEntity.getVillagerData().withProfession(VillagerProfession.NONE),
                VillagerUpdateProfessionEvent.Action.LOST_JOB
        ));
        ChatUtils.info("Posted in Event Bus!");

    }
}
