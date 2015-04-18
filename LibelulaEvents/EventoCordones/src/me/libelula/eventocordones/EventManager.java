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
package me.libelula.eventocordones;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class EventManager implements Listener {

    private final Main plugin;

    public EventManager(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onNameTag(AsyncPlayerReceiveNameTagEvent e) {
        e.setTag(plugin.pm.getNameTagColor(e.getNamedPlayer()) + e.getNamedPlayer().getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin.gm.isInGame(e.getPlayer()) || !e.getPlayer().hasPermission("ec.modify")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (plugin.gm.isInGame(e.getPlayer()) || !e.getPlayer().hasPermission("ec.modify")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.DEFAULT) {
            e.setCancelled(true);
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
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSignChange(SignChangeEvent e) {
        plugin.sm.checkForJoinSignCreation(e);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (e.getClickedBlock().getType() == Material.WALL_SIGN
                    || e.getClickedBlock().getType() == Material.SIGN_POST) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                plugin.sm.checkForJoin(sign, e.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent e) {
        if (plugin.gm.isInGame(e.getPlayer()) || !e.getPlayer().hasPermission("ec.modify")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (!plugin.am.isArena(e.getTo().getWorld())
                && plugin.am.isArena(e.getFrom().getWorld())) {
            plugin.pm.backToNormal(e.getPlayer());
            plugin.gm.leftGame(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (plugin.spawn != null) {
            e.getPlayer().teleport(plugin.spawn);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.gm.leftGame(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (plugin.gm.isInGame(e.getPlayer()) && !plugin.pm.hasTeam(e.getPlayer())) {
            plugin.gm.checkPortalUse(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        if (plugin.gm.isInGame(e.getPlayer()) && plugin.pm.hasTeam(e.getPlayer())) {
            e.getPlayer().getInventory().setArmorContents(plugin.pm.getArmour(e.getPlayer()));
            plugin.pm.setStuff(e.getPlayer(), true);
        } else {
            e.getPlayer().teleport(plugin.spawn);
            plugin.pm.backToNormal(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (plugin.gm.isInGame(player) && plugin.pm.hasTeam(player)) {
            e.getDrops().remove(player.getInventory().getBoots());
            e.getDrops().remove(player.getInventory().getChestplate());
            e.getDrops().remove(player.getInventory().getHelmet());
            e.getDrops().remove(player.getInventory().getLeggings());
            ItemStack ingot = new ItemStack(Material.IRON_INGOT);
            e.getDrops().add(ingot);
            plugin.pm.setArmour(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (plugin.gm.isInGame(player) && plugin.pm.hasTeam(player)) {
            e.setCancelled(true);
            for (Player inGamePlayer : player.getWorld().getPlayers()) {
                inGamePlayer.sendMessage(ChatColor.YELLOW + "<" + plugin.pm.getNameTagColor(player)
                        + player.getName() + ChatColor.YELLOW + ">" + ChatColor.WHITE + " " + e.getMessage());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Arrow arrow;
        if (e.getEntity() instanceof Player == false) {
            return;
        }
        final Player player = (Player) e.getEntity();
        

        if (!plugin.gm.isInGame(player) || !plugin.pm.hasTeam(player)) {
            return;
        }
        
        Player damager;
        if (e.getDamager() instanceof Player == false) {
            if (e.getDamager() instanceof Arrow == false) {
                return;
            } else {
                arrow = (Arrow) e.getDamager();
                if (arrow.getShooter() instanceof Player) {
                    damager = (Player) arrow.getShooter();
                } else {
                    return;
                }
            }
        } else {
            damager = (Player) e.getDamager();
        }
        
        if (!plugin.gm.isInGame(damager) && plugin.pm.hasTeam(damager)) {
            return;
        }


        if (plugin.pm.getTeamName(player).equals(plugin.pm.getTeamName(damager))) {
            e.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        plugin.gm.checkForWin((Player) e.getPlayer());
        
    }

    
}
