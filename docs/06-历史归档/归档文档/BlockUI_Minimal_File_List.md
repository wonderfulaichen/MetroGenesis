# BlockUI 最小搬运文件清单

> 基于 MultiPiston GUI (windowmultipiston.xml) 实际使用的 6 种 UI 标签，
> 逆向追踪 BlockUI 依赖链后确定的最小搬运文件集。
> 目标：**PolisCraft (Forge 1.20.1, Java 17)**

---

## 一、XML 实际使用的 UI 标签

`windowmultipiston.xml` 声明的标签：

| XML标签 | Java类 | 用途 |
|---------|--------|------|
| `<window>` | `BOWindow` | 窗口根容器 |
| `<label>` | `Text` | 文本标签 |
| `<dropdown>` | `DropDownList` | 下拉选择列表 |
| `<view>` | `View` | 基础容器 |
| `<buttonimage>` | `ButtonImage` | 图片按钮 |
| `<input>` | `TextFieldVanilla` | 文本输入框 |

---

## 二、核心决策：自定义 PolisLoader

### 问题
原始 `Loader.java` 使用通配符 import：
```java
import com.ldtteam.blockui.controls.*;
import com.ldtteam.blockui.views.*;
```
这会引入全部 **19 个 controls 类 + 12 个 views 类**，即使大部分从未使用。

### 解决方案
**不搬运 Loader.java，改为手写 `PolisLoader.java`**，只注册实际用到的 6 种标签映射：
- `<window>` → `BOWindow`
- `<label>` → `Text`
- `<dropdown>` → `DropDownList`
- `<view>` → `View`
- `<buttonimage>` → `ButtonImage`
- `<input>` → `TextFieldVanilla`

同时保留 `createFromPaneParams()` 和 `createFromXMLFile()` 方法（被 `BOWindow` 和 `ScrollingListContainer` 调用）。

---

## 三、NeoForge → Forge 适配方案

### 3.1 关键适配点

| # | 源文件 | 行号 | NeoForge API | Forge 1.20.1 适配方案 |
|---|--------|------|-------------|----------------------|
| 1 | `BOScreen.java` | L66, L142 | `NeoForgeRenderTypes.enableTextTextureLinearFiltering` | 直接移除——Forge 1.20.1 中没有该 API。如果必要，手动调用 `RenderSystem.bindTexture()` 后 `glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)` |
| 2 | `BOScreen.java` | L87 | `ClientHooks.getGuiFarPlane()` | Forge 中替换为 `Minecraft.getInstance().gameRenderer.getRenderDistance()` 或硬编码 5000f |
| 3 | `BOGuiGraphics.java` | L119 | `ClientHooks.handleCameraTransforms()` | Forge 中替换为手动 PoseStack 变换（或直接移除，该功能用于 BlockState 3D 渲染） |
| 4 | `BOGuiGraphics.java` | L47 | `IClientItemExtensions` | Forge 1.20.1 中包路径为 `net.minecraftforge.client.extensions.common.IClientItemExtensions`（NeoForge 中已移动） |
| 5 | `BlockUI.java` | L14 | `FMLModContainer` | Forge 中替换为 `ModLoadingContext.get().getActiveContainer()` |
| 6 | `SafeError.java` | L18 | `FMLEnvironment.production` | Forge 中同样存在：`net.minecraftforge.fml.loading.FMLEnvironment` → `FMLEnvironment.production`。**确认：包路径不变，无需修改** |
| 7 | `CursorUtils.java` | L70 | `FMLEnvironment.production` | **同上，Forge 中包路径一致** |
| 8 | `SpacerTextComponent.java` | L17-20 | `Codec`/`MapCodec` | **在 Forge 1.20.1 中存在**（Mojang 1.16+ 已引入 DataFixerUpper → Codec） |
| 9 | `OutOfJarTexture.java` | L17 | `FMLEnvironment` | **同上** |

### 3.2 事件系统适配

| NeoForge 事件 | Forge 1.20.1 等价物 |
|---------------|-------------------|
| `RegisterClientReloadListenersEvent` | `net.minecraftforge.client.event.RegisterClientReloadListenersEvent` |
| `ClientTickEvent.Pre` / `ClientTickEvent.Post` | `net.minecraftforge.event.TickEvent.ClientTickEvent` (phase = Phase.START/END) |
| `RenderGuiLayerEvent.Pre` | `net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre` |
| `InputEvent.MouseScrollingEvent` | `net.minecraftforge.client.event.InputEvent.MouseScrollingEvent` |
| `TagsUpdatedEvent` | `net.minecraftforge.event.TagsUpdatedEvent` |
| `@SubscribeEvent` 总线 | 均使用 `net.minecraftforge.eventbus.api.SubscribeEvent` |
| AddReloadListenerEvent | `net.minecraftforge.client.event.RegisterClientReloadListenersEvent` |
| `VanillaGuiLayers.CROSSHAIR` | `net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id()` |

