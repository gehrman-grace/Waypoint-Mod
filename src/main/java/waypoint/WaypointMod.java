package waypoint;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import waypoint.gui.WaypointEditScreen;
import waypoint.gui.WaypointListScreen;

public final class WaypointMod implements ClientModInitializer {
   public static final String MOD_ID = "waypoint";
   public static final WaypointStore STORE = new WaypointStore();
   private static final DeathWatcher DEATH_WATCHER = new DeathWatcher();

   private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

   private static KeyBinding createKey;
   private static KeyBinding listKey;

   @Override
   public void onInitializeClient() {
      createKey = KeyBindingHelper.registerKeyBinding(
         new KeyBinding("key.waypoint.create", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY)
      );
      listKey = KeyBindingHelper.registerKeyBinding(
         new KeyBinding("key.waypoint.list", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY)
      );

      WaypointRenderer.register();
      ClientTickEvents.END_CLIENT_TICK.register(this::tick);
   }

   private void tick(MinecraftClient client) {
      if (client.player == null || client.world == null) {
         return;
      }

      STORE.refreshKey(client);
      DEATH_WATCHER.tick(client);

      while (createKey.wasPressed()) {
         BlockPos pos = client.player.getBlockPos();
         client.setScreen(new WaypointEditScreen(null, -1, new Waypoint("Waypoint", pos.getX(), pos.getY(), pos.getZ(), 12)));
      }

      while (listKey.wasPressed()) {
         client.setScreen(new WaypointListScreen(null));
      }
   }
}
