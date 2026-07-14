# Effortless 架构深度分析报告

> 基于完整源码阅读（227 个 Java 文件，12 个子包）
> 阅读对象：dev.huskuraft.effortless（Forge 1.20.1 建筑辅助模组）

---

## 一、架构全景

Effortless 是一个**客户端驱动的建筑辅助模组**，核心依赖 `universal.api` 抽象层（不直接使用 Forge API）。

### 文件总览

```
dev.huskuraft.effortless/
├── Effortless.java                    — 主入口 (@Mod)
├── EffortlessClient.java              — 客户端入口（6个单例）
├── 14个顶层接口/类                     — Entrance, NetworkChannel, StructureBuilder 等
├── building/       (16)               — Operation, Pattern, Structure 核心
│   ├── clipboard/  (17)               — Clipboard 剪贴板系统
│   ├── config/     (12)               — 配置序列化（Builder/Client/Clipboard/Pattern/Passive/Render）
│   ├── history/    (6)                — Undo/Redo 历史栈
│   ├── interceptor/ (5)               — 第三方模组拦截（OpenPac, FTB Chunks）
│   ├── operation/  (3)                — Operation 抽象
│   │   ├── batch/  (5)                — BatchOperation 批量执行
│   │   ├── block/  (16)               — BlockOperation 实现（6种具体操作）
│   │   └── empty/  (2)                — 空操作占位
│   ├── pattern/    (16)               — Pattern 变换系统（Array/Mirror/Radial/Randomizer）
│   ├── replace/    (5)                — Replace 替换策略
│   ├── session/    (2)                — Session 管理器
│   ├── settings/   (2)                — 玩家设置
│   └── structure/  (2)                — Structure 抽象
│       └── builder/ (4)               — StructureBuilder
│           └── standard/ (16)         — 14种BuildMode的具体Structure实现
├── networking/   (22)                 — 网络协议层
├── renderer/     (20)                 — 渲染管线
├── screen/       (35)                 — GUI界面
└── session/      (5)                  — 服务端Session管理
```

### 核心依赖关系

```
Entrance (抽象入口)
 ├── ClientEntrance
 │    ├── EffortlessClientNetworkChannel
 │    ├── EffortlessClientStructureBuilder  ← 核心（799行）
 │    ├── EffortlessClientManager
 │    ├── EffortlessClientConfigStorage
 │    ├── EffortlessClientTagConfigStorage (@Deprecated)
 │    └── EffortlessClientSessionManager
 └── ServerEntrance
      ├── EffortlessServerNetworkChannel
      ├── EffortlessStructureBuilder         ← stub，实际为空
      ├── EffortlessServerManager
      └── EffortlessServerSessionManager
```

---

## 二、BuildState FSM（核心状态机）

### 状态转换

BuildState 由**客户端手持物品自动检测**，无需用户手动切换：

```
手中是方块 + Clipboard禁用 → PLACE_BLOCK
耐用工具（镐/斧/铲）       → BREAK_BLOCK
空手                       → INTERACT_BLOCK（交互物品/实体）
Clipboard启用 + 空剪贴板   → COPY_STRUCTURE
Clipboard启用 + 有数据     → PASTE_STRUCTURE
```

### 6 种状态

| 状态 | 操作描述 | 触发动作 |
|------|----------|----------|
| IDLE | 无操作 | - |
| BREAK_BLOCK | 破坏方块 | 左键攻击 |
| PLACE_BLOCK | 放置方块 | 右键使用 |
| INTERACT_BLOCK | 交互（门/按钮/箱子等） | 右键使用 |
| COPY_STRUCTURE | 复制结构到剪贴板 | 左键或右键 |
| PASTE_STRUCTURE | 从剪贴板粘贴结构 | 右键使用 |

---

## 三、Client Tick 管线（完整数据流）

`EffortlessClientStructureBuilder.onClientTick(ClientTick.Phase.START)` ：

