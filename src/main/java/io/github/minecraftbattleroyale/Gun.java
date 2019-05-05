package io.github.minecraftbattleroyale;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;


public class Gun {
  public ItemType type;
  public String name;
  public int ammo;
  public long reloadTime;
  public int fireRate;
  public int damage;

  public Gun(ItemType type, String name, int ammo, long reloadTime, int fireRate, int damage) {
    this.type = type;
    this.name = name;
    this.ammo = ammo;
    this.reloadTime = reloadTime;
    this.fireRate = fireRate;
    this.damage = damage;
  }

  /** Create the item */
  public ItemStack createItem() {
    ItemStack itemStack = ItemStack.builder()
            .itemType(this.type)
            .quantity(1)
            .build();
    itemStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, name));
    //itemStack.setQuantity(ammo);
    // gun is not reloaded at start
    return itemStack;
  }
}
