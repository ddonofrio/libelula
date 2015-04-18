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
package me.libelula.hiipovssurvival;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public final class EventManager {

    private final Main plugin;
    private final GameListeners gameEvents;

    private class GameListeners implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerMove(PlayerMoveEvent e) {
            if (!plugin.pm.isGhost(e.getPlayer())) {
                for (Player ghost : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.pm.isGhost(ghost)) {
                        if (e.getPlayer().getLocation().distance(ghost.getLocation()) < 4) {
                            Location newPossibleLoc;
                            newPossibleLoc = ghost.getLocation().add(0, 5, 0);
                            if (newPossibleLoc.getBlock().getType() != Material.AIR
                                    || newPossibleLoc.add(0, 1, 0).getBlock().getType() != Material.AIR) {
                                newPossibleLoc = ghost.getLocation().add(5, 0, 0);
                                if (newPossibleLoc.getBlock().getType() != Material.AIR
                                        || newPossibleLoc.add(0, 1, 0).getBlock().getType() != Material.AIR) {
                                    newPossibleLoc = ghost.getLocation().add(-5, 0, 0);
                                    if (newPossibleLoc.getBlock().getType() != Material.AIR
                                            || newPossibleLoc.add(0, 1, 0).getBlock().getType() != Material.AIR) {
                                        newPossibleLoc = ghost.getLocation().add(0, 0, 5);
                                        if (newPossibleLoc.getBlock().getType() != Material.AIR
                                                || newPossibleLoc.add(0, 1, 0).getBlock().getType() != Material.AIR) {
                                            newPossibleLoc = ghost.getLocation().add(0, 0, -5);
                                        }
                                    }
                                }

                            }
                            ghost.teleport(newPossibleLoc);
                            ghost.setAllowFlight(true);
                            ghost.setFlying(true);
                        }
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDeath(PlayerDeathEvent e) {
            if (plugin.pm.isGhost(e.getEntity())) {
                e.setDeathMessage("");
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
            if (plugin.pm.isGhost(e.getPlayer())) {
                int timeToTalk = plugin.pm.getTimeToTalkOrCmd(e.getPlayer());
                if (timeToTalk <= 0) {
                    plugin.pm.registerChatOrCommand(e.getPlayer());
                } else {
                    e.getPlayer().sendMessage(plugin.prefix + ChatColor.RED + "Debes esperar "
                            + timeToTalk + " segundos.");
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerChat(AsyncPlayerChatEvent e) {
            if (plugin.pm.isGhost(e.getPlayer())) {
                int timeToTalk = plugin.pm.getTimeToTalkOrCmd(e.getPlayer());
                if (timeToTalk <= 0) {
                    plugin.pm.registerChatOrCommand(e.getPlayer());
                } else {
                    e.getPlayer().sendMessage(plugin.prefix + ChatColor.RED + "Debes esperar "
                            + timeToTalk + " segundos.");
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onInventoryInteract(InventoryInteractEvent e) {
            Player player = (Player) e.getWhoClicked();
            if (plugin.pm.isGhost(player)) {
                e.setCancelled(true);
                player.closeInventory();
            }

        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerInteract(PlayerInteractEvent e) {
            if (plugin.pm.isGhost(e.getPlayer())) {
                e.setCancelled(true);
                if (e.getItem() == null) {
                    return;
                }
                if (e.getAction().equals(Action.LEFT_CLICK_AIR)
                        || e.getAction().equals(Action.LEFT_CLICK_BLOCK)
                        || e.getAction().equals(Action.RIGHT_CLICK_AIR)
                        || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    if (e.getItem().getType() == Material.ENDER_PEARL) {
                        String targetName = e.getItem().getItemMeta().getDisplayName();
                        for (Player target : plugin.getServer().getOnlinePlayers()) {
                            if (target.getName().equals(targetName)) {
                                e.getPlayer().teleport(target);
                                e.getPlayer().sendMessage(plugin.prefix + "Has sido teleportado a "
                                        + ChatColor.GOLD + targetName);
                                break;
                            }
                        }
                    }
                }

            }
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerDrop(PlayerDropItemEvent e) {
            if (plugin.pm.isGhost(e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (e.getDamager() instanceof Player) {
                if (plugin.pm.isGhost((Player) e.getDamager())) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (e.getEntity() instanceof Player) {
                if (plugin.pm.isGhost((Player) e.getEntity())) {
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerFish(PlayerFishEvent e) {
            if (plugin.pm.isGhost((Player) e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
            if (plugin.pm.isGhost((Player) e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onInventoryClick(InventoryClickEvent e) {
            Player player = (Player) e.getWhoClicked();
            if (plugin.pm.isGhost(player)) {
                if (e.getCurrentItem() == null) {
                    return;
                }
                if (e.getCurrentItem().getType() == Material.ENDER_PEARL) {
                    String targetName = e.getCurrentItem().getItemMeta().getDisplayName();
                    for (Player target : plugin.getServer().getOnlinePlayers()) {
                        if (target.getName().equals(targetName)) {
                            player.teleport(target);
                            player.sendMessage(plugin.prefix + "Has sido teleportado a "
                                    + ChatColor.GOLD + targetName);
                            e.setCancelled(true);
                            player.closeInventory();
                            break;
                        }
                    }
                }
            }
        }

        /*
         @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
         public void onPlayerTeleport(PlayerTeleportEvent e) {

         }
         */
        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent e) {
            if (plugin.pm.isGhost(e.getPlayer())) {
                e.setQuitMessage("");
            } else {
                plugin.pm.removePlayer(e.getPlayer());
                e.setQuitMessage(ChatColor.GOLD + e.getPlayer().getName()
                        + ChatColor.YELLOW + " se ha ido.");
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onJoinEvent(PlayerJoinEvent e) {
            if (plugin.pm.isGhost(e.getPlayer())) {
                e.setJoinMessage("");
                plugin.pm.setSpectator(e.getPlayer());
            } else {
                plugin.pm.setPlayer(e.getPlayer());
                e.setJoinMessage(plugin.prefix + "Ha llegado a la partida el jugador "
                        + ChatColor.GOLD + e.getPlayer().getName());
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            if (plugin.pm.isGhost((Player) e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            if (plugin.pm.isGhost((Player) e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerPickupItem(PlayerPickupItemEvent e) {
            if (plugin.pm.isGhost((Player) e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityTarget(EntityTargetEvent e) {
            if (e.getTarget() instanceof Player) {
                if (plugin.pm.isGhost((Player) e.getTarget())) {
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockDamage(BlockDamageEvent e) {
            if (plugin.pm.isGhost((Player) e.getPlayer())) {
                e.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent e) {
            if (e.getEntity() instanceof Player) {
                if (plugin.pm.isGhost((Player) e.getEntity())) {
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onFoodLevelChange(FoodLevelChangeEvent e) {
            if (plugin.pm.isGhost((Player) e.getEntity())) {
                e.setCancelled(true);
            }
        }

    }

    public EventManager(Main plugin) {
        this.plugin = plugin;
        gameEvents = new GameListeners();
    }

    public void registerGameEvents() {
        plugin.getServer().getPluginManager().registerEvents(gameEvents, plugin);
    }

    public void unregisterGameEvents() {
        HandlerList.unregisterAll(gameEvents);
    }

}
