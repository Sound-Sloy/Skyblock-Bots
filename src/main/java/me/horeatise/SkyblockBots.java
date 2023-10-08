package me.horeatise;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.horeatise.alphas.ForagingBot;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

public class SkyblockBots implements ModInitializer {

    private final MithrilBot mithrilBot = new MithrilBot();
    private final CaneBot caneBot = new CaneBot();
    private final MovementBlocks movementBlocks = new MovementBlocks();

    private final String chatPrefix = "&4[Skyblock Bots]";

    private ArrayList<Bot> bots = new ArrayList<>();
    private ForagingBot foragingBot = new ForagingBot();


    @Override
    public void onInitialize() {
        CreateDirectories();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(ClientCommandManager.literal("loadbot")
                .then(ClientCommandManager.argument("FileName", StringArgumentType.word())
                        .executes(ctx -> {
                            String filename = StringArgumentType.getString(ctx, "FileName");
                            if (!new File(MinecraftClient.getInstance().runDirectory + "/config/Skyblock Bots/bots/" + filename + ".json").isFile()) {
                                Utils.LogChat("ERROR", "No Such File");
                                return 1;
                            }
                            bots.add(new Bot(filename));
                            return 0;
                        }))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(ClientCommandManager.literal("bot")
                .then(ClientCommandManager.literal("foragingtoggle")
                                .executes(ctx -> {
                                    foragingBot.toggle();
                                    return 0;
                                }))
                .then(ClientCommandManager.literal("toggle")
                        .then(ClientCommandManager.argument("Bot ID", StringArgumentType.word())
                                .executes(ctx -> {
                                    this.ToggleBot(StringArgumentType.getString(ctx, "Bot ID"));
                                    return 0;
                                })))
                .then(ClientCommandManager.literal("load")
                        .then(ClientCommandManager.argument("Bot ID", StringArgumentType.word())
                                .executes(ctx -> {
                                    this.LoadBot(StringArgumentType.getString(ctx, "Bot ID"));
                                    return 0;
                                })))
                .then(ClientCommandManager.literal("unload")
                        .then(ClientCommandManager.argument("Bot ID", StringArgumentType.word())
                                .executes(ctx -> {
                                    this.UnloadBot(StringArgumentType.getString(ctx, "Bot ID"));
                                    return 0;
                                })))
                .then(ClientCommandManager.literal("reload")
                        .then(ClientCommandManager.argument("Bot ID", StringArgumentType.word())
                                .executes(ctx -> {
                                    this.ReloadBot(StringArgumentType.getString(ctx, "Bot ID"));
                                    return 0;
                                })))
                .then(ClientCommandManager.literal("target")
                        .then(ClientCommandManager.argument("block x", FloatArgumentType.floatArg())
                                .then(ClientCommandManager.argument("block y", FloatArgumentType.floatArg())
                                        .then(ClientCommandManager.argument("block z", FloatArgumentType.floatArg())
                                            .executes(ctx -> {
                                                float blockX = FloatArgumentType.getFloat(ctx, "block x");
                                                float blockY = FloatArgumentType.getFloat(ctx, "block y");
                                                float blockZ = FloatArgumentType.getFloat(ctx, "block z");
                                                assert MinecraftClient.getInstance().cameraEntity != null;
                                                Utils.RotateCameraAt(Utils.calculateYawPitch(MinecraftClient.getInstance().cameraEntity.getEyePos(), new Vec3d(blockX, blockY, blockZ)));
                                                return 0;
                                            })))))
                .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            this.ListBots();
                            return 0;
                        }))
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(toggleMithril()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(toggleCane()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(movementBlocks.ToggleCommand()));
        //PlayerBlockBreakEvents.AFTER.register(mithrilBot::BlockBreakEvent);
        //ClientTickEvents.START_CLIENT_TICK.register(movementBlocks::onTick);

        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);