```
1. 验证 session / permission 有效性
2. 检查死亡/维度变更/禁用模式/被动模式 → 跳过
3. reloadContext() → finalize with BuildStage.TICK
4. getContextTraced() → 解析 BuildState + trace（射线追踪）
5. 设置 BuildType.PREVIEW
6. 检查 maxRenderVolume → 超过则不渲染
7. create() BatchBuildSession → 构建完整操作管线
8. commit() → 生成 OperationResult
9. showContext() → 渲染预览
   ├── Pattern 变换渲染（PatternRenderer）
   ├── 边界框轮廓（OutlineRenderer.showBoundingBox）
   └── 方块簇渲染（OutlineRenderer.showCluster → BlockOperationRenderer）
10. showTooltip() → 物品统计 + 设置信息
11. 发送 PlayerBuildPacket 到服务端（信息同步）
```

---

## 四、交互处理管线（用户输入）

`onPlayerInteract()` ~120行：

```
1. 解析 BuildState：
   - ATTACK  → BREAK_BLOCK / COPY_STRUCTURE
   - USE_ITEM → PLACE_BLOCK / PASTE_STRUCTURE / INTERACT_BLOCK
2. trace() — 射线检测目标方块
3. 校验链（逐项失败则中断）：
   ├── target = MISS？→ 距离超限警告
   ├── target = ENTITY？→ 跳过
   ├── state 不匹配？→ 警告并取消
   ├── 权限不足？→ 提示
   └── 体积超限？→ 提示
4. fulfilled → finalize with BuildStage.BUILD_CLIENT
5. create() → commit() → 生成 OperationResult
6. 发送 PlayerBuildPacket 到服务端
7. 播放方块操作音效
8. 显示 tooltip
9. 返回 EventResult.interrupt(true/false)
```

---

## 五、Operation 系统

### 核心接口

```
Operation (接口)
├── commit() → OperationResult
├── commit(Level) → OperationResult
├── collectOperations() → Stream<Operation>
├── move(Vector3i) → Operation
├── mirror(Vector3d, MirrorAxis) → Operation
├── rotate(Rotation) → Operation
└── refactor(int, int) → Operation

OperationResult
├── getReverseOperation() → Operation          ← 用于 Undo
├── getAffectedBlocks() → Set<BlockPos>
└── getSummary() → Map<ItemStack, Integer>
```

### 6 种具体 BlockOperation

| Operation | 用途 | 数据量 |
|-----------|------|--------|
| BlockStateUpdateOperation | 放置/破坏方块 | 302行（最复杂） |
| BlockInteractOperation | 交互（门/按钮） | ~30行 |
| BlockStateCopyOperation | 复制方块状态 | ~20行 |
| BlockAirUpdateOperation | 空气更新（破坏） | 复用UpdateOperation |
| BlockEntityUpdateOperation | 方块实体更新 | ~40行 |
| BlockEventOperation | 事件触发 | ~20行 |

### BatchOperation 执行流程

`BatchBuildSession.create()` → `commit()`

```
BuildState → switch 匹配操作类型
├── BREAK       → BlockStateUpdateOperation(方块→空气)
├── PLACE       → BlockStateUpdateOperation(空气→方块)
├── INTERACT    → BlockInteractOperation
├── COPY        → BlockStateCopyOperation（同时保存 Clipboard）
└── PASTE       → clipboard.snapshot().blockData() → Map<BlockPos, BlockState> 遍历创建 Operation

→ 包装为 DeferredBatchOperation
→ PLACE_BLOCK → 应用 ItemRandomizer（随机替换部分方块）
→ Pattern enabled → 依次应用 transformers:
   ArrayTransformer → MirrorTransformer → RadialTransformer
→ flatten() → filter(distinctBlockOperations) → commit()
```

### BlockStateUpdateOperation.updateBlock() 验证链（~180行）

```
1. 维度检查
2. 游戏模式检查
3. 目标方块状态检查
4. 权限检查
5. 世界边界检查
6. Y轴高度检查（0~255）
7. Replace策略检查（4种策略）
8. 耐久度保留逻辑
9. 方块破坏执行（世界）
10. 物品数量检查
11. 方块放置执行
```

---

## 六、Pattern 变换系统

### 架构

