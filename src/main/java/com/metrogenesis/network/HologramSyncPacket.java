package com.metrogenesis.network;

import com.metrogenesis.hologram.HologramRenderLayer;
import com.metrogenesis.init.BuildingType;
import com.metrogenesis.network.messages.AbstractClientMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * 鍏ㄦ伅鍥惧悓姝ュ寘 鈥?缁ф壙 AbstractClientMessage
 * <p>
 * 鏈嶅姟绔啋瀹㈡埛绔細閫氱煡瀹㈡埛绔覆鏌?绉婚櫎鍏ㄦ伅钃濆浘
 */
public class HologramSyncPacket extends AbstractClientMessage
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final byte ACTION_ADD = 0;
    private static final byte ACTION_REMOVE = 1;
    private static final byte ACTION_CLEAR = 2;

    private byte action;
    private Set<BlockPos> positions = new HashSet<>();
    private String buildingTypeId;
    private float alpha;

    /** 鏃犲弬鏋勯€狅紙鍙嶅簭鍒楀寲鐢級 */
    public HologramSyncPacket() {}

    /** 娣诲姞鍏ㄦ伅鍥?*/
    public HologramSyncPacket(Set<BlockPos> positions, String buildingTypeId, float alpha)
    {
        this.action = ACTION_ADD;
        this.positions = positions;
        this.buildingTypeId = buildingTypeId;
        this.alpha = alpha;
    }

    /** 绉婚櫎鍏ㄦ伅鍥?*/
    public HologramSyncPacket(Set<BlockPos> positions)
    {
        this.action = ACTION_REMOVE;
        this.positions = positions;
    }

    /** 娓呯┖ */
    public HologramSyncPacket clear()
    {
        this.action = ACTION_CLEAR;
        return this;
    }

    @Override
    protected void toBytes(final FriendlyByteBuf buf)
    {
        buf.writeByte(action);
        buf.writeInt(positions.size());
        for (BlockPos pos : positions) buf.writeBlockPos(pos);

        if (action == ACTION_ADD)
        {
            buf.writeUtf(buildingTypeId);
            buf.writeFloat(alpha);
        }
    }

    @Override
    protected void fromBytes(final FriendlyByteBuf buf)
    {
        action = buf.readByte();
        int size = buf.readInt();
        positions = new HashSet<>();
        for (int i = 0; i < size; i++) positions.add(buf.readBlockPos());

        if (action == ACTION_ADD)
        {
            buildingTypeId = buf.readUtf();
            alpha = buf.readFloat();
        }
    }

    @Override
    protected void onExecute(final Player player)
    {
        switch (action)
        {
            case ACTION_ADD -> {
                BuildingType type = BuildingType.fromId(buildingTypeId);
                if (type != null) {
                    for (BlockPos pos : positions) HologramRenderLayer.queueHologram(pos, type, alpha);
                    LOGGER.debug("[Hologram] 娣诲姞 {} 涓叏鎭浘", positions.size());
                }
            }
            case ACTION_REMOVE -> {
                for (BlockPos pos : positions) HologramRenderLayer.removeHologram(pos);
            }
            case ACTION_CLEAR -> HologramRenderLayer.clearAll();
        }
    }
}
