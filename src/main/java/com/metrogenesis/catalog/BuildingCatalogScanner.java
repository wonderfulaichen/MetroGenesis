package com.metrogenesis.catalog;

import com.metrogenesis.structurize.api.util.Log;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.metrogenesis.structurize.storage.StructurePackMeta;
import com.metrogenesis.structurize.storage.StructurePacks;
import com.metrogenesis.structurize.util.IOPool;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 建筑图鉴扫描器 — 从殖民地 WindowBuildingBrowser 提取并改造。
 * <p>
 * 消化吸收要点：
 * <ul>
 *   <li>去掉 IBuildingBrowsableBlock（殖民地特有）</li>
 *   <li>改为按目录结构分类（agriculture→生产建筑, fundamentals→住宅, ...）</li>
 *   <li>保留 StructurePacks 蓝图加载管线（structurize 层已改姓）</li>
 *   <li>保留异步扫描 + 进度条模式</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 *   BuildingCatalogScanner scanner = new BuildingCatalogScanner();
 *   Future&lt;List&lt;BuildingCatalogEntry&gt;&gt; future = scanner.scanAll();
 *   // ... 等待 future 完成 ...
 *   List&lt;BuildingCatalogEntry&gt; entries = future.get();
 * </pre>
 */
public class BuildingCatalogScanner {

    private static final int WORKER_THREADS = 4;

    /** 缓存：packName → 扫描结果 */
    private static final Map<String, List<BuildingCatalogEntry>> packCache = new ConcurrentHashMap<>();

    /** 全局扫描进度 */
    private final AtomicInteger progress = new AtomicInteger();
    private volatile int totalPacks = 0;

    /**
     * 异步扫描所有已加载的结构包，返回图鉴条目列表。
     *
     * @return Future，完成后可获取所有 BuildingCatalogEntry
     */
    public Future<List<BuildingCatalogEntry>> scanAllAsync() {
        return IOPool.submit(this::scanAll);
    }

    /**
     * 同步扫描所有已加载的结构包（在 IOPool 线程中执行）。
     *
     * @return 所有图鉴条目
     */
    public List<BuildingCatalogEntry> scanAll() {
        if (!StructurePacks.waitUntilFinishedLoading()) {
            return Collections.emptyList();
        }

        final Collection<StructurePackMeta> packs = StructurePacks.getPackMetas();
        totalPacks = packs.size();
        progress.set(0);

        // 多线程扫描每个包
        final List<BuildingCatalogEntry> allEntries = new ArrayList<>();
        for (StructurePackMeta pack : packs) {
            try {
                allEntries.addAll(scanPack(pack));
            } catch (Exception e) {
                Log.getLogger().warn("[CatalogScanner] Error scanning pack '{}': {}", pack.getName(), e.getMessage());
            }
            progress.incrementAndGet();
        }

        // 按分类排序，然后按名称排序
        allEntries.sort(Comparator
            .comparing(BuildingCatalogEntry::category)
            .thenComparing(BuildingCatalogEntry::name));

        return allEntries;
    }

    /**
     * 扫描单个结构包。
     *
     * @param pack 结构包元数据
     * @return 该包中的所有图鉴条目
     */
    private List<BuildingCatalogEntry> scanPack(StructurePackMeta pack) {
        // 检查缓存
        List<BuildingCatalogEntry> cached = packCache.get(pack.getName());
        if (cached != null) {
            return cached;
        }

        final List<BuildingCatalogEntry> entries = new ArrayList<>();
        final Path packPath = pack.getPath();

        try (Stream<Path> paths = Files.walk(packPath)) {
            paths.forEach(file -> {
                if (!Files.isDirectory(file) && file.toString().endsWith(".blueprint")) {
                    try {
                        BuildingCatalogEntry entry = createEntry(pack, file);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    } catch (Exception e) {
                        // 静默跳过无法解析的蓝图
                    }
                }
            });
        } catch (IOException e) {
            Log.getLogger().error("[CatalogScanner] Error walking pack '{}': {}", pack.getName(), e.getMessage());
        }

        // 合并多级建筑
        List<BuildingCatalogEntry> merged = mergeLevels(entries);

        // 缓存结果
        packCache.put(pack.getName(), merged);

        return merged;
    }

