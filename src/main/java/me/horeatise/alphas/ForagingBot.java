package me.horeatise.alphas;

import me.horeatise.AimingHelper;
import me.horeatise.Utils;
import me.horeatise.mixin.ItemUseInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ForagingBot {
    private final String chatPrefix = "!Only for Sound! [Foraging Bot - Alpha]";
    private boolean bActive = false;
    private boolean shouldHoldAttack = false;
    private boolean shouldHoldUse = false;
    private boolean shouldWait = false;
    private long waitTime;
    private BlockPos allowedBlockPos = new BlockPos(18, 100, 31);
    private int actionIndex = 0;
    private List<String> actions = new ArrayList<>(
            List.of(
                    "HOLD-USE",
                    "SELECT-SLOT 7",
                    "ROTATE-YAW-AT -5", "ROTATE-PITCH-AT 20",
                    "WAIT-MILLIS 250",
                    "ROTATE-YAW-AT 34", "ROTATE-PITCH-AT 18",
                    "WAIT-MILLIS 250",
                    "SELECT-SLOT 6",
                    "RELEASE-ALL-KEYS",
                    "HOLD-USE",
                    "WAIT-MILLIS 1000",
                    "RELEASE-ALL-KEYS",
                    "SELECT-SLOT 5",
                    "HOLD-ATTACK",
                    "WAIT-MILLIS 500",
                    "RELEASE-ALL-KEYS"
            ));

    public boolean isActive() {
        return this.bActive;
    }
    private void handleAction(String action) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        ForagingBot.BotCommandManager commandManager = new ForagingBot.BotCommandManager();

        ForagingBot.BotCommandManager.ParsedBotCommand parsedCommand = commandManager.parse(action);
        switch (parsedCommand.command) {
            case RotateYawAt -> rotateCamera(Integer.parseInt(parsedCommand.args.get(0)), player.getPitch());
            case RotatePitchAt -> rotateCamera(player.getYaw(), Integer.parseInt(parsedCommand.args.get(0)));
            case RotateYawBy ->
                    rotateCamera(player.getYaw() + Integer.parseInt(parsedCommand.args.get(0)), player.getPitch());
            case RotatePitchBy ->
                    rotateCamera(player.getYaw(), player.getPitch() + Integer.parseInt(parsedCommand.args.get(0)));
            case HoldAttack -> this.shouldHoldAttack = true;
            case HoldUse -> this.shouldHoldUse = true;
            case WaitMillis -> {
                this.shouldWait = true;
                this.waitTime = System.currentTimeMillis() + Integer.parseInt(parsedCommand.args.get(0));
            }
            case ReleaseAllKeys ->
                    this.shouldHoldAttack = this.shouldHoldUse = false;
            case ToggleOff -> {
                Utils.LogChat(chatPrefix, "Bot has toggled itself off! BYEE!");
                this.bActive = false;
            }
            case AimAtBlockFromBlockPos -> {
                Vec3d aimPoint = AimingHelper.findVisibleAimingPoint(new BlockPos(Integer.parseInt(parsedCommand.args.get(0)), Integer.parseInt(parsedCommand.args.get(1)), Integer.parseInt(parsedCommand.args.get(2))).toCenterPos(), player.getPos());
                if (aimPoint != null) {
                    AimingHelper.aimAtBlock(aimPoint);
                }
            }
            case SelectSlot -> selectHotbarSlot(Integer.parseInt(parsedCommand.args.get(0)));
        }
    }

    private void rotateCamera(float yaw, float pitch) {
        Entity camera = MinecraftClient.getInstance().getCameraEntity();
        assert camera != null;

        camera.setYaw(yaw);
        camera.setPitch(pitch);
    }


    private void resetKeyHolds() {
        this.shouldHoldAttack = false;
        this.shouldHoldUse = false;
    }
    public void emergencyHalt() {
        this.bActive = false;
        resetKeyHolds();
    }

    public void toggle() {
        bActive = !bActive;
        if (bActive) {
            Utils.LogChat(chatPrefix, "Toggled ON");
        } else {
            Utils.LogChat(chatPrefix, "Toggled OFF");
        }
    }

    private boolean doBlockPositionsMatch(@NotNull BlockPos pos1, @NotNull BlockPos pos2) {
        return pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
    }

    private void selectHotbarSlot(int slotID) {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;
        PlayerInventory playerInventory = client.player.getInventory();
        if (PlayerInventory.isValidHotbarIndex(slotID)) {
            playerInventory.selectedSlot = slotID;
        }
    }

    public void tick(MinecraftClient client) {
        if (!this.bActive) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player == null) {
            return;
        }

        if (!this.doBlockPositionsMatch(this.allowedBlockPos, player.getBlockPos())) {
            emergencyHalt();
            Utils.LogChat(chatPrefix, "Bot stopped due to safety measures!");
        }

        if (!(this.shouldWait && System.currentTimeMillis() <= this.waitTime)) {
            this.shouldWait = false;

            this.handleAction(actions.get(actionIndex));
            actionIndex++;
            if (actionIndex >= actions.size()){
                actionIndex = 0;
            }
        }

        if(this.shouldHoldUse){
            MinecraftClient mc = MinecraftClient.getInstance();
            ((ItemUseInvoker) mc).invokeDoItemUse();
        }
        if (this.shouldHoldAttack) {
            doAttack(true);
        }
    }

    private void doAttack(boolean ignoreAttackCooldown) {
        //  MinecraftClient > tick() > line 1652
        //  Prevents attacking while a screen is rendered
        //  I've added an argument that toggles ON/OFF everything related to attackCooldown
        // if (this.currentScreen != null) {
        //     this.attackCooldown = 10000;
        // }

        assert MinecraftClient.getInstance().interactionManager != null;
        assert MinecraftClient.getInstance().player != null;
        assert MinecraftClient.getInstance().world != null;

        if (MinecraftClient.getInstance().attackCooldown > 0 && !ignoreAttackCooldown) {
            return;
        }
        if (MinecraftClient.getInstance().crosshairTarget == null) {
            //LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (MinecraftClient.getInstance().interactionManager.hasLimitedAttackSpeed()) {
                MinecraftClient.getInstance().attackCooldown = 10;
            }
            return;
        }
        if (MinecraftClient.getInstance().player.isRiding()) {
            return;
        }
        ItemStack itemStack = MinecraftClient.getInstance().player.getStackInHand(Hand.MAIN_HAND);
        if (!itemStack.isItemEnabled(MinecraftClient.getInstance().world.getEnabledFeatures())) {
            return;
        }
        boolean bl = false;
        switch (MinecraftClient.getInstance().crosshairTarget.getType()) {
            case ENTITY: {
                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                break;
            }
            case BLOCK: {
                BlockHitResult blockHitResult = (BlockHitResult) MinecraftClient.getInstance().crosshairTarget;
                BlockPos blockPos = blockHitResult.getBlockPos();
                if (!MinecraftClient.getInstance().world.getBlockState(blockPos).isAir()) {
                    assert MinecraftClient.getInstance().interactionManager != null;
                    MinecraftClient.getInstance().interactionManager.attackBlock(blockPos, blockHitResult.getSide());
                    if (!MinecraftClient.getInstance().world.getBlockState(blockPos).isAir()) break;
                    bl = true;
                    break;
                }
            }
            case MISS: {
                assert MinecraftClient.getInstance().interactionManager != null;
                if (MinecraftClient.getInstance().interactionManager.hasLimitedAttackSpeed()) {
                    MinecraftClient.getInstance().attackCooldown = 10;
                }
                MinecraftClient.getInstance().player.resetLastAttackedTicks();
            }
        }
        MinecraftClient.getInstance().player.swingHand(Hand.MAIN_HAND);
    }

    private static class BotCommandManager {
        public enum BotCommands {
            RotateYawAt,
            RotatePitchAt,
            RotateYawBy,
            RotatePitchBy,
            HoldAttack,
            HoldUse,
            WaitMillis,
            ReleaseAllKeys,
            AllowRepetition,
            ToggleOff,
            AimAtBlockFromBlockPos,
            SelectSlot
        }

        public Map<ForagingBot.BotCommandManager.BotCommands, String> regexes = new HashMap<>(){
            {
                put(BotCommands.RotateYawAt, "ROTATE-YAW-AT (-?\\d+(\\.\\d+)?)");
                put(BotCommands.RotatePitchAt, "ROTATE-PITCH-AT (-?\\d+(\\.\\d+)?)");
                put(BotCommands.RotateYawBy, "ROTATE-YAW-BY (-?\\d+(\\.\\d+)?)");
                put(BotCommands.RotatePitchBy, "ROTATE-PITCH-BY (-?\\d+(\\.\\d+)?)");
                put(BotCommands.WaitMillis, "WAIT-MILLIS (-?\\d+(\\.\\d+)?)");
                put(BotCommands.HoldAttack, "HOLD-ATTACK");
                put(BotCommands.HoldUse, "HOLD-USE");
                put(BotCommands.ReleaseAllKeys, "RELEASE-ALL-KEYS");
                put(BotCommands.AllowRepetition, "ALLOW-REPETITION");
                put(BotCommands.ToggleOff, "TOGGLE-OFF");
                put(BotCommands.AimAtBlockFromBlockPos, "^AIM-BLOCK-AT\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)$");
                put(BotCommands.SelectSlot, "SELECT-SLOT (-?\\d+(\\.\\d+)?)");

            }
        };

        public static class ParsedBotCommand {
            public ForagingBot.BotCommandManager.BotCommands command = null;
            public List<String> args = null;

            public ParsedBotCommand(ForagingBot.BotCommandManager.BotCommands command, List<String> args) {
                this.command = command;
                this.args = args;
            }
        }

        public boolean isValid(String command) {
            for (String regex : regexes.values()) {
                if (Pattern.matches(regex, command)) {
                    return true;
                }
            }
            return false;
        }

        private BotCommands getType(String command) {
            for (Map.Entry<BotCommands, String> entry : regexes.entrySet()) {
                if (Pattern.matches(entry.getValue(), command)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public @NotNull ParsedBotCommand parse(String command) {
            ParsedBotCommand parsedCommand = new ParsedBotCommand(getType(command), null);
            if (getType(command) == null) {
                Utils.LogChat("[ERROR]", "Invalid command '" + command + "'");
            } else {
                List<String> args = new ArrayList<String>(List.of(command.split("\\s+")));
                args.remove(0);
                parsedCommand.args = args;
            }
            return parsedCommand;
        }
    }

    class RunnableLogic implements Runnable {
        private Thread t;
        private String threadName;

        RunnableLogic( String name) {
            threadName = name;
            System.out.println("Creating " +  threadName );
        }
        public void run() {
            System.out.println("Running " +  threadName );
            try {
                for(int i = 4; i > 0; i--) {
                    System.out.println("Thread: " + threadName + ", " + i);
                    // Let the thread sleep for a while.
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                System.out.println("Thread " +  threadName + " interrupted.");
            }
            System.out.println("Thread " +  threadName + " exiting.");
        }

        public void start () {
            System.out.println("Starting " +  threadName );
            if (t == null) {
                t = new Thread (this, threadName);
                t.start ();
            }
        }
    }
}
