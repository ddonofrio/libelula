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

package me.libelula.autoshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public final class EventListener implements org.bukkit.event.Listener {

    private final Main plugin;

    public EventListener(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.WALL_SIGN
                && plugin.isPlayerCreatingShops(e.getPlayer())) {
            final Sign sign = (Sign) e.getBlock().getState();
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.createShop(sign, e.getPlayer());
            });
            e.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onQuitEvent(PlayerQuitEvent e) {
        plugin.removeShopCreator(e.getPlayer());
    }
    
}
