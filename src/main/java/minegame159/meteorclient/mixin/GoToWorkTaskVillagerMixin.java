package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.VillagerUpdateProfessionEvent;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.GoToWorkTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(GoToWorkTask.class)
public abstract class GoToWorkTaskVillagerMixin {

    /**
     * @author notch or someone else & spasskopf
     * @reason because i am lazy and couldn't find another way
     */
    @Overwrite()
    public void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        GlobalPos globalPos = (GlobalPos) villagerEntity.getBrain().getOptionalMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();

        villagerEntity.getBrain().forget(MemoryModuleType.POTENTIAL_JOB_SITE);
        villagerEntity.getBrain().remember(MemoryModuleType.JOB_SITE, globalPos);
        serverWorld.sendEntityStatus(villagerEntity, (byte) 14);
        if (villagerEntity.getVillagerData().getProfession() == VillagerProfession.NONE) {
            MinecraftServer minecraftServer = serverWorld.getServer();

            Optional.ofNullable(minecraftServer.getWorld(globalPos.getDimension())).flatMap((serverWorldx) -> {
                return serverWorldx.getPointOfInterestStorage().getType(globalPos.getPos());
            }).flatMap((pointOfInterestTypexx) -> {
                return Registry.VILLAGER_PROFESSION.stream().filter((villagerProfessionx) -> {
                    return villagerProfessionx.getWorkStation() == pointOfInterestTypexx;
                }).findFirst();
            }).ifPresent((villagerProfessionx) -> {

                //Begin Meteor
                final VillagerData newData = villagerEntity.getVillagerData().withProfession(villagerProfessionx);

                MeteorClient.EVENT_BUS.post(VillagerUpdateProfessionEvent.get(
                        villagerEntity,
                        villagerEntity.getVillagerData(), newData,
                        VillagerUpdateProfessionEvent.Action.GOT_JOB
                ));

                villagerEntity.setVillagerData(newData);
                //End Meteor
                villagerEntity.reinitializeBrain(serverWorld);
            });
        }
    }

}
