package com.metrogenesis.domumornamentum.client.model.utils;

import com.metrogenesis.domumornamentum.util.SingleBlockLevelReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Optional;

public class ModelSpriteQuadTransformer implements IQuadTransformer
{
    public static IQuadTransformer create(final ModelSpriteQuadTransformerData target) {
        return new ModelSpriteQuadTransformer(target);
    }

    private final ModelSpriteQuadTransformerData target;

    private ModelSpriteQuadTransformer(
      final ModelSpriteQuadTransformerData target
    )
    {
        this.target = target;
    }

    @Override
    public void processInPlace(final BakedQuad quad)
    {
        final float minU = quad.getSprite().getU0();
        final float uDelta = quad.getSprite().getU1() - minU;

        final float minV = quad.getSprite().getV0();
        final float vDelta = quad.getSprite().getV1() - minV;
        
        // 使用反射访问
        protected 字段 sprite
        TextureAtlasSprite targetSprite;
        try {
            Field spriteField = BakedQuad.class.getDeclaredField("sprite");
            spriteField.setAccessible(true);
            targetSprite = (TextureAtlasSprite) spriteField.get(target.quad());
            spriteField.set(quad, targetSprite);
        } catch (Exception e) { e.printStackTrace(); return; }

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++)
        {
            final float[] uv = new float[2];
            int offset = vertexIndex * STRIDE + UV0;
            uv[0] = Float.intBitsToFloat(quad.getVertices()[offset]);
            uv[1] = Float.intBitsToFloat(quad.getVertices()[offset + 1]);

            final float u = ( uv[0] - minU ) / uDelta;
            final float v = ( uv[1] - minV ) / vDelta;

            final float newU = targetSprite.getU(u * 16);
            final float newV = targetSprite.getV(v * 16);

            quad.getVertices()[offset] = Float.floatToRawIntBits(newU);
            quad.getVertices()[offset + 1] = Float.floatToRawIntBits(newV);
        }

        int color = getColorFor(target.state());
        // 使用反射访问
        protected 字段 tintIndex
        int tint;
        try {
            Field tintField = BakedQuad.class.getDeclaredField("tintIndex");
            tintField.setAccessible(true);
            tint = tintField.getInt(target.quad());
        } catch (Exception e) { tint = -1; }

        color = tint != -1 ? color : 0xffffffff;

        if (0x00 <= tint && tint <= 0xff) {
            color = 0xffffffff;
            tint = (Block.getId(target.state()) << 8) | tint;
        } else {
            tint = -1;
        }

        if (color != -1) {
            QuadTransformers.applyingColor(color).processInPlace(quad);
        }

        if (tint != -1) {
            try {
                Field tintField = BakedQuad.class.getDeclaredField("tintIndex");
                tintField.setAccessible(true);
                tintField.setInt(quad, tint);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private int getColorFor(final BlockState state) {
        int out;
        final Fluid fluid = state.getFluidState().getType();
        if (fluid != Fluids.EMPTY) {
            out = IClientFluidTypeExtensions.of(fluid).getTintColor(new FluidStack(fluid, 1));
        } else {
            final ItemStack target = getItemStackFromBlockState(state);

            if (target.isEmpty()) {
                out = 0xffffff;
            } else {
                out = Minecraft.getInstance().getItemColors().getColor(target, 0);
            }
        }

        return out;
    }
    
    private static ItemStack getItemStackFromBlockState(BlockState state) {
        if (state.getBlock() instanceof LiquidBlock liquidBlock)
        {
            return new ItemStack(liquidBlock.getFluidState(state).getType().getBucket());
        }

        final Item item = getItem(state);
        if (item != Items.AIR && item != null)
        {
            return new ItemStack(item, 1);
        }

        return new ItemStack(state.getBlock(), 1);
    }

    private static Item getItem(@NotNull final BlockState state)
    {
        final Block block = state.getBlock();
        if (block.equals(Blocks.LAVA))
        {
            return Items.LAVA_BUCKET;
        }
        else if (block instanceof CropBlock)
        {
            final ItemStack stack = block.getCloneItemStack(new SingleBlockLevelReader(state), BlockPos.ZERO, state);
            if (!stack.isEmpty())
            {
                return stack.getItem();
            }

            return Items.WHEAT_SEEDS;
        }
        // oh no...
        else if (block instanceof FarmBlock || block instanceof DirtPathBlock)
        {
            return Blocks.DIRT.asItem();
        }
        else if (block instanceof FireBlock)
        {
            return Items.FLINT_AND_STEEL;
        }
        else if (block instanceof FlowerPotBlock)
        {
            return Items.FLOWER_POT;
        }
        else if (block == Blocks.BAMBOO_SAPLING)
        {
            return Items.BAMBOO;
        }
        else
        {
            return block.asItem();
        }
    }
}
