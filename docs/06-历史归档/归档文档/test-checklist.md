# MetroGenesis 手动测试清单

版本: 0.0.1 (构建时间: 2026-06-05 04:39)

## P0 — 核心功能（必须通过）

- [ ] `/cvalue` 命令可用
- [ ] RTS 相机（管理书内 WASD 移动 + 滚轮缩放）
- [ ] 区块区域绘制（左键拖拽画矩形）
- [ ] 道路绘制（R 键进入道路模式 → 拖拽 → 右键确认）
- [ ] 建筑蓝图选择（T 键打开图鉴 → 选择 → 放置）
- [ ] 区块数据持久化（放置区域 → 退出管理书 → 重进 → 区域还在）
- [ ] 构建工具（`/building_tool` 拿物品 → 右键地面打开建筑选择窗口）

## P1 — 重要功能

- [ ] 吸附节点高亮（Shift 约束 45°）
- [ ] 弯道道路（滚轮调整曲率）
- [ ] 图鉴分类浏览
- [ ] 区块撤销

## P2 — 边缘情况

- [ ] 重复区域覆盖
- [ ] 大范围区域（100+ 格）性能
- [ ] 跨维度测试

---

## 构建验证记录

| 检查项 | 状态 | 说明 |
|--------|------|------|
| `gradlew build` | ✅ | BUILD SUCCESSFUL，0 错误 |
| Deprecation 警告 | ✅ | 无源代码级 deprecation 警告（仅有 ForgeGradle Gradle API deprecation, 不影响运行） |
| Release jar 路径 | ✅ | `build/libs/metrogenesis-0.0.1.jar` (68MB) |
| `scan_tool` 模型引用 | ✅ | 已从注册中移除（行注释），无残留模型/纹理文件，不会出现紫黑块 |
| `building_tool` 模型引用 | ✅ | BUILDING_TOOL 已注册 (building_tool → BuildingToolItem::new)，纹理/模型路径正确 |

## 已知修复

| 版本 | 问题 | 修复 |
|------|------|------|
| v0.0.1-hotfix | 旧道路被新道路覆盖 | `placeRoad()` 中间位置增加已有道路方块检查，跳过已铺路格子 |
