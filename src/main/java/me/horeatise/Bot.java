package me.horeatise;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;

public class Bot {
    private boolean bActive = false;
    private String chatPrefix = "[@UnconfiguredBot]";
    private BotConfig config;
    private final String scriptPath;
    private final String botID;
    private boolean shouldHoldForward = false;
    private boolean shouldHoldBack = false;
    private boolean shouldHoldLeft = false;
    private boolean shouldHoldRight = false;
    private boolean shouldHoldAttack = false;
    private boolean shouldHoldSprint = false;
    private boolean shouldHoldSneak = false;
    private BlockPos lastBlockPos = null;

    public String getBotID() {
        return this.botID;
    }

    public String getName() {
        return this.config.name;
    }

    public boolean isActive() {
        return this.bActive;
    }

    public void toggle() {
        bActive = !bActive;
        if (bActive) {
            Utils.LogChat(chatPrefix, "Toggled ON");

            assert MinecraftClient.getInstance().player != null;

            this.lastBlockPos = null;
        } else {
            Utils.LogChat(chatPrefix, "Toggled OFF");
        }
    }

    public void reload() {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(this.scriptPath)) {
            this.config = gson.fromJson(reader, BotConfig.class);
            this.chatPrefix = "[" + this.config.name + "]";
            Utils.LogChat(chatPrefix, "Reloaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.LogChat(chatPrefix, "Reload failed! Check console!");
        }

    }

    public void emergencyHalt() {
        this.bActive = false;
        resetKeyHolds();
    }

    public void tick(MinecraftClient client) {
        if (this.config == null) {
            return;
        }

        this.chatPrefix = "[" + this.config.name + "]";
        if (!this.bActive) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player == null) {
            return;
            //Utils.LogChat(this.chatPrefix, "You are not connected to a server!");
            //toggle();
        }

        if (!isPlayerInBounds()/* || player.getWorld() != initialPlayerWorld*/) {
            emergencyHalt();
            Utils.LogChat(chatPrefix, "Bot stopped due to safety measures!");
        }

