package com.metrogenesis.client;

import com.metrogenesis.entity.RtsCameraEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * RTS 相机实体渲染器 — 完全不渲染任何可见内容
 * 相机实体仅用于确定渲染视角，不产生任何视觉输出
 */
public class RtsCameraEntityRenderer extends EntityRenderer<RtsCameraEntity> {

    public RtsCameraEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RtsCameraEntity entity) {
        // 返回空纹理占位
        return ResourceLocation.withDefaultNamespace("textures/misc/empty.png");
    }

    @Override
    public boolean shouldRender(RtsCameraEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                 double camX, double camY, double camZ) {
        return false; // 永不渲染
    }
}
