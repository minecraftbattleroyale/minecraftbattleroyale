package io.github.minecraftbattleroyale.core;

import io.github.minecraftbattleroyale.MinecraftBattleRoyale;
import net.year4000.utilities.Conditions;
import net.year4000.utilities.sponge.Messages;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBar;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.resourcepack.ResourcePacks;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Collectors;

public class UserPlayer {
  private ArenaGame game;
  private Player player;
  private UserPlayerMode mode = UserPlayerMode.LOBBY;
  private int kills;

  public UserPlayer(ArenaGame game, Player player) {
    this.game = Conditions.nonNull(game, "game");
    this.player = Conditions.nonNull(player, "player");
  }

  /** Send the resource pack to the player or at least attempt to */
  public void sendResourcePack() {
    try {
      String url = Sponge.getGame().getServer().getDefaultResourcePack().get().getUri().toString() + "?z=" + System.currentTimeMillis();
      player.sendResourcePack(ResourcePacks.fromUriUnchecked(new URL(url).toURI()));
    } catch (URISyntaxException | MalformedURLException error) {
      player.sendMessage(Text.of(Messages.CMD_ERROR_PLAYER, TextColors.RED, "Fail to send resource pack."));
    }
  }

  /** This will start the player in lobby mode */
  public void joinLobby() {
    sendResourcePack();
    player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
    player.offer(Keys.CAN_FLY, true);
    player.offer(Keys.HEALTH, 20.0);
    player.offer(Keys.SATURATION, 20.0);
    player.offer(Keys.FOOD_LEVEL, 20);
    CarriedInventory inventory = player.getInventory();
    MinecraftBattleRoyale mcbr = MinecraftBattleRoyale.get();
    inventory.offer(mcbr.guns.get(ItemTypes.IRON_PICKAXE).createItem());
    inventory.offer(mcbr.guns.get(ItemTypes.IRON_AXE).createItem());
    inventory.offer(mcbr.guns.get(ItemTypes.STONE_SWORD).createItem());
    // todo broken atm
//    ItemStack map = ItemStack.of(ItemTypes.FILLED_MAP, 1);
//    player.setItemInHand(HandTypes.OFF_HAND, map);
    player.setChestplate(ItemStack.of(ItemTypes.ELYTRA, 1));
  }

  public void startFighting() {
    if (mode == UserPlayerMode.START_GAME) {
      mode = UserPlayerMode.IN_GAME;
      player.offer(Keys.CAN_FLY, false);
      CarriedInventory inventory = player.getInventory();
      inventory.clear();
      player.offer(Keys.EXPERIENCE_LEVEL, game.alivePlayers());
//      inventory.offer(ItemStack.of(ItemTypes.IRON_PICKAXE, 1));
//      inventory.offer(ItemStack.of(ItemTypes.IRON_AXE, 1));
//      inventory.offer(ItemStack.of(ItemTypes.STONE_SWORD, 1));
//      inventory.offer(ItemStack.of(ItemTypes.FEATHER, 16));
      // todo broken atm
//      ItemStack map = ItemStack.of(ItemTypes.FILLED_MAP, 1);
//      player.setItemInHand(HandTypes.OFF_HAND, map);
    }
  }

  /** When a player dies */
  public void death() {
    if (mode == UserPlayerMode.IN_GAME) {
      mode = UserPlayerMode.DEATH;
      CarriedInventory inventory = player.getInventory();
      inventory.clear();
      this.player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
      ServerBossBar bossBar = ServerBossBar.builder()
              .name(Text.of(TextColors.GOLD, "Spectator Mode"))
              .percent(1)
              .color(BossBarColors.WHITE)
              .overlay(BossBarOverlays.PROGRESS)
              .build();
      bossBar.addPlayer(this.player);
      bossBar.setVisible(true);
    }
  }

  public void startGame() {
    mode = UserPlayerMode.START_GAME;
    player.sendMessage(Text.of(TextColors.AQUA, "PRESS F5 TO TOGGLE TO 3RD PERSON MODE."));
    player.offer(Keys.EXPERIENCE_LEVEL, game.alivePlayers());
  }

  public boolean isAlive() {
    return mode != UserPlayerMode.DEATH;
  }

  public UserPlayerMode getMode() {
    return mode;
  }

  /** Get the internal player */
  public Player getPlayer() {
    return player;
  }

  /** This is the hash code for the userplayer it will match the entity player */
  public int hashCode() {
    return player.getUniqueId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Player) {
      return this.player.getUniqueId().equals(((Player) obj).getUniqueId());
    }
    if (obj instanceof UserPlayer) {
      return this.player.getUniqueId().equals(((UserPlayer) obj).player.getUniqueId());
    }
    return false;
  }
}
