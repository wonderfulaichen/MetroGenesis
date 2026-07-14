# MetroGenesis 文档快速索引

> 最后更新：2026-07-15

---

## 快速查找

| 我想了解... | 看这里 |
|-------------|--------|
| 项目是什么 | `01-架构设计/设计方案.md` |
| 技术怎么选型 | `01-架构设计/架构决策记录.md` |
| UI 怎么设计 | `01-架构设计/UI设计规范.md` |
| 道路系统设计 | `01-架构设计/design/road/道路系统设计.md` |
| 区域生长设计 | `02-功能设计/区域生长系统.md` |
| 用户需求 | `02-功能设计/用户需求记录.md` |
| 开发进度 | `03-实施计划/开发路线图.md` |
| 项目现状 / 吸收度 / 功能完成度 | `03-实施计划/现状与审计总览.md` |
| Bug 修复记录 | `04-修复记录/诊断修复报告.md` |
| 分析报告 | `05-分析诊断/analysis/` |
| 测试文档 | `05-分析诊断/测试/` |
| 项目历史 | `06-历史归档/交接文档/` `06-历史归档/会话记录/` |

---

## 目录结构

```
docs/
├── 01-架构设计/              # 架构、设计、规范、道路设计
│   ├── 架构决策记录.md
│   ├── 设计方案.md
│   ├── UI设计规范.md          # 当前（v1.2）
│   ├── UI设计规范_旧版.md     # 废弃
│   ├── 参考项目笔记.md
│   └── design/road/道路系统设计.md
├── 02-功能设计/
│   ├── 区域生长系统.md
│   └── 用户需求记录.md
├── 03-实施计划/
│   ├── 开发路线图.md
│   └── 现状与审计总览.md      # 整合三份审计
├── 04-修复记录/
│   └── 诊断修复报告.md
├── 05-分析诊断/
│   ├── analysis/
│   └── 测试/
├── 06-历史归档/
│   ├── 交接文档/
│   ├── 会话记录/
│   └── 归档文档/             # 含已合并的三份审计
├── README.md
└── INDEX.md
```

---

## 文档版本

| 文档 | 版本 | 最后更新 |
|------|------|----------|
| 现状与审计总览 | v1.0（整合）| 2026-07-14 |
| 道路系统设计 | v1.0（重建）| 2026-07-14 |
| 设计方案 | v2.2 | 2026-06-26 |
| UI 设计规范 | v1.2 | 2026-07-08 |
| 开发路线图 | v3.0 | 2026-07-08 |
| 用户需求记录 | v1.0 | 2026-07-14 |

---

## 参考库索引（`参考/`）

只读参考模组源码库（不参与编译），共 **18** 个来源约 **304 MB**。详细类目映射与体量见 [`参考/README.md`](../参考/README.md)。

| 类目 | 包含来源（原名） |
|------|------------------|
| 城市建造/殖民地核心 | minecolonies-version-main、Architect-s-Dream-main、RTSbuilding-source |
| 建筑方块/装饰/家具 | DomumOrnamentum-source、FurnitureRefurbished-source、Effortless |
| 结构/蓝图基础设施 | Structurize-source |
| 道路 | RoadArchitect-1.20.1-multiloader |
| 市民/AI/对话/城市模拟 | New-Sim-U-Kraft、TownTalk-source、simcity-threejs-clone、citybound、OpenGlassBox |
| 工业/机械/管道 | BuildCraft-8.0.x-1.12.2、Piston-Unlimited-version-main |
| 交通/运输模拟 | OpenTTD |
| 动画/渲染 | geckolib-1.20.1 |
| 餐饮/生产 | OrdertoCook-main |

> 其中 🟢 已嵌入工程：`minecolonies` / `structurize` / `domumornamentum`（改包名 `com.metrogenesis.*`）；其余为消化灵感，算法类（citybound/simcity/OpenGlassBox/OpenTTD）多非 MC 实现。

---

## 相关资源

- **项目根目录**：`forge-1.20.1-47.4.10-mdk/`（Minecraft Forge 模组源码）
- **参考模组**：`参考/`（[类目索引](./参考/README.md)，MineColonies、Structurize 等，只读）
- **概念图**：`generated-images/`（UI 概念图、设计稿）
