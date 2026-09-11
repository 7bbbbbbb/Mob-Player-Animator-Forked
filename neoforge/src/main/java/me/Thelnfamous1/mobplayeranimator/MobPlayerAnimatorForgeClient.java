package me.Thelnfamous1.mobplayeranimator;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class MobPlayerAnimatorForgeClient {

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!MobPlayerAnimatorClient.isEPCLoaded()) {
            if (event.getPlayer() != null) {
                event.getPlayer().sendSystemMessage(MobPlayerAnimatorClient.getMissingCompatMessage());
            }
        }
    }
}