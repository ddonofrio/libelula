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
import java.util.TreeMap;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class GameControler {

    private class Listener implements org.bukkit.event.Listener {

        Main.LocationComparator locComp;
        private Player player;

        public Listener() {
            locComp = new Main.LocationComparator();
        }

        public void setPLayer(Player player) {
            this.player = player;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent e) {
            if (player == null) {
                return;
            }
            Block block = e.getBlock();
            if (e.getPlayer().equals(player)) {
                if (block.getState() instanceof Sign) {
                    Sign sign = (Sign) block.getState();
                    String arenaName = sign.getLine(0);
                    MapManager.Arena arena = plugin.mapMan.getArena(arenaName);
                    if (arena == null) {
                        e.getPlayer().sendMessage(ChatColor.RED + "Incorrect arena name");
                    } else {
                        sign.setLine(0, arenaName);
                        sign.setLine(1, "- " + arena.minPlayers + " equipos -");
                        sign.setLine(2, "0/" + arena.maxPlayers + " jugadores");
                        sign.setLine(3, ChatColor.GREEN + "[Lista]");
                        sign.update();
                        signList.put(signList.size(), block.getLocation());
                    }
                    e.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent e) {
            if (e.getClickedBlock() == null || e.getClickedBlock().getState() instanceof Sign == false) {
                return;
            }
            Sign sign = (Sign) e.getClickedBlock().getState();
            Player player = e.getPlayer();
            for (Location signloc : signList.values()) {

                if (locComp.compare(signloc, e.getClickedBlock().getLocation()) == 0) {
                    Game game = games.get(sign.getLine(0));
                    if (game == null) {
                        player.sendMessage(ChatColor.RED + "Este juego no está en marcha");
                        return;
                    }
                    addPlayer(player, sign.getLine(0));
                }
            }

        }

    }

    private class GameTicker extends BukkitRunnable {

        @Override
        public void run() {
            for (Game game : games.values()) {
                game.tick();
            }
        }
    }

    private final Main plugin;
    private final TreeMap<String, Game> games;
    private TreeMap<Integer, Location> signList;
    private final Listener listener;

    public GameControler(Main plugin) {
        this.plugin = plugin;
        games = new TreeMap<>();
        for (String mapName : plugin.mapMan.getMaps()) {
            MapManager.Arena arena = plugin.mapMan.getArena(mapName);
            if (arena.enabled) {
                if (arena.area == null) {
                    plugin.getLogger().info("Incorrect area for ".concat(mapName));
                    continue;
                }
                plugin.getLogger().info("Loading ".concat(mapName));
                games.put(mapName, new Game(plugin, mapName));
            }
        }
        new GameTicker().runTaskTimer(plugin, 10, 10);
        signList = new TreeMap<>();
        listener = new Listener();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        FileConfiguration customConfig = new YamlConfiguration();
        customConfig = new YamlConfiguration();

        File globalsFile = new File(plugin.getDataFolder(), "globals.yml");
        if (globalsFile.exists()) {
            try {
                customConfig.load(globalsFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.alert(ChatColor.RED, ex.toString());
            }
        }
        ConfigurationSection cs = customConfig.getConfigurationSection("startsigns");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                World world = plugin.getServer().getWorld(customConfig.getString("startsigns." + id + ".world"));
                int x = customConfig.getInt("startsigns." + id + ".X");
                int y = customConfig.getInt("startsigns." + id + ".Y");
                int z = customConfig.getInt("startsigns." + id + ".Z");
                Location loc = new Location(world, x, y, z);
                signList.put(Integer.parseInt(id), loc);
            }
            //plugin.getLogger().info("Loaded signs: " + signList);
        } else {
            plugin.getLogger().info("No Scoreboard wall has been defined.");
        }
    }

    public boolean addPlayer(Player player, String gameName) {
        Game game = games.get(gameName);
        if (game != null) {
            return game.addPlayer(player);
        } else {
            return false;
        }
    }

    public boolean addPlayer(Player player, String gameName, String teamName) {
        Game game = games.get(gameName);
        if (game != null) {
            return game.addPlayer(player, teamName);
        } else {
            return false;
        }
    }

    public void setSignsStart(Player player) {
        signList = new TreeMap<>();
        listener.setPLayer(player);
    }

    public void setSignsFinish() {
        listener.setPLayer(null);
        FileConfiguration customConfig = new YamlConfiguration();
        customConfig = new YamlConfiguration();
        HandlerList.unregisterAll(listener);
        File globalsFile = new File(plugin.getDataFolder(), "globals.yml");
        try {
            customConfig.load(globalsFile);
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.alert(ChatColor.RED, ex.toString());
        }

        customConfig.getKeys(false).remove("startsigns");

        for (Integer id : signList.keySet()) {
            customConfig.set("startsigns." + id + ".world", signList.get(id).getWorld().getName());
            customConfig.set("startsigns." + id + ".X", signList.get(id).getBlockX());
            customConfig.set("startsigns." + id + ".Y", signList.get(id).getBlockY());
            customConfig.set("startsigns." + id + ".Z", signList.get(id).getBlockZ());
        }

        try {
            customConfig.save(globalsFile);
        } catch (IOException ex) {
            plugin.alert(ChatColor.RED, ex.toString());
        }
    }

    public void playerLeave(Player player, String gameName) {
        Game game = games.get(gameName);
        game.removePlayer(player);
    }

    public void playersLeaveAll() {
        for (Game game : games.values()) {
            game.removeAllPlayers();
        }
    }

    public void updateSigngs(Game game) {
        plugin.getLogger().info("Here->" + signList.values());

        for (Location signs : signList.values()) {
            if (signs.getBlock().getState() instanceof Sign == false) {
                continue;
            }

            Sign sign = (Sign) signs.getBlock().getState();
            plugin.getLogger().info("Here++" + sign.getLine(0) + " -> " + game.getArena().name);
            if (sign.getLine(0).equals(game.getArena().name)) {
                sign.setLine(2, game.getPlayerCount() + "/" + game.getArena().maxPlayers + " jugadores");
                if (game.getGameState() == Game.gameState.WAITING_FOR_PLAYERS) {
                    sign.setLine(3, ChatColor.GREEN + "[Lista]");
                } else {
                    sign.setLine(3, ChatColor.DARK_GRAY + "[En juego]");
                }
                sign.update();
            }
        }
    }

}
