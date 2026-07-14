package com.metrogenesis.colony;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.citizen.types.CitizenData;
import com.metrogenesis.colony.managers.CitizenManager;
import com.metrogenesis.entity.MetroGenesisCitizen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 姣忔棩鍑屾櫒鑷姩缁忔祹缁撶畻
 * <p>
 * 鍦ㄦ瘡鏃ュ紑濮嬫椂锛坉ayTime 璺ㄨ繃 24000 杈圭晫锛夎繍琛屼竴娆★細
 * <ol>
 *   <li>閬嶅巻鎵€鏈夊競姘戞暟鎹紝鏌ユ壘瀵瑰簲瀹炰綋</li>
 *   <li>甯傛皯宸ヨ祫浠庡浗搴撴墸闄?鈫?鍙戝埌涓汉閽卞寘</li>
 *   <li>婊℃剰搴﹂殢宸ヨ祫瓒抽鎯呭喌璋冩暣</li>
 *   <li>甯傛皯浜ょ◣锛?0%锛夆啋 鍏ュ浗搴?/li>
 *   <li>椋熺墿娑堣垂浠庨挶鍖呮墸闄?/li>
 *   <li>鍩庡競缁存姢璐逛粠鍥藉簱鎵ｉ櫎</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = MetroGenesis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DailyScheduler {

    private static final Logger LOGGER = MetroGenesis.LOGGER;
    private static long lastDay = -1;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.level;
        if (level.dimension() != Level.OVERWORLD) return;

        // 姣忎釜 tick锛氬煄甯傚瓙绯荤粺椹卞姩
        ColonyState colony = ColonyState.get(level);
        colony.tick(level);

        // 每日经济结算（仅首次跨天执行）：
        long currentDay = level.getDayTime() / 24000;
        if (currentDay <= lastDay) return;
        lastDay = currentDay;
        settle(level);
    }

    private static void settle(ServerLevel level) {
        ColonyState colony = ColonyState.get(level);
        CitizenManager mgr = colony.getCitizenManager();

        int totalWages = 0;
        int totalTax = 0;
        int totalFood = 0;
        int processed = 0;

        // 閬嶅巻甯傛皯鏁版嵁锛堟柊浣撶郴锛?
        for (final CitizenData data : mgr.getCitizens()) {
            processed++;

            // 鏌ユ壘瀹炰綋
            Entity entity = level.getEntity(data.getUUID());

            // 1. 璁＄畻宸ヨ祫
            int baseSalary = data.getBaseSalary();
            float efficiency = data.getWorkEfficiency(); // 鍩轰簬婊℃剰搴?
            int income = Math.max(1, (int) (baseSalary * efficiency));

            // 2. 涓汉鎵€寰楃◣ 10%
            int tax = Math.max(1, (int) (income * 0.1f));
            int foodCost = 5;
            int netIncome = income - tax - foodCost;

            // 3. 鍥藉簱鍙戝伐璧?
            boolean fullPay = colony.getFunds() >= income;
            if (fullPay) {
                colony.spend(income);
                data.addToWallet(netIncome);
                data.addSatisfaction(5); // 瓒抽鍙?鈫?婊℃剰 +5
                totalWages += income;
            } else {
                // 鍥藉簱娌￠挶锛屽競姘戝彧鑳介鍓╀綑閮ㄥ垎
                int actualPay = colony.getFunds();
                int netActual = Math.max(0, actualPay - tax - foodCost);
                if (actualPay > 0) {
                    colony.spend(actualPay);
                    data.addToWallet(netActual);
                    totalWages += actualPay;
                }
                data.addSatisfaction(-10); // 鍙戜笉鍑哄伐璧?鈫?婊℃剰 -10
            }

            // 4. 椋熺墿娑堣垂浠庨挶鍖呮墸
            int currentMoney = data.getWallet();
            int actualFoodCost = Math.min(foodCost, currentMoney);
            data.addToWallet(-actualFoodCost);
            totalFood += actualFoodCost;
            if (actualFoodCost < foodCost) {
                data.addSatisfaction(-5); // 涔颁笉璧烽鐗?鈫?婊℃剰 -5
            }

            // 5. 绋庢敹鍏ュ浗搴?            totalTax += tax;

            // 6. 灏嗘暟鎹啓鍥炲疄浣擄紙瀹㈡埛绔樉绀虹敤锛?
            if (entity instanceof MetroGenesisCitizen citizen) {
                citizen.setWallet(data.getWallet());
                citizen.setSatisfaction(data.getSatisfaction());
            }
        }

        // 鍥藉簱鏀剁◣
        colony.addToTreasury(totalTax);

        // 鍩庡競缁存姢璐癸紙姣忎釜甯傛皯姣忓ぉ 3 C-Value锛?
        int maintenance = colony.getPopulation() * 3;
        colony.spend(maintenance);

        LOGGER.info("[Day {}] 结算完成 → 市民 {} 人 工资 -{}, 税收 +{}, 食物 -{}, 维护 -{}, 国库 {}",
                lastDay, processed, totalWages, totalTax, totalFood, maintenance, colony.getFunds());
    }

    /** 閲嶇疆锛堢敤浜庤皟璇?/ 閲嶈浇涓栫晫锛?*/
    public static void reset() {
        lastDay = -1;
    }
}
