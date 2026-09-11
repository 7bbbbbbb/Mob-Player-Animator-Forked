package me.Thelnfamous1.mobplayeranimator.mixin;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import java.util.List;

public class MobPlayerAnimatorMixinCanceller implements MixinCanceller {

    private static final List<String> BLOCKED_MIXINS = List.of(
            "dev.sterner.guardvillagers.mixin.GuardEntityAnimatedPlayerMixin"
    );

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        return BLOCKED_MIXINS.contains(mixinClassName);
    }
}