package me.Thelnfamous1.mobplayeranimator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class MobPlayerAnimatorFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MobPlayerAnimatorClient.init();

        if (!MobPlayerAnimatorClient.isEPCLoaded()) {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                if (client.player != null) {
                    client.player.sendSystemMessage(MobPlayerAnimatorClient.getMissingCompatMessage());
                }
            });
        }
    }
}