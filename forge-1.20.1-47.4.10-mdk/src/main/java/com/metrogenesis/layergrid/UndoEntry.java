package com.metrogenesis.layergrid;

/**
 * UndoLog 中的单条操作记录。
 * <p>
 * 记录操作类型、层名、坐标，以及操作前的 bitmap 快照（用于回滚）。
 * </p>
 */
public class UndoEntry
{
    public enum Operation { CLAIM, RELEASE }

    /** 操作时间戳 */
    public final long timestamp;

    /** 操作类型 */
    public final Operation operation;

    /** 操作所在的层名 */
    public final String layerName;

    /** 方块的世界 X 坐标 */
    public final int x;

    /** 方块的世界 Z 坐标 */
    public final int z;

    /** 操作前被影响 position 的 claim 状态 (true=已声明, false=未声明) */
    public final boolean wasClaimedBefore;

    public UndoEntry(final long timestamp, final Operation operation,
                     final String layerName, final int x, final int z,
                     final boolean wasClaimedBefore)
    {
        this.timestamp = timestamp;
        this.operation = operation;
        this.layerName = layerName;
        this.x = x;
        this.z = z;
        this.wasClaimedBefore = wasClaimedBefore;
    }

    @Override
    public String toString()
    {
        return "UndoEntry[" + operation + " " + layerName + " (" + x + "," + z + ")]";
    }
}
