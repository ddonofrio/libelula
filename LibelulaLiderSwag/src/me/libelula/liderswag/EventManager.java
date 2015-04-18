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
package me.libelula.liderswag;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.Material;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class EventManager {

    public enum setupEvents {

        HEADS, SIGNS
    }

    private class SetupListener implements Listener {

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlace(BlockPlaceEvent e) {
            setupEvents setupEvent = setupPlayers.get(e.getPlayer());
            switch (setupEvent) {
                case HEADS:
                    if (e.getBlock().getType() == Material.SKULL) {
                        //Skull skull = new CraftSkull(e.getBlock());
                        Skull skull = (Skull) e.getBlock().getState();
                        plugin.am.addScoreHead(skull,
                                setupPlayersArena.get(e.getPlayer()));
                        e.getPlayer().sendMessage(plugin.lm.getText("score-heads-added"));
                        e.getPlayer().sendMessage(plugin.lm.getText("listen-setup-finish"));
                    }
                    break;
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreak(BlockBreakEvent e) {
            setupEvents setupEvent = setupPlayers.get(e.getPlayer());
            switch (setupEvent) {
                case HEADS:
                    if (e.getBlock().getType() == Material.SKULL) {
                        //Skull skull = new CraftSkull(e.getBlock());
                        Skull skull = (Skull) e.getBlock().getState();
                        if (plugin.am.delScoreHead(skull,
                                setupPlayersArena.get(e.getPlayer()))) {
                            e.getPlayer().sendMessage(plugin.lm.getText("score-heads-removed"));
                            e.getPlayer().sendMessage(plugin.lm.getText("listen-setup-finish"));
                        } else {
                            e.getPlayer().sendMessage(plugin.lm.getText("score-heads-desordered-del"));
                            e.setCancelled(true);
                        }
                    }
                    break;
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent e) {
            removeSetUpListerners(e.getPlayer());
        }
    }

    private class GameEvents implements Listener {

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent e) {
            for (CuboidSelection area : protectedAreas) {
                if (area.contains(e.getBlock().getLocation())) {
                    e.setCancelled(true);
                    break;
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onBlockPlace(BlockPlaceEvent e) {
            for (CuboidSelection area : protectedAreas) {
                if (area.contains(e.getBlock().getLocation())) {
                    e.setCancelled(true);
                    break;
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onMobSpawn(CreatureSpawnEvent e) {
            if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                    || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.DEFAULT) {
                for (CuboidSelection area : protectedAreas) {
                    if (area.contains(e.getLocation())) {
                        e.setCancelled(true);
                        break;
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onWeatherChange(WeatherChangeEvent e) {
            if (e.toWeatherState()) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onBlockIgnite(BlockIgniteEvent e) {
            for (CuboidSelection area : protectedAreas) {
                if (area.contains(e.getBlock().getLocation())) {
                    e.setCancelled(true);
                    break;
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onSignChange(SignChangeEvent e) {
            plugin.sgm.checkUpdateJoin(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onPlayerInteract(PlayerInteractEvent e) {
            plugin.sgm.checkJoin(e);
            if (plugin.pm.isSpectator(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerPickupItem(PlayerPickupItemEvent e) {
            if (plugin.pm.isSpectator(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onEntityTarget(EntityTargetEvent e) {
            if (e.getTarget() instanceof Player) {
                if (plugin.pm.isSpectator((Player) e.getTarget())) {
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onBlockDamage(BlockDamageEvent e) {
            if (plugin.pm.isSpectator(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onEntityDamage(EntityDamageEvent e) {
            if (e.getEntity() instanceof Player) {
                if (plugin.pm.isSpectator((Player) e.getEntity())) {
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onEntityDamage(EntityDamageByEntityEvent e) {
            if (e.getDamager() instanceof Player) {
                Player player = (Player) e.getDamager();
                if (plugin.pm.isSpectator(player)) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (e.getEntity() instanceof Player) {
                if (plugin.pm.isSpectator((Player) e.getEntity())) {
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onEntityDamage(EntityDamageByBlockEvent e) {
            if (e.getEntity() instanceof Player) {
                if (plugin.pm.isSpectator((Player) e.getEntity())) {
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onFoodLevelChange(FoodLevelChangeEvent e) {
            if (plugin.pm.isSpectator((Player) e.getEntity())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerDrop(PlayerDropItemEvent e) {
            if (plugin.pm.isSpectator(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
            if (plugin.pm.isSpectator(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onInventoryClick(InventoryClickEvent e) {
            if (plugin.pm.isSpectator((Player) e.getWhoClicked())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onInventoryOpen(InventoryOpenEvent e) {
            if (plugin.pm.isSpectator((Player) e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerTeleport(PlayerTeleportEvent e) {
            plugin.gm.controlPlayerMovement(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerMove(PlayerMoveEvent e) {
            plugin.gm.controlPlayerMovement(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            if (plugin.pm.isSpectator(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            if (plugin.pm.isSpectator(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerQuit(PlayerQuitEvent e) {
            plugin.gm.removePlayer(e.getPlayer());
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onRespawn(PlayerRespawnEvent e) {
            if (plugin.pm.isSpectator(e.getPlayer())) {
                plugin.gm.teleportToSpectator(e.getPlayer());
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onDeath(PlayerDeathEvent e) {
            if (plugin.pm.isInGame(e.getEntity())) {
                Player player = (Player) e.getEntity();
                if (plugin.pm.isInGame(player)) {
                    plugin.gm.gameOver(player);
                    plugin.gm.teleportToSpectator(player);
                    e.setDeathMessage("");
                    e.getDrops().clear();
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerChat(AsyncPlayerChatEvent e) {

        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onPlayerChat(FoodLevelChangeEvent e) {
            if (e.getEntity() instanceof Player) {
                Player player = (Player) e.getEntity();
                if (plugin.pm.isInGame(player) || plugin.pm.isSpectator(player)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    private final Main plugin;
    private final SetupListener setupListener;
    private final GameEvents gameEvents;
    private final TreeMap<Player, setupEvents> setupPlayers;
    private final TreeMap<Player, String> setupPlayersArena;
    private final TreeSet<CuboidSelection> protectedAreas;

    public EventManager(Main plugin) {
        this.plugin = plugin;
        setupListener = new SetupListener();
        gameEvents = new GameEvents();
        setupPlayers = new TreeMap<>(new Tools.PlayerComparator());
        setupPlayersArena = new TreeMap<>(new Tools.PlayerComparator());
        protectedAreas = new TreeSet<>(new Tools.SelectionComparator());
        plugin.getServer().getPluginManager().registerEvents(gameEvents, plugin);
    }

    public void addSetUpListerners(Player player, setupEvents event, String arenaName) {
        if (setupPlayers.isEmpty()) {
            plugin.getServer().getPluginManager().registerEvents(setupListener, plugin);
        }
        setupPlayers.put(player, event);
        setupPlayersArena.put(player, arenaName);
    }

    public void removeSetUpListerners(Player player) {
        if (setupPlayers.remove(player) != null) {
            setupPlayersArena.remove(player);
            if (setupPlayers.isEmpty()) {
                HandlerList.unregisterAll(setupListener);
            }
        }
    }

    public void addProtectedArea(CuboidSelection area) {
        protectedAreas.add(area);
    }

    public void removeProtectedArea(CuboidSelection area) {
        protectedAreas.remove(area);
    }

}
