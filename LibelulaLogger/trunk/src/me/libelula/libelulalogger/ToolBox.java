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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class ToolBox of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class ToolBox implements Listener {

    private final LibelulaLogger plugin;
    private TreeMap<Player, Tool> assignedTools;
    private final Lock _discoveryTools_mutex;

    private enum ToolType {

        DISCOVERY, RESTORE
    }

    private class Tool {

        public ToolType type;
        public ItemStack item;
    }

    @SuppressWarnings("unchecked")
    public ToolBox(final LibelulaLogger plugin) {
        this.plugin = plugin;
        _discoveryTools_mutex = new ReentrantLock(true);
        assignedTools = new TreeMap<>(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Player player1;
                Player player2;
                player1 = (Player) o1;
                player2 = (Player) o2;
                return player1.getUniqueId().compareTo(player2.getUniqueId());
            }
        });
    }

    private void startListeningForEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void stopListeningForEvents() {
        BlockBreakEvent.getHandlerList().unregister(this);
        BlockPlaceEvent.getHandlerList().unregister(this);
        PlayerDropItemEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
    }

    public void giveDiscoveryTool(Player player) {
        giveTool(player, ToolType.DISCOVERY);
    }

    public void giveRestoreTool(Player player) {
        giveTool(player, ToolType.RESTORE);
    }

    private void giveTool(Player player, ToolType toolType) {
        ItemStack itemTool;

        if (assignedTools.containsKey(player) && assignedTools.get(player).type == toolType) {
            itemTool = assignedTools.get(player).item;
            if (player.getItemInHand().equals(itemTool)) {
                player.sendMessage(ChatColor.YELLOW + "Just hit the blocks to know who placed them...");
                player.sendMessage(ChatColor.YELLOW + "...or put put this block to know who broken one.");
            } else {
                player.getInventory().remove(itemTool);
                addToolToPlayerInventory(itemTool, player);
            }
        } else {
            if (assignedTools.containsKey(player)) {
                itemTool = assignedTools.get(player).item;
                player.getInventory().remove(itemTool);
                removeAssignedToolfromList(player);
            }
            itemTool = forgeTool(toolType);
            if (addToolToPlayerInventory(itemTool, player)) {
                Tool tool = new Tool();
                tool.item = itemTool;
                tool.type = toolType;
                addAssignedToolToList(player, tool);
                player.sendMessage(ChatColor.GREEN + "Here you go.");
            } else {
                player.sendMessage(ChatColor.RED + "You don't have enough space in inventory.");
            }
        }
    }

    private boolean addToolToPlayerInventory(ItemStack tool, Player player) {
        if (player.getInventory().firstEmpty() != -1) {
            if (player.getItemInHand().getTypeId() == 0) {
                player.setItemInHand(tool);
            } else {
                player.getInventory().setItem(player.getInventory().firstEmpty(), player.getItemInHand());
                player.setItemInHand(tool);
            }
            return true;
        }

        return false;
    }

    private ItemStack forgeTool(ToolType type) {
        ItemStack tool;
        ArrayList<String> lore = new ArrayList<>();
        String toolName;
        switch (type) {
            case DISCOVERY:
                toolName = "LibelulaLogger Discovery tool";
                Material discovery = Material.matchMaterial(plugin.getConfig().getString("discovery-item"));
                if (discovery == null) {
                    plugin.getLogger().warning("Invalid configured material at configuration key discovery-item="
                            .concat(plugin.getConfig().getString("discovery-item")));
                    discovery = Material.BURNING_FURNACE;
                }
                tool = new ItemStack(discovery);
                lore.add("Who made this?");
                break;
            case RESTORE:
                toolName = "LibelulaLogger fixing tool";
                Material restore = Material.matchMaterial(plugin.getConfig().getString("restore-item"));
                if (restore == null || restore == Material.AIR || restore == Material.REDSTONE_LAMP_ON) {
                    plugin.getLogger().warning("Invalid configured material at configuration key restore-item="
                            .concat(plugin.getConfig().getString("restore-item")));
                    restore = Material.REDSTONE_LAMP_OFF;
                    plugin.getLogger().warning("Auto configured default item =" + restore.name());
                }
                tool = new ItemStack(restore);
                lore.add("I can fix it!");
                break;
            default:
                return null;
        }
        ItemMeta dtMeta = tool.getItemMeta();
        dtMeta.setDisplayName(ChatColor.AQUA + toolName);
        dtMeta.setLore(lore);
        tool.setItemMeta(dtMeta);
        return tool;
    }

    private void addAssignedToolToList(Player player, Tool tool) {
        _discoveryTools_mutex.lock();
        try {
            if (assignedTools.isEmpty()) {
                startListeningForEvents();
            }
            assignedTools.put(player, tool);
        } finally {
            _discoveryTools_mutex.unlock();
        }

    }

    public void removeAllTools() {
        while (!assignedTools.isEmpty()) {
            Player player = assignedTools.firstEntry().getKey();
            Tool tool = assignedTools.firstEntry().getValue();
            player.getInventory().removeItem(tool.item);
            removeAssignedToolfromList(player);
        }
    }

    private void removeAssignedToolfromList(Player player) {
        _discoveryTools_mutex.lock();
        try {
            assignedTools.remove(player);
            if (assignedTools.isEmpty()) {
                stopListeningForEvents();
            }
        } finally {
            _discoveryTools_mutex.unlock();
        }
    }

    // Events -----------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerDropItem(PlayerDropItemEvent e) {

        Player player = e.getPlayer();

        if (assignedTools.containsKey(player)) {
            Tool tool = assignedTools.get(player);
            if (tool.item.equals(e.getItemDrop().getItemStack())) {
                e.getItemDrop().remove();
                removeAssignedToolfromList(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (assignedTools.containsKey(player)) {
            player.getInventory().remove(assignedTools.get(player).item);
            removeAssignedToolfromList(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (assignedTools.containsKey(player)) {
            Tool tool = assignedTools.get(player);
            if (tool.item.equals(player.getItemInHand())) {
                e.setCancelled(true);
                if (tool.type == ToolType.DISCOVERY) {
                    plugin.meode.asyncTellBlockInfo(e.getBlock(), player, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockDamage(BlockDamageEvent e) {
        Player player = e.getPlayer();
        if (assignedTools.containsKey(player)) {
            Tool tool = assignedTools.get(player);
            if (tool.item.equals(player.getItemInHand())) {
                e.setCancelled(true);
                if (tool.type == ToolType.DISCOVERY) {
                    plugin.meode.asyncTellBlockInfo(e.getBlock(), player, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (assignedTools.containsKey(player)) {
            Tool tool = assignedTools.get(player);
            if (tool.item.equals(player.getItemInHand())) {
                e.setCancelled(true);
                if (tool.type == ToolType.DISCOVERY) {
                    plugin.meode.asyncTellBlockInfo(e.getBlock(), player, false);
                } else {
                    plugin.meode.asyncRestoreBlock(e.getBlock(), player);
                }
            }
        }
    }

    // Prevents to get in bed while trying to restore a home.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void playerBedEnter(PlayerBedEnterEvent e) {
        Player player = e.getPlayer();
        if (assignedTools.containsKey(player)) {
            Tool tool = assignedTools.get(player);
            if (tool.item.equals(player.getItemInHand())) {
                e.setCancelled(true);
            }
        }
    }
}
