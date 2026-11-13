package waypoint;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Projects every enabled waypoint in the current dimension to the screen and draws
 * its name and distance.
 *
 * Per frame this allocates nothing: the working arrays, the quaternion and the vector
 * are reused, labels are cached until the rounded distance changes, and only the
 * nearest {@link #MAX_LABELS} waypoints within {@link #MAX_DISTANCE} are considered
 * (insertion sorted, nearest first).
 */
public final class WaypointRenderer {
   private static final double MAX_DISTANCE = 1024.0;
   private static final int MAX_LABELS = 48;
   private static final int LABEL_BACKGROUND = 0x40000000;

   private final Vector3f view = new Vector3f();
   private final Quaternionf inverse = new Quaternionf();

   private float[] distances = new float[16];
   private int[] order = new int[16];
   private String[] labels = new String[16];
   private int[] labelDistances = new int[16];

   public static void register() {
      HudElementRegistry.addLast(Identifier.of(WaypointMod.MOD_ID, "labels"), new WaypointRenderer()::render);
   }

   private void ensureCapacity(int needed) {
      if (this.distances.length >= needed) {
         return;
      }

      int size = Math.max(needed, this.distances.length * 2);
      this.distances = new float[size];
      this.order = new int[size];
      this.labels = new String[size];
      this.labelDistances = new int[size];
   }

   private void render(DrawContext context, RenderTickCounter tickCounter) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null || client.world == null || client.options.hudHidden || client.currentScreen != null) {
         return;
      }

      Waypoint[] waypoints = WaypointMod.STORE.snapshot();
      if (waypoints.length == 0) {
         return;
      }

      Camera camera = client.gameRenderer.getCamera();
      if (camera == null || !camera.isReady()) {
         return;
      }

      Vec3d cameraPos = camera.getCameraPos();
      this.inverse.set(camera.getRotation()).conjugate();

      int width = context.getScaledWindowWidth();
      int height = context.getScaledWindowHeight();
      double focal = 1.0 / Math.tan(Math.toRadians(client.options.getFov().getValue()) * 0.5);
      double aspect = (double)width / (double)height;

      this.ensureCapacity(waypoints.length);
      int visible = 0;

      for (int i = 0; i < waypoints.length; i++) {
         Waypoint waypoint = waypoints[i];
         if (!waypoint.isEnabled()) {
            continue;
         }

         this.view.set(
            (float)(waypoint.getX() + 0.5 - cameraPos.x),
            (float)(waypoint.getY() + 0.5 - cameraPos.y),
            (float)(waypoint.getZ() + 0.5 - cameraPos.z)
         );

         float distance = this.view.length();
         if (distance > MAX_DISTANCE) {
            continue;
         }

         this.inverse.transform(this.view);
         if (this.view.z > -0.15F) {
            // Behind the camera; projecting it would mirror the label onto the screen.
            continue;
         }

         this.distances[visible] = distance;
         this.order[visible] = i;
         visible++;
      }

      if (visible == 0) {
         return;
      }

      for (int i = 1; i < visible; i++) {
         float distance = this.distances[i];
         int index = this.order[i];
         int j = i - 1;

         while (j >= 0 && this.distances[j] > distance) {
            this.distances[j + 1] = this.distances[j];
            this.order[j + 1] = this.order[j];
            j--;
         }

         this.distances[j + 1] = distance;
         this.order[j + 1] = index;
      }

      int limit = Math.min(visible, MAX_LABELS);

      for (int i = 0; i < limit; i++) {
         int waypointIndex = this.order[i];
         Waypoint waypoint = waypoints[waypointIndex];
         float distance = this.distances[i];
         int rounded = (int)distance;

         this.view.set(
            (float)(waypoint.getX() + 0.5 - cameraPos.x),
            (float)(waypoint.getY() + 0.5 - cameraPos.y),
            (float)(waypoint.getZ() + 0.5 - cameraPos.z)
         );
         this.inverse.transform(this.view);

         float depth = -this.view.z;
         if (depth < 0.25F) {
            depth = 0.25F;
         }

         double screenX = (this.view.x / depth) * focal / aspect;
         double screenY = (this.view.y / depth) * focal;
         int x = (int)((screenX * 0.5 + 0.5) * width);
         int y = (int)((0.5 - screenY * 0.5) * height);

         if (x < -200 || x > width + 200 || y < -40 || y > height + 40) {
            continue;
         }

         String label = this.labels[waypointIndex];
         if (label == null || this.labelDistances[waypointIndex] != rounded) {
            label = waypoint.getName() + "  " + rounded + "m";
            this.labels[waypointIndex] = label;
            this.labelDistances[waypointIndex] = rounded;
         }

         int textWidth = client.textRenderer.getWidth(label);
         int left = x - textWidth / 2;
         int top = y - 4;

         context.fill(left - 2, top - 2, left + textWidth + 2, top + 10, LABEL_BACKGROUND);
         context.drawText(client.textRenderer, label, left, top, waypoint.rgb() | 0xFF000000, true);
      }
   }
}
