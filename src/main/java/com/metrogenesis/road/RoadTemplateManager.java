package com.metrogenesis.road;

import com.metrogenesis.road.RoadTemplate.TemplateType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 道路模板管理器（单例）。
 * <p>
 * 管理当前活跃的道路模板和模板库。
 * 线程安全：{@code activeTemplate} 以 {@code volatile} 保证可见性，
 * 写操作以 {@code synchronized} 保证原子性。
 * </p>
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>持有并切换 {@link RoadTemplate} 作为当前活跃模板</li>
 *   <li>管理模板库（按类型索引）</li>
 *   <li>提供默认模板的构造与恢复</li>
 *   <li>线程安全的读写 API</li>
 * </ul>
 *
 * @author program-yuan (Phase 1)
 */
public final class RoadTemplateManager {

    // ══ 单例 ═══════════════════════════════════════════════

    /** 全局唯一实例 */
    private static final RoadTemplateManager INSTANCE = new RoadTemplateManager();

    /**
     * 获取管理器全局单例。
     *
     * @return RoadTemplateManager 唯一实例
     */
    public static RoadTemplateManager getInstance() {
        return INSTANCE;
    }

    // ══ 字段 ═══════════════════════════════════════════════

    /** 当前活跃模板 — volatile 保证多线程可见性 */
    private volatile RoadTemplate activeTemplate;

    /** 模板库：按类型索引 */
    private final Map<TemplateType, List<RoadTemplate>> templateLibrary = new EnumMap<>(TemplateType.class);

    // ══ 构造 ═══════════════════════════════════════════════

    /** 私有构造，初始化 activeTemplate 为默认模板 */
    private RoadTemplateManager() {
        this.activeTemplate = buildDefaultTemplate();
        initDefaultTemplates();
    }

    /**
     * 初始化默认模板库。
     */
    private void initDefaultTemplates() {
        // 直行道模板
        registerTemplate(buildDefaultTemplate());
        // 十字道模板
        registerTemplate(buildDefaultCrossroad());
    }

    // ══ 默认模板构建 ═══════════════════════════════════════

