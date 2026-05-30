package com.metrogenesis.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

/**
 * 鎵弿宸ュ叿閫夊尯鏂规娓叉煋鍣?鈥?WorldEdit 椋庢牸
 * <p>
 * 褰撶帺瀹舵墜鎸佹壂鎻忓伐鍏蜂笖宸茶缃袱涓瑙掓椂锛? * 鍦ㄩ€夊尯鍐呮覆鏌撳崐閫忔槑鏂规 + 杈规绾裤€? */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScanToolSelectionRenderer {

    private static final String TAG_FIRST = "firstPos";
    private static final String TAG_SECOND = "secondPos";

    // 鎵弿宸ュ叿 item 鐨勫叏绫诲悕锛堢敤浜庡垽鏂級
    private static final String SCAN_TOOL_CLASS = "com.metrogenesis.item.ScanToolItem";

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_FIRST) || !tag.contains(TAG_SECOND)) {
            // 涔熸鏌ュ壇鎵?            stack = player.getOffhandItem();
            tag = stack.getTag();
            if (tag == null || !tag.contains(TAG_FIRST) || !tag.contains(TAG_SECOND)) return;
        }

        BlockPos first = NbtUtils.readBlockPos(tag.getCompound(TAG_FIRST));
        BlockPos second = NbtUtils.readBlockPos(tag.getCompound(TAG_SECOND));

        // 璁＄畻鐩告満鍋忕Щ
        Vec3 camera = event.getCamera().getPosition();

        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX()) + 1;
        int maxY = Math.max(first.getY(), second.getY()) + 1;
        int maxZ = Math.max(first.getZ(), second.getZ()) + 1;

        float x1 = (float) (minX - camera.x);
        float y1 = (float) (minY - camera.y);
        float z1 = (float) (minZ - camera.z);
        float x2 = (float) (maxX - camera.x);
        float y2 = (float) (maxY - camera.y);
        float z2 = (float) (maxZ - camera.z);

        // 鈹€鈹€ 娓叉煋杈规绾?鈹€鈹€
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0f);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        // 12 鏉¤竟锛岄鑹蹭负浜粍鑹?
        int r = 255, g = 200, b = 0, a = 200;

        // 搴曢儴 4 鏉?        addLine(buffer, x1, y1, z1, x2, y1, z1, r, g, b, a);
        addLine(buffer, x2, y1, z1, x2, y1, z2, r, g, b, a);
        addLine(buffer, x2, y1, z2, x1, y1, z2, r, g, b, a);
        addLine(buffer, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // 椤堕儴 4 鏉?        addLine(buffer, x1, y2, z1, x2, y2, z1, r, g, b, a);
        addLine(buffer, x2, y2, z1, x2, y2, z2, r, g, b, a);
        addLine(buffer, x2, y2, z2, x1, y2, z2, r, g, b, a);
        addLine(buffer, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // 4 鏉＄珫绾?        addLine(buffer, x1, y1, z1, x1, y2, z1, r, g, b, a);
        addLine(buffer, x2, y1, z1, x2, y2, z1, r, g, b, a);
        addLine(buffer, x2, y1, z2, x2, y2, z2, r, g, b, a);
        addLine(buffer, x1, y1, z2, x1, y2, z2, r, g, b, a);

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void addLine(BufferBuilder buffer, float x1, float y1, float z1,
                                 float x2, float y2, float z2, int r, int g, int b, int a) {
        buffer.vertex(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(x2, y2, z2).color(r, g, b, a).endVertex();
    }
}
