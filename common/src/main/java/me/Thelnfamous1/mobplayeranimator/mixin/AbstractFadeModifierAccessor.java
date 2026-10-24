package me.Thelnfamous1.mobplayeranimator.mixin;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractFadeModifier.class, remap = false)
public interface AbstractFadeModifierAccessor {

    @Accessor(value = "beginAnimation", remap = false)
    IAnimation mobplayeranimator$getBeginAnimation();
}