```
Pattern record
├── boolean enabled
├── List<Transformer> transformers
├── finalize(Player, BuildStage)    — 对所有 transformer 调用 finalize()
└── volumeMultiplier()              — 所有 transformer 的 multiplier 乘积

Transformer interface
└── Operation transform(Operation operation)   ← 核心抽象
```

### 4 种 Transformer

| Transformer | 参数 | 变换逻辑 |
|-------------|------|----------|
| **ArrayTransformer** | offset(Vector3i), count(int) | 沿指定方向等距复制（如 /offset 1,1,1 count 4 → 4个格点排列） |
| **MirrorTransformer** | position(Vector3d), axis(MirrorAxis), size(int) | 沿X/Y/Z轴镜像对称复制 |
| **RadialTransformer** | position(Vector3d), axis(MirrorAxis), slices(int), radius(int), length(int) | 辐射状旋转复制（如 Y轴 8切片 → 八角形排列） |
| **ItemRandomizer** | entries(Map<Ingredient, WeightedEntry>) | 随机替换部分方块（按权重） |

### 默认初始值

```java
ArrayTransformer:  offset=(1,1,1), count=4
MirrorTransformer: position=(0,0,0), axis=X/Y/Z, size=16  (3个分别对应3个轴)
RadialTransformer: position=(0,0,0), axis=Y, slices=4, radius=16, length=128
ItemRandomizer:    预配置的物品列表（石块变种、木板变种等）
```

### 变换链执行顺序

```
原始 Operation
  → ArrayTransformer.transform (阵列复制)
  → MirrorTransformer[X].transform (X轴镜像)
  → MirrorTransformer[Y].transform (Y轴镜像)
  → MirrorTransformer[Z].transform (Z轴镜像)
  → RadialTransformer.transform (辐射复制)
  → flatten()
```

---

## 七、网络协议

### 通信架构

```
EffortlessNetworkChannel (继承 NetworkChannel<AllPacketListener>)
├── Client→Server: SessionPacket, SessionConfigPacket, PlayerCommandPacket,
│                  PlayerSettingsPacket, PlayerBuildPacket, PlayerPermissionCheckPacket
└── Server→Client: PlayerBuildTooltipPacket, PlayerSnapshotCapturePacket, PlayerSnapshotSharePacket
```

### 9 种数据包汇

| Packet | 方向 | 用途 |
|--------|------|------|
| SessionPacket | C→S | Session 创建/加入/离开 |
| SessionConfigPacket | C→S | Session 配置同步 |
| PlayerCommandPacket | C→S | UNDO/REDO 命令 |
| PlayerSettingsPacket | C→S | 玩家个人设置 |
| PlayerBuildPacket | C→S | 建筑操作上下文 |
| PlayerPermissionCheckPacket | C↔S | 权限检查请求/回复 |
| PlayerBuildTooltipPacket | S→C | 建筑统计提示 |
| PlayerSnapshotCapturePacket | S→C | 快照抓取请求 |
| PlayerSnapshotSharePacket | S→C | 快照分享 |

### 服务端处理逻辑

```java
switch (packet.command()) {
    case REDO  → getEntrance().getStructureBuilder().redo(player);
    case UNDO  → getEntrance().getStructureBuilder().undo(player);
    case BUILD → getEntrance().getStructureBuilder().onContextReceived(player, packet.context());
}
```

> ⚠️ 服务端的 `StructureBuilder` 是 **stub 实现**（方法体返回 null），真正逻辑全部在客户端。这表明 Effortless 是**以客户端逻辑为主、服务端仅做同步和权限校验**的设计。

---

## 八、Undo/Redo 系统

### OperationResultStack

```java
class OperationResultStack {
    Stack<OperationResult> undoStack;     // ← push(), pop()
    Stack<OperationResult> redoStack;     // ← push() 时清空

    void push(OperationResult result) {
        redoStack.clear();                // 新操作清空 redo
        undoStack.push(result);
    }

    OperationResult undo() {
        var op = undoStack.pop();
        var reverse = op.getReverseOperation();
        reverse.commit();                 // 执行逆操作
        redoStack.push(op);
        return reverse;
    }

    OperationResult redo() {
        var op = redoStack.pop();
        var reverse = op.getReverseOperation();
        reverse.commit();
        undoStack.push(op);
        return reverse;
    }
}
```

