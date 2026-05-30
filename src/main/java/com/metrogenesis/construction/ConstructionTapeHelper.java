package com.metrogenesis.construction;

import com.metrogenesis.MetroGenesis;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/**
 * 鏂藉伐鍥存爮杈呭姪宸ュ叿 鈥?鍦?Zone 鐨?perimeter 涓婃斁缃?绉婚櫎鍥存爮鏂瑰潡
 * <p>
 * 鍙傝€?MineColonies {@code ConstructionTapeHelper}
 */
public class ConstructionTapeHelper {

    private static Block tapeBlock = null;

    /**
     * 璁剧疆鍥存爮鏂瑰潡寮曠敤锛堢敱 MetroGenesis.java 娉ㄥ唽鏃惰皟鐢級
     */
    public static void setTapeBlock(Block block) {
        tapeBlock = block;
    }

    /**
     * 鍦?Zone 鐨?perimeter 涓婃斁缃洿鏍忔柟鍧?     * <p>
     * 浠呮斁缃簳灞傦紙Y = minY锛夌殑 X 鍜?Z 杈圭晫绾匡紝
     * 浠ュ強鍦ㄨ钀芥斁鏌卞瓙鏍囪楂樺害銆?     */
    public static void placeConstructionTape(Zone zone, ServerLevel level) {
        if (tapeBlock == null) {
            MetroGenesis.LOGGER.warn("[Tape] 鍥存爮鏂瑰潡鏈敞鍐岋紝璺宠繃鏀剧疆");
            return;
        }

        BlockPos min = zone.getMin();
        BlockPos max = zone.getMax();
        int minY = min.getY();

        // 娌?X 杞村簳杈?
        for (int x = min.getX(); x <= max.getX(); x++) {
            tryPlace(level, x, minY, min.getZ());
            tryPlace(level, x, minY, max.getZ());
        }
        // 娌?Z 杞村簳杈?
        for (int z = min.getZ(); z <= max.getZ(); z++) {
            tryPlace(level, min.getX(), minY, z);
            tryPlace(level, max.getX(), minY, z);
        }
    }

    /**
     * 绉婚櫎 Zone 鑼冨洿鍐呯殑鎵€鏈夊洿鏍忔柟鍧?     */
    public static void removeConstructionTape(Zone zone, ServerLevel level) {
        if (tapeBlock == null) return;

        BlockPos min = zone.getMin();
        BlockPos max = zone.getMax();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(tapeBlock)) {
                level.destroyBlock(pos, false);
            }
        }
    }

    private static void tryPlace(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, tapeBlock.defaultBlockState(), 3);
        }
    }
}
