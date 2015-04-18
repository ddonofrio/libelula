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
package me.libelula.eventohiipo50k;

import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public final class Listener implements org.bukkit.event.Listener {

   private final Main plugin;

    public Listener(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeatherChange(WeatherChangeEvent e) {
        if (e.toWeatherState()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        e.getPlayer().teleport(plugin.getServer().getWorld("lobby").getSpawnLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.setDeathMessage("");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onJoinEvent(PlayerJoinEvent e) {
        e.getPlayer().teleport(plugin.getServer().getWorld("lobby").getSpawnLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlaceEvent(BlockPlaceEvent e) {
        if (plugin.winner != null) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.YELLOW + "El jugador " + 
            ChatColor.GOLD + plugin.winner + ChatColor.YELLOW + " ha ganado la partida.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreakEvent(BlockBreakEvent e) {
        if (plugin.winner != null) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.YELLOW + "El jugador " + 
            ChatColor.GOLD + plugin.winner + ChatColor.YELLOW + " ha ganado la partida.");
            plugin.getLogger().log(Level.INFO, "{0} ha ganado la partida.", plugin.winner);
        } else {
            if (e.getBlock().getType() == Material.SPONGE) {
                plugin.winner = e.getPlayer().getName();
                plugin.broadcast("¡¡¡" + e.getPlayer().getName() + " ha ganado la partida!!!");
            }
        }
        
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent e) {
        if (plugin.winner != null) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.YELLOW + "El jugador " + 
            ChatColor.GOLD + plugin.winner + ChatColor.YELLOW + " ha ganado la partida.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        e.setCancelled(true);
    }

}
