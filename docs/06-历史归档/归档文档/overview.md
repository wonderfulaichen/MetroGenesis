# MetroGenesis Phase 4 完成报告

## 汇总

Phase 4 目标：构建 release jar + 填补资产漏洞 + 准备手动测试清单。

## 完成事项

### 1. BuildingToolItem 注册 🐛

`BuildingToolItem` 类已存在（`com.metrogenesis.item.BuildingToolItem`）且有完整中文/英文 lang 条目和纹理文件，但**从未注册**到 ITEMS 注册表。

**改动：** `MetroGenesis.java` — 新增 `BUILDING_TOOL` RegistryObject + import

```java
public static final RegistryObject<Item> BUILDING_TOOL =
    ITEMS.register("building_tool", BuildingToolItem::new);
```

**影响：** 现在 `/building_tool` 可以拿到物品，右键地面打开建筑选择窗口。

### 2. Lang 键名修正 🏷️

发现 `zh_cn.json` 和 `en_us.json` 中大量条目使用了 `block.MetroGenesis.xxx`、`item.MetroGenesis.building_tool` 等大写 M 前缀。Forge 自动 lang 查找的键名格式是 `block.<小写modid>.<注册名>`，大写 M 不会被匹配，导致游戏内显示 raw key。

**修改范围：** 两项 lang 文件中的所有旧格式键名
- `block.MetroGenesis.*` → `block.metrogenesis.*`
- `container.MetroGenesis.*` → `container.metrogenesis.*`
- `item.MetroGenesis.building_tool` → `item.metrogenesis.building_tool`
- `MetroGenesis.gui.*` → `gui.metrogenesis.*`

共修复 ~12 个条目/文件，新代码（mayor_book / blueprint_eye）已使用正确格式，未受影响。

### 3. Release Jar

- `build/libs/metrogenesis-0.0.1.jar` — **68 MB**, BUILD SUCCESSFUL
- 0 编译错误，0 我方代码 deprecation 警告

### 4. 手动测试清单

`docs/test-checklist.md` — P0/P1/P2 三级测试清单，含构建验证表。

## 构建状态

| 项 | 状态 |
|---|---|
| Phase 0 (架构/选框/修复) | ✅ |
| Phase 1 (道路系统) | ✅ |
| Phase 2a (GUI 统一基座) | ✅ |
| Phase 2b (市长管理书图鉴分离) | ✅ |
| Phase 3 (LayerGrid 核心) | ✅ |
| **Phase 4 (构建/补贴图/测试计划)** | **✅** |

## 后续建议

1. **加载 jar 到 Minecraft 1.20.1 进行运行时测试** — 按测试清单逐项验证
2. 给 building_tool / mayor_book 做实际纹理（目前是简单 placeholder）
3. 道路模板选择 UI（目前材质硬编码）
4. MayorBookScreen 3D 视图升级到 BlockUI
