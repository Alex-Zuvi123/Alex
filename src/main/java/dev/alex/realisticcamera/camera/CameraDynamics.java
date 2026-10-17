package dev.alex.realisticcamera.camera;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Self-contained, allocation-free camera animation state. No SPB classes are used. */
public final class CameraDynamics {
    private CameraDynamics() {}
    private static double phase, intensity, frequency;
    private static float yawLast, turning, turnRoll, strafeRoll, roll;
    private static boolean initialized;
    private static long lastFrame = -1;
    private static float shakeYaw, shakePitch;
    private static double filteredSpeed, smoothedYaw, smoothedPitch, smoothedRoll;

    public static void reset() {
        initialized = false; phase = 0; intensity = 0; frequency = 0; turning = 0;
        turnRoll = 0; strafeRoll = 0; roll = 0; shakeYaw = 0; shakePitch = 0;
        lastFrame = -1; filteredSpeed = 0; smoothedYaw = 0; smoothedPitch = 0; smoothedRoll = 0;
    }

    public static void update(ClientPlayerEntity player, float tickDelta) {
        // Camera.update can be invoked more than once per frame. Keep simulation time stable.
        long now = System.nanoTime();
        double dt = lastFrame < 0 ? 1.0 / 60.0 : MathHelper.clamp((now - lastFrame) / 1_000_000_000.0, 0.0, 0.05);
        lastFrame = now;
        float yaw = player.getYaw(tickDelta);
        if (!initialized) { yawLast = yaw; initialized = true; }
        Vec3d v = player.getVelocity();
        double horizontal = Math.hypot(v.x, v.z);
        // Preserve original camera behavior, but stabilize speed and share a single gait phase.
        double speedEase = 1.0 - Math.exp(-7.0 * dt);
        filteredSpeed += (horizontal - filteredSpeed) * speedEase;
        double activity = MathHelper.clamp(filteredSpeed / 0.22, 0.0, 1.5);
        if (!player.isOnGround()) activity *= 0.5;
        if (player.isSneaking()) activity /= 6.0;
        double ease = 1.0 - Math.exp(-7.0 * dt);
        intensity += (activity - intensity) * ease;
        frequency += ((0.1 + 0.25 * activity) - frequency) * ease;
        if (intensity < 0.0005) intensity = 0;
        // Stop gait phase when standing still: prevents jitter while stationary.
        phase += frequency * dt * 35.0 * MathHelper.clamp(intensity * 2.0, 0.0, 1.0);
        if (phase > 10000.0) phase -= 10000.0;
        double strength = 4.0 * Math.pow(MathHelper.clamp(intensity * 0.6, 0.0, 1.5), 2);
        // Running amplitude reduced by up to 40%, smoothly blended with walking.
        double runBlend = MathHelper.clamp((filteredSpeed - 0.17) / 0.11, 0.0, 1.0);
        strength *= (1.0 - 0.40 * runBlend);
        // Correlated gait channels (shared phase) + restrained procedural detail.
        double gait = Math.sin(phase * 1.15);
        double step = Math.cos(phase * 2.30);
        double yawTarget = strength * (0.42 * gait + 0.18 * smoothNoise(phase * 0.45, 73));
        double pitchTarget = strength * (0.45 * step + 0.12 * smoothNoise(phase * 0.45, 1));
        float yawChange = MathHelper.wrapDegrees(yaw - yawLast);
        yawLast = yaw;
        // Turning is visual only: never smooth or delay the actual mouse input.
        double turnEase = 1.0 - Math.exp(-10.0 * dt);
        turning += (yawChange - turning) * turnEase;
        turnRoll += ((float)MathHelper.clamp(turning * 0.07, -8, 8) - turnRoll) * turnEase;
        double radians = Math.toRadians(yaw);
        double sideways = v.x * Math.cos(radians) + v.z * Math.sin(radians);
        float lateralTarget = (float)MathHelper.clamp(-sideways * 3.5, -5, 5);
        strafeRoll += (lateralTarget - strafeRoll) * ease;
        double rollTarget = MathHelper.clamp(turnRoll + strafeRoll + 0.20 * strength * gait, -9, 9);
        // Final low-pass filter avoids abrupt changes from frame-to-frame.
        double stabilization = 1.0 - Math.exp(-12.0 * dt);
        smoothedYaw += (yawTarget - smoothedYaw) * stabilization;
        smoothedPitch += (pitchTarget - smoothedPitch) * stabilization;
        smoothedRoll += (rollTarget - smoothedRoll) * stabilization;
        shakeYaw = (float)smoothedYaw;
        shakePitch = (float)smoothedPitch;
        roll = (float)smoothedRoll;
    }

    // Original independent value noise, smoothly interpolated in 1D; not copied from SPB.
    private static double smoothNoise(double x, int seed) {
        long i = (long)Math.floor(x);
        double f = x - i;
        f = f*f*(3.0-2.0*f);
        return (1-f)*hash(i, seed) + f*hash(i+1, seed);
    }
    private static double hash(long x, int seed) {
        long n = x * 0x9E3779B97F4A7C15L + seed * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 30)) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 27)) * 0x94D049BB133111EBL;
        n ^= n >>> 31;
        return ((n >>> 11) * 0x1.0p-53) * 2 - 1;
    }
    public static float yaw() { return shakeYaw; }
    public static float pitch() { return shakePitch; }
    public static float roll() { return roll; }
    public static boolean active() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.options.getPerspective().isFirstPerson();
    }
}
