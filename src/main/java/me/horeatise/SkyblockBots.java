package me.horeatise;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class SkyblockBots implements ModInitializer {

    private boolean minerToggle = false;
    private MithrilBot mithrilBot = new MithrilBot();

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(toggleMithril()));
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> mithrilBot.BlockBreakEvent(world, player,pos,state,blockEntity));
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> toggleMithril() {
        return ClientCommandManager.literal("mithril")
                                .executes(ctx -> {
                                    mithrilBot.Toggle();
                                    return 1;
                                });
    }
}
