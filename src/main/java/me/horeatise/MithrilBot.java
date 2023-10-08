package me.horeatise;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector2d;

import java.util.Vector;


public class MithrilBot {
    private static class SndBlock {
        public Block block;
        public BlockPos blockPos;

        public SndBlock(Block block, BlockPos blockPos) {
            this.block = block;
            this.blockPos = blockPos;
        }
    }
    public boolean active = false;
    private boolean killCurrentGoal = false;
    private final String chatPrefix = "[Mithril Miner]";

    public void Toggle() {
        active = !active;
        if(active) {
            Utils.LogChat(chatPrefix, "Toggled ON");
        }
        else {
            Utils.LogChat(chatPrefix, "Toggled OFF");
        }
    }

    public void BlockBreakEvent(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity){
        if (!active) return;

        killCurrentGoal = true;

        NewGoal();
    }

    private void MakePlayerLookAtBlock(BlockPos blockPos){
        Vector2d rotationVec = Utils.GetCameraRotationAngleFromBlockPos(blockPos);
        assert MinecraftClient.getInstance().cameraEntity != null;
        MinecraftClient.getInstance().cameraEntity.setYaw((float) rotationVec.x);
        MinecraftClient.getInstance().cameraEntity.setPitch((float) rotationVec.y);
    }

    private BlockPos GetTargetedBlockPos(){
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult hit = client.crosshairTarget;

        switch (hit.getType()) {
            case MISS, ENTITY -> {
                assert client.player != null;
                return client.player.getBlockPos();
            }
            //nothing near enough

            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                return blockHit.getBlockPos();
            }
        }
        assert client.player != null;
        return client.player.getBlockPos();
    }



    private Vector<SndBlock> mblocks = new Vector<SndBlock>();
    private Vector<BlockPos> bannedBlockPoses = new Vector<BlockPos>();
    private BlockPos currentGoal;

    private boolean IsBlockPosBanned(BlockPos blockPos){
        for(BlockPos bannedBlockPos : bannedBlockPoses){
            if(bannedBlockPos.getX() == blockPos.getX() && bannedBlockPos.getY() == blockPos.getY() && bannedBlockPos.getZ() == blockPos.getZ()){
                return true;
            }
        }
        return false;
    }

    private void NewGoal() {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;
        int playerX = client.player.getBlockX();
        int playerY = client.player.getBlockY();
        int playerZ = client.player.getBlockZ();

        for(int x = playerX - 5; x <= playerX + 5; x++){
            for(int y = playerY - 5; y<= playerY + 5; y++){
                for(int z = playerZ - 5; z <= playerY + 5; z++ ){
                    BlockPos blockPos = new BlockPos(x,y,z);
                    Block block = Utils.GetBlockFromBlockPos(blockPos);
                    if(block.equals(Blocks.POLISHED_DIORITE) || block.equals(Blocks.GRAY_WOOL) || block.equals(Blocks.CYAN_TERRACOTTA)){
                        assert client.cameraEntity != null;
                        if (blockPos.isWithinDistance(client.cameraEntity.getBlockPos(), 4)){
                            mblocks.add(new SndBlock(block, blockPos));
                        }
                    }
                }
            }
        }

        BlockPos closestBlockPos = GetTargetedBlockPos();
        double smallestDistance = 0;
        boolean smallestDistanceChosen = false;

        for (SndBlock block : mblocks) {

            if (IsBlockPosBanned(block.blockPos)){
                continue;
            }

            if (block.block.equals(Blocks.POLISHED_DIORITE)) {
                MakePlayerLookAtBlock(block.blockPos);
                break;
            }

            BlockPos targetedBlockPos = GetTargetedBlockPos();
            double distance = Utils.GetBlockPosDistance(targetedBlockPos, block.blockPos);
            if (!smallestDistanceChosen) {
                smallestDistance = distance;
                closestBlockPos = block.blockPos;
                smallestDistanceChosen = true;
            }
            else {
                if(distance < smallestDistance){
                    smallestDistance = distance;
                    closestBlockPos = block.blockPos;
                }
            }
        }

        MakePlayerLookAtBlock(closestBlockPos);
        BlockPos targetedBlockPos = GetTargetedBlockPos();
        if (targetedBlockPos.getX() != closestBlockPos.getX() && targetedBlockPos.getY() != closestBlockPos.getY() && targetedBlockPos.getZ() != closestBlockPos.getZ()){
            bannedBlockPoses.add(closestBlockPos);
            NewGoal();
        }

        currentGoal = closestBlockPos;

        // Use tweakeroo's HoldAttack tweak
        // TODO: Replace this with actual block break handling

        // movement this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.getYaw(), this.getPitch(), this.isOnGround()));



    }



}
