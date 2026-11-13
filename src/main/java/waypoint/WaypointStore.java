package waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;

/**
 * Waypoint storage for one world + dimension, with a copy-on-write array for the
 * renderer so it never iterates a list that is being modified.
 */
public final class WaypointStore {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Type LIST_TYPE = new TypeToken<List<Waypoint>>() {
   }.getType();

   private final List<Waypoint> waypoints = new ArrayList<>();
   private Waypoint[] snapshot = new Waypoint[0];
   private String worldKey = "";
   private String dimension = "";
   private Path file;
   private boolean ready;

   /**
    * Switches storage when the player changes world or dimension.
    * Returns true when the active file changed and waypoints were reloaded.
    */
   public boolean refreshKey(MinecraftClient client) {
      if (client.world == null) {
         return false;
      }

      String world = resolveWorldKey(client);
      String dim = client.world.getRegistryKey().getValue().toString().replace(':', '_').replace('/', '_');
      if (world.equals(this.worldKey) && dim.equals(this.dimension)) {
         return false;
      }

      this.save();
      this.worldKey = world;
      this.dimension = dim;
      this.file = FabricLoader.getInstance().getConfigDir().resolve("waypoint").resolve(world).resolve(dim + ".json");
      this.load();
      return true;
   }

   private static String resolveWorldKey(MinecraftClient client) {
      IntegratedServer server = client.getServer();
      if (server != null) {
         return "sp_" + safe(server.getSaveProperties().getLevelName());
      }

      ServerInfo info = client.getCurrentServerEntry();
      return info != null ? "mp_" + safe(info.address) : "unknown";
   }

   private static String safe(String value) {
      StringBuilder builder = new StringBuilder(value.length());
      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         builder.append(Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_' ? c : '_');
      }

      return builder.toString();
   }

   private void load() {
      this.waypoints.clear();
      this.ready = this.file != null;
      if (this.ready && Files.exists(this.file)) {
         try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
            List<Waypoint> loaded = GSON.fromJson(reader, LIST_TYPE);
            if (loaded != null) {
               this.waypoints.addAll(loaded);
            }
         } catch (Exception ignored) {
            // A corrupt file must never stop the game from starting; it is simply replaced on the next save.
         }
      }

      this.rebuild();
   }

   public void save() {
      if (!this.ready || this.file == null) {
         return;
      }

      try {
         Files.createDirectories(this.file.getParent());
         try (Writer writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
            GSON.toJson(this.waypoints, LIST_TYPE, writer);
         }
      } catch (IOException ignored) {
         // Keeping the in-memory waypoints is better than crashing on a read-only config directory.
      }
   }

   private void rebuild() {
      this.snapshot = this.waypoints.toArray(new Waypoint[0]);
   }

   public Waypoint[] snapshot() {
      return this.snapshot;
   }

   public int size() {
      return this.waypoints.size();
   }

   public Waypoint get(int index) {
      return index >= 0 && index < this.waypoints.size() ? this.waypoints.get(index) : null;
   }

   public void add(Waypoint waypoint) {
      this.waypoints.add(waypoint);
      this.rebuild();
      this.save();
   }

   public void set(int index, Waypoint waypoint) {
      if (index >= 0 && index < this.waypoints.size()) {
         this.waypoints.set(index, waypoint);
         this.rebuild();
         this.save();
      }
   }

   public void remove(int index) {
      if (index >= 0 && index < this.waypoints.size()) {
         this.waypoints.remove(index);
         this.rebuild();
         this.save();
      }
   }

   public void setEnabled(int index, boolean enabled) {
      Waypoint waypoint = this.get(index);
      if (waypoint != null) {
         waypoint.setEnabled(enabled);
         this.save();
      }
   }
}
