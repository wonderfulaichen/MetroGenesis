package com.metrogenesis.road.storage;

import com.metrogenesis.road.storage.components.Node;
import com.metrogenesis.util.KeyUtil;
import com.metrogenesis.util.NbtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.nbt.CompoundTag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores binary edges between nodes. Each edge has a single
 * {@link Edge} entry containing both nodes and a {@link Status}.
 *
 * <p>Edge id is formed as "min(idA,idB)+"+"+max(idA,idB)" — this guarantees
 * the same key for a pair of nodes regardless of order.
 */
public class EdgeStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("MetroGenesis/" + EdgeStorage.class.getSimpleName());

    /** Connection radius. */
    private final double radius;

    /** All edges by edge-id. */
    private final Map<String, Edge> edges = new ConcurrentHashMap<>();

    public EdgeStorage(double radius) {
        this.radius = radius;
    }

    public static EdgeStorage fromNbt(CompoundTag tag, double radius) {
        EdgeStorage storage = new EdgeStorage(radius);
        for (String edgeId : tag.getAllKeys()) {
            CompoundTag entry = tag.getCompound(edgeId);
            String a = entry.getString("a");
            String b = entry.getString("b");
            Status status = NbtUtils.getEnumOrDefault(entry, "status", Status.class, Status.NEW);
            storage.edges.put(edgeId, new Edge(a, b, status));
        }
        return storage;
    }

    /* ───────────────────────────── Edge API ───────────────────────────── */

    public double radius() {
        return radius;
    }

    /**
     * Tries to add an edge between two nodes.
     * @return true if the edge was created (or already existed)
     */
    public boolean add(Node a, Node b) {
        if (a.id().equals(b.id())) return false; // self-loop guard
        String edgeId = KeyUtil.edgeKey(a.id(), b.id());
        edges.put(edgeId, new Edge(a.id(), b.id(), Status.NEW));
        return true;
    }

    /* ───────────────────────────── CRUD ───────────────────────────── */

    public boolean remove(String edgeId) {
        return edges.remove(edgeId) != null;
    }

    public Status getStatus(String edgeId) {
        Edge edge = edges.get(edgeId);
        return edge == null ? null : edge.status();
    }

    public void setStatus(String edgeId, Status status) {
        Edge old = edges.get(edgeId);
        if (old != null) {
            edges.put(edgeId, new Edge(old.nodeA(), old.nodeB(), status));
        }
    }

    public Set<String> neighbors(String nodeId) {
        Set<String> set = new HashSet<>();
        for (Edge edge : edges.values()) {
            if (edge.connects(nodeId)) {
                set.add(edge.other(nodeId));
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /* ───────────────────────────── Query helpers ───────────────────────────── */

    public Map<String, Edge> all() {
        return Collections.unmodifiableMap(new HashMap<>(edges));
    }

    public Map<String, Status> allWithStatus() {
        Map<String, Status> map = new HashMap<>();
        for (Map.Entry<String, Edge> e : edges.entrySet()) {
            map.put(e.getKey(), e.getValue().status());
        }
        return Collections.unmodifiableMap(map);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        for (Edge edge : edges.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("a", edge.nodeA());
            entry.putString("b", edge.nodeB());
            entry.putString("status", edge.status().name());
            tag.put(edge.id(), entry);
        }
        return tag;
    }

    /* ───────────────────────────── Persistence ───────────────────────────── */

    public void clear() {
        edges.clear();
    }

    /**
     * Status used by the path-finding algorithm.
     */
    public enum Status {
        NEW,
        SUCCESS,
        FAILURE
    }

    /* ───────────────────────────── Internals ───────────────────────────── */

    public record Edge(String nodeA, String nodeB, Status status) {
        public String id() {
            return KeyUtil.edgeKey(nodeA, nodeB);
        }

        public boolean connects(String nodeId) {
            return nodeA.equals(nodeId) || nodeB.equals(nodeId);
        }

        public String other(String nodeId) {
            return nodeA.equals(nodeId) ? nodeB : nodeA;
        }
    }
}
