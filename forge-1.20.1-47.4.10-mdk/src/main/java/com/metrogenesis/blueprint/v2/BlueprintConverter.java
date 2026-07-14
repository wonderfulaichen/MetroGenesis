package com.metrogenesis.blueprint.v2;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.blueprint.v1.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 蓝图转换器 — 将轻量 v1 Blueprint 转换为 structurize 全功能 Blueprint。
 * <p>
 * v1.Blueprint 是自研的轻量蓝图类，仅包含基础体素+调色板数据；
 * structurize.Blueprint 实现了 {@code IFakeLevelBlockGetter} 接口，
 * 可用于 {@link com.metrogenesis.structurize.placement.StructurePlacementUtils#loadAndPlaceStructureWithRotation}。
 * <p>
 * <b>调色板映射说明</b>：
 * <ul>
 *   <li>v1 palette[0] = AIR（空气方块），structure[y][z][x]==0 表示空气，扫描时跳过</li>
 *   <li>structurize palette[0] = blockSubstitution（替换占位），structure[y][z][x]==0 同样被跳过</li>
 *   <li>v1 palette[1+] 与 structurize palette[1+] 对齐 — 都是真实方块状态</li>
 *   <li>直接复制 structure 数组即可，index 0 的语义（空气↔替换）在放置时均被跳过</li>
 * </ul>
 */
public final class BlueprintConverter
{
    private BlueprintConverter() {}

    /**
     * 将 v1 Blueprint 转换为 structurize Blueprint。
     * <p>
     * 转换步骤：
     * <ol>
     *   <li>构建调色板：跳过 v1 palette[0]（AIR），逐个 addBlockState 填充 structurize 调色板</li>
     *   <li>直接复制 3D structure 数组（index 映射天然对齐）</li>
     *   <li>复制 TileEntity 3D 数组</li>
     *   <li>复制名称</li>
     * </ol>
     *
     * @param source v1 蓝图（自研轻量版）
     * @return structurize 全功能 Blueprint，可直接传入 StructurePlacementUtils
     */
    public static com.metrogenesis.structurize.blueprints.v1.Blueprint convert(Blueprint source)
    {
        if (source == null) return null;

        short sizeX = source.getSizeX();
        short sizeY = source.getSizeY();
        short sizeZ = source.getSizeZ();

        com.metrogenesis.structurize.blueprints.v1.Blueprint target =
            new com.metrogenesis.structurize.blueprints.v1.Blueprint(sizeX, sizeY, sizeZ);

        // ── 1. 构建调色板 ──
        // addBlockState 会自动去重并分配 palette index。
        // v1 palette[0]=AIR（index 0 表示空气，structure 中不存储），
        // structurize palette[0]=blockSubstitution（index 0 同样在放置时被跳过）。
        // 因此 v1 palette[i] (i>=1) 对应 structurize palette[i]。
        List<BlockState> srcPalette = source.getPalette();
        BlockPos tempPos = new BlockPos(0, 0, 0);

        for (int i = 1; i < srcPalette.size(); i++)
        {
            target.addBlockState(tempPos, srcPalette.get(i));
            // addBlockState 在 (0,0,0) 写入 structure[0][0][0]，
            // 下面会用原始数据覆盖整个 structure，所以无影响。
        }

        // ── 2. 直接复制 structure 数组 ──
        short[][][] srcStruct = source.getStructure();
        short[][][] tgtStruct = target.getStructure();

        for (short y = 0; y < sizeY; y++)
        {
            for (short z = 0; z < sizeZ; z++)
            {
                System.arraycopy(srcStruct[y][z], 0, tgtStruct[y][z], 0, sizeX);
            }
        }

        // ── 3. 复制 TileEntity 3D 数组 ──
        CompoundTag[][][] srcTE = source.getTileEntities();
        CompoundTag[][][] tgtTE = target.getTileEntities();

        for (short y = 0; y < sizeY; y++)
        {
            for (short z = 0; z < sizeZ; z++)
            {
                for (short x = 0; x < sizeX; x++)
                {
                    if (srcTE[y][z][x] != null)
                    {
                        tgtTE[y][z][x] = srcTE[y][z][x].copy();
                    }
                }
            }
        }

        // ── 4. 复制名称 ──
        target.setName(source.getName());

        MetroGenesis.LOGGER.debug("[BlueprintConverter] Converted v1→structurize: {} ({}×{}×{})",
            source.getName(), sizeX, sizeY, sizeZ);

        return target;
    }
}
