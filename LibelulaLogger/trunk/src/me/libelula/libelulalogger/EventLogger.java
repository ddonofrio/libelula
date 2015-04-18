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
package me.libelula.libelulalogger;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Iterator;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Class EventLogger of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public class EventLogger implements Listener {

    private final LibelulaLogger plugin;

    public EventLogger(LibelulaLogger plugin) {
        this.plugin = plugin;
    }

    private boolean mustBeLogged(Player player, Block block) {

        for (Iterator<Material> it = plugin.config.getIgnoredMaterials().iterator(); it.hasNext();) {
            Material material = it.next();
            if (block.getType().equals(material)) {
                return false;
            }
        }

        if (!plugin.config.getIgnoredPlayerNames().isEmpty()) {
            if (!plugin.config.getIgnoredPlayerNames().contains(player.getName())) {
                return false;
            }
        }

        if (!plugin.config.getIgnoredWorlds().isEmpty()) {
            if (!plugin.config.getIgnoredWorlds().contains(block.getWorld())) {
                return false;
            }
        }


        if (plugin.config.getFlagValue("only-modified-blocks") && !plugin.meode.hasItBeenChanged(block)) {
            return false;
        }

        if (!plugin.config.wgRegionPolicyIsDefault()) {
            if (plugin.worldGuardPlugin != null) {
                RegionManager regionManager = plugin.worldGuardPlugin.getRegionManager(block.getWorld());
                switch (plugin.config.getWgRegionPolicy()) {
                    case "NEVER":
                        if (regionManager.getApplicableRegions(block.getLocation()).size() != 0) {
                            return false;
                        }
                        break;
                    case "DIFFERS":
                        for (ProtectedRegion region : regionManager.getApplicableRegions(block.getLocation())) {
                            if (region.isMember(player.getName())) {
                                return false;
                            } else {
                                break;
                            }
                        }

                }
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {

        if (mustBeLogged(e.getPlayer(), e.getBlock())) {
            plugin.meode.addEvent(e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {

        if (mustBeLogged(e.getPlayer(), e.getBlock())) {
            plugin.meode.addEvent(e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignEdit(SignChangeEvent e) {
        if (plugin.config.logSignsToFile()) {
            Object objects[] = {e};
            new LogToTextFile(plugin, LogToTextFile.types.SIGN, objects).runTaskAsynchronously(plugin);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() != InventoryType.CHEST || !plugin.config.logChestToFile()) {
            return;
        }
        plugin.playerOpenChest((Player) e.getPlayer(), e.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestClose(InventoryCloseEvent e) {
        if (e.getInventory().getType() != InventoryType.CHEST || !plugin.config.logChestToFile()) {
            return;
        }
        plugin.playerCloseChest((Player) e.getPlayer(), e.getInventory());
    }
}
