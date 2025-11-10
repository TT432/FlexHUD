package io.github.tt432.flexhud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * FlexHUD Configuration Screen
 * Allows users to visually configure HUD element positions
 *
 * @author TT432
 */
public class FlexHudConfigScreen extends Screen {
    private final FlexHudApi.Impl hudApi;
    private FlexHudApi.HudElement draggedElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean isDragging = false;

    // Resize support
    private static final int HANDLE_SIZE = 6; // visual square size for handles
    private static final int HANDLE_RADIUS = 4; // hit test leniency
    private static final int MIN_SIZE = 10; // minimal width/height

    private boolean isResizing = false;
    private FlexHudApi.HudElement resizingElement = null;
    private HandleType activeHandle = HandleType.NONE;
    private FlexHudApi.Rect initialRectDuringResize = null;
    private float initialAspectRatio = 1f;

    private enum HandleType {
        NONE,
        LEFT, RIGHT, TOP, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public FlexHudConfigScreen() {
        super(Component.literal("FlexHUD Configuration"));
        this.hudApi = (FlexHudApi.Impl) FlexHudApi.INSTANCE;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render all HUD elements as outlined rectangles
        Map<ResourceLocation, FlexHudApi.HudElement> elements = hudApi.getRegisteredElements();
        for (FlexHudApi.HudElement element : elements.values()) {
            renderElementPlaceholder(guiGraphics, element, mouseX, mouseY);
        }
    }

    private void renderElementPlaceholder(GuiGraphics guiGraphics, FlexHudApi.HudElement element, int mouseX, int mouseY) {
        FlexHudApi.Rect rect = element.rect;
        int x = (int) rect.getX();
        int y = (int) rect.getY();
        int width = (int) rect.getW();
        int height = (int) rect.getH();

        // Determine colors based on hover state
        boolean isHovered = isMouseOverElement(mouseX, mouseY, rect);
        boolean isDraggedElement = element == draggedElement;

        int outlineColor;
        int fillColor;

        if (isDraggedElement) {
            outlineColor = 0xFFFF0000; // Red when dragging
            fillColor = 0x40FF0000;    // Semi-transparent red fill
        } else if (isHovered) {
            outlineColor = 0xFFFFFF00; // Yellow when hovered
            fillColor = 0x40FFFF00;    // Semi-transparent yellow fill
        } else {
            outlineColor = 0xFFFFFFFF; // White normally
            fillColor = 0x20FFFFFF;    // Semi-transparent white fill
        }

        // Draw filled rectangle
        guiGraphics.fill(x, y, x + width, y + height, fillColor);

        // Draw outline
        guiGraphics.hLine(x, x + width - 1, y, outlineColor); // Top
        guiGraphics.hLine(x, x + width - 1, y + height - 1, outlineColor); // Bottom
        guiGraphics.vLine(x, y, y + height - 1, outlineColor); // Left
        guiGraphics.vLine(x + width - 1, y, y + height - 1, outlineColor); // Right

        // Draw resize handles if resizable
        if (element.resizeMode != FlexHudApi.ResizeMode.Fixed) {
            drawHandle(guiGraphics, x, y); // top-left
            drawHandle(guiGraphics, x + width / 2 - HANDLE_SIZE / 2, y); // top-center
            drawHandle(guiGraphics, x + width - HANDLE_SIZE, y); // top-right

            drawHandle(guiGraphics, x, y + height / 2 - HANDLE_SIZE / 2); // mid-left
            drawHandle(guiGraphics, x + width - HANDLE_SIZE, y + height / 2 - HANDLE_SIZE / 2); // mid-right

            drawHandle(guiGraphics, x, y + height - HANDLE_SIZE); // bottom-left
            drawHandle(guiGraphics, x + width / 2 - HANDLE_SIZE / 2, y + height - HANDLE_SIZE); // bottom-center
            drawHandle(guiGraphics, x + width - HANDLE_SIZE, y + height - HANDLE_SIZE); // bottom-right
        }

        // Draw element ID label
        Component label = Component.literal(element.id.toString());
        int labelX = x + 2;
        int labelY = y - 12;

        // Ensure label is visible on screen
        if (labelY < 50) { // Account for title area
            labelY = y + height + 2;
        }

        // Draw label background
        int labelWidth = this.font.width(label);
        guiGraphics.fill(labelX - 2, labelY - 1, labelX + labelWidth + 2, labelY + 9, 0xC0000000);

        // Draw label text
        guiGraphics.drawString(this.font, label, labelX, labelY, 0xFFFFFF);

        // Draw resize mode indicator in the top-right corner of the element
        String resizeMode = element.resizeMode.name();
        Component modeLabel = Component.literal("[" + resizeMode + "]");
        int modeWidth = this.font.width(modeLabel);
        int modeX = x + width - modeWidth - 2;
        int modeY = y + 2;

        // Ensure mode label fits within the element
        if (modeX < x + 2) {
            modeX = x + 2;
        }
        if (modeY + 8 > y + height - 2) {
            modeY = y + height - 10;
        }

        guiGraphics.fill(modeX - 1, modeY - 1, modeX + modeWidth + 1, modeY + 9, 0xC0000000);
        guiGraphics.drawString(this.font, modeLabel, modeX, modeY, 0xFFFF00);

        // Draw size information in the center
        String sizeInfo = String.format("%.0f x %.0f", rect.getW(), rect.getH());
        Component sizeLabel = Component.literal(sizeInfo);
        int sizeWidth = this.font.width(sizeLabel);
        int sizeX = x + (width - sizeWidth) / 2;
        int sizeY = y + (height - 8) / 2;

        if (sizeX >= x + 2 && sizeX + sizeWidth <= x + width - 2 &&
                sizeY >= y + 2 && sizeY + 8 <= y + height - 2) {
            guiGraphics.fill(sizeX - 1, sizeY - 1, sizeX + sizeWidth + 1, sizeY + 9, 0x80000000);
            guiGraphics.drawString(this.font, sizeLabel, sizeX, sizeY, 0xCCCCCC);
        }
    }

