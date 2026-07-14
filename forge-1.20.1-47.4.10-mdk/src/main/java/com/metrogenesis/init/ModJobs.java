package com.metrogenesis.init;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.citizen.job.BuilderWorkGoal;
import com.metrogenesis.citizen.job.FarmerWorkGoal;
import com.metrogenesis.citizen.job.MerchantWorkGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 鑱屼笟娉ㄥ唽琛?鈥?杞婚噺绾?Map 鏂规锛屾浛浠?switch-case
 * <p>
 * 鏂板鑱屼笟鍙渶锛? * 1. 鍐欎竴涓户鎵?Goal 鐨勭被
 * 2. 鍦ㄨ繖閲屾敞鍐岋紙涓€琛屼唬鐮侊級
 * 涓嶉渶瑕佹敼 {@link com.metrogenesis.citizen.CitizenEvents} 鎴栦换浣曞叾浠栨枃浠躲€? */
public class ModJobs {

    /** 鑱屼笟 ID 鈫?宸ュ巶鍑芥暟鏄犲皠 */
    private static final Map<String, Function<Mob, Goal>> REGISTRY = new HashMap<>();

    static {
        // 鍐呯疆鑱屼笟娉ㄥ唽
        register("farmer",   FarmerWorkGoal::new);
        register("builder",  BuilderWorkGoal::new);
        register("merchant", MerchantWorkGoal::new);
    }

    /**
     * 娉ㄥ唽涓€涓亴涓?     *
     * @param id      鑱屼笟 ID锛堜笌 entity 鐨?getCitizenJob() 杩斿洖鍊间竴鑷达級
     * @param factory 宸ュ巶鍑芥暟锛屾帴鏀?Mob 杩斿洖 Goal
     */
    public static void register(final String id, final Function<Mob, Goal> factory) {
        if (REGISTRY.containsKey(id)) {
            MetroGenesis.LOGGER.warn("[ModJobs] \u804C\u4E1A '{}' \u91CD\u590D\u6CE8\u518C\uFF0C\u8DF3\u8FC7", id);
            return;
        }
        REGISTRY.put(id, factory);
        MetroGenesis.LOGGER.debug("[ModJobs] 娉ㄥ唽鑱屼笟: {}", id);
    }

    /**
     * 鍒涘缓鑱屼笟 AI
     *
     * @param id  鑱屼笟 ID
     * @param mob 鐩爣瀹炰綋
     * @return Goal 瀹炰緥锛岃嫢鏈敞鍐岃繑鍥?null
     */
    public static Goal createGoal(final String id, final Mob mob) {
        final Function<Mob, Goal> factory = REGISTRY.get(id);
        if (factory == null) {
            MetroGenesis.LOGGER.warn("[ModJobs] 鏈煡鑱屼笟 '{}'锛屾棤娉曞垱寤?AI", id);
            return null;
        }
        return factory.apply(mob);
    }

    /**
     * 妫€鏌ユ煇鑱屼笟鏄惁宸叉敞鍐?     */
    public static boolean isRegistered(final String id) {
        return REGISTRY.containsKey(id);
    }

    /**
     * 鑾峰彇鎵€鏈夊凡娉ㄥ唽鐨勮亴涓?ID
     */
    public static String[] getAllJobIds() {
        return REGISTRY.keySet().toArray(new String[0]);
    }
}
