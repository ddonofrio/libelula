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

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class MapManager {

    private class Listener implements org.bukkit.event.Listener {

        private final Player player;
        private final Arena arena;

        public Listener(Player player, Arena arena) {
            this.player = player;
            this.arena = arena;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreack(BlockBreakEvent e) {
            if (e.getPlayer().equals(player)) {
                if (arena.blockingBlocks == null) {
                    arena.blockingBlocks = new ArrayList<>();
                }
                arena.blockingBlocks.add(e.getBlock());
                player.sendMessage(ChatColor.GREEN + "Añadido bloque " + e.getBlock().getType().toString() + " en "
                        + "(" + e.getBlock().getWorld().getName() + ") " + e.getBlock().getX() + " "
                        + e.getBlock().getY() + " " + e.getBlock().getZ() + " ");
                e.setCancelled(true);
            }
        }
    }

    public class Arena implements Comparable<Arena> {

        protected final String name;
        protected TreeMap<String, List<Location>> teamSpawnPoints;
        protected ProtectedRegion area;
        protected int waitSecForPlayers;
        protected Location capturePoint;
        protected World world;
        protected boolean enabled;
        protected List<Block> blockingBlocks;
        protected int maxPlayers;
        protected int minPlayers;

        public Arena(String name) {
            this.name = name;
            teamSpawnPoints = null;
            area = null;
            capturePoint = null;
            world = null;
            waitSecForPlayers = 20;
        }

        @Override
        public int compareTo(Arena o) {
            return name.compareTo(o.name);
        }
    }
    private final Main plugin;
    private final TreeMap<String, Arena> arenas;
    private final TreeMap<String, Listener> listeners;
    private Location lobby;

    public enum result {

        OK, ALLREADY_EXISTS, DONT_EXISTS, REGION_DONT_EXISTS, WORLD_MISSSMATCH,
        TEAM_DONT_EXISTS, NOT_IN_ARENA, NOT_SETTING_UP, UNCONFIG_TEAM,
        ONLY_ONE_TEAM, UNCONFIG_AREA, UNCONFIG_CAPPOINT, NOT_ENABLED,
        LOBBY_NOT_SET
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public MapManager(Main plugin) {
        this.plugin = plugin;
        arenas = new TreeMap<>();
        listeners = new TreeMap<>();
        try {
            load();
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe(ex.toString());
        }
    }

    public void setLobby(Location location) {
        lobby = location;
    }

    public Location getLobby() {
        return lobby;
    }

    public result create(String name) {
        if (arenas.keySet().contains(name)) {
            return result.ALLREADY_EXISTS;
        }
        Arena newArena = new Arena(name);
        arenas.put(name, newArena);
        return result.OK;
    }

    public void setWorld(String name, World world) {
        arenas.get(name).world = world;
    }

    public result setProtectedRegion(String arenaName, String regionName) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            return result.DONT_EXISTS;
        }

        ProtectedRegion pr = plugin.wgm.getRegionByName(arena.world, regionName);
        if (pr == null) {
            return result.REGION_DONT_EXISTS;
        }

        arena.area = pr;

        return result.OK;
    }

    public result setCapture(String arenaName, Location capturePoint) {
        Arena arena = arenas.get(arenaName);

        if (arena == null) {
            return result.DONT_EXISTS;
        }

        if (!capturePoint.getWorld().getName().equals(arena.world.getName())) {
            return result.WORLD_MISSSMATCH;
        }

        arenas.get(arenaName).capturePoint = capturePoint;

        return result.OK;
    }

    public result setSpawn(String teamName, Location location) {
        if (!plugin.teamMan.isValid(teamName)) {
            return result.TEAM_DONT_EXISTS;
        }
        Arena arena = null;
        for (ProtectedRegion pr : plugin.wgm.getRegionManager(location.getWorld()).getApplicableRegions(location)) {
            for (Arena arenaTemp : arenas.values()) {
                if (arenaTemp.area.getId().equals(pr.getId())) {
                    arena = arenaTemp;
                    break;
                }
            }
        }

        if (arena == null) {
            return result.NOT_IN_ARENA;
        }

        if (arena.teamSpawnPoints == null) {
            arena.teamSpawnPoints = new TreeMap<>();
        }

        if (!arena.teamSpawnPoints.containsKey(teamName)) {
            arena.teamSpawnPoints.put(teamName, new ArrayList<Location>());
        }

        arena.teamSpawnPoints.get(teamName).add(location);
        arena.maxPlayers++;
        return result.OK;
    }

    public result setBlockingBlocks(String arenaName, Player player) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            return result.DONT_EXISTS;
        }
        if (listeners.containsKey(player.getName())) {
            return result.OK;
        }
        Listener listener = new Listener(player, arena);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listeners.put(player.getName(), listener);
        return result.OK;
    }

    public result finish(String arenaName, Player player) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            return result.DONT_EXISTS;
        }
        if (!listeners.containsKey(player.getName())) {
            return result.NOT_SETTING_UP;
        }
        HandlerList.unregisterAll(listeners.get(player.getName()));
        arena.minPlayers = arena.teamSpawnPoints.keySet().size();
        return result.OK;
    }

    public result disable(String arenaName) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            return result.DONT_EXISTS;
        }
        if (!arena.enabled) {
            return result.NOT_ENABLED;
        }
        plugin.stopGame(arenaName);
        return result.OK;
    }

    public result enable(String arenaName) {
        Arena arena = arenas.get(arenaName);
        if (lobby == null) {
            return result.LOBBY_NOT_SET;
        }

        if (arena == null) {
            return result.DONT_EXISTS;
        }

        if (arena.teamSpawnPoints == null) {
            return result.UNCONFIG_TEAM;
        }

        if (arena.teamSpawnPoints.keySet().size() < 2) {
            return result.ONLY_ONE_TEAM;
        }

        if (arena.area == null) {
            return result.UNCONFIG_AREA;
        }

        if (arena.capturePoint == null) {
            return result.UNCONFIG_CAPPOINT;
        }

        arena.enabled = true;
        plugin.startGame(arenaName);

        return result.OK;
    }

    public final void load() throws IOException, FileNotFoundException, InvalidConfigurationException {
        File dir = new File(plugin.getDataFolder(), "arenas");
        FileConfiguration customConfig = new YamlConfiguration();

        File globalsFile = new File(plugin.getDataFolder(), "globals.yml");

        if (!globalsFile.exists()) {
            String message = "Globals file missing, aborting arena load process.";
            plugin.getLogger().severe(message);
            plugin.alert(ChatColor.RED, message);
            return;
        }
        customConfig.load(globalsFile);
        World lobbyWorld = null;
        if (customConfig.getString("lobby.world") != null) {
            lobbyWorld = plugin.getServer().getWorld(customConfig.getString("lobby.world"));
        }
        if (lobbyWorld != null) {
            lobby = new Location(plugin.getServer().getWorld(customConfig.getString("lobby.world")),
                    customConfig.getDouble("lobby.X"), customConfig.getDouble("lobby.Y"),
                    customConfig.getDouble("lobby.Z"), (float) customConfig.getDouble("lobby.yaw"),
                    (float) customConfig.getDouble("lobby.pitch"));
        } else {
            plugin.getLogger().warning("Lobby not set, all arenas will be disabled.");
        }

        for (File customConfigFile : dir.listFiles()) {
            Arena arena = new Arena(customConfigFile.getName());
            arena.teamSpawnPoints = new TreeMap<>();
            customConfig.load(customConfigFile);
            /*
             protected List<Block> blockingBlocks;
             */
            arena.enabled = customConfig.getBoolean("enabled") && (lobby != null);

            arena.world = plugin.getServer().getWorld(customConfig.getString("world"));
            arena.area = plugin.wgm.getRegionByName(arena.world, customConfig.getString("area"));
            arena.waitSecForPlayers = customConfig.getInt("wait-before-start");
            arena.capturePoint = new Location(arena.world,
                    customConfig.getInt("capture-point.X"),
                    customConfig.getInt("capture-point.Y"),
                    customConfig.getInt("capture-point.Z"));

            for (String teamName : customConfig.getConfigurationSection("spawn").getKeys(false)) {
                List<Location> teamSpawns = new ArrayList<>();
                for (String locId : customConfig.getConfigurationSection("spawn." + teamName).getKeys(false)) {
                    double x = customConfig.getDouble("spawn." + teamName + "." + locId + ".X");
                    double y = customConfig.getDouble("spawn." + teamName + "." + locId + ".Y");
                    double z = customConfig.getDouble("spawn." + teamName + "." + locId + ".Z");
                    double pitch = customConfig.getDouble("spawn." + teamName + "." + locId + ".pitch");
                    double yaw = customConfig.getDouble("spawn." + teamName + "." + locId + ".yaw");
                    teamSpawns.add(new Location(arena.world, x, y, z, (float) yaw, (float) pitch));
                    arena.maxPlayers++;
                }
                arena.teamSpawnPoints.put(teamName, teamSpawns);
            }
            arena.blockingBlocks = new ArrayList<>();
            for (String id : customConfig.getConfigurationSection("blocking").getKeys(false)) {
                arena.blockingBlocks.add(new Location(arena.world, customConfig.getInt("blocking." + id + ".X"),
                        customConfig.getInt("blocking." + id + ".Y"),
                        customConfig.getInt("blocking." + id + ".Z")).getBlock());
            }
            arena.minPlayers = arena.teamSpawnPoints.keySet().size();
            arenas.put(arena.name, arena);
            if (arena.enabled) {
                enable(arena.name);
            }

        }
    }

    public void persist() {
        File dir = new File(plugin.getDataFolder(), "arenas");
        FileConfiguration customConfig = new YamlConfiguration();
        File customConfigFile;
        dir.mkdirs();

        File globalsFile = new File(plugin.getDataFolder(), "globals.yml");

        if (globalsFile.exists()) {
            try {
                customConfig.load(globalsFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().info(ex.toString());
            }
        }

        if (lobby != null) {
            customConfig.set("lobby.world", lobby.getWorld().getName());
            customConfig.set("lobby.X", lobby.getX());
            customConfig.set("lobby.Y", lobby.getY());
            customConfig.set("lobby.Z", lobby.getZ());
            customConfig.set("lobby.pitch", lobby.getPitch());
            customConfig.set("lobby.yaw", lobby.getYaw());
        }

        try {
            customConfig.save(globalsFile);
        } catch (IOException ex) {
            plugin.alert(ChatColor.RED, ex.toString());
        }

        for (Arena arena : arenas.values()) {
            customConfig = new YamlConfiguration();
            customConfigFile = new File(dir, arena.name);

            customConfig.set("world", arena.world.getName());

            customConfig.set("wait-before-start", arena.waitSecForPlayers);
            customConfig.set("area", arena.area.getId());
            for (String teamName : arena.teamSpawnPoints.keySet()) {
                int id = 0;
                for (Location loc : arena.teamSpawnPoints.get(teamName)) {
                    customConfig.set("spawn." + teamName + "." + id + ".X", loc.getX());
                    customConfig.set("spawn." + teamName + "." + id + ".Y", loc.getY());
                    customConfig.set("spawn." + teamName + "." + id + ".Z", loc.getZ());
                    customConfig.set("spawn." + teamName + "." + id + ".pitch", loc.getPitch());
                    customConfig.set("spawn." + teamName + "." + id + ".yaw", loc.getYaw());
                    id++;
                }
            }
            customConfig.set("capture-point.X", arena.capturePoint.getBlockX());
            customConfig.set("capture-point.Y", arena.capturePoint.getBlockY());
            customConfig.set("capture-point.Z", arena.capturePoint.getBlockZ());
            customConfig.set("enabled", arena.enabled);

            int id = 0;
            for (Block block : arena.blockingBlocks) {
                customConfig.set("blocking." + id + ".X", block.getX());
                customConfig.set("blocking." + id + ".Y", block.getY());
                customConfig.set("blocking." + id + ".Z", block.getZ());
                id++;
            }

            try {
                customConfig.save(customConfigFile);
            } catch (IOException ex) {
                plugin.alert(ChatColor.RED, ex.toString());
            }
        }
    }

    public Set<String> getMaps() {
        return arenas.keySet();
    }

}
