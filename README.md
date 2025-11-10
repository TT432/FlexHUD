# FlexHUD

语言: [English](README.en.md) | [中文](README.md)

一个基于 NeoForge 的可扩展 HUD 布局与渲染库。它通过“相对布局 + 锚点”描述 HUD 元素在屏幕上的位置与尺寸，并提供交互式配置界面，支持拖拽、缩放与多种约束模式。位置数据持久化到客户端配置，随屏幕尺寸变化自动重新计算。

## 核心目标

- 以“相对矩形”(`RelativeRect`) + 锚点(`Anchor`)的方式，稳定描述 HUD 元素在不同分辨率/比例下的位置与大小。
- 暴露简洁的注册 API：开发者只需实现一个渲染层回调(`Layer#render`)，即可将自定义 HUD 元素接入。
- 提供可视化配置界面(`FlexHudConfigScreen`)：支持拖拽移动与带把手的缩放，`ResizeMode` 控制约束策略（自由、等比、水平/垂直、固定）。
- 持久化与热更新：通过 NeoForge `ModConfig` 同步保存位置数据，运行时可更新并立即生效。

## 快速上手

以下示例演示如何注册一个可自由缩放的热键栏 HUD 元素：

```java
public class BuiltInFlexHud {
    public static void registerHotbarHud() {
        ResourceLocation id = FlexHudApi.HOTBAR_ID; // 元素唯一 ID

        // 默认相对矩形：以底部居中为锚点，宽 182、高 22
        FlexHudApi.RelativeRect defaultRelativeRect = new FlexHudApi.RelativeRect(
                FlexHudApi.Anchor.BOTTOM_CENTER,
                0, 0,
                182, 22
        );

        // 渲染层：收到计算好的绝对矩形后进行绘制
        FlexHudApi.Layer hotbarLayer = (rect, guiGraphics, deltaTracker) -> {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();

            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            // 以“默认相对矩形”计算出原版热键栏的默认绝对矩形
            FlexHudApi.Rect defaultRect = defaultRelativeRect.toAbsolute(screenWidth, screenHeight);
            // 根据目标矩形 rect 做仿射变换，实现自由缩放与移动
            pose.mulPose(defaultRect.transform(rect));

            ((GuiAccessor) Minecraft.getInstance().gui).callRenderHotbar(guiGraphics, deltaTracker);
            pose.popPose();
        };

        // 注册元素：选择 Free 模式，使用默认相对矩形与渲染层
        FlexHudApi.INSTANCE.register(id, FlexHudApi.ResizeMode.Free, defaultRelativeRect, hotbarLayer);
    }
}
```

运行时库会在渲染前调用 `updateScreenDimensions()`，将每个元素的 `RelativeRect` 转换为当前分辨率下的绝对矩形 `Rect` 并传入你的 `Layer#render`。

## API 速览

- `FlexHudApi#register(ResourceLocation id, ResizeMode mode, RelativeRect defaultRect, Layer layer)`
  - 注册一个 HUD 元素；若配置中已有保存的位置，则以配置为准，否则写入默认。
- `FlexHudApi.Impl#updateElementRelativeRect(ResourceLocation id, RelativeRect newRect)`
  - 运行时更新元素位置/尺寸并持久化；配置界面操作会调用此方法。
- `FlexHudApi.Impl#updateScreenDimensions()`
  - 当屏幕尺寸或比例变化时，基于相对矩形重新计算所有元素的绝对矩形。
- `FlexHudApi.Layer#render(Rect rect, GuiGraphics g, DeltaTracker dt)`
  - 渲染回调：收到绝对矩形后绘制你的 HUD 内容。
- `FlexHudApi.ResizeMode`
  - `Free`（自由缩放）、`Aspect`（等比）、`Horizontal`（仅水平）、`Vertical`（仅垂直）、`Fixed`（禁止缩放）。
- `FlexHudApi.Anchor`
  - 提供九宫格锚点：`TOP_LEFT/ TOP_CENTER/ TOP_RIGHT/ CENTER_LEFT/ CENTER/ CENTER_RIGHT/ BOTTOM_LEFT/ BOTTOM_CENTER/ BOTTOM_RIGHT`。
- `FlexHudApi.RelativeRect`
  - 字段：`anchor, offsetX, offsetY, width, height, useRelativeSize`
  - 方法：`toAbsolute(int w, int h)` 将相对描述转为绝对矩形；`fromAbsolute(Rect rect, Anchor a, int w, int h)` 反推相对矩形。
- `FlexHudApi.Rect#transform(Rect to)`
  - 计算从源矩形到目标矩形的仿射变换（位移 + 缩放），用于把已有绘制逻辑映射到新位置与尺寸。

## 可视化配置

- 游戏内按 `Alt + H` 打开 `FlexHudConfigScreen`。
- 支持拖拽移动与把手缩放；`ResizeMode` 决定缩放约束。
- 变更会保存到客户端配置（NeoForge `ModConfig`），例如：

  ```toml
  [hud]
  relative_rects = [
    "flexhud:hotbar|BOTTOM_CENTER|-2.9042664|-37.03853|188.19147|22.961472|false"
  ]
  ```

## 环境要求

- `Minecraft 1.21.1`
- `NeoForge 21.x`（示例：`21.1.213`）
- `Java 21`

## 许可

- MIT（见构建脚本/项目配置）。