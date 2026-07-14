package com.metrogenesis.road.storage;

import com.metrogenesis.util.GeometryUtils;
import com.metrogenesis.util.KeyUtil;
import com.metrogenesis.util.PersistentStateUtil;
import com.metrogenesis.road.storage.components.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Stores road nodes and edges as a {@link SavedData}.
 */
public class RoadGraphState extends SavedData {
    private static final String KEY = "metrogenesis_road_graph";
    private static final String NODES_KEY = "nodes";
    private static final String EDGES_KEY = "edges";
    private static final String RADIUS_KEY = "radius";

    private final NodeStorage nodeStorage;
    private final EdgeStorage edgeStorage;

    public RoadGraphState(double radius) {
        this(new NodeStorage(), new EdgeStorage(radius));
    }

    private RoadGraphState(NodeStorage nodes, EdgeStorage edges) {
        this.nodeStorage = nodes;
        this.edgeStorage = edges;
    }

    public static RoadGraphState get(ServerLevel world, double maxConnectionDistance) {
        return PersistentStateUtil.get(world,
                () -> new RoadGraphState(maxConnectionDistance),
                RoadGraphState::fromNbt,
                KEY);
    }

    /*========== helpers ==========*/

    public static RoadGraphState fromNbt(CompoundTag tag) {
        double radius = tag.getDouble(RADIUS_KEY);
        NodeStorage nodes = NodeStorage.fromNbt(tag.getList(NODES_KEY, Tag.TAG_COMPOUND));
        EdgeStorage edges = EdgeStorage.fromNbt(tag.getCompound(EDGES_KEY), radius);
        return new RoadGraphState(nodes, edges);
    }

    public NodeStorage nodes() {
        return nodeStorage;
    }

    public EdgeStorage edges() {
        return edgeStorage;
    }

    /**
     * Adds a new node and immediately builds all valid edges to existing nodes.
     * @param pos  position for the new node
     * @return the created node
     */
    public Node addNodeWithEdges(BlockPos pos, String type) {
        Node newNode = this.nodeStorage.add(pos, type);
        for (Node other : this.nodeStorage.all().values()) {
            if (!other.id().equals(newNode.id())) {
                connect(newNode, other);
            }
        }
        this.setDirty();
        return newNode;
    }

    /**
     * Tries to connect two nodes, preventing crossing edges.
     */
    public void connect(Node nodeA, Node nodeB) {
        if (nodeA == null || nodeB == null) return;
        String idNodeA = nodeA.id();
        String idNodeB = nodeB.id();
        if (idNodeA.equals(idNodeB)) return;

        // 1) distance check
        double dx = nodeA.pos().getX() - nodeB.pos().getX();
        double dz = nodeA.pos().getZ() - nodeB.pos().getZ();
        double max = edgeStorage.radius() * 2.0;
        if (dx * dx + dz * dz > max * max) return;

        // 2) already exists?
        if (edgeStorage.all().containsKey(KeyUtil.edgeKey(idNodeA, idNodeB))) return;

        // 3) would new edge cross any existing edge?
        for (EdgeStorage.Edge e : edgeStorage.all().values()) {
            if (e.connects(idNodeA) || e.connects(idNodeB)) continue;
            Node n1 = nodeStorage.all().get(e.nodeA());
            Node n2 = nodeStorage.all().get(e.nodeB());
            if (n1 == null || n2 == null) continue;
            if (GeometryUtils.segmentsIntersect2D(nodeA.pos(), nodeB.pos(), n1.pos(), n2.pos())) {
                return;
            }
        }

        // 4) all clear — delegate actual creation
        boolean added = edgeStorage.add(nodeA, nodeB);
        if (added) this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putDouble(RADIUS_KEY, edgeStorage.radius());
        tag.put(NODES_KEY, nodeStorage.toNbt());
        tag.put(EDGES_KEY, edgeStorage.toNbt());
        return tag;
    }
}
