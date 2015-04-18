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
package me.libelula.limbo;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public final class EventManager implements Listener {

    private final Main plugin;

    public EventManager(Main plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeatherChange(WeatherChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        e.getPlayer().teleport(plugin.getNextSpawn());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.getEntity().teleport(plugin.getNextSpawn());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawnEvent(ItemSpawnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
        e.getPlayer().sendMessage(ChatColor.GOLD + "Lo sentimos, no podemos permitir chat en el limbo.");
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onJoinEvent(PlayerJoinEvent e) {
        e.setJoinMessage("");
        e.getPlayer().teleport(plugin.getNextSpawn());
        for (Player player : e.getPlayer().getWorld().getPlayers()) {
            player.hidePlayer(e.getPlayer());
            e.getPlayer().hidePlayer(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlaceEvent(BlockPlaceEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreakEvent(BlockBreakEvent e) {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().toLowerCase().startsWith("/me")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "No puedes hacer eso aquí.");
        }
    }
}
