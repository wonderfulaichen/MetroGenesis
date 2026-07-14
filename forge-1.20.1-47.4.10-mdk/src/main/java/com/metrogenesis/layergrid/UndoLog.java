package com.metrogenesis.layergrid;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * UndoLog — 操作回滚日志（内存实现）。
 * <p>
 * 基础实现使用 Deque 暂存，支持撤销到任意深度。
 * Phase 4 将扩展到硬盘持久化。
 * </p>
 */
public class UndoLog
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 撤销历史栈，FILO — 最新操作在顶部 */
    private final Deque<UndoEntry> history = new ArrayDeque<>();

    /** 最大保留条目数（-1 = 无限制） */
    private final int maxEntries;

    public UndoLog()
    {
        this(-1);
    }

    /**
     * @param maxEntries 最大保留条目数。-1 表示无限制。
     */
    public UndoLog(final int maxEntries)
    {
        this.maxEntries = maxEntries;
    }

    /**
     * 记录一条操作到日志。
     *
     * @param entry 操作记录
     */
    public void push(final UndoEntry entry)
    {
        if (maxEntries > 0 && history.size() >= maxEntries)
        {
            history.pollLast(); // 移除最旧的条目
        }
        history.push(entry);
        LOGGER.trace("UndoLog pushed: {}", entry);
    }

    /**
     * 弹出最近的一条操作（撤销）。
     *
     * @return 最近的操作记录，或 null（历史为空）
     */
    public UndoEntry pop()
    {
        final UndoEntry entry = history.poll();
        if (entry != null)
        {
            LOGGER.trace("UndoLog popped: {}", entry);
        }
        return entry;
    }

    /**
     * 查看最近的操作（不移除）。
     */
    public UndoEntry peek()
    {
        return history.peek();
    }

    /**
     * 清空所有历史。
     */
    public void clear()
    {
        history.clear();
        LOGGER.debug("UndoLog cleared");
    }

    /**
     * 当前历史中待撤销的操作数。
     */
    public int size()
    {
        return history.size();
    }
}
