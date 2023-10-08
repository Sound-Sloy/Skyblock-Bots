package me.horeatise;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AfterPlayerChange;

import java.io.File;
import java.util.*;

public class CaneBot {
    private boolean bActive = false;
    private final String chatPrefix = "[Cane Farmer]";
    private BlockPos bound1 = new BlockPos(-142, 73, -49);
    private BlockPos bound2 = new BlockPos(238, 71, -143);
    private ClientWorld initialPlayerWorld;
    private Map<BlockPos, BlockPosEvent> blockPosEvents = new HashMap<>();
    private boolean shouldHoldForward = false;
    private boolean shouldHoldBack = false;
    private boolean shouldHoldLeft = false;
    private boolean shouldHoldRight = false;
    private boolean shouldHoldAttack = false;
    private boolean shouldHoldSprint = false;

    private void ResetKeyHolds(){
        this.shouldHoldBack = false;
        this.shouldHoldLeft = false;
        this.shouldHoldAttack = false;
        this.shouldHoldRight = false;
        this.shouldHoldSprint = false;
        this.shouldHoldForward = false;
    }

    public class BlockPosEvent {
        public BlockPos blockPos;
        public ArrayList<String> actions;
        public boolean alreadyTriggered = false;
        public BlockPosEvent(BlockPos blockPos, ArrayList<String> actions){
            this.blockPos = blockPos;
            this.actions = actions;
            this.alreadyTriggered = false;
        }

        public void HandleActions() {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            assert player != null;
            MinecraftClient client = MinecraftClient.getInstance();
            //if(this.alreadyTriggered) return;
            for(String action : actions){
                this.alreadyTriggered = true;
                switch (action){
                    case "ROTATEYAW180" -> {
                        rotateCamera(player.getYaw()+ 180f, player.getPitch());
                    }
                    case "ROTATEYAW90" -> {
                        rotateCamera(player.getYaw()+ 90f, player.getPitch());
                    }
                    case "ROTATEYAW45" -> {
                        rotateCamera(player.getYaw()+ 45f, player.getPitch());
                    }
                    case "MOVEFORWARD" -> {
                        shouldHoldForward = true;
                    }
                    case "MOVEBACK" -> {
                        shouldHoldBack = true;
                    }
                    case "MOVELEFT" -> {
                        shouldHoldLeft = true;
                    }
                    case "MOVERIGHT" -> {
                        shouldHoldRight = true;
                    }
                    case "SPRINT" -> {
                        shouldHoldSprint = true;
                    }
                    case "HOLDATTACK" -> {
                        shouldHoldAttack = true;
                    }
                    case "RELEASEATTACK" -> {
                        shouldHoldAttack = false;
                    }
                    case "CANCELALL" -> {
                        ResetKeyHolds();
                    }
                    case "RESET" -> {
                        alreadyTriggered = false;
                    }
                }
            }
        }
    }

