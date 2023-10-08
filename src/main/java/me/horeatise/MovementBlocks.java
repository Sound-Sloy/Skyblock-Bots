package me.horeatise;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class MovementBlocks {
    private boolean bActive = false;
    private final String chatPrefix = "[Movement Blocks]";
    private final ArrayList<Block> commandBlocks = new ArrayList<>(List.of(
            Blocks.DIAMOND_BLOCK,
            Blocks.EMERALD_BLOCK,
            Blocks.REDSTONE_BLOCK,
            Blocks.IRON_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.LAPIS_BLOCK,
            Blocks.OBSIDIAN
    ));

    private enum Commands {
        MoveLeftUntilStopped,
        MoveRightUntilStopped,
        MoveForwardUntilStopped,
        MoveBackwardsUntilStopped,
        Rotate180DegreesAndGoForward,
        Rotate180DegreesAndGoBackward,
        Stop,
        NONE
    }




    private Commands currentCommand = Commands.NONE;
    public LiteralArgumentBuilder<FabricClientCommandSource> ToggleCommand() {
        return ClientCommandManager.literal("movementblocks")
                .executes(ctx -> {
                    bActive = !bActive;
                    if(bActive) {
                        Utils.LogChat(chatPrefix, "Toggled ON");
                    }
                    else {
                        Utils.LogChat(chatPrefix, "Toggled OFF");
                    }
                    return 1;
                });
    }

    public void onTick(MinecraftClient minecraftClient) {
        if (!bActive) return;

        ClientPlayerEntity player = minecraftClient.player;
        ClientWorld world = minecraftClient.world;
        assert player != null;
        assert world != null;

        //Utils.LogChat(chatPrefix, minecraftClient.player.getBlockPos().toString());
        Block blockUnderPlayer = Utils.GetBlockFromBlockPos(new BlockPos(player.getBlockX(), player.getBlockY()-1, player.getBlockZ()));

        if (blockUnderPlayer.equals(Blocks.DIAMOND_BLOCK)){
            currentCommand = Commands.MoveRightUntilStopped;
        }
        if (blockUnderPlayer.equals(Blocks.EMERALD_BLOCK)){
            currentCommand = Commands.MoveLeftUntilStopped;
        }
        if (blockUnderPlayer.equals(Blocks.IRON_BLOCK)){
            currentCommand = Commands.MoveForwardUntilStopped;
        }
        if (blockUnderPlayer.equals(Blocks.GOLD_BLOCK)){
            currentCommand = Commands.MoveBackwardsUntilStopped;
        }
        if (blockUnderPlayer.equals(Blocks.REDSTONE_BLOCK)){
            currentCommand = Commands.Rotate180DegreesAndGoBackward;
        }
        if (blockUnderPlayer.equals(Blocks.LAPIS_BLOCK)){
            currentCommand = Commands.Rotate180DegreesAndGoBackward;
        }
        if (blockUnderPlayer.equals(Blocks.OBSIDIAN)){
            currentCommand = Commands.Stop;
        }

        switch (currentCommand) {
            case MoveRightUntilStopped -> {
                //player.move(MovementType.SELF, )
                //player.move(MovementType.SELF, new Vec3d(10,0,0));
                //player.networkHandler.sendPacket(new PlayerInputC2SPacket(1,0,false,false));
                minecraftClient.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(1,0,false,false));
            }
        }
    }
}
