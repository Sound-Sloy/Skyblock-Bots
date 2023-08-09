package me.horeatise;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;

public class Utils {
    public static void LogChat(String prefix, String message){
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(Text.literal(prefix + " " + message));
    }

    public static Vector2d GetCameraRotationAngleFromBlockPos(BlockPos blockPos){
        assert MinecraftClient.getInstance().player != null;
        Vec3d playerPos = MinecraftClient.getInstance().player.getPos();
        Vec3d targetPos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);

        Vec3d lookVector = targetPos.subtract(playerPos).normalize();

        double yaw = Math.atan2(lookVector.z, lookVector.x) * 180.0 / Math.PI - 90.0;
        double pitch = Math.atan2(Math.sqrt(lookVector.x * lookVector.x + lookVector.z * lookVector.z), lookVector.y) * 180.0 / Math.PI - 90.0;
        return new Vector2d(yaw,pitch);
    }

    public static double GetBlockPosDistance(BlockPos pos1, BlockPos pos2){
        return Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                Math.pow(pos1.getY() - pos2.getY(), 2) +
                Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }

    public static Block GetBlockFromBlockPos(BlockPos blockPos){
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;
        return client.player.getWorld().getBlockState(blockPos).getBlock();
    }
}
