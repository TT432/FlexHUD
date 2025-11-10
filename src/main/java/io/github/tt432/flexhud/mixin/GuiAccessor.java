package io.github.tt432.flexhud.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author TT432
 */
@Mixin(Gui.class)
public interface GuiAccessor {
    @Invoker
    void callRenderHotbar(GuiGraphics p_316628_, DeltaTracker p_348543_);
}
