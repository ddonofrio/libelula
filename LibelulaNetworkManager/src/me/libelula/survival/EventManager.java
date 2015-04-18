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
package me.libelula.survival;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import me.libelula.networkmanager.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class EventManager implements Listener {

    private final Main plugin;
    private final TreeMap<String, List<Location>> lastEdited;
    private final TreeMap<String, Long> timeToEdit;
    private final ReentrantLock _lastEdited_mutex;

    public EventManager(Main plugin) {
        this.plugin = plugin;
        this.lastEdited = new TreeMap<>();
        this.timeToEdit = new TreeMap<>();
        this._lastEdited_mutex = new ReentrantLock(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerBucketEmptyEvent e) {
        if (e.getBucket().equals(Material.LAVA_BUCKET)
                && !e.getPlayer().hasPermission("lnm.survival.allow-lava")) {
            e.getPlayer().sendMessage(plugin.getPrefix() + ChatColor.RED
                    + "Debido a tu rango no tienes permitido poner lava.");
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("lnm.lnm.survival.allow-tnt")
                && e.getBlock().getType() == Material.TNT) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(plugin.getPrefix() + ChatColor.RED
                    + "Debido a tu rango no tienes permitido poner TNT.");

        } else if (!e.getPlayer().hasPermission("lnm.survival.allow-vertical-place")
                && !canEdit(e.getPlayer(), e.getBlock().getLocation(), true)) {

            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("lnm.survival.allow-vertical-break")
                && !canEdit(e.getPlayer(), e.getBlock().getLocation(), false)) {

            e.setCancelled(true);
        }
    }
    
    
    private boolean canEdit(Player player, Location location, boolean placed) {
        boolean ret = true;
        _lastEdited_mutex.lock();
        try {
            List<Location> lastBlocks = lastEdited.get(player.getName());
            if (lastBlocks == null) {
                lastBlocks = new ArrayList<>();
                lastEdited.put(player.getName(), lastBlocks);
            } else if (lastBlocks.size() < 5) {
                lastBlocks.add(location);
            } else {

                Long time = timeToEdit.get(player.getName());
                if (time != null) {
                    long now = new Date().getTime();
                    if (now < time) {
                        int left = (int) (time - now) / 1000;
                        if (left > 0) {
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED
                                    + "Ahora deberás esperar " + left + " segundos para volver a poner un bloque.");
                            ret = false;
                        }
                    } else {
                        timeToEdit.remove(player.getName());
                    }
                }

                if (ret) {
                    lastBlocks.remove(0);
                    lastBlocks.add(location);
                    boolean verticalPlaced = true;
                    int direction;
                    if (placed) {
                        direction = -1;
                    } else {
                        direction = 1;
                    }
                    
                    for (int i = lastBlocks.size() - 1; i > 0; i--) {
                        if (lastBlocks.get(i).getBlockX() != lastBlocks.get(i - 1).getBlockX()
                                || lastBlocks.get(i).getBlockZ() != lastBlocks.get(i - 1).getBlockZ()
                                || lastBlocks.get(i).getBlockY() == lastBlocks.get(i - 1).getBlockY() + direction) {
                            verticalPlaced = false;
                        }
                    }
                    if (verticalPlaced) {
                        if (placed) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED
                                + "Debido a tu rango no tienes permitido poner más bloques en una sola fila.");
                        } else {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED
                                + "Debido a tu rango no tienes permitido cabar más blocues en la misma fila.");
                        }
                        ret = false;
                        timeToEdit.put(player.getName(), new Date().getTime() + 5000);
                    }
                }
            }
        } finally {
            _lastEdited_mutex.unlock();
        }
        return ret;
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        _lastEdited_mutex.lock();
        try {
            lastEdited.remove(e.getPlayer().getName());
            timeToEdit.remove(e.getPlayer().getName());
        } finally {
            _lastEdited_mutex.unlock();
        }
    }
}
