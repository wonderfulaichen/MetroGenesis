package com.metrogenesis.layergrid;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * LayerGrid — 多层 bitmap 区域管理系统。
 * <p>
 * 核心职责：
 * <ol>
 *   <li>管理多个命名 Layer，每个 Layer 维护自己的 BitSet 位图</li>
 *   <li>提供 {@link #claim}/{@link #release} 操作，内置冲突检测</li>
 *   <li>每步操作自动写入 {@link UndoLog}</li>
 *   <li>支持按坐标查询所有层的归属状态</li>
 * </ol>
 * </p>
 *
 * <h3>冲突规则</h3>
 * <ul>
 *   <li>共享层（owner=""）不与其他任何层冲突</li>
 *   <li>不同 owner 的非共享层互相冲突：claim 时检查目标位置的所有层</li>
 *   <li>相同 owner 的非共享层可以共存</li>
 * </ul>
 */
public class LayerGrid
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 所有层的映射：层名 → Layer */
    private final Map<String, Layer> layers = new LinkedHashMap<>();

    /** 操作回滚日志 */
    private final UndoLog undoLog = new UndoLog();

    /** 默认网格大小（覆盖范围），512×512 方块 */
    public static final int DEFAULT_GRID_SIZE = 512;

    // ════════════════════════════════════════════════════════
    //  层管理
    // ════════════════════════════════════════════════════════

    /**
     * 注册一个已有 Layer 实例。
     *
     * @param layer 层
     * @throws IllegalArgumentException 如果层名已存在
     */
    public void addLayer(final Layer layer)
    {
        if (layers.containsKey(layer.getName()))
        {
            throw new IllegalArgumentException("Layer already exists: " + layer.getName());
        }
        layers.put(layer.getName(), layer);
        LOGGER.debug("Layer added: {}", layer.getName());
    }

    /**
     * 创建并注册一个新层。
     *
     * @param name  层名（唯一）
     * @param owner 所有者标识；"" 表示共享层
     * @return 创建的 Layer
     */
    public Layer createLayer(final String name, final String owner)
    {
        return createLayer(name, owner, DEFAULT_GRID_SIZE);
    }

    /**
     * 创建并注册一个新层，指定网格大小。
     *
     * @param name     层名
     * @param owner    所有者标识
     * @param gridSize 网格大小
     * @return 创建的 Layer
     */
    public Layer createLayer(final String name, final String owner, final int gridSize)
    {
        if (layers.containsKey(name))
        {
            throw new IllegalArgumentException("Layer already exists: " + name);
        }
        final Layer layer = new Layer(name, owner, gridSize);
        layers.put(name, layer);
        LOGGER.debug("Layer created: {} (owner={}, grid={})", name, owner, gridSize);
        return layer;
    }

    /**
     * 按名称获取层。
     */
    @Nullable
    public Layer getLayer(final String name)
    {
        return layers.get(name);
    }

    /**
     * 获取所有已注册的层名。
     */
    public Set<String> getLayerNames()
    {
        return layers.keySet();
    }

    // ════════════════════════════════════════════════════════
    //  Claim / Release（核心业务操作）
    // ════════════════════════════════════════════════════════

    /**
     * 声明一个方块归属于指定层。
     *
     * @param x         世界 X 坐标
     * @param z         世界 Z 坐标
     * @param layerName 目标层名
     * @return ClaimResult — 操作结果
     */
    public ClaimResult claim(final int x, final int z, final String layerName)
    {
        final Layer layer = layers.get(layerName);
        if (layer == null)
        {
            return ClaimResult.failure("Layer not found: " + layerName);
        }

        // 冲突检测：共享层跳过检查
        if (!layer.isShared())
        {
            final String conflictLayer = detectConflict(x, z, layer);
            if (conflictLayer != null)
            {
                return ClaimResult.failure("Conflict with layer: " + conflictLayer);
            }
        }

        // 执行声明
        final boolean changed = layer.claim(x, z);
        if (changed)
        {
            final boolean wasClaimedBefore = false; // claim 前一定未声明
            undoLog.push(new UndoEntry(
                System.currentTimeMillis(), UndoEntry.Operation.CLAIM,
                layerName, x, z, wasClaimedBefore));
            LOGGER.debug("Claimed ({},{}) in layer '{}'", x, z, layerName);
            return ClaimResult.success();
        }
        return ClaimResult.failure("Already claimed");
    }

    /**
     * 释放一个方块从指定层。
     *
     * @param x         世界 X 坐标
     * @param z         世界 Z 坐标
     * @param layerName 目标层名
     * @return true 如果释放成功
     */
    public boolean release(final int x, final int z, final String layerName)
    {
        final Layer layer = layers.get(layerName);
        if (layer == null) return false;

        final boolean wasClaimed = layer.contains(x, z);
        final boolean changed = layer.release(x, z);
        if (changed)
        {
            undoLog.push(new UndoEntry(
                System.currentTimeMillis(), UndoEntry.Operation.RELEASE,
                layerName, x, z, wasClaimed));
            LOGGER.debug("Released ({},{}) from layer '{}'", x, z, layerName);
            return true;
        }
        return false;
    }

    /**
     * 查询指定位置的所有层归属。
     *
     * @param x 世界 X 坐标
     * @param z 世界 Z 坐标
     * @return 该位置所属的层名列表
     */
    public List<String> query(final int x, final int z)
    {
        final List<String> result = new ArrayList<>();
        for (final Map.Entry<String, Layer> entry : layers.entrySet())
        {
            if (entry.getValue().contains(x, z))
            {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // ════════════════════════════════════════════════════════
    //  冲突检测
    // ════════════════════════════════════════════════════════

    /**
     * 检测在 (x,z) 位置声明 {@code candidateLayer} 是否与已有层冲突。
     *
     * @param x              世界 X 坐标
     * @param z              世界 Z 坐标
     * @param candidateLayer 要声明的层
     * @return 冲突的层名，或 null（无冲突）
     */
    @Nullable
    private String detectConflict(final int x, final int z, final Layer candidateLayer)
    {
        for (final Layer existing : layers.values())
        {
            if (existing == candidateLayer) continue;        // 跳过自身
            if (existing.isShared()) continue;               // 共享层不冲突
            if (existing.getOwner().equals(candidateLayer.getOwner())) continue; // 同 owner 不冲突
            if (existing.contains(x, z))
            {
                return existing.getName();
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════
    //  撤销
    // ════════════════════════════════════════════════════════

    /**
     * 撤销最近一次操作。
     *
     * @return true 如果撤销成功
     */
    public boolean undo()
    {
        final UndoEntry entry = undoLog.pop();
        if (entry == null) return false;

        final Layer layer = layers.get(entry.layerName);
        if (layer == null)
        {
            LOGGER.warn("Undo failed: layer '{}' no longer exists", entry.layerName);
            return false;
        }

        switch (entry.operation)
        {
            case CLAIM:
                // 撤销 claim = release
                layer.release(entry.x, entry.z);
                break;
            case RELEASE:
                // 撤销 release = re-claim
                layer.claim(entry.x, entry.z);
                break;
        }
        LOGGER.debug("Undid {} on ({},{}) in layer '{}'",
            entry.operation, entry.x, entry.z, entry.layerName);
        return true;
    }

    /**
     * 获取 UndoLog 实例。
     */
    public UndoLog getUndoLog()
    {
        return undoLog;
    }

    // ════════════════════════════════════════════════════════
    //  结果类型
    // ════════════════════════════════════════════════════════

    /**
     * claim 操作的结果。
     */
    public static class ClaimResult
    {
        public final boolean success;
        public final String reason;

        private ClaimResult(final boolean success, final String reason)
        {
            this.success = success;
            this.reason = reason;
        }

        public static ClaimResult success() { return new ClaimResult(true, ""); }
        public static ClaimResult failure(final String reason) { return new ClaimResult(false, reason); }
    }
}
