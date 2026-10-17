# RealisticCamera 1.21.1 — GitHub Actions build

Client-only Fabric mod. Original procedural camera motion and roll; no SPB dependency, no items or gameplay changes.

## Build on GitHub (no local Java/Gradle needed)
1. Create a GitHub repository and upload **the contents of this folder**, preserving `.github/workflows/build.yml`.
2. Open the repository's **Actions** tab and select **Build RealisticCamera**.
3. Click **Run workflow**, or push to `main` to start a build.
4. After a successful run, open its **Artifacts** section and download **RealisticCamera-1.21.1-Fabric**.
5. Extract the downloaded artifact ZIP. Inside is `RealisticCamera-1.21.1-Fabric.jar` for the Minecraft `mods` folder.

GitHub Actions uses Java 21, Gradle 8.10.2 and Fabric Loom 1.8.13. No Gradle wrapper is needed.

## Verification limitations
This source has **not** been compiled or launched in Minecraft in this environment. A green GitHub Actions run proves compilation and archive checks, not in-game compatibility or Sodium coexistence. Test in a separate Fabric 1.21.1 profile first. The camera mixin targets `Camera.update` and changes only the client-side camera quaternion, avoiding direct GameRenderer and Sodium hooks. It is not a byte-for-byte recreation of SPB Revamped.
