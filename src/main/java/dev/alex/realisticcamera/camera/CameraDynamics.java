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

    public static void reset() {
        initialized = false; phase = 0; intensity = 0; frequency = 0; turning = 0;
        turnRoll = 0; strafeRoll = 0; roll = 0; shakeYaw = 0; shakePitch = 0;
        lastFrame = -1;
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
        // Original uses limb-animation movement speed. Velocity is a separate stable approximation.
        double activity = MathHelper.clamp(horizontal / 0.22, 0.0, 1.5);
        if (!player.isOnGround()) activity *= 0.5;
        if (player.isSneaking()) activity /= 6.0;
        double ease = 1.0 - Math.exp(-12.0 * dt);
        intensity += (activity - intensity) * ease;
        frequency += ((0.1 + 0.25 * activity) - frequency) * ease;
        if (intensity < 0.0005) intensity = 0;
        phase += frequency * dt * 35.0;
        if (phase > 10000.0) phase -= 10000.0;
        double strength = 4.0 * Math.pow(MathHelper.clamp(intensity * 0.6, 0.0, 1.5), 2);
        shakeYaw = (float)(strength * smoothNoise(phase, 73));
        shakePitch = (float)(strength * smoothNoise(phase, 1));
        float yawChange = MathHelper.wrapDegrees(yaw - yawLast);
        yawLast = yaw;
        turning += (yawChange - turning) * ease;
        turnRoll += ((float)MathHelper.clamp(turning * 0.1, -20, 20) - turnRoll) * ease;
        double radians = Math.toRadians(yaw);
        double sideways = v.x * Math.cos(radians) + v.z * Math.sin(radians);
        float lateralTarget = (float)MathHelper.clamp(-sideways * 5.0, -20, 20);
        strafeRoll += (lateralTarget - strafeRoll) * ease;
        // Procedural roll vanishes at rest; turning and strafing fade out naturally.
        roll = MathHelper.clamp(turnRoll + strafeRoll + (float)(2 * strength * smoothNoise(phase, 146)), -20, 20);
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