    /**
     * 构建默认 3×1×3 直道模板。
     * <pre>
     *   Z →
     * X ┌─────────┐
     * 0 │ S B S   │  S = STONE_BRICKS（路缘）
     * 1 │ P P P   │  P = SMOOTH_STONE（路面）
     * 2 │ S B S   │
     *   └─────────┘
     * </pre>
     * <ul>
     *   <li>X=0, X=2：两侧路缘 = {@code STONE_BRICKS}</li>
     *   <li>X=1：中间路面 = {@code SMOOTH_STONE}</li>
     *   <li>Y 层：单层 ({@code sizeY = 1})</li>
     *   <li>Z 方向：3 格，全部使用相同模式</li>
     *   <li>路基方块：{@code DIRT}</li>
     *   <li>名称：{@code "default"}</li>
     * </ul>
     *
     * @return 新的默认模板实例
     */
    private static RoadTemplate buildDefaultTemplate() {
        BlockState curb    = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState surface = Blocks.SMOOTH_STONE.defaultBlockState();

        // 3 (X) × 1 (Y) × 3 (Z)
        BlockState[][][] blocks = new BlockState[3][1][3];

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                // X=0 和 X=2 为路缘（石砖），X=1 为路面（平滑石）
                blocks[x][0][z] = (x == 0 || x == 2) ? curb : surface;
            }
        }

        return new RoadTemplate("default", TemplateType.STRAIGHT, blocks, Blocks.DIRT.defaultBlockState());
    }

    // ══ 公开 API ══════════════════════════════════════════

    /**
     * 获取当前活跃模板。
     * <p>
     * 返回的引用指向内部模板对象，调用方不应直接修改模板内容；
     * 如需修改，应通过 {@link #setActiveTemplate(RoadTemplate)} 设置新模板。
     * </p>
     *
     * @return 当前活跃模板，永不为 null
     */
    public RoadTemplate getActiveTemplate() {
        // volatile 读已保证可见性；返回引用本身不需要互斥
        return activeTemplate;
    }

    /**
     * 获取新的默认模板副本。
     * <p>
     * 每次调用都构建一个新的 {@link RoadTemplate} 实例，
     * 不影响当前活跃模板。
     * </p>
     *
     * @return 默认模板的新副本
     */
    public RoadTemplate getDefaultTemplate() {
        return buildDefaultTemplate();
    }

    /**
     * 设置当前活跃模板。
     *
     * @param template 要设置为活跃的模板（非 null）
     * @throws NullPointerException 如果 template 为 null
     */
    public void setActiveTemplate(@NotNull RoadTemplate template) {
        Objects.requireNonNull(template, "Active template must not be null");
        // synchronized 保证赋值操作的原子性和 happens-before
        synchronized (this) {
            this.activeTemplate = template;
        }
    }

    /**
     * 重置当前活跃模板为默认模板。
     * <p>
     * 相当于调用 {@code setActiveTemplate(getDefaultTemplate())}。
     * </p>
     */
    public void resetToDefault() {
        synchronized (this) {
            this.activeTemplate = buildDefaultTemplate();
        }
    }

    // ══ 模板库 API ═════════════════════════════════════════

    /**
     * 注册模板到模板库。
     *
     * @param template 要注册的模板
     */
    public void registerTemplate(@NotNull RoadTemplate template) {
        Objects.requireNonNull(template, "Template must not be null");
        synchronized (this) {
            templateLibrary.computeIfAbsent(template.getType(), k -> new ArrayList<>()).add(template);
        }
    }

    /**
     * 获取指定类型的模板列表。
     *
     * @param type 模板类型
     * @return 模板列表（不可修改），如果没有该类型则返回空列表
     */
    public List<RoadTemplate> getTemplatesByType(@NotNull TemplateType type) {
        synchronized (this) {
            return List.copyOf(templateLibrary.getOrDefault(type, List.of()));
        }
    }

    /**
     * 获取指定类型的第一个模板。
     *
     * @param type 模板类型
     * @return 模板，如果没有则返回 null
     */
    @Nullable
    public RoadTemplate getFirstTemplateByType(@NotNull TemplateType type) {
        synchronized (this) {
            List<RoadTemplate> list = templateLibrary.get(type);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }
    }

    /**
     * 获取所有已注册的模板类型。
     *
     * @return 模板类型集合
     */
    public Set<TemplateType> getRegisteredTypes() {
        synchronized (this) {
            return Set.copyOf(templateLibrary.keySet());
        }
    }

    /**
     * 根据上下文自动选择合适的模板。
     * <p>
     * 规则：
     * <ul>
     *   <li>交叉口（3+ 连接）→ 十字道模板</li>
     *   <li>其他情况 → 直行道模板</li>
     * </ul>
     *
     * @param connectionCount 连接数（0-4）
     * @return 合适的模板
     */
    public RoadTemplate getAutoTemplate(int connectionCount) {
        if (connectionCount >= 3) {
            RoadTemplate cross = getFirstTemplateByType(TemplateType.JUNCTION_CROSS);
            if (cross != null) return cross;
        }
        RoadTemplate straight = getFirstTemplateByType(TemplateType.STRAIGHT);
        return straight != null ? straight : activeTemplate;
    }

    // ══ 十字道模板构建 ═════════════════════════════════════

    /**
     * 构建默认 5×1×5 十字道模板。
     * <pre>
     *   Z →
     * X ┌───────────────┐
     * 0 │ S B B B S     │  S = 路缘 (石砖)
     * 1 │ B P P P B     │  P = 路面 (平滑石)
     * 2 │ B P P P B     │  B = 路面 (平滑石，交叉区域)
     * 3 │ B P P P B     │
     * 4 │ S B B B S     │
     *   └───────────────┘
     * </pre>
     */
    private static RoadTemplate buildDefaultCrossroad() {
        BlockState curb    = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState surface = Blocks.SMOOTH_STONE.defaultBlockState();

        // 5 (X) × 1 (Y) × 5 (Z)
        BlockState[][][] blocks = new BlockState[5][1][5];

        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                // 四角路缘，其余路面
                boolean isCorner = (x == 0 || x == 4) && (z == 0 || z == 4);
                blocks[x][0][z] = isCorner ? curb : surface;
            }
        }

        return new RoadTemplate("default_crossroad", TemplateType.JUNCTION_CROSS, blocks, Blocks.DIRT.defaultBlockState());
    }
}
