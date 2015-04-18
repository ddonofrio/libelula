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
package me.libelula.climber;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ScoreBoardManager {

    private class Listener implements org.bukkit.event.Listener {

        private Player player;

        public Listener() {
        }

        public void setPLayer(Player player) {
            this.player = player;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent e) {
            Block block = e.getBlock();
            if (e.getPlayer().equals(player)) {
                if (block.getState() instanceof Sign) {
                    Sign sign = (Sign) block.getState();
                    sign.setLine(0, ChatColor.GREEN + "-  # " + (signList.size() + 1) + "  -");
                    sign.setLine(2, "----");
                    sign.setLine(3, ChatColor.BLUE + "0");
                    signList.put(signList.size() + 1, block.getLocation());
                    sign.update();
                    e.setCancelled(true);
                }
            }
        }
    }

    private final Main plugin;
    private TreeMap<Integer, Location> signList;
    private final Listener listener;

    public ScoreBoardManager(Main plugin) {
        this.plugin = plugin;
        signList = new TreeMap<>();
        listener = new Listener();
        FileConfiguration customConfig = new YamlConfiguration();
        customConfig = new YamlConfiguration();
        HandlerList.unregisterAll(listener);

        File globalsFile = new File(plugin.getDataFolder(), "globals.yml");
        if (globalsFile.exists()) {
            try {
                customConfig.load(globalsFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.alert(ChatColor.RED, ex.toString());
            }
        }
        ConfigurationSection cs = customConfig.getConfigurationSection("signs");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                World world = plugin.getServer().getWorld(customConfig.getString("signs." + id + ".world"));
                int x = customConfig.getInt("signs." + id + ".X");
                int y = customConfig.getInt("signs." + id + ".Y");
                int z = customConfig.getInt("signs." + id + ".Z");
                Location loc = new Location(world, x, y, z);
                signList.put(Integer.parseInt(id), loc);
            }
            //plugin.getLogger().info("Loaded signs: " + signList);
        } else {
            plugin.getLogger().info("No Scoreboard wall has been defined.");
        }
    }

    public void setScoreboardStart(Player player) {
        signList = new TreeMap<>();
        listener.setPLayer(player);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public void setScoreboardFinish() {
        FileConfiguration customConfig = new YamlConfiguration();
        customConfig = new YamlConfiguration();
        HandlerList.unregisterAll(listener);
        File globalsFile = new File(plugin.getDataFolder(), "globals.yml");
        try {
            customConfig.load(globalsFile);
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.alert(ChatColor.RED, ex.toString());
        }

        customConfig.getKeys(false).remove("signs");

        for (Integer id : signList.keySet()) {
            customConfig.set("signs." + id + ".world", signList.get(id).getWorld().getName());
            customConfig.set("signs." + id + ".X", signList.get(id).getBlockX());
            customConfig.set("signs." + id + ".Y", signList.get(id).getBlockY());
            customConfig.set("signs." + id + ".Z", signList.get(id).getBlockZ());
        }

        try {
            customConfig.save(globalsFile);
        } catch (IOException ex) {
            plugin.alert(ChatColor.RED, ex.toString());
        }
    }

    static <K, V extends Comparable<? super V>>
            SortedSet<Map.Entry<K, V>> reversedSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
                    @Override
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public void updateScores(Map<String, Integer> scores) {
        try {
            plugin.sql.incrementScores(scores);
            Map<String, Integer> TopScores = plugin.sql.getBestScores(signList.size());
            int pos = 0;
            //plugin.getLogger().info("here! (" + signList.size() + ") " + TopScores);
            for (Map.Entry <String, Integer>value : reversedSortedByValues(TopScores)) {
                //plugin.getLogger().info("here! + " + value.getKey());
                pos++;
                Location loc = signList.get(pos);
                if (loc != null) {
                    Block block = loc.getBlock();
                    if (block.getState() instanceof Sign) {
                        Sign sign = (Sign) block.getState();
                        sign.setLine(2, value.getKey());
                        sign.setLine(3, ChatColor.BLUE + "" + TopScores.get(value.getKey()));
                        sign.update();
                    }
                } else {
              //      plugin.getLogger().info("Debug + (" + pos + ") " + signList);
                    break;
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.toString());
        }
    }

}
