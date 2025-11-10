package io.github.tt432.flexhud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface FlexHudApi {
    FlexHudApi INSTANCE = new Impl();
    
    // Hotbar element ID
    ResourceLocation HOTBAR_ID = ResourceLocation.fromNamespaceAndPath("flexhud", "hotbar");
    
    @Getter
    final class Impl implements FlexHudApi {
        private final Map<ResourceLocation, HudElement> registeredElements = new ConcurrentHashMap<>();
        private final FlexConfig config = FlexConfig.INSTANCE;

        @Override
        public void register(ResourceLocation id, ResizeMode resizeMode, RelativeRect defaultRelativeRect, Layer layer) {
            // Get saved relative rectangle from config, use default if not found
            RelativeRect relativeRect = config.getRelativeRect(id);
            if (relativeRect == null) {
                relativeRect = defaultRelativeRect;
                config.setRelativeRect(id, relativeRect);
            }
            
            // Convert relative rect to absolute using current screen dimensions
            Minecraft minecraft = Minecraft.getInstance();
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            Rect rect = relativeRect.toAbsolute(screenWidth, screenHeight);
            
            HudElement element = new HudElement(id, resizeMode, rect, layer, relativeRect);
            registeredElements.put(id, element);
        }

        /**
         * Update HUD element relative rectangle
         */
        public void updateElementRelativeRect(ResourceLocation id, RelativeRect newRelativeRect) {
            HudElement element = registeredElements.get(id);
            if (element != null) {
                // Update the relative rect and recalculate absolute position
                Minecraft minecraft = Minecraft.getInstance();
                int screenWidth = minecraft.getWindow().getGuiScaledWidth();
                int screenHeight = minecraft.getWindow().getGuiScaledHeight();
                element.rect = newRelativeRect.toAbsolute(screenWidth, screenHeight);
                config.setRelativeRect(id, newRelativeRect);
            }
        }

        /**
         * Update screen dimensions and recalculate relative positions
         */
        public void updateScreenDimensions() {
            Minecraft minecraft = Minecraft.getInstance();
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();

            for (HudElement element : registeredElements.values()) {
                if (element.relativeRect != null) {
                    // Recalculate absolute position from relative position
                    element.rect = element.relativeRect.toAbsolute(screenWidth, screenHeight);
                }
            }
        }
    }

    /**
     * Register HUD element with relative positioning
     */
    void register(ResourceLocation id, ResizeMode resizeMode, RelativeRect defaultRelativeRect, Layer layer);

    enum ResizeMode {
        Free,      // Free resize width and height
        Aspect,    // Scale proportionally (maintain aspect ratio)
        Horizontal,// Only allow horizontal (X) scaling
        Vertical,  // Only allow vertical (Y) scaling
        Fixed,     // Completely disable scaling
    }

    @Getter
    enum Anchor {
        TOP_LEFT(0.0f, 0.0f),
        TOP_CENTER(0.5f, 0.0f),
        TOP_RIGHT(1.0f, 0.0f),
        CENTER_LEFT(0.0f, 0.5f),
        CENTER(0.5f, 0.5f),
        CENTER_RIGHT(1.0f, 0.5f),
        BOTTOM_LEFT(0.0f, 1.0f),
        BOTTOM_CENTER(0.5f, 1.0f),
        BOTTOM_RIGHT(1.0f, 1.0f);

        private final float xPercent;
        private final float yPercent;

        Anchor(float xPercent, float yPercent) {
            this.xPercent = xPercent;
            this.yPercent = yPercent;
        }
    }

    @Data
    @AllArgsConstructor
    class Rect {
        private float x;
        private float y;
        private float w;
        private float h;

        public Rect copy() {
            return new Rect(x, y, w, h);
        }

        /**
         * Compute affine transform that maps this rect to the target rect.
         * The transformation is composed as: translate to target, scale by size ratio,
         * then translate by negative source position.
         */
        public Matrix4f transform(Rect to) {
            float sx = this.w == 0f ? 0f : to.w / this.w;
            float sy = this.h == 0f ? 0f : to.h / this.h;

            Matrix4f m = new Matrix4f();
            m.translate(to.x, to.y, 0f)
             .scale(sx, sy, 1f)
             .translate(-this.x, -this.y, 0f);
            return m;
        }
    }

    @Data
    @AllArgsConstructor
    class RelativeRect {
        private Anchor anchor;
        private float offsetX;
        private float offsetY;
        private float width;
        private float height;
        private boolean useRelativeSize;

        public RelativeRect(Anchor anchor, float offsetX, float offsetY, float width, float height) {
            this(anchor, offsetX, offsetY, width, height, false);
        }

        /**
         * Convert relative position to absolute Rect based on screen dimensions
         */
        public Rect toAbsolute(int screenWidth, int screenHeight) {
            // Calculate anchor position
            float anchorX = screenWidth * anchor.getXPercent();
            float anchorY = screenHeight * anchor.getYPercent();

            // Apply offset
            float absoluteX = anchorX + offsetX;
            float absoluteY = anchorY + offsetY;

            // Calculate size
            float absoluteWidth = useRelativeSize ? screenWidth * width : width;
            float absoluteHeight = useRelativeSize ? screenHeight * height : height;

            // Adjust position based on element size (anchor point is relative to element)
            absoluteX -= absoluteWidth * anchor.getXPercent();
            absoluteY -= absoluteHeight * anchor.getYPercent();

            return new Rect(absoluteX, absoluteY, absoluteWidth, absoluteHeight);
        }

        /**
         * Create RelativeRect from absolute Rect based on screen dimensions
         */
        public static RelativeRect fromAbsolute(Rect rect, Anchor anchor, int screenWidth, int screenHeight) {
            // Calculate what the anchor position would be
            float anchorX = screenWidth * anchor.getXPercent();
            float anchorY = screenHeight * anchor.getYPercent();

            // Calculate element's anchor point
            float elementAnchorX = rect.getX() + rect.getW() * anchor.getXPercent();
            float elementAnchorY = rect.getY() + rect.getH() * anchor.getYPercent();

            // Calculate offset
            float offsetX = elementAnchorX - anchorX;
            float offsetY = elementAnchorY - anchorY;

            return new RelativeRect(anchor, offsetX, offsetY, rect.getW(), rect.getH(), false);
        }
    }
    
    /**
     * HUD element data class
     */
    class HudElement {
        public final ResourceLocation id;
        public final ResizeMode resizeMode;
        public Rect rect; // Calculated absolute position for rendering
        public final Layer layer;
        public RelativeRect relativeRect; // Relative positioning definition

        public HudElement(ResourceLocation id, ResizeMode resizeMode, Rect rect, Layer layer, RelativeRect relativeRect) {
            this.id = id;
            this.resizeMode = resizeMode;
            this.rect = rect;
            this.layer = layer;
            this.relativeRect = relativeRect;
        }
    }

    interface Layer {
        void render(Rect rect, GuiGraphics guiGraphics, DeltaTracker deltaTracker);
    }
}