### 客户端 Undo/Redo 流程

```
用户按 Ctrl+Z / Ctrl+Y
  → 发送 PlayerCommandPacket(UNDO/REDO) 到服务端
  → 服务端 EffortlessStructureBuilder.undo()/redo()
  → 服务端处理（stub，实际效果依赖客户端同步）
```

> 实际 Undo 效果由客户端侧的 `OperationResultStack` 管理，服务端仅用于跨玩家同步。

---

## 九、渲染系统

### OutlineRenderer

管理 `Map<Object, OutlineEntry>`，4 种渲染类型：

| 方法 | 用途 | 实现类 |
|------|------|--------|
| showLine() | 射线/线条 | SimpleLineOutline |
| showBoundingBox() | 边界框 | BoundingBoxOutline |
| chaseBoundingBox() | 平滑过渡边界框 | ChasingBlockBoundingBoxOutline |
| showCluster() | 方块簇集合 | BlockClusterOutline |
| endChasingLine() | 结束过渡动画 | - |

所有 OutlineEntry 有 `FADE_TICKS = 10` tick 的淡出过渡。

### PatternRenderer

根据 `context.pattern().transformers()` 选择渲染器：

| Transformer | 渲染器 | 效果 |
|-------------|--------|------|
| MirrorTransformer | MirrorTransformerRenderer | 镜像轴辅助线 |
| ArrayTransformer | ArrayTransformerRenderer | 阵列点阵 |
| RadialTransformer | RadialTransformerRenderer | 辐射轴线 |

### BlockOperationRenderer

将方块簇按操作类型分组渲染（颜色编码）：
- 放置（绿/白）
- 破坏（红）
- 交互（黄）
- 复制（蓝）

---

## 十、BuildMode 系统

### 14 种模式，5 个分类

```java
enum BuildMode {
    // BASIC  (蓝 #007FFF80)
    DISABLED, SINGLE,

    // SQUARE (橙 #FF8A3D80)
    LINE      (→ LINE_DIRECTION),    // 方向
    WALL,
    FLOOR,
    CUBOID    (→ CUBE_FILLING, PLANE_FACING, PLANE_LENGTH),

    // DIAGONAL (紫 #9047DE80)
    DIAGONAL_LINE,
    DIAGONAL_WALL,
    SLOPE_FLOOR (→ RAISED_EDGE),

    // CIRCULAR (绿 #4AC24D80)
    CIRCLE      (→ CIRCLE_START, PLANE_FILLING, PLANE_FACING, PLANE_LENGTH),
    CYLINDER    (→ PLANE_FILLING, PLANE_FACING, PLANE_LENGTH),
    SPHERE      (→ PLANE_FILLING),

    // ROOF (黄 #D4DE3B80)
    PYRAMID,
    CONE,
}
```

### BuildFeature（模式选项）

每个 BuildMode 关联一组 BuildFeatures，控制用户可调节的参数：

| Feature | 作用 |
|---------|------|
| LINE_DIRECTION | 直线方向（X/Z/对角线） |
| CUBE_FILLING | 立方体填充（实心/空心） |
| PLANE_FACING | 平面朝向（水平/垂直） |
| PLANE_LENGTH | 平面长度 |
| CIRCLE_START | 圆形起始点 |
| RAISED_EDGE | 斜坡边缘 |
| PLANE_FILLING | 平面填充（空心/实心） |

### Structure（操作计算）

每种 BuildMode 对应一个 Structure 实现类，负责**根据玩家射线位置计算具体操作集**：

```java
interface Structure {
    Stream<BlockInteraction> collect(StructureBuilderContext context);
}
```

14 个 Structure 实现：`Disabled`, `Single`, `Line`, `Wall`, `Floor`, `Cuboid`, `DiagonalLine`, `DiagonalWall`, `SlopeFloor`, `Circle`, `Cylinder`, `Sphere`, `Pyramid`, `Cone`。

---

## 十一、Clipboard 系统 (building/clipboard/)

