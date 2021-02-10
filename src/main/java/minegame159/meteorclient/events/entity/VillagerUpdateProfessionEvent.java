package minegame159.meteorclient.events.entity;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerData;

public class VillagerUpdateProfessionEvent {
    private static final VillagerUpdateProfessionEvent INSTANCE = new VillagerUpdateProfessionEvent();

    public VillagerEntity entity;
    public VillagerData oldData;
    public VillagerData newData;
    public Action action;

    public static VillagerUpdateProfessionEvent get(VillagerEntity entity, VillagerData oldData, VillagerData newData, Action newProfession) {
        INSTANCE.entity = entity;
        INSTANCE.oldData = oldData;
        INSTANCE.newData = newData;
        INSTANCE.action = newProfession;
        return INSTANCE;
    }

    public enum Action {
        LOST_JOB,
        GOT_JOB
    }

}
