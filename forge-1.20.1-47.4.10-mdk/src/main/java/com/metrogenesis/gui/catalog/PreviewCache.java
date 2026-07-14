package com.metrogenesis.gui.catalog;

import com.metrogenesis.MetroGenesis;
import com.metrogenesis.structurize.blueprints.v1.Blueprint;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图鉴缩略图文件缓存 — 双模式（内置预设 + 外部缓存）。
 * <p>
 * 三级查找顺序：
 * <ol>
 *   <li><b>内置预设</b>：从 mod assets 加载 PNG（只读，不写入）
 *       → 路径: {@code assets/metrogenesis/{packName}/preview/{resourcePath}.png}</li>
 *   <li><b>外部缓存</b>：从游戏外部目录加载 PNG
 *       → 路径: {@code {gameDir}/metrogenesis/cache/blueprint_preview/{packName}/{resourcePath}.png}</li>
 *   <li><b>渲染并保存</b>：无缓存时渲染体素预览，保存到外部缓存目录</li>
 * </ol>
 * <p>
 * 命名规范：{@code resourcePath} 中的 "/" 转为 "_"，全部小写。
 * 例如 {@code medievaloak/buildings/big_well} → {@code medievaloak_buildings_big_well.png}
 */
public final class PreviewCache
{
    /** 外部缓存根目录（相对于游戏目录） */
    private static final String EXTERNAL_CACHE_ROOT = "metrogenesis/cache/blueprint_preview";

    /** 内置预设资源前缀 */
    private static final String BUILTIN_RESOURCE_PREFIX = "preview";

    /** 预览缩略图目标尺寸（宽/高像素） */
    private static final int PREVIEW_SIZE = 80;

    /** 内存中的纹理缓存 (key = packName + ":" + resourcePath) */
    private static final Map<String, DynamicTexture> TEXTURE_CACHE = new HashMap<>();

    /** 防止加载中的递归循环 */
    private static final Map<String, Boolean> LOADING_IN_PROGRESS = new HashMap<>();

    /** 防止同一文件的并发写入（Fix 4：并发写保护） */
    private static final Set<String> SAVING_IN_PROGRESS = ConcurrentHashMap.newKeySet();

    private PreviewCache() {}

    // ========================================================================
    //  Cache key & path helpers
    // ========================================================================

