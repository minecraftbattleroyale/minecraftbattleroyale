package io.github.minecraftbattleroyale;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "mcbr", name = "Minecraft Battle Royale")
public class MinecraftBattleRoyale {

  @Listener
  public void onStart(GameStartedServerEvent event) {
    System.out.println(event);
  }

  @Listener
  public void onRightClick(InteractItemEvent.Primary event, @First Player player) {
    ItemStackSnapshot item = event.getItemStack();
    System.out.println(item);
    // Use iron axe right now
    boolean pistole = item.getType().matches(ItemStack.of(ItemTypes.IRON_AXE));
    boolean sniper = item.getType().matches(ItemStack.of(ItemTypes.IRON_PICKAXE));
    if (pistole || sniper) {
      Vector3d positon = player.getPosition();
      double yaw = Math.toRadians(player.getHeadRotation().getY() + 90);
      double x = 1.25 * Math.cos(yaw);
      double y = 1.25 * Math.sin(yaw);
      Entity arrow = player.getWorld().createEntity(EntityTypes.TIPPED_ARROW, positon.add(x, 1.62, y));
      System.out.println(arrow);
      player.getWorld().spawnEntity(arrow);

      Vector3d velocity = new Vector3d(x, 0, y).mul(5);
      arrow.setVelocity(velocity);
    }
    event.setCancelled(true);
  }
}
