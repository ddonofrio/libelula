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

package me.libelula.capturethewool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 * 
 */

public final class RoomManager {

    private final Main plugin;
    private final YamlConfiguration roomsConfig;
    private final File roomsConfigFile;
    private final TreeMap<String, Room> rooms;

    private class Room {

        String name;
        List<World> maps;
        List<World> worlds;
        boolean enabled;
        int mapIndex;

        public Room(String name) {
            this.name = name;
        }
        
    }

    public void removeWools(String mapName, World newWorld) {
        for (Location loc : plugin.mm.getWoolWinLocations(mapName)) {
            Location capturePoint = new Location(newWorld, loc.getBlockX(),
                    loc.getBlockY(), loc.getBlockZ());
            capturePoint.getBlock().setType(Material.AIR);
        }
    }

    public RoomManager(Main plugin) {
        this.plugin = plugin;
        roomsConfig = new YamlConfiguration();
        roomsConfigFile = new File(plugin.getDataFolder(), "rooms.yml");
        rooms = new TreeMap<>();
    }

    public void init() {
        if (rooms.isEmpty()) {
            load();
            for (String roomName : getRooms(true)) {
                Room room = rooms.get(roomName);
                

                plugin.gm.addGame(roomName);
            }
        }
    }

    public void load() {
        if (roomsConfigFile.exists()) {
            try {
                roomsConfig.load(roomsConfigFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }
        }
        rooms.clear();
        for (String roomName : roomsConfig.getKeys(false)) {
            Room room = new Room(roomName);
            room.enabled = roomsConfig.getBoolean(roomName + ".enabled");
            List<String> worldNames = roomsConfig.getStringList(roomName +  ".map");
            if (worldNames != null) {
                room.worlds = new ArrayList<>();
                room.maps = new ArrayList<>();
                for (String worldName : worldNames) {
                    World map = plugin.wm.loadWorld(worldName);
                    if (map != null) {
                        if (plugin.mm.getRestaurationArea(worldName) == null) {
                            plugin.alert("Ignoring map \"" + worldName + "\": restauration area is not set.");
                            continue;
                        }
                        room.maps.add(map);
                        plugin.wm.cloneWorld(map, roomName + "_" + worldName);
                        World world = plugin.wm.loadWorld(roomName + "_" + worldName);
                        room.worlds.add(world);
                         plugin.wm.restoreMap(plugin.mm.getMapData(worldName), world); // Restore map.
                        removeWools(map.getName(), world);
                    } 
                }
            }
            if (room.maps == null || room.maps.isEmpty()) {
                room.enabled = false;
            } else if (room.worlds == null || room.worlds.isEmpty()) {
                room.enabled = false;
                plugin.getLogger().info("There is no worlds to load for " + room.name);
            }
            
            rooms.put(roomName, room);
        }
    }

    public void persist() {
        for (Room room : rooms.values()) {
            if (room.maps != null) {
                List<String> mapList = new ArrayList<>();
                for (World map:room.maps) {
                    mapList.add(map.getName());
                }
                roomsConfig.set(room.name + ".map", mapList);
            }
            roomsConfig.set(room.name + ".enabled", room.enabled);
        }
        try {
            roomsConfig.save(roomsConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
        }
    }

    public boolean add(String roomName) {
        if (rooms.containsKey(roomName)) {
            return false;
        }
        Room room = new Room(roomName);
        rooms.put(roomName, room);
        return true;
    }

    public boolean exists(String roomName) {
        return rooms.containsKey(roomName);
    }

    public boolean isEnabled(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }

        return room.enabled;
    }

    public boolean hasMaps(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }
        return room.maps != null;
    }

