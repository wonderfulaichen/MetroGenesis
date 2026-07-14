package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.citizen.types.CitizenData;
import com.metrogenesis.colony.ColonyState;
import com.metrogenesis.colony.managers.CitizenManager;
import com.metrogenesis.entity.MetroGenesisCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 甯傛斂鍘呰彍鍗?鈥?0 涓Ы浣嶏紝绾俊鎭睍绀?+ 鎸夐挳閫氫俊
 * <p>
 * ContainerData 鍚屾锛?=鍥藉簱, 1=褰撳墠浜哄彛, 2=鏈€澶т汉鍙? * 鍒濆鍖呭悓姝ワ細甯傛皯鍒楄〃锛堝悕瀛?鑱屼笟/婊℃剰搴︼級
 */
public class TownHallMenu extends AbstractContainerMenu {

    private final BlockPos townHallPos;
    private final ContainerData data;

    /** 甯傛皯鍒楄〃锛堝鎴风锛?*/
    public String[] citizenNames = {};
    public String[] citizenJobs = {};
    public int[] citizenSatisfactions = {};
    public int[] citizenIds = {}; // 绋冲畾鐨勫競姘?ID锛岀敤浜庢寜閽紪鐮?
    // ══ 瀹㈡埛绔瀯閫狅紙浠庣綉缁滃寘鍙嶅簭鍒楀寲锛?═════════════════

    public TownHallMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(MetroGenesis.TOWN_HALL_MENU.get(), id);
        this.townHallPos = buf.readBlockPos();

        int funds = buf.readInt();
        int pop = buf.readInt();
        int maxPop = buf.readInt();

        // 璇诲彇甯傛皯鍒楄〃
        int count = buf.readInt();
        citizenNames = new String[count];
        citizenJobs = new String[count];
        citizenSatisfactions = new int[count];
        citizenIds = new int[count];
        for (int i = 0; i < count; i++) {
            citizenIds[i] = buf.readInt();
            citizenNames[i] = buf.readUtf(64);
            citizenJobs[i] = buf.readUtf(32);
            citizenSatisfactions[i] = buf.readInt();
        }

        this.data = new ContainerData() {
            private int f = funds;
            private int p = pop;
            private int m = maxPop;

            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> f; case 1 -> p; case 2 -> m;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {
                if (i == 0) f = v;
                else if (i == 1) p = v;
                else if (i == 2) m = v;
            }
            @Override public int getCount() { return 3; }
        };
        addDataSlots(this.data);
    }

    // ══ 鏈嶅姟绔瀯閫?═══════════════════════════════════

    public TownHallMenu(int id, Inventory inv, ColonyState colony, BlockPos pos) {
        super(MetroGenesis.TOWN_HALL_MENU.get(), id);
        this.townHallPos = pos;

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> colony.getFunds();
                    case 1 -> colony.getPopulation();
                    case 2 -> colony.getMaxPopulation();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                if (index == 0) colony.setFunds(value);
            }
            @Override public int getCount() { return 3; }
        };
        addDataSlots(this.data);
    }

    // ══ 搴忓垪鍖栧競姘戝垪琛ㄥ埌缂撳啿鍖猴紙渚?TownHallBlock 鍐欏叆鍖咃級 ══

    public static void writeCitizens(FriendlyByteBuf buf, ColonyState colony) {
        CitizenManager mgr = colony.getCitizenManager();
        List<CitizenData> citizens = mgr.getCitizens();
        buf.writeInt(citizens.size());
        for (CitizenData c : citizens) {
            buf.writeInt(c.getId());
            buf.writeUtf(c.getName(), 64);
            buf.writeUtf(c.getJob(), 32);
            buf.writeInt(c.getSatisfaction());
        }
    }

    // ══ 鎸夐挳浜嬩欢锛堜粎鏈嶅姟绔墽琛岋級 ═════════════════════

    private static final String[] JOBS = {"farmer", "merchant", "builder"};
    private static final String[] JOB_NAMES = {"农夫", "杂货商", "建造师"};
    private static final String[] ALL_JOBS = {"unemployed", "farmer", "builder", "merchant"};

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        // 鎹㈠伐浣滆姹傦細buttonId 缂栫爜 = 1000 + citizenId*10 + jobIdx
        // jobIdx: 0=unemployed, 1=farmer, 2=builder, 3=merchant
        if (buttonId >= 1000 && player.level() instanceof ServerLevel serverLevel) {
            int id = buttonId - 1000;
            int citizenId = id / 10;
            int jobIdx = id % 10;
            ColonyState colony = ColonyState.get(serverLevel);
            CitizenData data = colony.getCitizenManager().getCivilian(citizenId);
            if (data != null && jobIdx >= 0 && jobIdx < ALL_JOBS.length) {
                String newJob = ALL_JOBS[jobIdx];
                data.setJob(newJob);

                // 鈽?鍏抽敭淇锛氬悓姝ュ疄浣撴暟鎹?+ 閲嶆柊鎸傝浇 AI
                Entity entity = serverLevel.getEntity(data.getUUID());
                if (entity instanceof MetroGenesisCitizen citizen) {
                    citizen.syncFromData(data);     // 鍚屾鎵€鏈夊瓧娈靛埌 SynchedEntityData
                    citizen.reloadJobAI();           // 鍘熷瓙鏇挎崲鑱屼笟 AI
                } else {
                    MetroGenesis.LOGGER.warn("[JobChange] #{} 瀹炰綋涓嶅湪涓栫晫涓?(uuid={})", citizenId, data.getUUID());
                }

                player.sendSystemMessage(Component.literal(
                        "§e" + data.getName() + " §7鐨勫伐浣滃凡鍙樻洿涓? §f" +
                        switch(newJob) {
                            case "farmer" -> "鍐滃か";
                            case "builder" -> "寤洪€犲笀";
                            case "merchant" -> "鍟嗕汉";
                            default -> "寰呬笟";
                        }));
                MetroGenesis.LOGGER.info("[JobChange] #{} {} 鈫?{}", citizenId, data.getName(), newJob);
                return true;
            }
            return false;
        }

        // 鎷涘嫙鎸夐挳锛? = 鍐滃か, 1 = 鏉傝揣鍟? 2 = 寤洪€犲笀
        if (buttonId >= 0 && buttonId <= 2 && player.level() instanceof ServerLevel serverLevel) {
            ColonyState colony = ColonyState.get(serverLevel);

            if (!colony.spend(100)) {
                player.sendSystemMessage(Component.literal("§c鍥藉簱涓嶈冻锛佹嫑鍕熼渶瑕?100 C-Value"));
                return false;
            }

            String jobId = JOBS[buttonId];
            String jobName = JOB_NAMES[buttonId];
            CitizenManager mgr = colony.getCitizenManager();
            CitizenData data = mgr.createAndRegisterCivilianData();
            data.setJob(jobId);

            BlockPos spawnPos = townHallPos != null ? townHallPos : player.blockPosition();
            mgr.spawnEntity(data, serverLevel, spawnPos);

            player.sendSystemMessage(Component.literal(
                    "§a鎷涘嫙鎴愬姛锛伮" + data.getName() + " §7(" + jobName + ")"));
            colony.addToTreasury(0);
            return true;
        }
        return false;
    }

    // ══ 蹇呴』瀹炵幇 ═════════════════════════════════════

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // ══ 渚?Screen 璇诲彇 ═══════════════════════════════

    public int getFunds() { return data.get(0); }
    public int getPopulation() { return data.get(1); }
    public int getMaxPopulation() { return data.get(2); }
    public BlockPos getTownHallPos() { return townHallPos; }
}