    private void rotateCamera(float yaw, float pitch) {
        Entity camera = MinecraftClient.getInstance().getCameraEntity();

        // Calculate the rotation difference and divide it into steps for smooth transition
        assert camera != null;
        float yawDiff = yaw - camera.getYaw();
        float pitchDiff = pitch - camera.getPitch();
        int steps = 20; // Number of steps for the transition



        float yawStep = yawDiff / steps;
        float pitchStep = pitchDiff / steps;

        // Gradually update the camera's rotation angles
        for (int i = 0; i < steps; i++) {
            float newYaw = camera.getYaw() + yawStep;
            float newPitch = camera.getPitch() + pitchStep;
            System.out.println(newYaw + " " + newPitch);

            camera.setYaw(newYaw);
            camera.setPitch(newPitch);

			/*
			In the context of the provided code, the line
			MinecraftClient.getInstance().getEntityRenderDispatcher().rotationChanged(entity);
			is being used to inform the game's rendering system that the player's (or entity's) rotation is being
			smoothly adjusted. This is necessary to ensure that the rendered visuals of the player accurately
			reflect the gradual rotation changes being made in the mod. Without this call, the rendered
			representation of the player might not update in real-time as the rotation changes.
			 */

            //MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes();
            try {
                Thread.sleep(20); // Sleep for a short duration between steps
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void Toggle() {
        bActive = !bActive;
        if(bActive) {
            Utils.LogChat(chatPrefix, "Toggled ON");

            assert MinecraftClient.getInstance().player != null;
            initialPlayerWorld = MinecraftClient.getInstance().player.clientWorld;
        }
        else {
            Utils.LogChat(chatPrefix, "Toggled OFF");
        }
    }
    public CaneBot(){
        ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
/*
        this.blockPosEvents.put(new BlockPos(-142, 71, -50), new BlockPosEvent(new BlockPos(-142, 71, -50), new ArrayList<>(List.of("CANCELALL", "MOVEFORWARD", "HOLDATTACK"))));
        this.blockPosEvents.put(new BlockPos(-142, 71, -142), new BlockPosEvent(new BlockPos(-142, 71, -142), new ArrayList<>(List.of("CANCELALL", "MOVERIGHT", "HOLDATTACK"))));
        this.blockPosEvents.put(new BlockPos(-141, 71, -142), new BlockPosEvent(new BlockPos(-141, 71, -142), new ArrayList<>(List.of("CANCELALL", "MOVEBACK", "HOLDATTACK"))));
        this.blockPosEvents.put(new BlockPos(-141, 71, -50), new BlockPosEvent(new BlockPos(-142, 71, -142), new ArrayList<>(List.of("CANCELALL", "MOVERIGHT", "HOLDATTACK"))));

        this.blockPosEvents.put(new BlockPos(-139, 71, -50), new BlockPosEvent(new BlockPos(-142, 71, -50), new ArrayList<>(List.of("CANCELALL", "MOVEFORWARD", "HOLDATTACK"))));
        this.blockPosEvents.put(new BlockPos(-139, 71, -142), new BlockPosEvent(new BlockPos(-142, 71, -142), new ArrayList<>(List.of("CANCELALL", "MOVERIGHT", "HOLDATTACK"))));
        this.blockPosEvents.put(new BlockPos(-138, 71, -142), new BlockPosEvent(new BlockPos(-141, 71, -142), new ArrayList<>(List.of("CANCELALL", "MOVEBACK", "HOLDATTACK"))));
        this.blockPosEvents.put(new BlockPos(-138, 71, -50), new BlockPosEvent(new BlockPos(-142, 71, -142), new ArrayList<>(List.of("CANCELALL", "MOVERIGHT", "HOLDATTACK"))));*/

        int rowCnt = 63;
        for(int i = 0;i<rowCnt-1;i++){
            this.blockPosEvents.put(new BlockPos(-142+i*3, 71, -50), new BlockPosEvent(new BlockPos(-142+i*3, 71, -50), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVEFORWARD"))));
            this.blockPosEvents.put(new BlockPos(-142+i*3, 71, -142), new BlockPosEvent(new BlockPos(-142+i*3, 71, -142), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVERIGHT"))));
            this.blockPosEvents.put(new BlockPos(-141+i*3, 71, -142), new BlockPosEvent(new BlockPos(-141+i*3, 71, -142), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVEBACK"))));
            this.blockPosEvents.put(new BlockPos(-141+i*3, 71, -50), new BlockPosEvent(new BlockPos(-142+i*3, 71, -142), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVERIGHT"))));
        }

        this.blockPosEvents.put(new BlockPos(-142+(rowCnt-1)*3, 71, -50), new BlockPosEvent(new BlockPos(-142+(rowCnt-1)*3, 71, -50), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVEFORWARD"))));
        this.blockPosEvents.put(new BlockPos(-142+(rowCnt-1)*3, 71, -142), new BlockPosEvent(new BlockPos(-142+(rowCnt-1)*3, 71, -142), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVERIGHT"))));
        this.blockPosEvents.put(new BlockPos(-141+(rowCnt-1)*3, 71, -142), new BlockPosEvent(new BlockPos(-141+(rowCnt-1)*3, 71, -142), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVEBACK"))));
        this.blockPosEvents.put(new BlockPos(-141+(rowCnt-1)*3, 71, -49), new BlockPosEvent(new BlockPos(-142+(rowCnt-1)*3, 71, -49), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVELEFT"))));

        this.blockPosEvents.put(new BlockPos(-142, 72, -49), new BlockPosEvent(new BlockPos(-142, 72, -49), new ArrayList<>(List.of("CANCELALL", "HOLDATTACK", "MOVEFORWARD"))));
    }

    private boolean isPlayerInBounds() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        BlockPos playerPos = player.getBlockPos();
        BlockPos lowerBound = new BlockPos(
                Math.min(this.bound1.getX(), this.bound2.getX()),
                Math.min(this.bound1.getY(), this.bound2.getY()),
                Math.min(this.bound1.getZ(), this.bound2.getZ())
        );
        BlockPos upperBound = new BlockPos(
                Math.max(this.bound1.getX(), this.bound2.getX()),
                Math.max(this.bound1.getY(), this.bound2.getY()),
                Math.max(this.bound1.getZ(), this.bound2.getZ())
        );
        return playerPos.getX() >= lowerBound.getX() && playerPos.getX() <= upperBound.getX()
                && playerPos.getY() >= lowerBound.getY() && playerPos.getY() <= upperBound.getY()
                && playerPos.getZ() >= lowerBound.getZ() && playerPos.getZ() <= upperBound.getZ();
    }
    public void emergencyHalt(){
        this.bActive = false;
        ResetKeyHolds();
    }
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player!=null;
        if(!this.bActive) return;
        if(!isPlayerInBounds()/* || player.getWorld() != initialPlayerWorld*/){
            emergencyHalt();
            Utils.LogChat(chatPrefix, "STOP IDIOT!");
            Utils.LogChat(chatPrefix, player.getBlockPos().toString());
        }
        client.options.sprintKey.setPressed(true);
        client.options.attackKey.setPressed(true);
        if (blockPosEvents.containsKey(player.getBlockPos())){
            blockPosEvents.get(player.getBlockPos()).HandleActions();
        }
        client.options.sprintKey.setPressed(this.shouldHoldSprint);
        //client.options.attackKey.setPressed(this.shouldHoldAttack);
        client.options.forwardKey.setPressed(this.shouldHoldForward);
        client.options.backKey.setPressed(this.shouldHoldBack);
        client.options.leftKey.setPressed(this.shouldHoldLeft);
        client.options.rightKey.setPressed(this.shouldHoldRight);
    }

}
