package waypoint;

/**
 * A single waypoint. Plain fields so Gson can read and write it directly without
 * reflective adapters or a second DTO type.
 */
public final class Waypoint {
   /** 16 chat colours, in the same order as the vanilla colour indices. */
   public static final int[] COLORS = {
      0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
      0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
      0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
      0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
   };

   public static final String[] COLOR_KEYS = {
      "black", "dark_blue", "dark_green", "dark_aqua",
      "dark_red", "dark_purple", "gold", "gray",
      "dark_gray", "blue", "green", "aqua",
      "red", "purple", "yellow", "white"
   };

   private String name = "";
   private int x;
   private int y;
   private int z;
   private int color = 12;
   private boolean enabled = true;
   private boolean death;

   public Waypoint() {
   }

   public Waypoint(String name, int x, int y, int z, int color) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.z = z;
      this.color = Math.max(0, Math.min(COLORS.length - 1, color));
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public void setPosition(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public int getColor() {
      return this.color;
   }

   public void setColor(int color) {
      this.color = Math.max(0, Math.min(COLORS.length - 1, color));
   }

   public int rgb() {
      return COLORS[this.color];
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   /** True for the automatically created death point, so a newer death can demote it. */
   public boolean isDeath() {
      return this.death;
   }

   public void setDeath(boolean death) {
      this.death = death;
   }

   public double distanceSq(double px, double py, double pz) {
      double dx = this.x + 0.5 - px;
      double dy = this.y + 0.5 - py;
      double dz = this.z + 0.5 - pz;
      return dx * dx + dy * dy + dz * dz;
   }

   /** Copies the user-editable state; used so screens never mutate live data mid-frame. */
   public Waypoint copy() {
      Waypoint copy = new Waypoint(this.name, this.x, this.y, this.z, this.color);
      copy.enabled = this.enabled;
      copy.death = this.death;
      return copy;
   }
}
