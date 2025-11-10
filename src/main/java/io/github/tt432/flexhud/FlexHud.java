package io.github.tt432.flexhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FlexHUD - Flexible HUD System
 *
 * @author TT432
 */
@Mod(value = FlexHud.MOD_ID, dist = Dist.CLIENT)
public class FlexHud {
    public static final String MOD_ID = "flexhud";
    public static final Logger LOGGER = LoggerFactory.getLogger(FlexHud.class);

    // Key binding for opening config screen (Alt + H)
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.flexhud.open_config",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_H),
            "key.categories.flexhud"
    );

    public FlexHud(IEventBus modEventBus, ModContainer modContainer) {
        // Register client config (NeoForge ModConfigSpec)
        modContainer.registerConfig(ModConfig.Type.CLIENT, FlexConfig.SPEC);

        // Register client setup event
        modEventBus.addListener(this::onClientSetup);

        // Register key mappings
        modEventBus.addListener(this::onRegisterKeyMappings);

        // Listen for config load/reload to sync in-memory cache
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG_KEY);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Register GUI render event
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);

        // Initialize example HUD elements
        event.enqueueWork(BuiltInFlexHud::initBuiltIn);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == FlexConfig.SPEC) {
            FlexConfig.INSTANCE.applyFromSpec();
            LOGGER.info("FlexHUD config loaded and applied");
        }
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == FlexConfig.SPEC) {
            FlexConfig.INSTANCE.applyFromSpec();
            LOGGER.info("FlexHUD config reloaded and applied");
        }
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        if (OPEN_CONFIG_KEY.isDown()) {
            Minecraft.getInstance().setScreen(new FlexHudConfigScreen());
        }

        // Don't render HUD elements when config screen is open
        if (Minecraft.getInstance().screen instanceof FlexHudConfigScreen) {
            return;
        }

        // Render all registered HUD elements
        FlexHudApi.Impl impl = (FlexHudApi.Impl) FlexHudApi.INSTANCE;

        // Update screen dimensions for relative positioned elements
        impl.updateScreenDimensions();

        for (FlexHudApi.HudElement element : impl.getRegisteredElements().values()) {
            if (element.layer != null) {
                element.layer.render(element.rect, event.getGuiGraphics(), event.getPartialTick());
            }
        }
    }
}
