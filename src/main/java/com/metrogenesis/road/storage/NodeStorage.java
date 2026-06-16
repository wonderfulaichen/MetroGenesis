package com.metrogenesis.road.storage;

import com.metrogenesis.road.storage.components.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage for all road network nodes in the world.
 */
public class NodeStorage {
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();

    public static NodeStorage fromNbt(ListTag list) {
        NodeStorage storage = new NodeStorage();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString("id");
            BlockPos pos = BlockPos.of(tag.getLong("pos"));
            String type = tag.getString("type");
            storage.nodes.put(id, new Node(id, pos, type));
        }
        return storage;
    }

    /**
     * Creates a new node at the given position and adds it to this storage.
     */
    public Node add(BlockPos pos, String type) {
        String id = UUID.randomUUID().toString();
        Node node = new Node(id, pos, type);
        nodes.put(id, node);
        return node;
    }

    /**
     * Removes the node with the given identifier.
     */
    public boolean remove(String id) {
        return nodes.remove(id) != null;
    }

    /**
     * Returns an unmodifiable view of all stored nodes.
     */
    public Map<String, Node> all() {
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Clears all stored nodes.
     */
    public void clear() {
        nodes.clear();
    }

    /**
     * Serializes all nodes into an NBT list.
     */
    public ListTag toNbt() {
        ListTag list = new ListTag();
        for (Node node : nodes.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", node.id());
            tag.putLong("pos", node.pos().asLong());
            tag.putString("type", node.type());
            list.add(tag);
        }
        return list;
    }
}
