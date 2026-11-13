package waypoint.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import waypoint.Waypoint;
import waypoint.WaypointMod;

public class WaypointEditScreen extends Screen {
   private final Screen parent;
   private final int index;
   private final Waypoint waypoint;

   private TextFieldWidget nameField;
   private TextFieldWidget xField;
   private TextFieldWidget yField;
   private TextFieldWidget zField;
   private ButtonWidget colorButton;
   private ButtonWidget enabledButton;
   private Text error;

   public WaypointEditScreen(Screen parent, int index, Waypoint waypoint) {
      super(Text.translatable(index < 0 ? "gui.waypoint.new" : "gui.waypoint.edit_title"));
      this.parent = parent;
      this.index = index;
      this.waypoint = waypoint;
   }

   @Override
   protected void init() {
      int center = this.width / 2;

      this.nameField = this.addDrawableChild(
         new TextFieldWidget(this.textRenderer, center - 100, 56, 200, 20, Text.translatable("gui.waypoint.name"))
      );
      this.nameField.setMaxLength(32);
      this.nameField.setText(this.waypoint.getName());

      this.xField = this.numericField(center - 100, 92);
      this.yField = this.numericField(center - 32, 92);
      this.zField = this.numericField(center + 36, 92);
      this.xField.setText(Integer.toString(this.waypoint.getX()));
      this.yField.setText(Integer.toString(this.waypoint.getY()));
      this.zField.setText(Integer.toString(this.waypoint.getZ()));

      this.colorButton = this.addDrawableChild(
         ButtonWidget.builder(this.colorText(), button -> {
            this.waypoint.setColor((this.waypoint.getColor() + 1) % Waypoint.COLORS.length);
            button.setMessage(this.colorText());
         }).dimensions(center - 100, 124, 96, 20).build()
      );

      this.enabledButton = this.addDrawableChild(
         ButtonWidget.builder(this.enabledText(), button -> {
            this.waypoint.setEnabled(!this.waypoint.isEnabled());
            button.setMessage(this.enabledText());
         }).dimensions(center + 4, 124, 96, 20).build()
      );

      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.confirm"), button -> this.confirm())
            .dimensions(center - 100, 156, 96, 20)
            .build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.waypoint.cancel"), button -> this.close())
            .dimensions(center + 4, 156, 96, 20)
            .build()
      );
   }

   private TextFieldWidget numericField(int x, int y) {
      TextFieldWidget field = this.addDrawableChild(new TextFieldWidget(this.textRenderer, x, y, 62, 20, Text.empty()));
      field.setMaxLength(10);
      field.setTextPredicate(text -> text.matches("-?\\d{0,9}"));
      return field;
   }

   private Text colorText() {
      return Text.translatable("color.waypoint." + Waypoint.COLOR_KEYS[this.waypoint.getColor()]);
   }

   private Text enabledText() {
      return Text.translatable(this.waypoint.isEnabled() ? "gui.waypoint.enabled" : "gui.waypoint.disabled");
   }

   private void confirm() {
      int x;
      int y;
      int z;

      try {
         x = Integer.parseInt(this.xField.getText().trim());
         y = Integer.parseInt(this.yField.getText().trim());
         z = Integer.parseInt(this.zField.getText().trim());
      } catch (NumberFormatException exception) {
         this.error = Text.translatable("gui.waypoint.invalid_coords");
         return;
      }

      String name = this.nameField.getText().trim();
      this.waypoint.setName(name.isEmpty() ? "Waypoint" : name);
      this.waypoint.setPosition(x, y, z);

      if (this.index >= 0) {
         WaypointMod.STORE.set(this.index, this.waypoint);
      } else {
         WaypointMod.STORE.add(this.waypoint);
      }

      this.close();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFFFF);
      context.drawText(this.textRenderer, Text.translatable("gui.waypoint.x"), this.width / 2 - 100, 81, 0xFFAAAAAA, false);
      context.drawText(this.textRenderer, Text.translatable("gui.waypoint.y"), this.width / 2 - 32, 81, 0xFFAAAAAA, false);
      context.drawText(this.textRenderer, Text.translatable("gui.waypoint.z"), this.width / 2 + 36, 81, 0xFFAAAAAA, false);

      if (this.error != null) {
         context.drawCenteredTextWithShadow(this.textRenderer, this.error, this.width / 2, 136, 0xFFFF5555);
      }

      super.render(context, mouseX, mouseY, deltaTicks);
   }

   @Override
   public void close() {
      this.client.setScreen(this.parent);
   }
}
