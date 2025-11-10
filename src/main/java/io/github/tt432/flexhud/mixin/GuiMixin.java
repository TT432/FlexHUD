package io.github.tt432.flexhud.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin to intercept the hotbar rendering in Minecraft's Gui class
 * and redirect it to FlexHUD's custom rendering system.
 */
@Mixin(Gui.class)
public class GuiMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/gui/GuiLayerManager;add(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/gui/LayeredDraw$Layer;)Lnet/neoforged/neoforge/client/gui/GuiLayerManager;"))
    private GuiLayerManager onGuiLayerRegister(GuiLayerManager instance, ResourceLocation name, LayeredDraw.Layer layer) {
        if (name.equals(VanillaGuiLayers.HOTBAR)) {
            return instance;
        }
        return instance.add(name, layer);
    }
}