# MetroGenesis — 智能体指南

## 项目概述

Minecraft Forge 1.20.1 模组：城市建设即时战略游戏。MODID: `metrogenesis`，包名: `com.metrogenesis`，Java 17。

## 构建与运行

所有命令在 `forge-1.20.1-47.4.10-mdk/` 目录下执行：

```bash
cd forge-1.20.1-47.4.10-mdk
./gradlew build --no-daemon      # 编译 + 打 jar 包
./gradlew runClient              # 启动开发客户端
./gradlew runServer              # 启动开发服务端
```

Windows 用户使用 `gradlew.bat` 代替 `./gradlew`。需要设置 `JAVA_HOME` 为 JDK 17。

## 关键构建约束

`build.gradle` 排除了大量包的编译（第 123-148 行）。这些是**故意跳过的**——不要在不理解排除原因的情况下修复或重新包含。主要排除的包：

- `com/metrogenesis/client/fakelevel/**`
- `com/metrogenesis/config/**`
- `com/metrogenesis/data/**`、`datagen/**`、`event/**`
- `com/metrogenesis/entity/client/**`
- `com/metrogenesis/job/**`、`workorder/**`
- 多个单独文件（TownHallBlock、TownHallMenu、CitizenInteractionScreen 等）

排除的代码是正在逐步淘汰的旧版自研代码。如果需要添加新代码，请放在未被排除的包中。

## 嵌入式依赖（不要随意修改）

MineColonies、Structurize、DomumOrnamentum 和 BlockUI 的源代码**嵌入**在项目中，位于 `com.metrogenesis.*` 下（已重命名包名）。共 **2,622 个第三方代码文件**（minecolonies: 2,091 + structurize: 222 + domum: 226 + blockui: 83），已经过分叉和重命名。

| 原始包名 | 嵌入后包名 |
|---------|----------|
| `com.minecolonies` | `com.metrogenesis.minecolonies` |
| `com.ldtteam.structurize` | `com.metrogenesis.structurize` |
| `com.ldtteam.domumornamentum` | `com.metrogenesis.domumornamentum` |
| `com.ldtteam.blockui` | `com.metrogenesis.blockui` |

**`Constants.MOD_ID` 必须保持 `"minecolonies"`** ——修改会导致 `NoClassDefFoundError`，因为 Forge 事件总线会扫描继承链。

## 项目结构

```
forge-1.20.1-47.4.10-mdk/src/main/java/com/metrogenesis/
├── MetroGenesis.java          # 主模组类
├── Config.java                # Forge 配置
├── road/                      # 道路系统（贝塞尔曲线、放置、撤销、存储）
├── blueprint/                 # 蓝图系统（v1 Blueprint + v2 StandardBlueprintData）
├── catalog/                   # 建筑图鉴（BuildingCatalogEntry/Scanner/CategoryMapper）
├── citizen/                   # 市民系统（CitizenData、job/、types/）
├── colony/                    # 殖民地管理（ColonyState、DailyScheduler、managers/）
├── construction/              # 建设系统（ConstructionManager/Site、Zone）
├── core/economy/              # C-Value 经济引擎（CValue、EconomyEngine、MarketData）
├── command/                   # /cvalue 命令
├── block/                     # 自定义方块（construction/、facility/）
├── item/                      # 物品（mayor_book、blueprint_eye、building_tool）
├── gui/                       # 界面（MayorBook、BuildTool、Catalog、CityMap 等）
├── network/                   # 网络包处理器
├── entity/                    # RTS 摄像机实体、市民实体
├── hologram/                  # 蓝图预览全息投影
├── init/                      # 方块/物品注册辅助（BuildingType、ModJobs）
├── layergrid/                 # 区块图层网格覆盖
├── util/                      # 工具类（BlueprintUtil、GeometryUtils 等）
├── minecolonies/              # 嵌入的 MineColonies（2,091 文件）
├── structurize/               # 嵌入的 Structurize（222 文件）
├── domumornamentum/           # 嵌入的 DomumOrnamentum（226 文件）
└── blockui/                   # 嵌入的 BlockUI（83 文件）
```

排除编译的遗留包：`config/`、`data/`、`client/fakelevel/`、`workorder/`

## 参考与备份目录

- `参考/` —— 参考模组源码（MineColonies、Structurize、Effortless 等）。**只读，不参与编译。** 类目索引见 [`参考/README.md`](参考/README.md)。
- `备份_poliscraft_v1_自研版/` —— 旧版 poliscraft 模组备份。不属于构建的一部分。
- `docs/` —— 架构、设计、计划、参考文档。

## 可复用的关键基础设施

来自嵌入的 Structurize：
- `RotationMirror` —— 8 种旋转/镜像状态枚举
- `ChangeStorage` —— 方块快照，用于撤销/重做
- `PlaceStructureOperation` —— 多 tick 放置管线
- `StructurePacks.getBlueprint()` —— 从数据包加载蓝图
- `Manager` —— 基于 tick 的操作队列

## UI 风格

Create 像素工业风（极深色背景+黄铜细线+像素艺术+绿色荧光粒子），参考 Factorio/Create 与 Cities: Skylines 的城建信息架构。完整配色、质感、组件规范见 `docs/UI重建设计规范_v1.md`（v1 设计基线）。`docs/UI_DESIGN.md` 为早期科技蓝风格、**已废弃**，勿再引用。

## Access Transformers

文件：`META-INF/accesstransformer_minecolonies.cfg` —— 为嵌入代码将 Minecraft 私有字段/方法设为公开。新增条目放在文件末尾并加注释。

## 常见陷阱

1. **MayorBookCatalogPanel.java 编译错误** —— `structurize.blueprints.v1.Blueprint` 和 `blueprint.v1.Blueprint` 类型不匹配。（已修复：改用 structurize Blueprint 导入。）
2. **Java 版本不匹配** —— 构建需要 JDK 17，但某些工具可能使用 JDK 21。验证 `JAVA_HOME` 或 `gradle.properties` 中的 `org.gradle.java.installations.fromEnv=JAVA17_HOME`。
3. **重复模组错误** —— 不要为 structurize/domumornamentum/blockui 添加 `fg.deobf()` 依赖（它们已嵌入）。唯一外部依赖是 JEI（`compileOnly`）。
4. **Access Transformer 文件名** —— AT 文件名为 `accesstransformer_minecolonies.cfg`（非默认名）。在 `build.gradle` 第 56 行引用。