        /*
        private LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
		return ClientCommandManager.literal("rotatecamera")
				.then(ClientCommandManager.argument("yaw", FloatArgumentType.floatArg(-180, 180))
						.then(ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(-90, 90))
								.executes(ctx -> {
									float yaw = FloatArgumentType.getFloat(ctx, "yaw");
									float pitch = FloatArgumentType.getFloat(ctx, "pitch");
									// Call the method to rotate the player's camera smoothly here
									rotateCamera(yaw, pitch);
									return 1;
								})));
	}
         */
    }


    private void CreateDirectories() {
        String botsDirectory = MinecraftClient.getInstance().runDirectory + "/config/Skyblock Bots/bots";
        try {
            Path path = Paths.get(botsDirectory);
            Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println("Failed to create directory!" + e.getMessage());
            Utils.LogChat(chatPrefix, "&4[ERROR] Failed to create 'config/Skyblock Bots/bots' directory!");
        }
    }

    public void onTick(MinecraftClient client) {
        for (Bot bot : bots) {
            if (bot.isActive())
                bot.tick(MinecraftClient.getInstance());
        }
        if (foragingBot.isActive()) {
            foragingBot.tick(MinecraftClient.getInstance());
        }
    }

    private void ToggleBot(String botID) {
        for (Bot bot : bots) {
            if (Objects.equals(bot.getBotID(), botID)) {
                bot.toggle();
                return;
            }
        }
        Utils.LogChat(this.chatPrefix, "&4[ERROR] There is no loaded bot with ID '" + botID + "'! See '/bot list'");
    }

    private void ReloadBot(String botID) {
        for (Bot bot : bots) {
            if (Objects.equals(bot.getBotID(), botID)) {
                bot.reload();
                return;
            }
        }
        Utils.LogChat(this.chatPrefix, "&4[ERROR] There is no loaded bot with ID '" + botID + "'! See '/bot list'");
    }

    private void LoadBot(String botID) {
        if (!new File(MinecraftClient.getInstance().runDirectory + "/config/Skyblock Bots/bots/" + botID + ".json").isFile()) {
            Utils.LogChat(this.chatPrefix, "&4[ERROR] There is no file named '" + botID + ".json' in '" + MinecraftClient.getInstance().runDirectory + "/config/Skyblock Bots/bots' !");
            return;
        }
        for (Bot bot : bots) {
            if (Objects.equals(bot.getBotID(), botID)) {
                Utils.LogChat(this.chatPrefix, "&4[ERROR] There is already a bot loaded with ID '" + botID + "'! Consider '/bot reload " + botID + "'");
                return;
            }
        }

        bots.add(new Bot(botID));
        Utils.LogChat(this.chatPrefix, "&a[LOAD] Bot loaded successfully! (" + botID + ")");

    }

    private void UnloadBot(String botID) {
        boolean success;
        for (int i = 0; i < bots.size(); i++) {
            if (Objects.equals(bots.get(i).getBotID(), botID)) {
                try {
                    bots.remove(i);
                    success = true;
                } catch (Exception e) {
                    success = false;
                    e.printStackTrace();
                }
                if (success) {
                    Utils.LogChat(this.chatPrefix, "&a Bot unloaded successfully!");
                } else {
                    Utils.LogChat(this.chatPrefix, "&4[ERROR] Bot unload failed! Check console for stacktrace!");
                }
                return;
            }
        }

        Utils.LogChat(this.chatPrefix, "&4[ERROR] There is no loaded bot with ID '" + botID + "'! See '/bot list'");

    }

    private void ListBots() {
        if (bots.isEmpty()) {
            Utils.LogChat(this.chatPrefix, "There are no loaded bots.");
            return;
        }
        Utils.LogChat(this.chatPrefix, "Loaded bots:");
        for (Bot bot : bots) {
            Utils.LogChat(this.chatPrefix, "BotID: " + bot.getBotID() + "  Name: " + bot.getName());
        }
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> toggleMithril() {
        return ClientCommandManager.literal("mithril")
                .executes(ctx -> {
                    mithrilBot.Toggle();
                    return 1;
                });
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> toggleCane() {
        return ClientCommandManager.literal("cane")
                .executes(ctx -> {
                    caneBot.Toggle();
                    return 1;
                });
    }
}