    /**
     * 从蓝图文件创建图鉴条目（不加载完整 NBT，只提取元数据）。
     *
     * @param pack 结构包元数据
     * @param file 蓝图文件路径
     * @return 图鉴条目，或 null 如果无法解析
     */
    private BuildingCatalogEntry createEntry(StructurePackMeta pack, Path file) {
        // 计算相对路径
        Path relativePath = pack.getPath().relativize(file);
        String relative = relativePath.toString().replace('\\', '/');

        // 解析目录结构：{category}/{subcategory?}/{buildingName}.blueprint
        String[] parts = relative.split("/");
        if (parts.length < 2) {
            return null; // 至少需要 category/building.blueprint
        }

        // 第一个目录是殖民地分类（agriculture, fundamentals, ...）
        String mcCategory = parts[0];

        // 最后一个部分是文件名（buildingName.blueprint）
        String fileName = parts[parts.length - 1];
        String buildingName = fileName.replace(".blueprint", "");

        // 构建资源路径（不含 .blueprint 后缀）
        String resourcePath = relative.replace(".blueprint", "");

        // 检查是否有图标
        boolean hasIcon = checkHasIcon(pack, mcCategory);

        // 加载蓝图获取尺寸（使用 StructurePacks 的管线）
        BlockPos size = loadBlueprintSize(pack, relative);
        if (size == null) {
            size = BlockPos.ZERO;
        }

        // 解析等级（从文件名末尾数字提取）
        int level = extractLevel(buildingName);
        String baseName = level > 0 ? buildingName.substring(0, buildingName.length() - 1) : buildingName;

        // 映射到 MetroGenesis 分类
        String category = CategoryMapper.toMetroGenesisCategory(mcCategory);

        return new BuildingCatalogEntry(
            baseName,
            pack.getName(),
            resourcePath,
            category,
            mcCategory,
            size,
            new TreeSet<>(Set.of(level > 0 ? level : 1)),
            hasIcon
        );
    }

    /**
     * 加载蓝图获取尺寸（轻量级，只读 NBT 头部）。
     * 使用 StructurePacks 的加载管线。
     */
    private BlockPos loadBlueprintSize(StructurePackMeta pack, String relativePath) {
        try {
            Blueprint bp = StructurePacks.getBlueprint(pack.getName(), relativePath, true);
            if (bp != null) {
                return new BlockPos(bp.getSizeX(), bp.getSizeY(), bp.getSizeZ());
            }
        } catch (Exception e) {
            // 静默失败
        }
        return null;
    }

    /**
     * 检查指定分类目录是否有图标。
     */
    private boolean checkHasIcon(StructurePackMeta pack, String mcCategory) {
        try {
            Path iconPath = pack.getPath().resolve(mcCategory).resolve("icon.png");
            return Files.exists(iconPath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从文件名末尾提取等级数字。
     * 如 "farmer3" → 3, "townhall" → 0
     */
    private int extractLevel(String name) {
        if (name.isEmpty()) return 0;
        char last = name.charAt(name.length() - 1);
        if (Character.isDigit(last)) {
            return Character.getNumericValue(last);
        }
        return 0;
    }

    /**
     * 合并多级建筑（同一 baseName 不同 level 合并为一个条目）。
     * 从 WindowBuildingBrowser.BuildingInfo.flattenLevels() 提取。
     */
    private List<BuildingCatalogEntry> mergeLevels(List<BuildingCatalogEntry> entries) {
        return entries.stream()
            .collect(Collectors.groupingBy(e -> e.packName() + ":" + e.resourcePath().replaceAll("\\d+$", "")))
            .values().stream()
            .map(group -> {
                BuildingCatalogEntry first = group.get(0);
                Set<Integer> allLevels = group.stream()
                    .flatMap(e -> e.levels().stream())
                    .collect(Collectors.toCollection(TreeSet::new));
                BlockPos maxSize = group.stream()
                    .map(BuildingCatalogEntry::size)
                    .max(BlockPos::compareTo)
                    .orElse(first.size());
                return new BuildingCatalogEntry(
                    first.name(),
                    first.packName(),
                    first.resourcePath(),
                    first.category(),
                    first.mcCategory(),
                    maxSize,
                    allLevels,
                    first.hasIcon()
                );
            })
            .toList();
    }

    /**
     * 获取扫描进度（0.0 ~ 1.0）。
     */
    public float getProgress() {
        if (totalPacks <= 0) return 0;
        return (float) progress.get() / totalPacks;
    }

    /**
     * 清除缓存（下次扫描会重新加载）。
     */
    public static void clearCache() {
        packCache.clear();
    }
}
