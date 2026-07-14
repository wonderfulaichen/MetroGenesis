package com.metrogenesis.blueprint.v1;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 钃濆浘鏂瑰潡杩唬鍣?鈥?璺熻釜涓嬩竴涓鏀剧疆鐨勬柟鍧椾綅缃? * <p>
 * 鍙傝€?MineColonies/Structurize {@code IBlueprintIterator} + {@code BlueprintIteratorDefault}銆? * <p>
 * 鎵弿妯″紡锛歑鈫抁鈫扽 铔囧舰鎵弿锛堜粠涓嬪線涓婏級
 * <pre>
 *   绗?0 灞?(Y=0): (0,0,0)鈫?1,0,0)鈫?..鈫?7,0,0) 鐒跺悗
 *                  (7,0,1)鈫?6,0,1)鈫?..鈫?0,0,1) 鐒跺悗
 *                   閲嶅铔囧舰...
 *   绗?1 灞?(Y=1): 閲嶅
 * </pre>
 */
public class BlueprintIterator
{
    /** 鐗规畩鏍囪锛氬皻鏈紑濮?*/
    private static final BlockPos START_POS = new BlockPos(-1, 0, -1);

    private final Blueprint blueprint;
    private final int sizeX, sizeY, sizeZ;

    // 褰撳墠杩涘害浣嶇疆锛堣摑鍥剧殑鏈湴鍧愭爣锛?
    private int px, py, pz;

    // 鏄惁瀹屾垚
    private boolean finished = false;

    public BlueprintIterator(final Blueprint blueprint)
    {
        this.blueprint = blueprint;
        this.sizeX = blueprint != null ? blueprint.getSizeX() : 0;
        this.sizeY = blueprint != null ? blueprint.getSizeY() : 0;
        this.sizeZ = blueprint != null ? blueprint.getSizeZ() : 0;
        this.px = -1;
        this.py = 0;
        this.pz = 0;
    }

    /**
     * 鑾峰彇涓嬩竴涓渶瑕佹斁缃殑鏂瑰潡淇℃伅銆?     * 璺宠繃 air 鏂瑰潡锛岃繑鍥炵涓€涓潪 air 鐨勬柟鍧椼€?     *
     * @return 涓嬩竴涓湁鏁堟柟鍧楃殑淇℃伅锛岃嫢鎵€鏈夋柟鍧楀凡澶勭悊瀹屽垯杩斿洖 null
     */
    public PlacedBlock next()
    {
        if (finished) return null;
        if (px < 0)
        {
            px = 0;
        }

        // 閫愬潡鎵弿锛岃烦杩囩┖姘?
        while (true)
        {
            // 妫€鏌ュ綋鍓嶄綅缃槸鍚﹀湪鑼冨洿鍐?
            if (py >= sizeY)
            {
                finished = true;
                return null;
            }

            // 鑾峰彇褰撳墠鏂瑰潡鐘舵€?
            BlockState state = getBlockState();
            BlockPos localPos = new BlockPos(px, py, pz);

            // 绉诲姩鍒颁笅涓€涓綅缃?            advance();

            // 濡傛灉褰撳墠鏂瑰潡涓嶆槸绌烘皵锛岃繑鍥炲畠
            if (state != null && !state.isAir())
            {
                return new PlacedBlock(localPos, state);
            }

            // 绌烘皵鏂瑰潡 鈥?缁х画
        }
    }

    /**
     * 鑾峰彇涓嬩竴涓渶瑕佹鏌ョ殑鏂瑰潡锛堜笉璺宠繃绌烘皵锛?     */
    public PlacedBlock nextIncludingAir()
    {
        if (finished) return null;
        if (px < 0) px = 0;

        if (py >= sizeY)
        {
            finished = true;
            return null;
        }

        BlockState state = getBlockState();
        BlockPos localPos = new BlockPos(px, py, pz);
        advance();
        return new PlacedBlock(localPos, state);
    }

    /**
     * 铔囧舰绉诲姩鍒颁笅涓€涓綅缃?     */
    private void advance()
    {
        // Z 鍋舵暟鍒?X 姝ｆ柟鍚戯紝濂囨暟鍒?X 璐熸柟鍚?
        if (pz % 2 == 0)
        {
            px++;
            if (px >= sizeX)
            {
                px = sizeX - 1;
                pz++;
                if (pz >= sizeZ)
                {
                    pz = 0;
                    py++;
                }
            }
        }
        else
        {
            px--;
            if (px < 0)
            {
                px = 0;
                pz++;
                if (pz >= sizeZ)
                {
                    pz = 0;
                    py++;
                }
            }
        }
    }

    /**
     * 鑾峰彇褰撳墠浣嶇疆鐨勬柟鍧楃姸鎬?     */
    private BlockState getBlockState()
    {
        if (blueprint == null || py >= sizeY || pz >= sizeZ || px >= sizeX)
        {
            return null;
        }
        return blueprint.getBlockStateDirect(new BlockPos(px, py, pz));
    }

    /**
     * 鏄惁杩樻湁涓嬩竴涓柟鍧楅渶瑕佸鐞?     */
    public boolean hasNext()
    {
        if (finished || blueprint == null) return false;
        if (px < 0) return true; // 杩樻病寮€濮?
        // 蹇€熸鏌ワ細濡傛灉褰撳墠灞傝繕娌℃壂鎻忓畬锛岃偗瀹氳繕鏈?
        if (py < sizeY) return true;

        finished = true;
        return false;
    }

    /**
     * 鑾峰彇褰撳墠杩涘害浣嶇疆锛堣摑鍥炬湰鍦板潗鏍囷級
     */
    public BlockPos getProgressPos()
    {
        if (finished) return new BlockPos(0, sizeY, 0);
        return new BlockPos(px, py, pz);
    }

    /**
     * 璁剧疆杩涘害浣嶇疆锛堜粠 NBT 鎭㈠鏃朵娇鐢級
     */
    public void setProgressPos(BlockPos pos)
    {
        this.px = pos.getX();
        this.py = pos.getY();
        this.pz = pos.getZ();
    }

    /**
     * 閲嶇疆杩唬鍣?     */
    public void reset()
    {
        this.px = -1;
        this.py = 0;
        this.pz = 0;
        this.finished = false;
    }

    public boolean isFinished() { return finished; }

    /**
     * 淇濆瓨杩唬鍣ㄧ姸鎬佸埌 NBT
     */
    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("px", px);
        tag.putInt("py", py);
        tag.putInt("pz", pz);
        tag.putBoolean("finished", finished);
        return tag;
    }

    /**
     * 浠?NBT 鍔犺浇杩唬鍣ㄧ姸鎬?     */
    public void load(CompoundTag tag)
    {
        this.px = tag.getInt("px");
        this.py = tag.getInt("py");
        this.pz = tag.getInt("pz");
        this.finished = tag.getBoolean("finished");
    }

    /**
     * 鍗曚釜鏀剧疆鐨勬柟鍧椾俊鎭?     */
    public record PlacedBlock(BlockPos localPos, BlockState state) {}
}