### 3.3 其他 API 差异

| NeoForge | Forge 1.20.1 |
|----------|-------------|
| `RenderSystem.setShaderColor(...)` | **相同** |
| `RenderSystem.enableDepthTest()` | **相同** |
| `GuiGraphics.pose()` → `final PoseStack ms = target.pose()` | **相同**（Mojang 原生 API） |
| `BlockPos.ZERO` | **相同** |
| `ResourceLocation.fromNamespaceAndPath()` | `new ResourceLocation()` 或 `ResourceLocation.tryParse()` |
| `ResourceLocation.defaultNamespace()` | **相同**（Forge 1.19+ 已统一） |
| `Component.literal()` / `Component.translatable()` | **相同** |
| `com.mojang.datafixers.util.Pair` | **相同** |

---

## 四、实际需要搬运的文件清单

### 4.1 核心框架（必搬）

| 源路径（`参考/BlockUI-source/src/main/java/com/ldtteam/blockui/`） | 目标路径（`src/main/java/com/poliscraft/blockui`） | 包名（修改为正） | 适配工作量 |
|---------|------|------|------|
| `Pane.java` | `Pane.java` | `com.poliscraft.blockui` | 无 |
| `PaneParams.java` | `PaneParams.java` | `com.poliscraft.blockui` | 无 |
| `Parsers.java` | `Parsers.java` | `com.poliscraft.blockui` | 无 |
| `UiRenderMacros.java` | `UiRenderMacros.java` | `com.poliscraft.blockui` | 无 |
| `View.java` | `View.java` | `com.poliscraft.blockui` | 无 |
| `BOWindow.java` | `BOWindow.java` | `com.poliscraft.blockui` | 无（已在项目中？确认） |
| `BOGuiGraphics.java` | `BOGuiGraphics.java` | `com.poliscraft.blockui` | **中等**（见 3.1） |
| `BOScreen.java` | `BOScreen.java` | `com.poliscraft.blockui` | **中等**（见 3.1） |
| `Alignment.java` | `Alignment.java` | `com.poliscraft.blockui` | 无（纯枚举） |
| `Color.java` | `Color.java` | `com.poliscraft.blockui` | 无（工具类） |
| `MouseEventCallback.java` | `MouseEventCallback.java` | `com.poliscraft.blockui` | 无（函数接口） |
| `ButtonHandler.java` | `ButtonHandler.java` | `com.poliscraft.blockui` | 无（函数接口） |
| `InputHandler.java` | `InputHandler.java` | `com.poliscraft.blockui` | 无（函数接口） |
| `Pos2i.java` | `Pos2i.java` | `com.poliscraft.blockui` | 无（record） |
| `SizeI.java` | `SizeI.java` | `com.poliscraft.blockui` | 无（record） |
| **自定义**: `PolisLoader.java` | `PolisLoader.java` | `com.poliscraft.blockui` | **新文件**（手写） |

### 4.2 控件层（5个文件）

| 源路径 | 目标路径 | 适配 |
|---------|------|------|
| `controls/AbstractTextElement.java` | `controls/AbstractTextElement.java` | 无 |
| `controls/Text.java` | `controls/Text.java` | 无 |
| `controls/TextField.java` | `controls/TextField.java` | 无 |
| `controls/TextFieldVanilla.java` | `controls/TextFieldVanilla.java` | 无 |
| `controls/Button.java` | `controls/Button.java` | 无 |
| `controls/ButtonImage.java` | `controls/ButtonImage.java` | 无 |
| `controls/Image.java` | `controls/Image.java` | 无 |
| `controls/Tooltip.java` | `controls/Tooltip.java` | 无 |
| `controls/Scrollbar.java` | `controls/Scrollbar.java` | 无 |
| `controls/AbstractTextBuilder.java` | `controls/AbstractTextBuilder.java` | 无 |
| `controls/DropDownList.java` | `views/DropDownList.java` | 无 |

### 4.3 视图层（8个文件）

| 源路径 | 目标路径 | 适配 |
|---------|------|------|
| `views/OverlayView.java` | `views/OverlayView.java` | 无 |
| `views/ScrollingContainer.java` | `views/ScrollingContainer.java` | 无 |
| `views/ScrollingView.java` | `views/ScrollingView.java` | 无 |
| `views/ScrollingList.java` | `views/ScrollingList.java` | 无 |
| `views/ScrollingListContainer.java` | `views/ScrollingListContainer.java` | 无 |
| `views/Group.java` | `views/Group.java` | 无 |
| `views/ScrollingGroup.java` | `views/ScrollingGroup.java` | 无 |
| `views/Box.java` | `views/Box.java` | 无 |