| 类 | 职责 |
|----|------|
| Clipboard | 核心接口：`snapshot()`, `push()`, `invalidate()` |
| ClipboardSnapshot | 不可变快照：`blockData()` → `Map<BlockPos, BlockState>` + `entityData()` |
| ClipboardBuilder | Builder 模式构建 Clipboard（支持追加/移除方块） |
| BlockData | `Map<BlockPos, Pair<BlockState, CompoundTag>>` 的包装 |
| ClipboardManager | 管理多个 Clipboard（UUID 索引） |

Clipboard 用于 COPY → PASTE 流程：
```
COPY: 射线选择区域 → BlockData 构建 → ClipboardBuilder → snapshot()
PASTE: clipboard.snapshot().blockData() → map(Map.Entry → createBlockPlaceOperation) → BatchOperation
```

---

## 十二、Replace 系统 (building/replace/)

| 类 | 类型 | 行为 |
|----|------|------|
| ReplaceMode | enum | DISABLED / BLOCKS_AND_AIR / BLOCKS_ONLY / OFFHAND_ONLY |
| ReplacePredicate | interface | 4 种实现对应 4 种模式 |
| ReplaceFilter | 辅助 | 根据 ReplaceMode 过滤可替换方块 |

Replace 策略在 `BlockStateUpdateOperation.updateBlock()` 中约第 8 步检查：
- **DISABLED**（默认）：仅替换 `replaceable` 属性为 true 的方块（如草、水）
- **BLOCKS_AND_AIR**：所有位置均可替换（含空气）
- **BLOCKS_ONLY**：仅替换已有方块的格子（不会在空气中放置）
- **OFFHAND_ONLY**：仅在副手物品相同时替换

---

## 十三、Session 系统 (session/ + building/session/)

| 类 | 职责 |
|----|------|
| SessionManager | 服务端核心：管理所有 Session 的生命周期 |
| Session | 持有 Player、配置、成员列表、权限信息 |
| SessionConfig | Session 级配置（最高优先级） |
| PlayerSettings | 玩家级配置（次优先级） |

配置读取优先级：`SessionConfig > PlayerSettings > ClientConfig`

### Session 生命周期

```
SessionManager.createSession()
  → SessionManager.addMember(sessionId, player)
  → Player 加入后同步 SessionConfig
  → 多人协同：多个玩家共享同一个 Session → 共享建筑操作
  → SessionManager.removeSession() → 清理资源
```

---

## 十四、配置系统 (building/config/)

| Config 类 | 用途 | 序列化器 |
|-----------|------|----------|
| BuilderConfig | 当前 BuildMode + 模式参数 | BuilderConfig.Serializer |
| ClientConfig | 客户端全局设置 | ClientConfig.Serializer |
| ClipboardConfig | 剪贴板行为 | ClipboardConfig.Serializer |
| PatternConfig | Pattern 开关 + transformer 参数 | PatternConfig.Serializer |
| PassiveConfig | 被动模式设置 | PassiveConfig.Serializer |
| RenderConfig | 渲染效果设置 | RenderConfig.Serializer |

所有 Config 实现了 `Config` 接口（一个简单的读写接口），使用自定义序列化器（非标准 Forge Config，而是 Effortless 自有的 JSON 序列化）。

---

## 十五、拦截器系统 (building/interceptor/)

用于检测和避免与其他建筑模组的冲突：

| 拦截器 | 目标模组 | 作用 |
|--------|----------|------|
| OpenPacInterceptor | Open Parties & Claims | 检查领地权限 |
| FtbChunksInterceptor | FTB Chunks | 检查领地权限 |
| PermissionInterceptor | 自身权限系统 | 检查玩家建筑权限 |
| AbstractInterceptor | 基类 | 模板方法模式 |

---

## 十六、集成策略分析

### 核心挑战：universal.api 依赖

Effortless 全部 227 个文件均依赖 `dev.huskuraft.universal.api` 抽象层，**不直接使用 Forge API**。这个抽象层包装了：
- `ResourceLocation`, `BlockPos`, `BlockState`, `ItemStack` 等 MC 基础类型
- `Player`, `Level`, `InteractionHand` 等游戏对象
- `Text`, `Component` 等 UI 组件

