package com.metrogenesis.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20C;
import com.metrogenesis.blueprint.v1.Blueprint;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 鍏ㄦ伅钃濆浘娓叉煋鍣?鈥?鍩轰簬 Structurize BlueprintRenderer
 * <p>
 * 浣跨敤 VertexBuffer + ChunkBufferBuilderPack 鏋勫缓骞剁紦瀛樺潡妯″瀷椤剁偣鏁版嵁锛? * 姣忓抚閫氳繃 TransparencyHack锛圕ONSTANT_ALPHA 娣峰悎妯″紡锛夊疄鐜板崐閫忔槑娓叉煋銆? */
public class BlueprintRenderer implements AutoCloseable
{
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Supplier<Map<RenderType, VertexBuffer>> VERTEX_BUFFER_FACTORY = () ->
        RenderType.chunkBufferLayers().stream()
            .collect(Collectors.toMap(t -> t, t -> new VertexBuffer(VertexBuffer.Usage.STATIC)));

    private final Blueprint blueprint;
    private Map<RenderType, VertexBuffer> vertexBuffers;
    private long lastGameTime = -1;
    private boolean initialized = false;

    public BlueprintRenderer(final Blueprint blueprint)
    {
        this.blueprint = blueprint;
    }

    /**
     * 鍒濆鍖栭《鐐圭紦鍐诧細閬嶅巻钃濆浘鎵€鏈夋柟鍧楋紝娓叉煋鍒板悇 RenderType 鐨?BufferBuilder锛?     * 鐒跺悗涓婁紶鍒?VertexBuffer銆?     */
    private void init()
    {
        final Minecraft mc = Minecraft.getInstance();
        final BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        final RandomSource random = RandomSource.create();

        clearVertexBuffers();

        // 鍒涘缓鍋囦笘鐣岀幆澧冿紝鎻愪緵钃濆浘鏂瑰潡鐘舵€?+ 鍏ㄤ寒鍏夌収锛岄伩鍏嶆煡璇㈢湡瀹炰笘鐣?
        final FakeBlockAndTintGetter fakeLevel = new FakeBlockAndTintGetter(blueprint);

        // 鍒涘缓 ChunkBufferBuilderPack锛堣鐩?builder() 鑷姩 begin锛?
        final ChunkBufferBuilderPack bufferPack = new ChunkBufferBuilderPack()
        {
            @Override
            public BufferBuilder builder(RenderType renderType)
            {
                BufferBuilder buffer = super.builder(renderType);
                if (!buffer.building())
                {
                    buffer.begin(renderType.mode(), renderType.format());
                }
                return buffer;
            }
        };
        final RenderType[] renderTypes = RenderType.chunkBufferLayers().toArray(RenderType[]::new);

        // 娓叉煋姣忎釜鏂瑰潡鍒板搴旂殑 BufferBuilder
        for (final var blockInfo : blueprint.getBlockInfoAsList())
        {
            BlockState state = blockInfo.state();
            if (state == null || state.isAir()) continue;

            BlockPos localPos = blockInfo.pos();
            if (state.getRenderShape() == RenderShape.INVISIBLE) continue;

            try
            {
                BakedModel model = blockRenderer.getBlockModel(state);

                PoseStack ps = new PoseStack();
                ps.translate(localPos.getX(), localPos.getY(), localPos.getZ());

                // 鑾峰彇鏂瑰潡鐨勬墍鏈夋覆鏌撶被鍨嬪苟鍒嗗埆娓叉煋锛坈heckSides=false 閬垮厤闈㈠墧闄わ級
                for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY))
                {
                    BufferBuilder buffer = bufferPack.builder(renderType);
                    blockRenderer.renderBatched(state, localPos, fakeLevel, ps, buffer, false, random, ModelData.EMPTY, renderType);
                    renderType.clearRenderState();
                }
            }
            catch (Exception e)
            {
                LOGGER.warn("[BlueprintRenderer] 鏂瑰潡娓叉煋澶辫触 {}: {}", state, e.toString());
            }
        }

        // 涓婁紶鍒?VertexBuffer
        vertexBuffers = VERTEX_BUFFER_FACTORY.get();
        for (RenderType renderType : renderTypes)
        {
            BufferBuilder.RenderedBuffer rendered = bufferPack.builder(renderType).endOrDiscardIfEmpty();
            if (rendered == null)
            {
                vertexBuffers.remove(renderType);
            }
            else
            {
                VertexBuffer vb = vertexBuffers.get(renderType);
                if (vb != null)
                {
                    vb.bind();
                    vb.upload(rendered);
                }
            }
        }
        bufferPack.clearAll();
        VertexBuffer.unbind();

        initialized = true;
    }

    /**
     * 姣忓抚娓叉煋锛氳缃?shader 鈫?搴旂敤閫忔槑搴?鈫?閫愬眰缁樺埗 鈫?娓呯悊
     */
    public void draw(final BlockPos anchorPos, final RenderLevelStageEvent event, final float alpha)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // 棣栨鍒濆鍖栭《鐐圭紦鍐诧紙钃濆浘鏁版嵁涓嶅彉鍒欎笉澶嶅缓锛?
        if (!initialized)
        {
            init();
            lastGameTime = mc.level.getGameTime();
        }

        if (vertexBuffers == null || vertexBuffers.isEmpty()) return;

        final PoseStack poseStack = event.getPoseStack();
        final Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // 鍋忕Щ鍒拌摑鍥句腑蹇冧笅鏂?
        BlockPos origin = anchorPos.subtract(
            new BlockPos(blueprint.getSizeX() / 2, 0, blueprint.getSizeZ() / 2));

        Vec3 renderOffset = Vec3.atLowerCornerOf(origin).subtract(cameraPos);
        Vector3f chunkOffset = renderOffset.toVector3f();

        poseStack.pushPose();
        // 娉ㄦ剰锛氫笉鍦ㄨ繖閲?translate 鈥斺€?CHUNK_OFFSET 宸插皢灞€閮ㄥ潗鏍囧亸绉诲埌姝ｇ‘涓栫晫浣嶇疆
        // poseStack 淇濇寔浜嬩欢鍘熷鐘舵€侊紙鍚浉鏈烘棆杞級

        // 鍏夌収
        Lighting.setupLevel(poseStack.last().pose());

        Matrix4f mvMatrix = poseStack.last().pose();
        Matrix4f projMatrix = event.getProjectionMatrix();

        // 閫愬眰娓叉煋锛坰olid 鈫?cutoutMipped 鈫?cutout 鈫?translucent锛?        renderLayer(RenderType.solid(), mvMatrix, projMatrix, chunkOffset, alpha);
        mc.getModelManager().getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
            .setBlurMipmap(false, mc.options.mipmapLevels().get() > 0);
        renderLayer(RenderType.cutoutMipped(), mvMatrix, projMatrix, chunkOffset, alpha);
        mc.getModelManager().getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
            .restoreLastBlurMipmap();
        renderLayer(RenderType.cutout(), mvMatrix, projMatrix, chunkOffset, alpha);
        renderLayer(RenderType.translucent(), mvMatrix, projMatrix, chunkOffset, alpha);

        // 鎭㈠
        RenderSystem.applyModelViewMatrix();
        Lighting.setupLevel(poseStack.last().pose());

        poseStack.popPose();
    }

    /**
     * 娓叉煋鍗曚釜娓叉煋灞?     */
    private void renderLayer(RenderType renderType, Matrix4f mvMatrix, Matrix4f projMatrix,
                              Vector3f chunkOffset, float alpha)
    {
        VertexBuffer vb = vertexBuffers.get(renderType);
        if (vb == null) return;

        renderType.setupRenderState();

        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) return;

        // 璁剧疆 shader uniforms
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(mvMatrix);
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projMatrix);
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (shader.FOG_START != null) shader.FOG_START.set(RenderSystem.getShaderFogStart());
        if (shader.FOG_END != null) shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (shader.FOG_COLOR != null) shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (shader.FOG_SHAPE != null) shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (shader.TEXTURE_MATRIX != null) shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (shader.GAME_TIME != null) shader.GAME_TIME.set(RenderSystem.getShaderGameTime());

        RenderSystem.setupShaderLights(shader);
        shader.apply();

        // CHUNK_OFFSET uniform 鈥?灏嗛《鐐瑰亸绉诲埌瀹為檯浣嶇疆
        var chunkOffsetUniform = shader.CHUNK_OFFSET;
        if (chunkOffsetUniform != null)
        {
            chunkOffsetUniform.set(chunkOffset);
            chunkOffsetUniform.upload();
        }

        // 搴旂敤閫忔槑搴︼紙CONSTANT_ALPHA blend锛?        Transparency.apply(alpha);

        // 缁樺埗
        vb.bind();
        vb.draw();

        // 娓呯悊
        Transparency.reset();

        if (chunkOffsetUniform != null)
        {
            chunkOffsetUniform.set(new Vector3f(0, 0, 0));
        }

        shader.clear();
        VertexBuffer.unbind();
        renderType.clearRenderState();
    }

    // 鈹€鈹€ 閫忔槑搴﹀鐞嗭紙鏉ヨ嚜 Structurize TransparencyHack锛?鈹€鈹€

    private static class Transparency
    {
        private static boolean applied = false;

        static void apply(float alpha)
        {
            if (applied) return;
            if (alpha <= 0 || alpha >= 0.99f) return;

            applied = true;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.CONSTANT_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
            GL20C.glBlendColor(0, 0, 0, alpha);
        }

        static void reset()
        {
            if (!applied) return;
            applied = false;
            RenderSystem.disableBlend();
        }
    }

    private void clearVertexBuffers()
    {
        if (vertexBuffers != null)
        {
            vertexBuffers.values().forEach(VertexBuffer::close);
            vertexBuffers = null;
        }
        initialized = false;
    }

    @Override
    public void close()
    {
        clearVertexBuffers();
    }

    public Blueprint getBlueprint() { return blueprint; }

    // 鈹€鈹€ 鍋囦笘鐣岀幆澧冿紙涓?renderBatched 鎻愪緵钃濆浘鏁版嵁 + 鍏ㄤ寒鍏夌収锛?鈹€鈹€

    private static class FakeBlockAndTintGetter implements BlockAndTintGetter
    {
        private final Blueprint blueprint;

        FakeBlockAndTintGetter(Blueprint blueprint) { this.blueprint = blueprint; }

        @Override
        public BlockState getBlockState(BlockPos pos)
        {
            return blueprint.isPosInside(pos) ? blueprint.getBlockStateDirect(pos) : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos)
        {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() { return blueprint.getSizeY(); }

        @Override
        public int getMinBuildHeight() { return 0; }

        @Override
        public float getShade(Direction direction, boolean shaded) { return 1.0f; }

        @Override
        public LevelLightEngine getLightEngine() { return null; }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) { return 15; }

        @Override
        public int getRawBrightness(BlockPos pos, int amount) { return 15; }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) { return -1; }

        @Override
        @javax.annotation.Nullable
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) { return null; }
    }
}
