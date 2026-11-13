package waypoint.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import waypoint.Waypoint;
import waypoint.WaypointMod;

public class WaypointListScreen extends Screen {
   private static final int ROW = 24;
   private static final int LIST_WIDTH = 340;
   private static final int LIST_TOP = 44;

   private final Screen parent;
   private Waypoint[] list = new Waypoint[0];
   private int selected = -1;
   private int scroll;
   private int listBottom;

   private ButtonWidget editButton;
   private ButtonWidget deleteButton;
   private ButtonWidget toggleButton;
   private ButtonWidget teleportButton;

   public WaypointListScreen(Screen parent) {
      super(Text.translatable("gui.waypoint.title"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.refresh();
      int center = this.width / 2;
      int firstRow = this.height - 52;
      int secondRow = this.height - 28;
      this.listBottom = firstRow - 8;

      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.add"), button -> this.client.setScreen(new WaypointEditScreen(this, -1, this.newWaypoint())))
            .dimensions(center - 156, firstRow, 100, 20)
            .build()
      );
      this.editButton = this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.edit"), button -> this.editSelected())
            .dimensions(center - 50, firstRow, 100, 20)
            .build()
      );
      this.deleteButton = this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.delete"), button -> this.deleteSelected())
            .dimensions(center + 56, firstRow, 100, 20)
            .build()
      );
      this.toggleButton = this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.enable"), button -> this.toggleSelected())
            .dimensions(center - 156, secondRow, 100, 20)
            .build()
      );
      this.teleportButton = this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.teleport"), button -> this.teleportSelected())
            .dimensions(center - 50, secondRow, 100, 20)
            .build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.done"), button -> this.close())
            .dimensions(center + 56, secondRow, 100, 20)
            .build()
      );

      this.updateButtons();
   }

   private void refresh() {
      this.list = WaypointMod.STORE.snapshot();
      if (this.selected >= this.list.length) {
         this.selected = this.list.length - 1;
      }
   }

   private Waypoint newWaypoint() {
      BlockPos pos = this.client.player != null ? this.client.player.getBlockPos() : BlockPos.ORIGIN;
      return new Waypoint("Waypoint", pos.getX(), pos.getY(), pos.getZ(), 12);
   }

   private Waypoint selectedWaypoint() {
      return this.selected >= 0 && this.selected < this.list.length ? this.list[this.selected] : null;
   }

   private void updateButtons() {
      Waypoint waypoint = this.selectedWaypoint();
      boolean hasSelection = waypoint != null;
      this.editButton.active = hasSelection;
      this.deleteButton.active = hasSelection;
      this.toggleButton.active = hasSelection;
      this.teleportButton.active = hasSelection;
      this.toggleButton.setMessage(
         Text.translatable(waypoint != null && waypoint.isEnabled() ? "gui.waypoint.disable" : "gui.waypoint.enable")
      );
   }

   private void editSelected() {
      Waypoint waypoint = this.selectedWaypoint();
      if (waypoint != null) {
         this.client.setScreen(new WaypointEditScreen(this, this.selected, waypoint.copy()));
      }
   }

   private void deleteSelected() {
      if (this.selectedWaypoint() != null) {
         WaypointMod.STORE.remove(this.selected);
         this.selected = -1;
         this.refresh();
         this.updateButtons();
      }
   }

   private void toggleSelected() {
      Waypoint waypoint = this.selectedWaypoint();
      if (waypoint != null) {
         WaypointMod.STORE.setEnabled(this.selected, !waypoint.isEnabled());
         this.updateButtons();
      }
   }

   private void teleportSelected() {
      Waypoint waypoint = this.selectedWaypoint();
      if (waypoint != null && this.client.player != null) {
         this.client.player
            .networkHandler
            .sendChatCommand("tp " + waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ());
         if (this.client.player != null) {
            this.client.player.sendMessage(Text.translatable("gui.waypoint.teleported"), true);
         }

         this.close();
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFFFF);

      int left = this.width / 2 - LIST_WIDTH / 2;
      int right = left + LIST_WIDTH;
      context.fill(left - 6, LIST_TOP - 6, right + 6, this.listBottom, 0x80000000);

      if (this.list.length == 0) {
         context.drawCenteredTextWithShadow(
            this.textRenderer, Text.translatable("gui.waypoint.empty"), this.width / 2, LIST_TOP + 4, 0xFFAAAAAA
         );
      } else {
         int visibleRows = this.visibleRows();
         int maxScroll = Math.max(0, this.list.length - visibleRows);
         if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
         }

         int shown = Math.min(visibleRows, this.list.length - this.scroll);

         for (int i = 0; i < shown; i++) {
            int index = this.scroll + i;
            Waypoint waypoint = this.list[index];
            int y = LIST_TOP + i * ROW;

            context.fill(left, y, right, y + ROW - 2, index == this.selected ? 0x60FFFFFF : 0x20FFFFFF);
            context.fill(left + 6, y + 6, left + 16, y + ROW - 8, waypoint.rgb() | 0xFF000000);
            context.drawText(
               this.textRenderer,
               waypoint.getName(),
               left + 22,
               y + 4,
               waypoint.isEnabled() ? 0xFFFFFFFF : 0xFF777777,
               true
            );
            context.drawText(
               this.textRenderer,
               waypoint.getX() + " " + waypoint.getY() + " " + waypoint.getZ(),
               left + 22,
               y + 13,
               0xFFAAAAAA,
               false
            );

            if (this.client.player != null) {
               int distance = (int)Math.sqrt(waypoint.distanceSq(this.client.player.getX(), this.client.player.getY(), this.client.player.getZ()));
               String label = distance + "m";
               context.drawText(
                  this.textRenderer, label, right - 8 - this.textRenderer.getWidth(label), y + 4, 0xFFAAAAAA, true
               );
            }
         }
      }

      super.render(context, mouseX, mouseY, deltaTicks);
   }

   private int visibleRows() {
      return Math.max(1, (this.listBottom - LIST_TOP) / ROW);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      int left = this.width / 2 - LIST_WIDTH / 2;
      int right = left + LIST_WIDTH;
      double mouseX = click.x();
      double mouseY = click.y();

      if (click.button() == 0 && mouseX >= left && mouseX <= right && mouseY >= LIST_TOP && mouseY < this.listBottom) {
         int index = this.scroll + (int)((mouseY - LIST_TOP) / ROW);
         if (index >= 0 && index < this.list.length) {
            this.selected = index;
            this.updateButtons();
            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int maxScroll = Math.max(0, this.list.length - this.visibleRows());
      this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - (int)Math.signum(verticalAmount)));
      return true;
   }

   @Override
   public void close() {
      this.client.setScreen(this.parent);
   }
}
