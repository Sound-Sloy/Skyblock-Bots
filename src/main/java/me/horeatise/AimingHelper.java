package me.horeatise;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;


public class AimingHelper {
    public static void aimAtBlock(Vec3d targetBlockPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert mc.player != null;
        Vec3d playerPos = mc.player.getCameraPosVec(1.0f);

        // Find a visible aiming point on the target block's surface
        Vec3d aimingPoint = findVisibleAimingPoint(targetBlockPos, playerPos);
        if (aimingPoint != null) {
            Vec3d direction = aimingPoint.subtract(playerPos).normalize();

            // Calculate the rotation angles (yaw and pitch)
            double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            double pitch = Math.toDegrees(Math.asin(direction.y));

            // Set player's rotation angles
            mc.player.setYaw((float)yaw);
            mc.player.setPitch((float)pitch);
        }
    }

    public static Vec3d findVisibleAimingPoint(Vec3d targetBlockPos, Vec3d playerPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        assert client.player != null;
        assert client.world != null;

        // Calculate a step vector for tracing rays towards the target block
        Vec3d stepVector = targetBlockPos.subtract(playerPos).normalize().multiply(0.1);

        for (double distance = 0; distance < 5.0; distance += 0.1) {
            Vec3d rayPos = playerPos.add(stepVector.multiply(distance));

            // Trace a ray from player to the current ray position

            RaycastContext context = new RaycastContext(playerPos, rayPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player);
            BlockHitResult result = client.world.raycast(context);
            if (result.getType() == HitResult.Type.BLOCK) {
                // Get the BlockPos of the intersected block
                BlockPos blockPos = result.getBlockPos();
                double distanceToBlock = rayPos.squaredDistanceTo(result.getPos());

                // Compare the squared distance to the target block's position
                if (blockPos.equals(new BlockPos(new Vec3i((int) targetBlockPos.getX(), (int) targetBlockPos.getY(), (int) targetBlockPos.getZ()))) && distanceToBlock < 0.01) {
                    return result.getPos();
                }
            }
        }

        // If no visible aiming point found, return null
        return null;
    }
}