### 三选一方案

| 方案 | 工作量 | 可维护性 | 推荐度 |
|------|--------|----------|--------|
| **A. 嵌入 universal.api** | 大（需完整移植抽象层） | ⭐⭐⭐ | ⭐ **（推荐）** |
| **B. 重写 Forge API** | 极大（逐类替换 import） | ⭐⭐ | ❌ |
| **C. 混合方案** | 中（部分模块重写） | ⭐⭐ | 备用 |

**方案 A 详解**：
1. 将 `dev.huskuraft.universal.api` 整体复制到项目（包名改姓）
2. 实现 6-8 个核心抽象接口的 Forge 绑定层
3. Effortless 的业务代码几乎**无需修改**即可运行
4. 后续升级 Effortless 版本时，只需更新绑定层

### 对 MetroGenesis 的映射

| Effortless 概念 | MetroGenesis 潜在映射 |
|-----------------|----------------------|
| BuildMode (14种) | ZoneShape (矩形/六边形/圆形分区) |
| Structure.collect() | ZoneBlueprint.getBlockPositions() |
| Pattern (Array/Mirror/Radial) | 分区排列模板 |
| Clipboard → Snapshot | Blueprint → BuildPlan |
| Operation/UndoRedo | Zoning操作的历史撤销 |
| OutlineRenderer | 已有的 HologramRenderer |
| Session/协作 | 多人规划协作 |

---

## 十七、Phase 0 实施路线图

如果决定集成 Effortless 的 BuildMode/Structure 系统到 MetroGenesis：

```
Step 1: 复制 universal.api 核心 → com.metrogenesis.effortless.api（~30个接口）
Step 2: 实现 Forge 绑定层（ResourceLocation/BlockPos/Player/Level 适配）
Step 3: 复制 building/structure/ + building/pattern/ → com.metrogenesis.effortless
Step 4: 复制 building/operation/block/ → 精简为只保留 UpdateOperation + AirUpdateOperation
Step 5: 连接 MetroGenesis ZoneSystem：ZoneTypes → BuildMode 映射
Step 6: 替换渲染层：OutlineRenderer → 已有的 HologramRenderer（减少依赖）
```

---

## 附录：关键文件阅读清单

| 序号 | 文件 | 行数 | 重要性 | 已读状态 |
|------|------|------|--------|----------|
| 1 | EffortlessClient.java | ~80 | ⭐⭐⭐ | ✅ |
| 2 | EffortlessClientStructureBuilder.java | 799 | ⭐⭐⭐ | ✅（全文） |
| 3 | BatchBuildSession.java | ~200 | ⭐⭐⭐ | ✅ |
| 4 | BuildState.java | ~80 | ⭐⭐⭐ | ✅ |
| 5 | BuildMode.java | ~300 | ⭐⭐⭐ | ✅ |
| 6 | Pattern.java | ~100 | ⭐⭐⭐ | ✅ |
| 7 | Transformer.java | ~200 | ⭐⭐⭐ | ✅ |
| 8 | BlockStateUpdateOperation.java | 302 | ⭐⭐⭐ | ✅ |
| 9 | OperationResultStack.java | ~80 | ⭐⭐ | ✅ |
| 10 | EffortlessNetworkChannel.java | ~200 | ⭐⭐⭐ | ✅ |
| 11 | OutlineRenderer.java | ~200 | ⭐⭐ | ✅ |
| 12 | Clipboard.java | ~50 | ⭐⭐ | ✅ |
| 13 | ReplaceMode.java | ~50 | ⭐⭐ | ✅ |
| 14 | SessionManager.java | ~150 | ⭐⭐ | ✅ |
| 15 | Structure (14个实现) | 各~50-150 | ⭐⭐⭐ | ✅ |
| 16 | ArrayTransformer.java | ~100 | ⭐⭐ | ✅ |
| 17 | MirrorTransformer.java | ~100 | ⭐⭐ | ✅ |
| 18 | RadialTransformer.java | ~150 | ⭐⭐ | ✅ |

---

> 报告生成日期：2026-06-02 18:35
> 源码版本：Huskuraft/Effortless (Forge 1.20.1)