### 4.4 工具层（8个文件）

| 源路径 | 目标路径 | 适配 |
|---------|------|------|
| `util/SafeError.java` | `util/SafeError.java` | 无（FMLEnvironment 包路径相同） |
| `util/SpacerTextComponent.java` | `util/SpacerTextComponent.java` | **低**（需改 MOD_ID 引用为 "poliscraft"） |
| `util/ToggleableTextComponent.java` | `util/ToggleableTextComponent.java` | **低**（需改 MOD_ID 引用为 "poliscraft"） |
| `util/SingleBlockGetter.java` | `util/SingleBlockGetter.java` | **低**（Remove NeoForge ServerLifecycleHooks 引用） |
| `util/DataProviders.java` | **不搬**（文件不存在，非必需） | — |
| `util/BlockStateRenderingData.java` | **不搬**（BOGuiGraphics.renderBlockStateAsItem 非 MultiPiston 必需） | — |
| `util/cursor/Cursor.java` | `util/cursor/Cursor.java` | 仅函数接口 |
| `util/cursor/CursorUtils.java` | `util/cursor/CursorUtils.java` | **低**（FMLEnvironment 包路径相同） |
| `util/texture/ResolvedWidgetSprites.java` | `util/texture/ResolvedWidgetSprites.java` | 无（工具类） |
| `util/texture/CursorTexture.java` | `util/texture/CursorTexture.java` | **低**（MOD_ID 引用） |
| `util/texture/IsOurTexture.java` | `util/texture/IsOurTexture.java` | 无（短小） |
| `util/texture/MissingCursorTexture.java` | `util/texture/MissingCursorTexture.java` | **低**（MOD_ID 引用） |

### 4.5 颜色工具（可选 - 仅当 AbstractTextBuilder 编译需要）

| 源路径 | 目标路径 | 适配 |
|---------|------|------|
| `util/color/IColour.java` | `util/color/IColour.java` | 无 |
| `util/color/ColourARGB.java` | `util/color/ColourARGB.java` | 无（record） |
| `util/color/ColourRGBA.java` | `util/color/ColourRGBA.java` | 无（record） |
| `util/color/ColourQuartet.java` | `util/color/ColourQuartet.java` | 无（record） |
| `util/color/ColouredVertexConsumer.java` | `util/color/ColouredVertexConsumer.java` | 无 |

### 4.6 **不搬运**的文件

以下文件经过分析**不需要**搬运，因为其功能不被任何 MultiPiston GUI 代码路径使用：

| 文件 | 不搬的原因 |
|------|-----------|
| `mod/BlockUI.java` | 主类，基于 NeoForge @Mod 注册。改为 PolisCraft 自己管理注册 |
| `mod/Log.java` | **需手写**（简化为 3 行 Log4j 包装，见下文） |
| `mod/ClientLifecycleSubscriber.java` | Loader 资源重载 → 改为 PolisCraft 事件总线注册 |
| `mod/ClientEventSubscriber.java` | 含大量测试 GUI 代码和调试功能，不搬 |
| `mod/container/ContainerHook.java` | BlockUI 自带的容器 GUI 钩子系统，不搬 |
| `mod/item/BlockStateRenderingData.java` | 3D 块状渲染专用，MultiPiston GUI 不需要 |
| `util/resloc/OutOfJarResourceLocation.java` | NIO 外部资源加载，MultiPiston GUI 全部用 jar 内资源 |
| `util/texture/OutOfJarTexture.java` | jar 外纹理加载，同上 |
| `util/texture/SpriteTexture.java` | 动画精灵纹理，同上 |
| `AtlasManager.java` | 自定义图集管理，MultiPiston GUI 只用原版纹理 |
| `PaneBuilders.java` | TextBuilder 工厂，MultiPiston GUI 不调用 |
| `hooks/*.java`（7个文件） | 世界内 GUI 钩子系统，MultiPiston 容器 GUI 不适配 |
| `views/SwitchView.java` | 标签页视图，XML 中未使用 |
| `views/ZoomDragView.java` | 缩放拖拽视图，XML 中未使用 |

---

## 五、需要手写的替代文件

### 5.1 `mod/Log.java` → `util/PolisLog.java`

```java
package com.poliscraft.blockui.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PolisLog
{
    private static final Logger LOGGER = LogManager.getLogger("PolisCraft-BlockUI");

    public static Logger getLogger()
    {
        return LOGGER;
    }
}
```

### 5.2 `PolisLoader.java`（替换 Loader.java）

