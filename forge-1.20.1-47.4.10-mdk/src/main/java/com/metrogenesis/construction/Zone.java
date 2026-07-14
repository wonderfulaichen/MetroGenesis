package com.metrogenesis.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

/**
 * 鍖哄煙瀹氫箟 鈥?绛夊悓浜?BuildCraft 鐨?Box锛岀敤浜庣晫瀹氭柦宸ヨ寖鍥?鍥存爮鍖哄煙
 */
public class Zone {

    private BlockPos min;
    private BlockPos max;

    public Zone() {
        this.min = BlockPos.ZERO;
        this.max = BlockPos.ZERO;
    }

    public Zone(BlockPos center, int radius) {
        this.min = new BlockPos(center.getX() - radius, center.getY() - 1, center.getZ() - radius);
        this.max = new BlockPos(center.getX() + radius, center.getY() + 1, center.getZ() + radius);
    }

    // 鈹€鈹€ NBT 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MinX", min.getX()); tag.putInt("MinY", min.getY()); tag.putInt("MinZ", min.getZ());
        tag.putInt("MaxX", max.getX()); tag.putInt("MaxY", max.getY()); tag.putInt("MaxZ", max.getZ());
        return tag;
    }

    public static Zone load(CompoundTag tag) {
        Zone z = new Zone();
        z.min = new BlockPos(tag.getInt("MinX"), tag.getInt("MinY"), tag.getInt("MinZ"));
        z.max = new BlockPos(tag.getInt("MaxX"), tag.getInt("MaxY"), tag.getInt("MaxZ"));
        return z;
    }

    // 鈹€鈹€ 妫€娴?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean isOnEdge(BlockPos pos) {
        return contains(pos) && (
            pos.getX() == min.getX() || pos.getX() == max.getX()
         || pos.getZ() == min.getZ() || pos.getZ() == max.getZ());
    }

    // 鈹€鈹€ 鍥存爮绮掑瓙锛圔uildCraft 椋庢牸绾挎锛?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public void renderFrame(ServerLevel level) {
        // 娌?X 杞村簳杈?
        for (int x = min.getX(); x <= max.getX(); x++) {
            spawnParticle(level, x, min.getY(), min.getZ());
            spawnParticle(level, x, min.getY(), max.getZ());
        }
        // 娌?Z 杞村簳杈?
        for (int z = min.getZ(); z <= max.getZ(); z++) {
            spawnParticle(level, min.getX(), min.getY(), z);
            spawnParticle(level, max.getX(), min.getY(), z);
        }
        // 鍥涜绔嬫煴
        for (int y = min.getY(); y <= max.getY(); y++) {
            spawnParticle(level, min.getX(), y, min.getZ());
            spawnParticle(level, max.getX(), y, min.getZ());
            spawnParticle(level, min.getX(), y, max.getZ());
            spawnParticle(level, max.getX(), y, max.getZ());
        }
    }

    private void spawnParticle(ServerLevel level, int x, int y, int z) {
        level.sendParticles(ParticleTypes.END_ROD,
                x + 0.5, y + 0.5, z + 0.5,
                1, 0, 0, 0, 0);
    }

    // 鈹€鈹€ getter 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€

    public BlockPos getMin() { return min; }
    public BlockPos getMax() { return max; }
    public BlockPos getCenter() {
        return new BlockPos(
            (min.getX() + max.getX()) / 2,
            (min.getY() + max.getY()) / 2,
            (min.getZ() + max.getZ()) / 2);
    }

    public int getSizeX() {
        return max.getX() - min.getX() + 1;
    }

    public int getSizeY() {
        return max.getY() - min.getY() + 1;
    }

    public int getSizeZ() {
        return max.getZ() - min.getZ() + 1;
    }
}
