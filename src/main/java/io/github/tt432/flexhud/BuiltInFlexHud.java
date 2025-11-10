package io.github.tt432.flexhud;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.flexhud.mixin.GuiAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 *
 * @author TT432
 */
public class BuiltInFlexHud {
    public static void registerHotbarHud() {
        ResourceLocation id = FlexHudApi.HOTBAR_ID;

        FlexHudApi.RelativeRect defaultRelativeRect = new FlexHudApi.RelativeRect(
                FlexHudApi.Anchor.BOTTOM_CENTER,
                0,
                0,
                182,
                22
        );

        FlexHudApi.Layer hotbarLayer = (rect, guiGraphics, deltaTracker) -> {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            // 根据 defaultRelativeRect 与当前 rect 计算从默认热键栏矩形到目标矩形的仿射变换
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();

            // 默认（原版）热键栏的矩形位置与大小（基于默认的相对布局）
            FlexHudApi.Rect defaultRect = defaultRelativeRect.toAbsolute(screenWidth, screenHeight);

            pose.mulPose(defaultRect.transform(rect));

            ((GuiAccessor) Minecraft.getInstance().gui).callRenderHotbar(guiGraphics, deltaTracker);
            pose.popPose();
        };

        // Register hotbar element with free resize mode using relative positioning
        FlexHudApi.INSTANCE.register(id, FlexHudApi.ResizeMode.Free, defaultRelativeRect, hotbarLayer);
    }

    /**
     * Initialize all example HUD elements
     */
    public static void initBuiltIn() {
        registerHotbarHud(); // Add hotbar registration
    }
}