        boolean shouldReset = false;
        for (Event event : this.config.events) {
            if (!this.doBlockPositionsMatch(event.coordinates.asBlockPos(), player.getBlockPos())) {
                continue;
            }

            // Checks that an event does not get called continuously if it hasn't been reset
            if (this.lastBlockPos != null && this.doBlockPositionsMatch(this.lastBlockPos, player.getBlockPos())) {
                continue;
            }

            if (event.actions.contains("RELEASE-ALL-KEYS")) {
                shouldReset = true;
            }

            this.handleEvent(event);

            if (shouldReset) {
                this.lastBlockPos = null;
            } else {
                this.lastBlockPos = player.getBlockPos();
            }

            break;
        }
        client.options.sprintKey.setPressed(this.shouldHoldSprint);
        if (this.shouldHoldAttack) {
            doAttack(true);
        }
        client.options.forwardKey.setPressed(this.shouldHoldForward);
        client.options.backKey.setPressed(this.shouldHoldBack);
        client.options.leftKey.setPressed(this.shouldHoldLeft);
        client.options.rightKey.setPressed(this.shouldHoldRight);
        client.options.sneakKey.setPressed(this.shouldHoldSneak);
    }

    public Bot(String scriptName) {
        this.scriptPath = MinecraftClient.getInstance().runDirectory + "/config/Skyblock Bots/bots/" + scriptName + ".json";
        this.botID = scriptName;
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(this.scriptPath)) {
            //Read JSON file
            this.config = gson.fromJson(reader, BotConfig.class);

            this.chatPrefix = "[" + config.name + "]";

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetKeyHolds() {
        this.shouldHoldBack = false;
        this.shouldHoldLeft = false;
        this.shouldHoldAttack = false;
        this.shouldHoldRight = false;
        this.shouldHoldSprint = false;
        this.shouldHoldForward = false;
    }

    private void rotateCamera(float yaw, float pitch) {
        Entity camera = MinecraftClient.getInstance().getCameraEntity();
        assert camera != null;

        camera.setYaw(yaw);
        camera.setPitch(pitch);
    }

    private boolean isPlayerInBounds() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        BlockPos playerPos = player.getBlockPos();
        BlockPos lowerBound = new BlockPos(
                Math.min(this.config.limits.firstPosition.x, this.config.limits.secondPosition.x),
                Math.min(this.config.limits.firstPosition.y, this.config.limits.secondPosition.y),
                Math.min(this.config.limits.firstPosition.z, this.config.limits.secondPosition.z)
        );
        BlockPos upperBound = new BlockPos(
                Math.max(this.config.limits.firstPosition.x, this.config.limits.secondPosition.x),
                Math.max(this.config.limits.firstPosition.y, this.config.limits.secondPosition.y),
                Math.max(this.config.limits.firstPosition.z, this.config.limits.secondPosition.z)
        );
        return playerPos.getX() >= lowerBound.getX() && playerPos.getX() <= upperBound.getX()
                && playerPos.getY() >= lowerBound.getY() && playerPos.getY() <= upperBound.getY()
                && playerPos.getZ() >= lowerBound.getZ() && playerPos.getZ() <= upperBound.getZ();
    }

    private void handleEvent(Event event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
            BotCommandManager commandManager = new BotCommandManager();

            for (String action : event.actions) {
                BotCommandManager.ParsedBotCommand parsedCommand = commandManager.parse(action);
                switch (parsedCommand.command) {
                    case RotateYawAt -> rotateCamera(Integer.parseInt(parsedCommand.args.get(0)), player.getPitch());
                    case RotatePitchAt -> rotateCamera(player.getYaw(), Integer.parseInt(parsedCommand.args.get(0)));
                    case RotateYawBy ->
                            rotateCamera(player.getYaw() + Integer.parseInt(parsedCommand.args.get(0)), player.getPitch());
                    case RotatePitchBy ->
                            rotateCamera(player.getYaw(), player.getPitch() + Integer.parseInt(parsedCommand.args.get(0)));
                    case HoldForward ->
                            this.shouldHoldForward = true; // TODO: Maybe these can be set only once, not every tick (ToggleKey(sth here))
                    case HoldBack -> this.shouldHoldBack = true;
                    case HoldLeft -> this.shouldHoldLeft = true;
                    case HoldRight -> this.shouldHoldRight = true;
                    case HoldSprint -> this.shouldHoldSprint = true;
                    case HoldSneak -> this.shouldHoldSneak = true;
                    case HoldAttack -> this.shouldHoldAttack = true;
                    case ReleaseAllKeys ->
                            this.shouldHoldForward = this.shouldHoldBack = this.shouldHoldLeft = this.shouldHoldRight = this.shouldHoldSprint = this.shouldHoldSneak = this.shouldHoldAttack = false;
                    case AllowRepetition -> this.lastBlockPos = null;
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
                }
            }
    }

    private boolean doBlockPositionsMatch(@NotNull BlockPos pos1, @NotNull BlockPos pos2) {
        return pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
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

    private static class BotConfig {
        String name;
        Limits limits;
        List<Event> events;
    }

    private static class Limits {
        @SerializedName("first_postion")
        Position firstPosition;

        @SerializedName("second_position")
        Position secondPosition;
    }

    private static class Position {
        int x;
        int y;
        int z;
    }

    private static class Event {
        Coordinates coordinates;
        List<String> actions;
    }

    private static class Coordinates {
        int x;
        int y;
        int z;

        BlockPos asBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    private static class BotCommandManager {
        public enum BotCommands {
            RotateYawAt,
            RotatePitchAt,
            RotateYawBy,
            RotatePitchBy,
            HoldForward,
            HoldBack,
            HoldLeft,
            HoldRight,
            HoldSprint,
            HoldSneak,
            HoldAttack,
            ReleaseAllKeys,
            AllowRepetition,
            ToggleOff,
            AimAtBlockFromBlockPos
        }

        public Map<BotCommands, String> regexes = new HashMap<>(){
            {
                put(BotCommands.RotateYawAt, "ROTATE-YAW-AT (-?\\d+(\\.\\d+)?)");
                put(BotCommands.RotatePitchAt, "ROTATE-PITCH-AT (-?\\d+(\\.\\d+)?)");
                put(BotCommands.RotateYawBy, "ROTATE-YAW-BY (-?\\d+(\\.\\d+)?)");
                put(BotCommands.RotatePitchBy, "ROTATE-PITCH-BY (-?\\d+(\\.\\d+)?)");
                put(BotCommands.HoldForward, "HOLD-FORWARD");
                put(BotCommands.HoldBack, "HOLD-BACK");
                put(BotCommands.HoldLeft, "HOLD-LEFT");
                put(BotCommands.HoldRight, "HOLD-RIGHT");
                put(BotCommands.HoldSprint, "HOLD-SPRINT");
                put(BotCommands.HoldSneak, "HOLD-SNEAK");
                put(BotCommands.HoldAttack, "HOLD-ATTACK");
                put(BotCommands.ReleaseAllKeys, "RELEASE-ALL-KEYS");
                put(BotCommands.AllowRepetition, "ALLOW-REPETITION");
                put(BotCommands.ToggleOff, "TOGGLE-OFF");
                put(BotCommands.AimAtBlockFromBlockPos, "^AIM-BLOCK-AT\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)$");
            }
        };

        public static class ParsedBotCommand {
            public BotCommands command = null;
            public List<String> args = null;

            public ParsedBotCommand(BotCommands command, List<String> args) {
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

        private @Nullable BotCommands getType(String command) {
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
}
