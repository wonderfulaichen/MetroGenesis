package com.metrogenesis.road.storage;

import com.metrogenesis.util.KeyUtil;
import com.metrogenesis.util.NbtUtils;
import com.metrogenesis.util.PersistentStateUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores computed paths between nodes as a {@link SavedData}.
 */
public class PathStorage extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis/" + PathStorage.class.getSimpleName());
    private static final String KEY = "metrogenesis_road_paths";
    private static final String PATHS_KEY = "paths";
    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";
    private static final String POS_KEY = "pos";
    private static final String STATUS_KEY = "status";

    private final Map<String, List<BlockPos>> paths = new ConcurrentHashMap<>();
    private final Map<String, Status> statuses = new ConcurrentHashMap<>();

    public static PathStorage get(ServerLevel world) {
        return PersistentStateUtil.get(world, PathStorage::new, PathStorage::fromNbt, KEY);
    }

    public static PathStorage fromNbt(CompoundTag tag) {
        PathStorage storage = new PathStorage();
        ListTag list = tag.getList(PATHS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String from = entry.getString(FROM_KEY);
            String to = entry.getString(TO_KEY);
            String key = KeyUtil.pathKey(from, to);
            ListTag posList = entry.getList(POS_KEY, Tag.TAG_LONG);
            List<BlockPos> positions = new ArrayList<>();
            for (Tag nbtElement : posList) {
                positions.add(BlockPos.of(((LongTag) nbtElement).getAsLong()));
            }
            storage.paths.put(key, positions);
            Status status = NbtUtils.getEnumOrDefault(entry, STATUS_KEY, Status.class, Status.READY);
            storage.statuses.put(key, status);
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, List<BlockPos>> entry : paths.entrySet()) {
            String[] ids = KeyUtil.parsePathKey(entry.getKey());
            if (ids.length != 2) continue;
            CompoundTag elem = new CompoundTag();
            elem.putString(FROM_KEY, ids[0]);
            elem.putString(TO_KEY, ids[1]);
            ListTag posList = new ListTag();
            for (BlockPos pos : entry.getValue()) {
                posList.add(LongTag.valueOf(pos.asLong()));
            }
            elem.put(POS_KEY, posList);
            Status st = statuses.getOrDefault(entry.getKey(), Status.PENDING);
            elem.putString(STATUS_KEY, st.name());
            list.add(elem);
        }
        tag.put(PATHS_KEY, list);
        return tag;
    }

    public void putPath(String from, String to, List<BlockPos> path, Status status) {
        String key = KeyUtil.pathKey(from, to);
        paths.put(key, List.copyOf(path));
        statuses.put(key, status);
        setDirty();
    }

    public void updatePath(String key, List<BlockPos> path, Status status) {
        paths.put(key, List.copyOf(path));
        statuses.put(key, status);
        setDirty();
    }

    public List<BlockPos> getPath(String from, String to) {
        return paths.getOrDefault(KeyUtil.pathKey(from, to), List.of());
    }

    public List<BlockPos> getPath(String key) {
        return paths.getOrDefault(key, List.of());
    }

    public Status getStatus(String key) {
        return statuses.getOrDefault(key, Status.PENDING);
    }

    public void setStatus(String key, Status status) {
        statuses.put(key, status);
        setDirty();
    }

    public Map<String, Status> allStatuses() {
        return Map.copyOf(statuses);
    }

    public boolean tryMarkProcessing(String key) {
        synchronized (statuses) {
            Status cur = statuses.get(key);
            if (cur != Status.PENDING) {
                return false;
            }
            statuses.put(key, Status.PROCESSING);
            setDirty();
            return true;
        }
    }

    public List<String> getPendingForChunk(ChunkPos chunk) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<BlockPos>> e : paths.entrySet()) {
            if (getStatus(e.getKey()) != Status.PENDING) continue;
            for (BlockPos p : e.getValue()) {
                if (new ChunkPos(p).equals(chunk)) {
                    out.add(e.getKey());
                    break;
                }
            }
        }
        return out;
    }

    public enum Status {
        PENDING,
        PROCESSING,
        READY,
        FAILED
    }
}