    private void drawHandle(GuiGraphics guiGraphics, int hx, int hy) {
        guiGraphics.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, 0xFFFFFFFF);
        guiGraphics.fill(hx + 1, hy + 1, hx + HANDLE_SIZE - 1, hy + HANDLE_SIZE - 1, 0xFF000000);
    }

    private boolean isMouseOverElement(int mouseX, int mouseY, FlexHudApi.Rect rect) {
        return mouseX >= rect.getX() && mouseX < rect.getX() + rect.getW() &&
                mouseY >= rect.getY() && mouseY < rect.getY() + rect.getH();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            Map<ResourceLocation, FlexHudApi.HudElement> elements = hudApi.getRegisteredElements();
            FlexHudApi.HudElement topElement = null;
            for (FlexHudApi.HudElement element : elements.values()) {
                if (isMouseOverElement((int) mouseX, (int) mouseY, element.rect)) {
                    topElement = element;
                }
            }

            if (topElement != null) {
                // Check if clicking on a resize handle first
                HandleType handle = getHandleUnderMouse(topElement.rect, (int) mouseX, (int) mouseY);
                if (handle != HandleType.NONE && topElement.resizeMode != FlexHudApi.ResizeMode.Fixed) {
                    // Start resizing
                    resizingElement = topElement;
                    isResizing = true;
                    activeHandle = handle;
                    initialRectDuringResize = topElement.rect.copy();
                    initialAspectRatio = initialRectDuringResize.getW() > 0 && initialRectDuringResize.getH() > 0
                        ? (initialRectDuringResize.getW() / initialRectDuringResize.getH())
                        : 1f;
                    return true;
                }

                // Otherwise start dragging
                draggedElement = topElement;
                dragOffsetX = (int) (mouseX - topElement.rect.getX());
                dragOffsetY = (int) (mouseY - topElement.rect.getY());
                isDragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isDragging) {
                isDragging = false;
                if (draggedElement != null) {
                    // Convert absolute rect back to relative rect and update
                    updateElementFromAbsoluteRect(draggedElement);
                    draggedElement = null;
                }
                return true;
            }
            if (isResizing) {
                isResizing = false;
                if (resizingElement != null) {
                    // Convert absolute rect back to relative rect and update
                    updateElementFromAbsoluteRect(resizingElement);
                    resizingElement = null;
                    activeHandle = HandleType.NONE;
                    initialRectDuringResize = null;
                }
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isDragging && draggedElement != null) {
            // Update element position
            float newX = (float) (mouseX - dragOffsetX);
            float newY = (float) (mouseY - dragOffsetY);

            // Clamp to screen bounds
            newX = Math.max(0, Math.min(newX, this.width - draggedElement.rect.getW()));
            newY = Math.max(0, Math.min(newY, this.height - draggedElement.rect.getH())); // 50px from top for UI

            draggedElement.rect.setX(newX);
            draggedElement.rect.setY(newY);

            return true;
        }
        if (button == 0 && isResizing && resizingElement != null && activeHandle != HandleType.NONE) {
            applyResize(resizingElement, (int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    private HandleType getHandleUnderMouse(FlexHudApi.Rect rect, int mouseX, int mouseY) {
        int x = (int) rect.getX();
        int y = (int) rect.getY();
        int w = (int) rect.getW();
        int h = (int) rect.getH();

        // Corners
        if (isInRect(mouseX, mouseY, x - HANDLE_RADIUS, y - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.TOP_LEFT;
        if (isInRect(mouseX, mouseY, x + w - HANDLE_SIZE - HANDLE_RADIUS, y - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.TOP_RIGHT;
        if (isInRect(mouseX, mouseY, x - HANDLE_RADIUS, y + h - HANDLE_SIZE - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.BOTTOM_LEFT;
        if (isInRect(mouseX, mouseY, x + w - HANDLE_SIZE - HANDLE_RADIUS, y + h - HANDLE_SIZE - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.BOTTOM_RIGHT;

        // Edges midpoints
        if (isInRect(mouseX, mouseY, x - HANDLE_RADIUS, y + h / 2 - HANDLE_SIZE / 2 - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.LEFT;
        if (isInRect(mouseX, mouseY, x + w - HANDLE_SIZE - HANDLE_RADIUS, y + h / 2 - HANDLE_SIZE / 2 - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.RIGHT;
        if (isInRect(mouseX, mouseY, x + w / 2 - HANDLE_SIZE / 2 - HANDLE_RADIUS, y - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.TOP;
        if (isInRect(mouseX, mouseY, x + w / 2 - HANDLE_SIZE / 2 - HANDLE_RADIUS, y + h - HANDLE_SIZE - HANDLE_RADIUS, HANDLE_SIZE + HANDLE_RADIUS * 2, HANDLE_SIZE + HANDLE_RADIUS * 2))
            return HandleType.BOTTOM;

        return HandleType.NONE;
    }

    private boolean isInRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    private void applyResize(FlexHudApi.HudElement element, int mouseX, int mouseY) {
        FlexHudApi.Rect rect = element.rect;
        float x = rect.getX();
        float y = rect.getY();
        float w = rect.getW();
        float h = rect.getH();

        float right = x + w;
        float bottom = y + h;

        switch (element.resizeMode) {
            case Fixed:
                return;
            case Free:
                resizeFree(rect, mouseX, mouseY, right, bottom);
                break;
            case Horizontal:
                resizeHorizontal(rect, mouseX, right);
                break;
            case Vertical:
                resizeVertical(rect, mouseY, bottom);
                break;
            case Aspect:
                resizeAspect(rect, mouseX, mouseY, right, bottom);
                break;
        }

        clampRectToScreen(rect);
    }

    private void resizeFree(FlexHudApi.Rect rect, int mouseX, int mouseY, float right, float bottom) {
        switch (activeHandle) {
            case LEFT: {
                float newX = Math.min(mouseX, right - MIN_SIZE);
                rect.setX(newX);
                rect.setW(right - newX);
                break;
            }
            case RIGHT: {
                float newW = Math.max(MIN_SIZE, mouseX - rect.getX());
                rect.setW(newW);
                break;
            }
            case TOP: {
                float newY = Math.min(mouseY, bottom - MIN_SIZE);
                rect.setY(newY);
                rect.setH(bottom - newY);
                break;
            }
            case BOTTOM: {
                float newH = Math.max(MIN_SIZE, mouseY - rect.getY());
                rect.setH(newH);
                break;
            }
            case TOP_LEFT: {
                float newX = Math.min(mouseX, right - MIN_SIZE);
                float newY = Math.min(mouseY, bottom - MIN_SIZE);
                rect.setX(newX);
                rect.setY(newY);
                rect.setW(right - newX);
                rect.setH(bottom - newY);
                break;
            }
            case TOP_RIGHT: {
                float newY = Math.min(mouseY, bottom - MIN_SIZE);
                float newW = Math.max(MIN_SIZE, mouseX - rect.getX());
                rect.setY(newY);
                rect.setH(bottom - newY);
                rect.setW(newW);
                break;
            }
            case BOTTOM_LEFT: {
                float newX = Math.min(mouseX, right - MIN_SIZE);
                float newH = Math.max(MIN_SIZE, mouseY - rect.getY());
                rect.setX(newX);
                rect.setW(right - newX);
                rect.setH(newH);
                break;
            }
            case BOTTOM_RIGHT: {
                float newW = Math.max(MIN_SIZE, mouseX - rect.getX());
                float newH = Math.max(MIN_SIZE, mouseY - rect.getY());
                rect.setW(newW);
                rect.setH(newH);
                break;
            }
        }
    }

    private void resizeHorizontal(FlexHudApi.Rect rect, int mouseX, float right) {
        switch (activeHandle) {
            case LEFT: {
                float newX = Math.min(mouseX, right - MIN_SIZE);
                rect.setX(newX);
                rect.setW(right - newX);
                break;
            }
            case RIGHT: {
                float newW = Math.max(MIN_SIZE, mouseX - rect.getX());
                rect.setW(newW);
                break;
            }
            default:
                // Ignore vertical handles
                break;
        }
    }

    private void resizeVertical(FlexHudApi.Rect rect, int mouseY, float bottom) {
        switch (activeHandle) {
            case TOP: {
                float newY = Math.min(mouseY, bottom - MIN_SIZE);
                rect.setY(newY);
                rect.setH(bottom - newY);
                break;
            }
            case BOTTOM: {
                float newH = Math.max(MIN_SIZE, mouseY - rect.getY());
                rect.setH(newH);
                break;
            }
            default:
                // Ignore horizontal handles
                break;
        }
    }

    private void resizeAspect(FlexHudApi.Rect rect, int mouseX, int mouseY, float right, float bottom) {
        float ratio = initialAspectRatio <= 0 ? 1f : initialAspectRatio;

        switch (activeHandle) {
            case LEFT: {
                float newX = Math.min(mouseX, right - MIN_SIZE);
                float newW = right - newX;
                float newH = Math.max(MIN_SIZE, newW / ratio);
                rect.setX(newX);
                rect.setW(newW);
                rect.setH(newH);
                // Anchor top
                rect.setY(Math.min(rect.getY(), bottom - newH));
                break;
            }
            case RIGHT: {
                float newW = Math.max(MIN_SIZE, mouseX - rect.getX());
                float newH = Math.max(MIN_SIZE, newW / ratio);
                rect.setW(newW);
                rect.setH(newH);
                // Anchor top
                rect.setY(Math.min(rect.getY(), bottom - newH));
                break;
            }
            case TOP: {
                float newY = Math.min(mouseY, bottom - MIN_SIZE);
                float newH = bottom - newY;
                float newW = Math.max(MIN_SIZE, newH * ratio);
                rect.setY(newY);
                rect.setH(newH);
                rect.setW(newW);
                // Anchor left
                rect.setX(Math.min(rect.getX(), right - newW));
                break;
            }
            case BOTTOM: {
                float newH = Math.max(MIN_SIZE, mouseY - rect.getY());
                float newW = Math.max(MIN_SIZE, newH * ratio);
                rect.setH(newH);
                rect.setW(newW);
                // Anchor left
                rect.setX(Math.min(rect.getX(), right - newW));
                break;
            }
            case TOP_LEFT: {
                float anchorX = initialRectDuringResize.getX() + initialRectDuringResize.getW();
                float anchorY = initialRectDuringResize.getY() + initialRectDuringResize.getH();
                float newW = Math.max(MIN_SIZE, Math.abs(anchorX - mouseX));
                float newH = Math.max(MIN_SIZE, newW / ratio);
                float newX = anchorX - newW;
                float newY = anchorY - newH;
                rect.setX(newX);
                rect.setY(newY);
                rect.setW(newW);
                rect.setH(newH);
                break;
            }
            case TOP_RIGHT: {
                float anchorX = initialRectDuringResize.getX();
                float anchorY = initialRectDuringResize.getY() + initialRectDuringResize.getH();
                float newW = Math.max(MIN_SIZE, Math.abs(mouseX - anchorX));
                float newH = Math.max(MIN_SIZE, newW / ratio);
                float newX = anchorX;
                float newY = anchorY - newH;
                rect.setX(newX);
                rect.setY(newY);
                rect.setW(newW);
                rect.setH(newH);
                break;
            }
            case BOTTOM_LEFT: {
                float anchorX = initialRectDuringResize.getX() + initialRectDuringResize.getW();
                float anchorY = initialRectDuringResize.getY();
                float newW = Math.max(MIN_SIZE, Math.abs(anchorX - mouseX));
                float newH = Math.max(MIN_SIZE, newW / ratio);
                float newX = anchorX - newW;
                float newY = anchorY;
                rect.setX(newX);
                rect.setY(newY);
                rect.setW(newW);
                rect.setH(newH);
                break;
            }
            case BOTTOM_RIGHT: {
                float anchorX = initialRectDuringResize.getX();
                float anchorY = initialRectDuringResize.getY();
                float newW = Math.max(MIN_SIZE, Math.abs(mouseX - anchorX));
                float newH = Math.max(MIN_SIZE, newW / ratio);
                float newX = anchorX;
                float newY = anchorY;
                rect.setX(newX);
                rect.setY(newY);
                rect.setW(newW);
                rect.setH(newH);
                break;
            }
        }
    }

    private void clampRectToScreen(FlexHudApi.Rect rect) {
        float x = Math.max(0, rect.getX());
        float y = Math.max(0, rect.getY());
        float w = Math.max(MIN_SIZE, rect.getW());
        float h = Math.max(MIN_SIZE, rect.getH());
        if (x + w > this.width) {
            w = Math.max(MIN_SIZE, this.width - x);
        }
        if (y + h > this.height) {
            h = Math.max(MIN_SIZE, this.height - y);
        }
        rect.setX(x);
        rect.setY(y);
        rect.setW(w);
        rect.setH(h);
    }

    /**
     * Convert absolute rect back to relative rect and update the element
     */
    private void updateElementFromAbsoluteRect(FlexHudApi.HudElement element) {
        if (element.relativeRect != null) {
            // Convert current absolute rect back to relative rect
            FlexHudApi.RelativeRect newRelativeRect = FlexHudApi.RelativeRect.fromAbsolute(
                element.rect, 
                element.relativeRect.getAnchor(), 
                this.width, 
                this.height
            );
            element.relativeRect = newRelativeRect;
            // Update the element's relative rect
            hudApi.updateElementRelativeRect(element.id, newRelativeRect);
        }
    }
}