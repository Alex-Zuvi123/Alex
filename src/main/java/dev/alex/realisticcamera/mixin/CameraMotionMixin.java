package dev.alex.realisticcamera.mixin;

import dev.alex.realisticcamera.camera.CameraDynamics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies all three camera rotations directly to the camera quaternion.
 * Avoids GameRenderer and Sodium rendering hooks and works with view bobbing disabled.
 */
@Mixin(Camera.class)
public abstract class CameraMotionMixin {
    @Shadow @Final private Quaternionf rotation;
    @Shadow private float yaw;
    @Shadow private float pitch;
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void realisticcamera$update(BlockView area, Entity focusedEntity,
            boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (thirdPerson || client.player == null || focusedEntity != client.player) {
            CameraDynamics.reset();
            return;
        }
        CameraDynamics.update(client.player, tickDelta);
        setRotation(yaw + CameraDynamics.yaw(), pitch + CameraDynamics.pitch());
        rotation.rotateZ((float) Math.toRadians(CameraDynamics.roll()));
    }
}
