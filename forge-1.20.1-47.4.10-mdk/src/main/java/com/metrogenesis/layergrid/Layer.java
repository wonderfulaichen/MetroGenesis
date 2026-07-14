package com.metrogenesis.layergrid;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * LayerGrid 中的单个数据层。
 * <p>
 * 每层维护一个 BitSet 位图表示方块归属，以及一个可选的 {@code BlockPos} 集合用于遍历。
 * 位图使用一维索引 {@code x + z * gridSize} 映射到二维世界坐标。
 * </p>
 *
 * <h3>层属性</h3>
 * <ul>
 *   <li><b>name</b> — 层名（如 "road", "zone_residential"），唯一标识</li>
 *   <li><b>owner</b> — 所有者标识。空字符串表示共享层（不与任何层冲突）</li>
 *   <li><b>bitmap</b> — 稠密位图，适合冲突检测</li>
 *   <li><b>blockPosSet</b> — 稀疏集合，适合遍历</li>
 * </ul>
 */
public class Layer
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 层名（唯一标识） */
    private final String name;

    /** 所有者标识。"" = 共享层（不参与冲突检测） */
    private final String owner;

    /** 网格大小（沿一个方向）；网格 = gridSize × gridSize */
    private final int gridSize;

    /** 方块归属位图，索引 = x + z * gridSize */
    private final BitSet bitmap;

    /** 稀疏集合，用于 O(1) 遍历 */
    private final Set<BlockPos> posSet = new HashSet<>();

    // ════════════════════════════════════════════════════════
    //  构造器
    // ════════════════════════════════════════════════════════

    /**
     * 创建新层。
     *
     * @param name     层名
     * @param owner    所有者标识；"" 表示共享层
     * @param gridSize 网格大小（覆盖范围）
     */
    public Layer(final String name, final String owner, final int gridSize)
    {
        this.name = name;
        this.owner = owner;
        this.gridSize = gridSize;
        this.bitmap = new BitSet(gridSize * gridSize);
    }

    // ════════════════════════════════════════════════════════
    //  claim / release
    // ════════════════════════════════════════════════════════

    /**
     * 声明一个方块归属于此层。
     *
     * @param x 世界 X 坐标
     * @param z 世界 Z 坐标
     * @return true 如果之前未被声明；false 如果已是此层成员
     */
    public boolean claim(final int x, final int z)
    {
        final int idx = index(x, z);
        if (idx < 0 || bitmap.get(idx)) return false;

        bitmap.set(idx);
        posSet.add(new BlockPos(x, 0, z));
        return true;
    }

    /**
     * 释放一个方块从该层的归属。
     *
     * @param x 世界 X 坐标
     * @param z 世界 Z 坐标
     * @return true 如果之前已声明；false 如果本来就不归属此层
     */
    public boolean release(final int x, final int z)
    {
        final int idx = index(x, z);
        if (idx < 0 || !bitmap.get(idx)) return false;

        bitmap.clear(idx);
        posSet.remove(new BlockPos(x, 0, z));
        return true;
    }

    /**
     * 检查方块是否属于此层。
     */
    public boolean contains(final int x, final int z)
    {
        final int idx = index(x, z);
        return idx >= 0 && bitmap.get(idx);
    }

    // ════════════════════════════════════════════════════════
    //  查询
    // ════════════════════════════════════════════════════════

    /** 层名 */
    public String getName() { return name; }

    /** 所有者标识 */
    public String getOwner() { return owner; }

    /** 网格大小 */
    public int getGridSize() { return gridSize; }

    /** 是否为共享层 */
    public boolean isShared() { return owner.isEmpty(); }

    /** 获取此层下所有已声明的坐标集合 */
    public Set<BlockPos> getPositions() { return posSet; }

    /** 当前声明方块数 */
    public int size() { return posSet.size(); }

    // ════════════════════════════════════════════════════════
    //  内部工具
    // ════════════════════════════════════════════════════════

    /**
     * 将世界坐标映射到位图索引。
     * 基于 gridSize 范围取模，确保在有效范围内。
     */
    private int index(final int x, final int z)
    {
        final int mx = Math.floorMod(x, gridSize);
        final int mz = Math.floorMod(z, gridSize);
        return mx + mz * gridSize;
    }
}
