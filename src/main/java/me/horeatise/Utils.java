package me.horeatise;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2d;

public class Utils {
    public static void LogChat(String prefix, String message) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(Text.literal(prefix + " " + message));
    }

    public static Vector2d GetCameraRotationAngleFromBlockPos(BlockPos blockPos) {
        assert MinecraftClient.getInstance().cameraEntity != null;
        //Vec3d cameraPos = Utils.WorldPosToBlockPosFloats(MinecraftClient.getInstance().cameraEntity.getPos());
        Vec3d cameraPos = MinecraftClient.getInstance().cameraEntity.getPos();
        Vec3d targetPos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);

        Vec3d lookVector = targetPos.subtract(cameraPos).normalize();

        //double yaw = Math.atan2(lookVector.z, lookVector.x) * 180.0 / Math.PI - 90.0;
        //double pitch = Math.atan2(Math.sqrt(lookVector.x * lookVector.x + lookVector.z * lookVector.z), lookVector.y) * 180.0 / Math.PI - 90.0;

        double yaw = Math.toDegrees(Math.atan2(lookVector.z, lookVector.x));
        //double pitch = Math.toDegrees(Math.asin(-lookVector.y));
        double pitch = Math.toDegrees(Math.asin(lookVector.y));

        return new Vector2d(yaw, pitch);
    }

    public static Vector2d calculateYawPitch(Vec3d playerPos, Vec3d targetBlockCenter) {
        double deltaX = targetBlockCenter.x - playerPos.getX();
        double deltaY = targetBlockCenter.y - playerPos.getY();
        double deltaZ = targetBlockCenter.z - playerPos.getZ();

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double yaw = Math.atan2(deltaZ, deltaX) * (180 / Math.PI) - 90;
        double pitch = -Math.atan2(deltaY, horizontalDistance) * (180 / Math.PI);

        return new Vector2d( yaw, pitch );
    }

    public static double GetBlockPosDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                        Math.pow(pos1.getY() - pos2.getY(), 2) +
                        Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }

    public static Block GetBlockFromBlockPos(BlockPos blockPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;
        return client.player.getWorld().getBlockState(blockPos).getBlock();
    }


    public static void RotateCameraAt(Vector2d rotation) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.setYaw((float)rotation.x);
        MinecraftClient.getInstance().player.setPitch((float)rotation.y);
    }

    public boolean raycastCheckOcclusion(double playerX, double playerY, double playerZ, BlockPos targetBlockPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.cameraEntity != null;
        ClientWorld world = mc.world;
        assert world != null;
        Vec3d start = new Vec3d(playerX, playerY, playerZ);
        Vec3d end = new Vec3d(targetBlockPos.getX() + 0.5, targetBlockPos.getY(), targetBlockPos.getZ() + 0.5);

        //BlockHitResult result = world.raycastBlock(start, end, targetBlockPos, RaycastContext.ShapeType.OUTLINE, );
        BlockHitResult result = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.cameraEntity));

        // Check if the ray hit a block before reaching the target
        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos hitBlockPos = result.getBlockPos();
            return !hitBlockPos.equals(targetBlockPos);
        }

        return false;
    }
}