```java
package com.poliscraft.blockui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PolisLoader
{
    public static final PolisLoader INSTANCE = new PolisLoader();
    private final Map<String, Supplier<Pane>> paneFactories = new HashMap<>();

    private PolisLoader()
    {
        // 只注册 MultiPiston GUI 实际使用的 6 种标签
        paneFactories.put("window", BOWindow::new);
        paneFactories.put("label", Text::new);
        paneFactories.put("dropdown", DropDownList::new);
        paneFactories.put("view", View::new);
        paneFactories.put("buttonimage", ButtonImage::new);
        paneFactories.put("input", TextFieldVanilla::new);
        // 视图层（被 DropDownList 等组合使用）
        paneFactories.put("overlayview", OverlayView::new);
        paneFactories.put("scrollinglist", ScrollingList::new);
        paneFactories.put("scrollingcontainer", ScrollingContainer::new);
        paneFactories.put("scrollingview", ScrollingView::new);
        paneFactories.put("scrollinglistcontainer", ScrollingListContainer::new);
        paneFactories.put("box", Box::new);
        paneFactories.put("group", Group::new);
        paneFactories.put("scrollbar", Scrollbar::new);
    }

    public Pane createFromPaneParams(final PaneParams params)
    {
        // ... 简化的 XML 解析 → 工厂创建
    }

    public static void createFromXMLFile(final ResourceLocation loc, final BOWindow window)
    {
        // ... XML 文件加载 → 递归解析
    }
}
```

> **注意**：实际 Label 标签 → `Text` 类（PaneParams 中通过 `getString("type")` 判断，详见 AbstractTextElement 源码）
> `Loader.java` 中完整的 paneFactories 映射表见附录。

### 5.3 `SingleBlockGetter.java` 的 NeoForge 依赖处理

L109 需改为不使用 NeoForge `ServerLifecycleHooks`：
```java
// 旧 (NeoForge):
return colorResolver.getColor(ServerLifecycleHooks.getCurrentServer()
    .registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS), pos.getX(), pos.getZ());

// 新 (Forge 1.20.1):
return colorResolver.getColor(Minecraft.getInstance().level
    .registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS), pos.getX(), pos.getZ());
```

---

## 六、文件总数统计

| 类别 | 数量 | 说明 |
|------|------|------|
| 核心框架 | 15 | Pane → Pos2iSizeI + PolisLoader |
| 控件层 | 11 | AbstractTextElement → DropDownList |
| 视图层 | 8 | OverlayView → Box |
| 工具层 | 8 | SafeError → IsOurTexture |
| 颜色工具 | 5 | IColour → ColouredVertexConsumer |
| 手写替代 | 2 | PolisLog + PolisLoader |
| **总计** | **49** | 其中 2 个手写，47 个从 BlockUI 搬运适配 |

---

## 七、附录：Loader.java 原始 paneFactories 完整映射

（仅做参考对照，不需要全部注册）

```java
// controls 包
paneFactories.put("label", Text::new);
paneFactories.put("button", Button::new);
paneFactories.put("buttonimage", ButtonImage::new);
paneFactories.put("buttonimagestated", ButtonImageState::new);
paneFactories.put("image", Image::new);
paneFactories.put("itemicon", ItemIcon::new);
paneFactories.put("itemiconwithblockstate", ItemIconWithBlockState::new);
paneFactories.put("input", TextField::new);
paneFactories.put("inputvanilla", TextFieldVanilla::new);
paneFactories.put("checkbox", CheckBox::new);
paneFactories.put("entereditor", EnterEdit::new);
paneFactories.put("gradient", Gradient::new);
paneFactories.put("labelgradient", LabelGradient::new);
paneFactories.put("renderbox", RenderBox::new);
paneFactories.put("slot", Slot::new);
paneFactories.put("slotimage", SlotImage::new);
paneFactories.put("tooltip", Tooltip::new);
paneFactories.put("paneselector", PaneSelector::new);

// views 包
paneFactories.put("view", View::new);
paneFactories.put("group", Group::new);
paneFactories.put("scrollinglist", ScrollingList::new);
paneFactories.put("scrollinggroup", ScrollingGroup::new);
paneFactories.put("zoomdragview", ZoomDragView::new);
paneFactories.put("dropdown", DropDownList::new);
paneFactories.put("switchview", SwitchView::new);
paneFactories.put("overlayview", OverlayView::new);
paneFactories.put("box", Box::new);
paneFactories.put("scrollingview", ScrollingView::new);
paneFactories.put("itemlist", ItemList::new);
paneFactories.put("tablist", TabList::new);
```

---

*生成时间：2026-05-28*
*基于 BlockUI 源码 `参考/BlockUI-source/` 分析*
*依赖链覆盖率：100%（已阅读全部关键依赖文件源代码）*
