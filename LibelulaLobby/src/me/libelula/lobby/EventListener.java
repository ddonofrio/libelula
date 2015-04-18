/*
 *            This file is part of Libelula Minecraft Edition Project.
 *
 *  Libelula Minecraft Edition is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libelula Minecraft Edition is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libelula Minecraft Edition. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.lobby;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import java.util.logging.Level;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public final class EventListener implements org.bukkit.event.Listener {

    private final Main plugin;
    private final ConfigurationManager cm;

    public EventListener(Main plugin) {
        this.plugin = plugin;
        this.cm = plugin.cm;
        register();
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /* 
     Map related
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeatherChange(WeatherChangeEvent e) {
        if (e.toWeatherState()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplosion(ExplosionPrimeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockIgnite(BlockIgniteEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPiston(BlockPistonExtendEvent e) {
        for (CuboidSelection area : plugin.cm.getEditionAreas()) {
            if (area.contains(e.getBlock().getLocation())) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onLeavesDecay(LeavesDecayEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawnEvent(ItemSpawnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromToEvent(BlockFromToEvent e) {
        e.setCancelled(true);
    }

    /*
     Player related
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onEntityBlockForm(EntityBlockFormEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent e) {
        boolean cancel = true;
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if (plugin.olmoTower.isInGame(player)) {
                cancel = false;
            }
        }
        e.setCancelled(cancel);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (plugin.olmoTower.isInGame(player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            if (e.getItem() != null) {
                switch (e.getItem().getType()) {
                    case POTION:
                    case MINECART:
                        e.setCancelled(true);
                        break;
                }
            }
            return;
        }
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || e.getAction().equals(Action.LEFT_CLICK_BLOCK)
                || e.getAction().equals(Action.RIGHT_CLICK_AIR)
                || e.getAction().equals(Action.LEFT_CLICK_AIR)) {
            if (e.getItem() != null
                    && e.getItem().getType().equals(Material.COMPASS)) {
                player.openInventory(plugin.menu.getNetworkMenu());
            } else if (e.getItem() != null && e.getItem().getType().equals(Material.SPONGE)) {
                plugin.pm.manegeSpecialClick(player, e.getItem());
            }
        }
        e.setCancelled(true);
        if (player.hasPermission("lobby.interact")) {
            e.setCancelled(false);
        } else {
            for (CuboidSelection area : plugin.cm.getInteractionAreas()) {
                if (area.contains(player.getLocation())) {
                    e.setCancelled(false);
                    break;
                }
            }
        }

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage("");
        plugin.olmoTower.removePlayer(e.getPlayer());
    }

    private boolean editionEval(Player player, Location blockLocation) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            return false;
        }

        if (!plugin.pm.isLogged(player)) {
            return false;
        }

        if (player.hasPermission("lobby.build")) {
            return true;
        }

        for (CuboidSelection area : plugin.cm.getEditionAreas()) {
            if (area.contains(blockLocation)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlaceEvent(BlockPlaceEvent e) {
        switch (e.getBlock().getType()) {
            case LAVA:
            case WATER:
            case SIGN:
            case SIGN_POST:
                e.setCancelled(true);
            default:
                e.setCancelled(!editionEval(e.getPlayer(), e.getBlock().getLocation()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreakEvent(BlockBreakEvent e) {
        e.setCancelled(!editionEval(e.getPlayer(), e.getBlock().getLocation()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        boolean cancel = true;
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if (plugin.olmoTower.isInGame(player)) {
                cancel = false;
            }
        }
        e.setCancelled(cancel);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        Player player = (Player) e.getPlayer();
        if (!plugin.pm.isLogged(player)) {
            e.setCancelled(true);
            player.closeInventory();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (plugin.pm.isLogged(player)) {
            if (player.getGameMode() != GameMode.CREATIVE
                    && !plugin.olmoTower.isInGame(player)) {
                if (!plugin.menu.processClick(e)) {
                    e.setCancelled(true);
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        if (plugin.olmoTower.isInGame(player)) {
            plugin.olmoTower.spawn(player);
        } else {
            plugin.pm.spawn(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.setDeathMessage("");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (plugin.cm.isFrontServer()) {
            e.setCancelled(true);
        } else {
            Player player = e.getPlayer();
            if (!plugin.pm.isLogged(player)) {
                e.setCancelled(true);
                plugin.sendMessage(player, "Pon /login y tu contraseña para poder hablar.");
                return;
            }
            if (player.hasPermission("lobby.verbiage")) {
                return;
            }
            for (CuboidSelection area : plugin.cm.getSilencedAreas()) {
                if (area.contains(player.getLocation())) {
                    plugin.sendMessage(player, "Shhhh... este es un área de silencio, si quieres hablar aléjate un poco.");
                    e.setCancelled(true);
                    break;
                }
            }
            if (!e.isCancelled()) {
                int secondsToTalk = (plugin.cm.getMsBetweenChat()
                        - plugin.pm.getMsBetweenLastTlkCmd(player)) / 1000;
                if (secondsToTalk > 0) {
                    plugin.sendMessage(player, "Debes esperar unos segundos para volver a hablar.");
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onJoinEvent(PlayerJoinEvent e) {
        e.setJoinMessage("");
        plugin.pm.registerUser(e.getPlayer());
        plugin.pm.spawn(e.getPlayer());
        plugin.sendMessage(e.getPlayer(), "~ Bienvenido al Lobby ~");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onLoginEvent(PlayerLoginEvent e) {
        if (plugin.cm.isFrontServer()) {

            if (plugin.pm.isBoggedDown(e.getPlayer())) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                        plugin.cm.getTarpittingPenaltyMessage());
                return;
            }

            if (!plugin.cm.isPremium()) {
                if (plugin.pm.isPremium(e.getPlayer())) {
                    e.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                            plugin.cm.getPremiumKickMessage());
                    return;
                }
            }
            plugin.pm.spawn(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (cm.isFrontServer()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        boolean permission = player.hasPermission("lobby.move-to-anyplace");
        if (!permission) {
            if (e.getTo().getBlockY() > cm.getPlayerMaxAllowedHeight()) {
                if (!plugin.olmoTower.isInGame(player)) {
                    e.setCancelled(true);
                    player.teleport(e.getFrom());
                }
            } else {
                if (e.getTo().getBlockY() <= 0) {
                    plugin.pm.spawn(player);
                }
            }
        }

        if (plugin.pm.isLogged(player)) {
            if (player.getAllowFlight() == true
                    && player.getLocation().distance(player.getWorld().getSpawnLocation()) - 1 > cm.getPlayerFlyBlocksFromZero()) {
                if (!permission && player.getGameMode() != GameMode.CREATIVE) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                    plugin.sendMessage(player, "A partir de aqui ya no puedes volar.");
                }
            } else if (player.getAllowFlight() == false
                    && player.getLocation().distance(player.getWorld().getSpawnLocation()) + 1 < cm.getPlayerFlyBlocksFromZero()) {
                player.teleport(player.getLocation().add(0, 1, 0));
                player.setAllowFlight(true);
                player.setFlying(true);
                plugin.sendMessage(player, "A partir de aqui puedes volver a volar.");
            }

            for (CuboidSelection area : plugin.cm.getSilencedAreas()) {
                if (area.contains(e.getFrom()) && !area.contains(e.getTo())) {
                    plugin.sendMessage(player, "A partir de aquí puedes hablar.");
                    break;
                }
            }

            if (player.getGameMode() == GameMode.ADVENTURE) {
                for (CuboidSelection area : plugin.cm.getEditionAreas()) {
                    if (area.contains(e.getTo()) && !area.contains(e.getFrom())) {
                        plugin.pm.clearStuff(player);
                        player.setGameMode(GameMode.CREATIVE);
                        break;
                    }
                }
            } else if (player.getGameMode() == GameMode.CREATIVE) {
                for (CuboidSelection area : plugin.cm.getEditionAreas()) {
                    if (!area.contains(e.getTo()) && area.contains(e.getFrom())) {
                        player.setGameMode(GameMode.ADVENTURE);
                        if (plugin.pm.isLogged(player)) {
                            plugin.pm.giveLoggedStuff(player);
                        } else {
                            plugin.pm.giveUnloggedStuff(player);
                        }
                        break;
                    }
                }

            }
        }
    }
    /*
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        plugin.getLogger().log(Level.INFO, 
                "From={0} | To={1} | Cause={2}", 
                new Object[]{e.getFrom(), e.getTo(), e.getCause()});
    }
    */
}
