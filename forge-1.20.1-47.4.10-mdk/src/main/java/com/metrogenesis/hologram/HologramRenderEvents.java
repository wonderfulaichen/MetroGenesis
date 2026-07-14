package com.metrogenesis.hologram;

import com.metrogenesis.block.construction.ConstructionMarkerBlockEntity;
import com.metrogenesis.blueprint.v1.Blueprint;
import com.metrogenesis.client.BlueprintRenderer;
import com.metrogenesis.construction.Zone;
import com.metrogenesis.init.BuildingType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鍏ㄦ伅钃濆浘娓叉煋 鈥?瀹㈡埛绔洿鎺ユ覆鏌?ConstructionMarkerBlockEntity 鐨?Blueprint
 * <p>
 * 浣跨敤鍖哄潡杩唬鍣ㄦ壂鎻?BE锛岄伩鍏?O(n鲁) 寰幆銆? */
@Mod.EventBusSubscriber(modid = "MetroGenesis", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HologramRenderEvents
{
    private static final float ALPHA = 0.6f;
    private static final int CHUNK_RADIUS = 4; // 娓叉煋璺濈锛堝尯鍧楀崟浣嶏級
    private static final Map<BlockPos, BlueprintRenderer> rendererCache = new ConcurrentHashMap<>();
    private static Level lastLevel = null;

    @SubscribeEvent
    public static void onRenderLevel(final RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Level level = mc.level;

        // 涓栫晫鍒囨崲鏃舵竻鐞嗙紦瀛?
        if (lastLevel != level)
        {
            rendererCache.values().forEach(BlueprintRenderer::close);
            rendererCache.clear();
            lastLevel = level;
        }

        // 閬嶅巻鐜╁鍛ㄥ洿宸插姞杞界殑鍖哄潡
        int cx = mc.player.chunkPosition().x;
        int cz = mc.player.chunkPosition().z;

        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++)
        {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++)
            {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                if (chunk == null) continue;

                // 杩唬璇ュ尯鍧楃殑鎵€鏈?BlockEntity
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet())
                {
                    BlockEntity be = entry.getValue();
                    if (be instanceof ConstructionMarkerBlockEntity marker && !marker.isCompleted())
                    {
                        renderMarker(marker, event);
                    }
                }
            }
        }

        // 娓呯悊宸插畬鎴愮殑锛圔E 宸茶绉婚櫎鎴栨爣璁板畬鎴愶級
        rendererCache.entrySet().removeIf(entry -> {
            BlockEntity be = level.getBlockEntity(entry.getKey());
            if (!(be instanceof ConstructionMarkerBlockEntity marker) || marker.isCompleted())
            {
                entry.getValue().close();
                return true;
            }
            return false;
        });
    }

    private static void renderMarker(ConstructionMarkerBlockEntity marker, RenderLevelStageEvent event)
    {
        BlockPos pos = marker.getBlockPos();
        Zone zone = marker.getZone();
        String typeId = marker.getBuildingTypeId();

        if (zone == null || typeId == null || typeId.isEmpty()) return;

        BlueprintRenderer renderer = rendererCache.get(pos);

        if (renderer == null)
        {
            // 浼樺厛浣跨敤 BE 宸叉湁钃濆浘锛圔uildingToolItem 棰勫垵濮嬪寲鏃跺凡鐢熸垚锛?
        Blueprint bp = marker.getBlueprint();
            if (bp == null) {
                BuildingType type = BuildingType.fromId(typeId);
                if (type == null) return;
                bp = HologramRenderer.createBlueprint(zone, type);
            }
            if (bp == null) return;

            renderer = new BlueprintRenderer(bp);
            rendererCache.put(pos, renderer);
        }

        renderer.draw(pos, event, ALPHA);
    }
}
