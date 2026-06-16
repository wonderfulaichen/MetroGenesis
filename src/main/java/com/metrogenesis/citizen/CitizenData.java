package com.metrogenesis.citizen;

import com.metrogenesis.colony.ColonyState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * @deprecated 宸茶縼绉昏嚦 {@link com.metrogenesis.citizen.types.CitizenData}
 *             姝ゆ枃浠朵粎淇濈暀 {@link #payToTreasury(Entity, ServerLevel, int)} 渚涘伐浣?AI 浣跨敤锛? *             鍚庣画閫愭杩佺Щ銆? */
@Deprecated
public class CitizenData {

    /**
     * 甯傛皯宸ヤ綔璧氬彇鐨勬敹鍏ョ洿鎺ヨ繘鍏ュ煄甯傚浗搴?     */
    public static void payToTreasury(Entity entity, ServerLevel level, int amount) {
        ColonyState colony = ColonyState.get(level);
        colony.addToTreasury(amount);
    }
}
