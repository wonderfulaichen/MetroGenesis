package com.metrogenesis.gui;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.catalog.BuildingCatalogEntry;
import com.metrogenesis.catalog.BuildingCatalogScanner;
import com.metrogenesis.catalog.CatalogSavedData;
import com.metrogenesis.catalog.CategoryMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 图鉴数据管理器 — 负责扫描 / 缓存 / 持久化 / 查询。
 * <p>
 * 从 MayorBookCatalogPanel 剥离的纯数据层，不依赖 GUI 组件。
 * 提供：
 * <ul>
 *   <li>异步扫描（首次或手动刷新）</li>
 *   <li>本地文件缓存（跨游戏会话持久化）</li>
 *   <li>按分类/风格包/子目录过滤查询</li>
 * </ul>
 */
public final class CatalogDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesisCatalogData");

    // ════════════════════════════════════════════════════════
    //  持久化常量
    // ════════════════════════════════════════════════════════

    private static final String CACHE_FILE_NAME = "catalog_cache.nbt";

    // ════════════════════════════════════════════════════════
    //  静态状态（会话级共享，跨面板实例）
    // ════════════════════════════════════════════════════════

    /** 已扫描完成的条目列表 */
    private static List<BuildingCatalogEntry> allEntries = List.of();

    /** 是否已发起过扫描 */
    private static boolean scanStarted = false;

    // ════════════════════════════════════════════════════════
    //  实例状态
    // ════════════════════════════════════════════════════════

    private final BuildingCatalogScanner scanner = new BuildingCatalogScanner();
    private Future<List<BuildingCatalogEntry>> scanFuture = null;

    // ════════════════════════════════════════════════════════
    //  公共查询
    // ════════════════════════════════════════════════════════

    /** 所有条目（只读） */
    public List<BuildingCatalogEntry> getAllEntries() {
        return allEntries;
    }

    /** 扫描是否正在进行 */
    public boolean isScanning() {
        return scanFuture != null && !scanFuture.isDone();
    }

    /** 是否已完成扫描（无论成功与否） */
    public boolean isScanDone() {
        return scanFuture == null && scanStarted;
    }

    /** 扫描进度 0.0 ~ 1.0 */
    public float getProgress() {
        return scanner.getProgress();
    }

    // ════════════════════════════════════════════════════════
    //  查询（过滤）
    // ════════════════════════════════════════════════════════

    /**
     * 根据分类 + 风格包 + 子目录过滤条目。
     *
     * @param category     MetroGenesis 分类（如 "农业"），或 "全部"
     * @param packName     选中的风格包名，null 表示不过滤
     * @param subPath      MineColonies 原始目录名，null 表示不过滤子目录
     */
    public List<BuildingCatalogEntry> getFilteredEntries(
            final String category,
            @Nullable final String packName,
            @Nullable final String subPath)
    {
        if (allEntries.isEmpty()) return List.of();

        var stream = allEntries.stream();

        // 按风格包过滤
        if (packName != null) {
            if (subPath != null) {
                // 按 pack + subPath 过滤
                stream = stream.filter(e -> e.packName().equals(packName) && e.mcCategory().equals(subPath));
            } else {
                // 仅按 pack 过滤
                stream = stream.filter(e -> e.packName().equals(packName));
            }
        }

        // 按分类过滤
        if (category != null && !CategoryMapper.isAllCategory(category)) {
            stream = stream.filter(e -> e.category().equals(category));
        }

        return stream.collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════
    //  扫描生命周期
    // ════════════════════════════════════════════════════════

    /**
     * 确保数据已加载（从缓存或扫描）。
     * 应在首次打开图鉴时调用。
     */
    public void ensureLoaded() {
        if (!scanStarted && allEntries.isEmpty()) {
            if (!tryLoadFromCache()) {
                startScan();
            }
        }
    }

    /**
     * 手动刷新 — 重新扫描所有蓝图。
     */
    public void refresh() {
        BuildingCatalogScanner.clearCache();
        scanStarted = true;
        allEntries = List.of();
        clearCacheFile();
        scanFuture = scanner.scanAllAsync();
    }

    /**
     * 每帧调用，检查扫描是否完成。完成时自动持久化。
     */
    public void tick() {
        if (scanFuture != null && scanFuture.isDone()) {
            try {
                final List<BuildingCatalogEntry> result = scanFuture.get();
                allEntries = result;
                LOGGER.info("[CatalogData] Scanned {} building entries", result.size());
                saveToCache();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("[CatalogData] Scan failed: {}", e.getMessage());
                allEntries = List.of();
            }
            scanFuture = null;
        }
    }

    /**
     * 启动异步扫描（由 ensureLoaded 或 refresh 调用）。
     */
    private void startScan() {
        scanStarted = true;
        scanFuture = scanner.scanAllAsync();
    }

    // ════════════════════════════════════════════════════════
    //  本地文件缓存
    // ════════════════════════════════════════════════════════

    @Nullable
    private static File getCacheFile() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.gameDirectory == null) return null;
        final File dir = new File(mc.gameDirectory, "config/metrogenesis");
        if (!dir.exists() && !dir.mkdirs()) return null;
        return new File(dir, CACHE_FILE_NAME);
    }

    private static boolean tryLoadFromCache() {
        final File file = getCacheFile();
        if (file == null || !file.exists()) return false;
        try {
            final CompoundTag tag = NbtIo.readCompressed(file);
            final CatalogSavedData data = new CatalogSavedData(tag);
            if (data.isValid(CategoryMapper.getAllCategories().length)) {
                allEntries = data.getEntries();
                scanStarted = true;
                LOGGER.info("[CatalogData] Loaded {} entries from local cache", allEntries.size());
                return true;
            }
        } catch (Exception e) {
            LOGGER.warn("[CatalogData] Failed to load cache file: {}", e.getMessage());
        }
        return false;
    }

    private static void saveToCache() {
        if (allEntries.isEmpty()) return;
        final File file = getCacheFile();
        if (file == null) return;
        try {
            final CatalogSavedData data = new CatalogSavedData();
            data.setEntries(allEntries, CategoryMapper.getAllCategories().length);
            final CompoundTag tag = data.save(new CompoundTag());
            NbtIo.writeCompressed(tag, file);
            LOGGER.info("[CatalogData] Saved {} entries to local cache", allEntries.size());
        } catch (Exception e) {
            LOGGER.warn("[CatalogData] Failed to save cache file: {}", e.getMessage());
        }
    }

    private static void clearCacheFile() {
        final File file = getCacheFile();
        if (file != null && file.exists()) file.delete();
    }
}
