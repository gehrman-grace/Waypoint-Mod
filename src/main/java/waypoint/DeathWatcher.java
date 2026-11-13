package waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Detects the player's death on the client, which works the same way in singleplayer
 * and on a server (health is synced to the client before the death screen appears).
 *
 * The newest death is kept as "Death" at the exact death position. When the player dies
 * again, every previous death waypoint is renamed "Old Death x y z" (its precise
 * coordinates) and turned into a normal grey waypoint, and a fresh "Death" is created.
 */
public final class DeathWatcher {
   private static final int DEATH_COLOR = 12;
   private static final int OLD_DEATH_COLOR = 7;
   /** Squared distance at which a death point counts as reached and is removed (4 blocks). */
   private static final double REACH_DISTANCE_SQ = 16.0;

   private boolean initialised;
   private boolean wasAlive = true;
   /**
    * A freshly created death point is only cleared once the player has been further away
    * than the reach distance, so respawning on top of it does not delete it instantly.
    */
   private boolean reachArmed;

   public void tick(MinecraftClient client) {
      ClientPlayerEntity player = client.player;
      if (player == null || client.world == null) {
         this.initialised = false;
         return;
      }

      boolean alive = player.getHealth() > 0.0F;

      // Joining while already dead (or respawning into a new world) must not create a point.
      if (!this.initialised) {
         this.initialised = true;
         this.wasAlive = alive;
         return;
      }

      if (this.wasAlive && !alive) {
         this.onDeath(player);
      } else if (alive) {
         this.clearReachedDeathPoint(player);
      }

      this.wasAlive = alive;
   }

   private void onDeath(ClientPlayerEntity player) {
      WaypointStore store = WaypointMod.STORE;
      int x = (int)Math.floor(player.getX());
      int y = (int)Math.floor(player.getY());
      int z = (int)Math.floor(player.getZ());

      for (int i = 0; i < store.size(); i++) {
         Waypoint waypoint = store.get(i);
         if (waypoint != null && waypoint.isDeath()) {
            waypoint.setDeath(false);
            waypoint.setName("Old Death " + waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ());
            waypoint.setColor(OLD_DEATH_COLOR);
         }
      }

      // add() rebuilds the renderer snapshot and writes the file, so the demotions
      // above and the new death point are saved together in one write.
      Waypoint death = new Waypoint("Death", x, y, z, DEATH_COLOR);
      death.setDeath(true);
      store.add(death);
      this.reachArmed = false;
   }

   /** Removes the current death point once the player has walked up to it. */
   private void clearReachedDeathPoint(ClientPlayerEntity player) {
      WaypointStore store = WaypointMod.STORE;

      for (int i = store.size() - 1; i >= 0; i--) {
         Waypoint waypoint = store.get(i);
         if (waypoint == null || !waypoint.isDeath()) {
            continue;
         }

         if (waypoint.distanceSq(player.getX(), player.getY(), player.getZ()) <= REACH_DISTANCE_SQ) {
            if (this.reachArmed) {
               store.remove(i);
               this.reachArmed = false;
            }
         } else {
            this.reachArmed = true;
         }
      }
   }
}