    /**
     * 将风格包名转为安全的资源位置路径。
     * 风格包显示名（如 "Urban Savanna"）可能包含空格和大写，
     * 不能直接用于 ResourceLocation，需要转为小写、空格→下划线。
     */
    private static String sanitizePackName(final String packName)
    {
        return packName.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    /**
     * 生成缓存键。
     */
    private static String cacheKey(final String packName, final String resourcePath)
    {
        return sanitizePackName(packName) + ":" + resourcePath;
    }

    /**
     * 将 resourcePath 转为缓存文件名。
     * "/" → "_"，全部小写。
     * 例如 {@code medievaloak/buildings/big_well} → {@code medievaloak_buildings_big_well.png}
     */
    private static String toCacheFileName(final String resourcePath)
    {
        return resourcePath.replace('/', '_').toLowerCase(java.util.Locale.ROOT) + ".png";
    }

    // ========================================================================
    //  三级查找：内置预设 → 外部缓存 → 渲染保存
    // ========================================================================

    /**
     * 加载缓存的缩略图（三级查找）。
     * <p>
     * 查找顺序：
     * <ol>
     *   <li>内置预设（mod assets）</li>
     *   <li>外部缓存（游戏目录）</li>
     *   <li>找不到 → 返回 null，由调用方渲染并保存</li>
     * </ol>
     *
     * @param packName     风格包名称
     * @param resourcePath 蓝图相对路径
     * @return 可绘制的纹理对象，或 null（所有缓存都不存在）
     */
    @Nullable
    public static DynamicTexture loadCachedPreview(final String packName, final String resourcePath)
    {
        final String key = cacheKey(packName, resourcePath);

        // 检查内存缓存
        final DynamicTexture cached = TEXTURE_CACHE.get(key);
        if (cached != null) return cached;

        // 防止递归加载
        if (LOADING_IN_PROGRESS.putIfAbsent(key, true) != null) return null;

        try
        {
            // 1st tier: 内置预设
            DynamicTexture texture = loadBuiltinPreview(packName, resourcePath);
            if (texture != null)
            {
                TEXTURE_CACHE.put(key, texture);
                return texture;
            }

            // 2nd tier: 外部缓存
            texture = loadExternalPreview(packName, resourcePath);
            if (texture != null)
            {
                TEXTURE_CACHE.put(key, texture);
                return texture;
            }

            // 3rd tier: 无缓存，返回 null
            return null;
        }
        finally
        {
            LOADING_IN_PROGRESS.remove(key);
        }
    }

    // ========================================================================
    //  第 1 层：内置预设（从 mod assets 加载，只读）
    // ========================================================================

    /**
     * 从 mod assets 加载内置预设缩略图。
     * <p>
     * 路径：{@code assets/metrogenesis/{sanitizedPackName}/preview/{resourcePath}.png}
     * <p>
     * 注意：packName 会被净化（小写、空格→下划线），因为 ResourceLocation 不支持大写和空格。
     *
     * @param packName     风格包名称
     * @param resourcePath 蓝图相对路径
     * @return 纹理对象，或 null（资源不存在/读取失败）
     */
    @Nullable
    static DynamicTexture loadBuiltinPreview(final String packName, final String resourcePath)
    {
        if (!isClient()) return null;

        final String safePackName = sanitizePackName(packName);
        final String fileName = toCacheFileName(resourcePath);
        final String assetPath = safePackName + "/" + BUILTIN_RESOURCE_PREFIX + "/" + fileName;
        final ResourceLocation resLoc;
        try
        {
            resLoc = new ResourceLocation(MetroGenesis.MODID, assetPath);
        }
        catch (final Exception e)
        {
            MetroGenesis.LOGGER.debug("[PreviewCache] Invalid resource path for pack '{}': {} ({})",
                packName, assetPath, e.getMessage());
            return null;
        }

        try
        {
            final Optional<Resource> resourceOpt = Minecraft.getInstance()
                .getResourceManager().getResource(resLoc);

            if (resourceOpt.isEmpty()) return null;

            try (final InputStream is = resourceOpt.get().open())
            {
                final NativeImage image = NativeImage.read(is);
                if (image == null) return null;

                final DynamicTexture texture = new DynamicTexture(image);
                texture.upload();
                return texture;
            }
        }
        catch (final IOException e)
        {
            MetroGenesis.LOGGER.debug("[PreviewCache] Builtin preview not found: {} ({})", resLoc, e.getMessage());
            return null;
        }
    }

    // ========================================================================
    //  第 2 层：外部缓存（从游戏目录加载）
    // ========================================================================

    /**
     * 加载外部缓存的缩略图。
     * <p>
     * 路径：{@code {gameDir}/metrogenesis/cache/blueprint_preview/{packName}/{resourcePath}.png}
     *
     * @param packName     风格包名称
     * @param resourcePath 蓝图相对路径
     * @return 纹理对象，或 null（文件不存在/读取失败）
     */
    @Nullable
    static DynamicTexture loadExternalPreview(final String packName, final String resourcePath)
    {
        final Path cachePath = getExternalCachePath(packName, resourcePath);
        if (cachePath == null || !Files.exists(cachePath)) return null;

        try (final InputStream is = Files.newInputStream(cachePath))
        {
            final NativeImage image = NativeImage.read(is);
            if (image == null) return null;

            final DynamicTexture texture = new DynamicTexture(image);
            texture.upload();
            return texture;
        }
        catch (final IOException e)
        {
            MetroGenesis.LOGGER.debug("[PreviewCache] External cache not found: {} ({})", cachePath, e.getMessage());
            return null;
        }
    }

    // ========================================================================
    //  第 3 层：渲染并保存到外部缓存
    // ========================================================================

    /**
     * 将蓝图渲染为等轴测缩略图并保存到外部缓存目录。
     * <p>
     * 保存路径：{@code {gameDir}/metrogenesis/cache/blueprint_preview/{packName}/{resourcePath}.png}
     * <p>
     * 文件夹不可写时静默跳过。
     *
     * @param packName     风格包名称
     * @param resourcePath 蓝图相对路径
     * @param bp           蓝图对象
     */
    public static void saveExternalPreview(final String packName, final String resourcePath, final Blueprint bp)
    {
        final Path cachePath = getExternalCachePath(packName, resourcePath);
        if (cachePath == null) return;

        final String key = cacheKey(packName, resourcePath);

        // Fix 4: 并发写保护 — 同一文件已在保存中则跳过
        if (!SAVING_IN_PROGRESS.add(key)) return;

        try
        {
            // 确保父目录存在
            Files.createDirectories(cachePath.getParent());

            // 渲染到 NativeImage
            final NativeImage image = renderPreviewToImage(bp);
            if (image == null) return;

            // Fix 4: 先写 .tmp 文件，再原子重命名
            final Path tmpPath = cachePath.resolveSibling(cachePath.getFileName() + ".tmp");
            try
            {
                image.writeToFile(tmpPath.toFile());

                // 原子重命名（如果跨分区不支持 ATOMIC_MOVE，回退到 REPLACE_EXISTING）
                try
                {
                    Files.move(tmpPath, cachePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (final UnsupportedOperationException e)
                {
                    // Fallback: 跨分区时无法原子移动，使用替换模式
                    Files.move(tmpPath, cachePath, StandardCopyOption.REPLACE_EXISTING);
                }

                MetroGenesis.LOGGER.debug("[PreviewCache] Saved external preview: {}", cachePath);
            }
            finally
            {
                image.close();
                // 清理残留的 .tmp 文件
                try
                {
                    Files.deleteIfExists(tmpPath);
                }
                catch (final IOException ignored) {}
            }
        }
        catch (final IOException e)
        {
            MetroGenesis.LOGGER.debug("[PreviewCache] Failed to save preview: {} ({})", cachePath, e.getMessage());
        }
        catch (final Exception e)
        {
            MetroGenesis.LOGGER.debug("[PreviewCache] Error rendering preview: {}", e.getMessage());
        }
        finally
        {
            SAVING_IN_PROGRESS.remove(key);
        }
    }

    // ========================================================================
    //  路径计算
    // ========================================================================

    /**
     * 获取外部缓存文件路径。
     * <p>
     * 路径：{@code {gameDir}/metrogenesis/cache/blueprint_preview/{sanitizedPackName}/{resourcePath}.png}
     *
     * @param packName     风格包名称
     * @param resourcePath 蓝图相对路径
     * @return 外部缓存文件的绝对路径，或 null（游戏目录不可用）
     */
    @Nullable
    static Path getExternalCachePath(final String packName, final String resourcePath)
    {
        try
        {
            final Path gameDir = FMLPaths.GAMEDIR.get();
            final String fileName = toCacheFileName(resourcePath);
            final String safePackName = sanitizePackName(packName);
            return gameDir.resolve(EXTERNAL_CACHE_ROOT)
                .resolve(safePackName)
                .resolve(fileName)
                .normalize();
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    // ========================================================================
    //  缓存失效
    // ========================================================================

    /**
     * 使指定蓝图的缓存失效（删除外部缓存文件和内存纹理）。
     * <p>
     * 内置预设（mod assets）是只读的，不会被删除。
     *
     * @param packName     风格包名称
     * @param resourcePath 蓝图相对路径
     */
    public static void invalidateCache(final String packName, final String resourcePath)
    {
        final String key = cacheKey(packName, resourcePath);

        // 清除内存纹理
        final DynamicTexture tex = TEXTURE_CACHE.remove(key);
        if (tex != null)
        {
            tex.close();
        }

        // 删除外部缓存文件（内置预设只读，不删除）
        final Path cachePath = getExternalCachePath(packName, resourcePath);
        if (cachePath != null && Files.exists(cachePath))
        {
            try
            {
                Files.delete(cachePath);
                MetroGenesis.LOGGER.debug("[PreviewCache] Invalidated external cache: {}", cachePath);
            }
            catch (final IOException e)
            {
                MetroGenesis.LOGGER.debug("[PreviewCache] Failed to delete cache: {}", cachePath);
            }
        }
    }

    /**
     * 清除全部内存纹理缓存（不影响磁盘文件）。
     */
    public static void clearMemoryCache()
    {
        for (final DynamicTexture tex : TEXTURE_CACHE.values())
        {
            tex.close();
        }
        TEXTURE_CACHE.clear();
    }

    // ========================================================================
    //  体素渲染 → NativeImage（与第 3 层共用）
    // ========================================================================

    /**
     * 将蓝图渲染为等轴测缩略图 NativeImage。
     * <p>
     * 使用等轴测投影算法，输出到 {@code NativeImage(80, 80)}。
     * 颜色使用 {@link BlockState#getMapColor} 获取真实方块材质色。
     */
    @Nullable
    private static NativeImage renderPreviewToImage(final Blueprint bp)
    {
        final short bpSizeX = bp.getSizeX();
        final short bpSizeY = bp.getSizeY();
        final short bpSizeZ = bp.getSizeZ();
        final short[][][] structure = bp.getStructure();
        final BlockState[] palette = bp.getPalette();

        if (bpSizeX == 0 && bpSizeY == 0 && bpSizeZ == 0) return null;

        final int size = PREVIEW_SIZE;
        final NativeImage image = new NativeImage(size, size, true);

        // 等轴测投影参数
        final float isoScaleX = Math.max(1.5f, Math.min(
            (float) size / (bpSizeX + bpSizeZ + 2),
            (float) size / (bpSizeY + 2)
        ));
        final float isoScaleY = isoScaleX * 0.5f;
        final float isoScaleH = isoScaleX * 1.0f;
        final int blockPixelSize = Math.max(2, (int) (isoScaleX * 0.9f));

        // 计算包围盒并居中
        float minIsoX = Float.MAX_VALUE, maxIsoX = -Float.MAX_VALUE;
        float minIsoY = Float.MAX_VALUE, maxIsoY = -Float.MAX_VALUE;

        for (short by = 0; by < bpSizeY; by++)
        {
            for (short bz = 0; bz < bpSizeZ; bz++)
            {
                for (short bx = 0; bx < bpSizeX; bx++)
                {
                    if (structure[by][bz][bx] == 0) continue;
                    final float ix = (bx - bz) * isoScaleX;
                    final float iy = (bx + bz) * isoScaleY - by * isoScaleH;
                    minIsoX = Math.min(minIsoX, ix);
                    maxIsoX = Math.max(maxIsoX, ix);
                    minIsoY = Math.min(minIsoY, iy);
                    maxIsoY = Math.max(maxIsoY, iy);
                }
            }
        }

        if (minIsoX == Float.MAX_VALUE) return null;

        final float centerX = size / 2f;
        final float centerY = size / 2f;
        final float centerIsoX = (minIsoX + maxIsoX) / 2;
        final float centerIsoY = (minIsoY + maxIsoY) / 2;
        final float offsetX = centerX - centerIsoX;
        final float offsetY = centerY - centerIsoY;

        // 从后到前绘制（等轴测遮挡顺序）
        for (short by = (short) (bpSizeY - 1); by >= 0; by--)
        {
            for (short bz = (short) (bpSizeZ - 1); bz >= 0; bz--)
            {
                for (short bx = 0; bx < bpSizeX; bx++)
                {
                    final short val = structure[by][bz][bx];
                    if (val == 0 || val >= palette.length) continue;

                    final BlockState state = palette[val];
                    if (state.isAir()) continue;

                    final float isoX = (bx - bz) * isoScaleX + offsetX;
                    final float isoY = (bx + bz) * isoScaleY - by * isoScaleH + offsetY;
                    final int dx = (int) isoX;
                    final int dy = (int) isoY;

                    // 获取方块颜色
                    int baseColor = 0xFF888888;
                    try
                    {
                        final MapColor mapColor = state.getMapColor(
                            Minecraft.getInstance().level, BlockPos.ZERO);
                        if (mapColor != null)
                        {
                            baseColor = 0xFF000000 | (mapColor.col & 0xFFFFFF);
                        }
                    }
                    catch (Exception e) { /* fallback */ }

                    // 提亮 40%
                    baseColor = brighten(baseColor, 0.4f);

                    if (blockPixelSize < 3)
                    {
                        drawPixel(image, dx, dy, blockPixelSize, blockPixelSize, baseColor);
                        continue;
                    }

                    final int tH = Math.max(1, blockPixelSize / 3);
                    // 3面方块：右侧暗面(35%)、左侧中面(65%)、顶面(100%)
                    drawPixel(image, dx + blockPixelSize / 2, dy + tH,
                        blockPixelSize - blockPixelSize / 2, blockPixelSize - tH,
                        shade(baseColor, 0.35f));
                    drawPixel(image, dx, dy + tH,
                        blockPixelSize / 2, blockPixelSize - tH,
                        shade(baseColor, 0.65f));
                    drawPixel(image, dx, dy,
                        blockPixelSize, tH,
                        baseColor);
                }
            }
        }

        return image;
    }

    /**
     * 在 NativeImage 上绘制矩形像素块。
     * 使用 ABGR 格式（NativeImage 使用 ABGR，与 ARGB 字节序不同）。
     */
    private static void drawPixel(final NativeImage image, final int x, final int y,
                                   final int w, final int h, final int argbColor)
    {
        final int a = (argbColor >> 24) & 0xFF;
        final int r = (argbColor >> 16) & 0xFF;
        final int g = (argbColor >> 8) & 0xFF;
        final int b = argbColor & 0xFF;
        // ABGR format for NativeImage.setPixelRGBA
        final int abgr = (a << 24) | (b << 16) | (g << 8) | r;

        for (int py = 0; py < h; py++)
        {
            for (int px = 0; px < w; px++)
            {
                final int ix = x + px;
                final int iy = y + py;
                if (ix >= 0 && ix < image.getWidth() && iy >= 0 && iy < image.getHeight())
                {
                    image.setPixelRGBA(ix, iy, abgr);
                }
            }
        }
    }

    // ========================================================================
    //  颜色工具
    // ========================================================================

    private static int shade(final int argb, final float factor)
    {
        final int r = (int) (((argb >> 16) & 0xFF) * factor);
        final int g = (int) (((argb >> 8) & 0xFF) * factor);
        final int b = (int) ((argb & 0xFF) * factor);
        return 0xFF000000 | (Math.min(255, r) << 16)
                          | (Math.min(255, g) << 8)
                          | Math.min(255, b);
    }

    private static int brighten(final int argb, final float amount)
    {
        final int r = (int) (((argb >> 16) & 0xFF) + 255 * amount);
        final int g = (int) (((argb >> 8) & 0xFF) + 255 * amount);
        final int b = (int) ((argb & 0xFF) + 255 * amount);
        return 0xFF000000 | (Math.min(255, r) << 16)
                          | (Math.min(255, g) << 8)
                          | Math.min(255, b);
    }

    // ========================================================================
    //  环境检测
    // ========================================================================

    private static boolean isClient()
    {
        return Minecraft.getInstance() != null;
    }
}
