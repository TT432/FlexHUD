package io.github.tt432.flexhud;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public class FlexConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexConfig.class);

    // Use NeoForge ModConfigSpec (client) with TOML storage
    public static final FlexConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new FlexConfig(builder);
        SPEC = builder.build();
    }

    // A list of serialized relative rect entries: "<id>|<anchor>|<offsetX>|<offsetY>|<width>|<height>|<useRelativeSize>"
    public final ModConfigSpec.ConfigValue<List<? extends String>> relativeRectsRaw;

    // In-memory cache for fast lookup
    private final Map<String, FlexHudApi.RelativeRect> relativeRects = new HashMap<>();

    // Constructor for ModConfigSpec.Builder#configure
    private FlexConfig(ModConfigSpec.Builder builder) {
        builder.push("hud");
        relativeRectsRaw = builder
            .comment(
                "List of HUD element relative rectangles",
                "Each entry format: <namespace:path>|<anchor>|<offsetX>|<offsetY>|<width>|<height>|<useRelativeSize>")
            .translation("flexhud.config.hud.relative_rects")
            .defineListAllowEmpty("relative_rects", ArrayList::new, o -> o instanceof String);
        builder.pop();
    }

    /**
     * Apply values from SPEC into in-memory map. Call on config load/reload.
     */
    public void applyFromSpec() {
        relativeRects.clear();
        
        // Load relative rectangles
        List<? extends String> relativeEntries = relativeRectsRaw.get();
        for (String entry : relativeEntries) {
            if (entry == null || entry.isEmpty()) continue;
            String[] parts = entry.split("\\|");
            if (parts.length != 7) {
                LOGGER.warn("Invalid relative rect entry: {}", entry);
                continue;
            }
            String idStr = parts[0];
            FlexHudApi.Anchor anchor = parseAnchorSafe(parts[1]);
            float offsetX = parseFloatSafe(parts[2]);
            float offsetY = parseFloatSafe(parts[3]);
            float width = parseFloatSafe(parts[4]);
            float height = parseFloatSafe(parts[5]);
            boolean useRelativeSize = Boolean.parseBoolean(parts[6]);
            relativeRects.put(idStr, new FlexHudApi.RelativeRect(anchor, offsetX, offsetY, width, height, useRelativeSize));
        }
    }

    private float parseFloatSafe(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception ignored) {
            return 0f;
        }
    }

    private FlexHudApi.Anchor parseAnchorSafe(String s) {
        try {
            return FlexHudApi.Anchor.valueOf(s);
        } catch (Exception ignored) {
            return FlexHudApi.Anchor.TOP_LEFT; // Default anchor
        }
    }

    /**
     * Get relative rectangle configuration for specified ID
     */
    public FlexHudApi.RelativeRect getRelativeRect(ResourceLocation id) {
        return relativeRects.get(id.toString());
    }

    /**
     * Set relative rectangle configuration for specified ID and persist to SPEC
     */
    public void setRelativeRect(ResourceLocation id, FlexHudApi.RelativeRect relativeRect) {
        relativeRects.put(id.toString(), relativeRect);
        save();
    }

    /**
     * Get all relative rectangle configurations
     */
    public Map<String, FlexHudApi.RelativeRect> getAllRelativeRects() {
        return new HashMap<>(relativeRects);
    }

    /**
     * Serialize in-memory map to SPEC and save TOML
     */
    public void save() {
        // Save relative rectangles
        List<String> relativeEntries = new ArrayList<>();
        for (Map.Entry<String, FlexHudApi.RelativeRect> e : relativeRects.entrySet()) {
            FlexHudApi.RelativeRect r = e.getValue();
            String line = e.getKey() + "|" + r.getAnchor().name() + "|" + r.getOffsetX() + "|" + r.getOffsetY() + 
                         "|" + r.getWidth() + "|" + r.getHeight() + "|" + r.isUseRelativeSize();
            relativeEntries.add(line);
        }
        relativeRectsRaw.set(relativeEntries);
        
        try {
            SPEC.save(); // Persist to disk when config is loaded
        } catch (IllegalStateException ex) {
            // Save can fail if config not yet loaded; log and continue
            LOGGER.debug("Config not yet loaded; deferred save: {}", ex.getMessage());
        }
    }

    /**
     * Reload configuration from SPEC (clears and re-applies cache)
     */
    public void reload() {
        applyFromSpec();
    }
}