    public boolean addMap(String roomName, World map) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }

        if (room.enabled) {
            return false;
        }

        if (room.maps == null) {
            room.maps = new ArrayList<>();
        }

        room.maps.add(map);
        return true;
    }

    public boolean hasMap(String roomName, World map) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (room.maps == null) {
            return false;
        }
        return room.maps.contains(map);
    }

    public boolean removeMap(String roomName, World map) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (room.enabled) {
            return false;
        }
        if (room.maps == null) {
            return false;
        }

        return room.maps.remove(map);
    }

    public boolean remove(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (room.enabled) {
            return false;
        }
        rooms.remove(roomName);
        return true;
    }

    public List<String> list() {
        List<String> list = new ArrayList<>();
        for (Room room : rooms.values()) {
            String entry = ChatColor.AQUA + room.name;
            if (room.enabled) {
                entry = entry.concat(ChatColor.GREEN + " (" + plugin.lm.getText("enabled") + ") ");
            } else {
                entry = entry.concat(ChatColor.RED + " (" + plugin.lm.getText("disabled") + ") ");
            }
            String mapList;
            if (room.maps != null) {
                mapList = ChatColor.GREEN + "[ ";
                for (World world : room.maps) {
                    mapList = mapList.concat(world.getName()).concat(" ");
                }
                mapList = mapList.concat("]");
            } else {
                mapList = ChatColor.RED + "[" + plugin.lm.getText("none") + "]";
            }

            entry = entry.concat(ChatColor.AQUA + plugin.lm.getText("maps") + ": " + mapList);

            list.add(entry);
        }
        return list;
    }
    
    public boolean enable(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (room.enabled) {
            return false;
        }
        room.enabled = true;
        prepareRoom(room);
        plugin.gm.addGame(roomName);
        plugin.sm.updateSigns(roomName);
        return true;
    }

    public boolean disable(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return false;
        }
        if (!room.enabled) {
            return false;
        }
        room.enabled = false;
        if (room.worlds != null) {
            for (World world : room.worlds) {
                plugin.wm.unloadWorld(world);
            }            
        }
        room.mapIndex = 0;
        plugin.gm.removeGame(roomName);
        plugin.sm.updateSigns(roomName);
        return true;
    }

    public Set<String> getRooms() {
        return rooms.keySet();
    }

    /**
     *
     * @param enabled Filters results for enabled/disabled rooms.
     * @return a list of enabled/disabled rooms.
     */
    public List<String> getRooms(boolean enabled) {
        List<String> results = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (room.enabled == enabled) {
                results.add(room.name);
            }
        }
        return results;
    }

    /**
     *
     * @param roomName the name of the room
     * @return the maps contained on this room
     */
    public Collection<World> getMaps(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return null;
        }
        return room.maps;
    }

    public String getCurrentMap(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return null;
        }
        return room.maps.get(room.mapIndex).getName();
    }

    public String getNextMap(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return null;
        }
        if (room.mapIndex + 1 >= room.maps.size()) {
            return room.maps.get(0).getName();
        } else {
            return room.maps.get(room.mapIndex + 1).getName();
        }
    }

    public World getCurrentWorld(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null || room.worlds == null) {
            return null;
        }
        if (room.worlds.isEmpty()) {
            return null;
        }
        return room.worlds.get(room.mapIndex);
    }

    public World getNextWorld(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return null;
        }
        if (room.mapIndex + 1 >= room.maps.size()) {
            return room.worlds.get(0);
        } else {
            return room.worlds.get(room.mapIndex + 1);
        }
    }

    public void swapMap(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return;
        }
        
        plugin.wm.restoreMap(plugin.mm.getMapData(room.maps.get(room.mapIndex).getName()), 
                room.worlds.get(room.mapIndex));
        
        if (room.maps.size() <= (room.mapIndex + 1)) {
            room.mapIndex = 0;
        } else {
            room.mapIndex++;
        }
    }

    public boolean isInGame(World world) {
        for (Room room:rooms.values()) {
            if (room.worlds.contains(world)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isProhibited(Item item){
        String roomName = getRoom(item.getWorld());
        if (roomName == null) {
            return false;
        }
        MapManager.MapData data = plugin.mm.getMapData(getCurrentMap(roomName));
        if (data.noDropOnBreak != null) {
            return data.noDropOnBreak.contains(item.getItemStack().getType());
        }
        return false;
    }

    public String getRoom(World world) {
        for (String roomName : rooms.keySet()) {
            World currentWorld = getCurrentWorld(roomName);
            if (currentWorld != null && currentWorld.getName().equals(world.getName())) {
                return roomName;
            }
        }
        return null;

    }
    
    private void prepareRoom(Room room) {
        room.worlds = new ArrayList<>();
        for (World map:room.maps) {
            String worldName = room.name + "_" + map.getName();
            World world = plugin.wm.loadWorld(worldName);
            if (world == null) {
                world = plugin.wm.cloneWorld(map, worldName);
            }
            removeWools(map.getName(), world);
            room.worlds.add(world);
            plugin.wm.clearEntities(world);
        }
    }
}
