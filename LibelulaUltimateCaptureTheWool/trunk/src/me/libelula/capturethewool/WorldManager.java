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

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.FileUtil;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public final class WorldManager {

    private final Main plugin;
    private final EmptyGenerator eg;
    private final YamlConfiguration worlds;
    private final File worldsConfigFile;
    private final List<Location> lobbySpawnLocations;
    private int currentLobbySpawnPoint;
    private final TreeMap<String, CuboidSelection> restoreAreas;
    private final ReentrantLock _restoreAreas_mutex;

    public class EmptyGenerator extends ChunkGenerator {

        private final ArrayList<BlockPopulator> populator;
        private final byte[][] blocks;

        public EmptyGenerator() {
            super();
            populator = new ArrayList<>();
            blocks = new byte[256 / 16][];
        }

        @Override
        public List<BlockPopulator> getDefaultPopulators(World world) {
            return populator;
        }

        @Override
        public boolean canSpawn(World world, int x, int z) {
            return true;
        }

        @Override
        public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
            return blocks;
        }

    }

    public WorldManager(Main plugin) {
        this.plugin = plugin;
        eg = new EmptyGenerator();
        worlds = new YamlConfiguration();
        worldsConfigFile = new File(plugin.getDataFolder(), "worlds.yml");
        lobbySpawnLocations = new ArrayList<>();
        load();
        currentLobbySpawnPoint = 0;
        _restoreAreas_mutex = new ReentrantLock(true);
        restoreAreas = new TreeMap<>();
    }

    public void load() {
        if (worldsConfigFile.exists()) {
            try {
                worlds.load(worldsConfigFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }
        }
        lobbySpawnLocations.clear();
        processLobbySpawnList();
    }

    public void persist() {
        try {
            worlds.save(worldsConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
        }
    }

    public EmptyGenerator getEmptyWorldGenerator() {
        return eg;
    }

    private void setDefaults(World world) {
        world.setAmbientSpawnLimit(0);
        world.setAnimalSpawnLimit(0);
        world.setAutoSave(true);
        world.setDifficulty(Difficulty.EASY);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setMonsterSpawnLimit(0);
        world.setPVP(true);
        world.setWaterAnimalSpawnLimit(0);
        world.setWeatherDuration(Integer.MAX_VALUE);
    }

    public World loadWorld(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            File worldDir = new File(worldName);
            if (worldDir.exists() && worldDir.isDirectory()) {
                WorldCreator creator = WorldCreator.name(worldName).seed(0).
                        environment(World.Environment.NORMAL);
                creator.generator(getEmptyWorldGenerator());
                world = Bukkit.createWorld(creator);
            }
        }
        if (world != null) {
            setDefaults(world);
        }
        return world;
    }

    private World createWorld(String worldName) {
        World world = loadWorld(worldName);
        if (world == null) {
            Random r = new Random();
            long seed = (long) (r.nextDouble() * Long.MAX_VALUE);
            World.Environment e = World.Environment.NORMAL;
            WorldCreator creator = WorldCreator.name(worldName).seed(seed).environment(e);
            creator.generator(getEmptyWorldGenerator());
            world = Bukkit.createWorld(creator);
            setDefaults(world);
        }
        return world;
    }

    public World createEmptyWorld(String worldName) {
        World world = createWorld(worldName);
        world.getBlockAt(0, 61, 0).setType(Material.GLASS);
        world.setSpawnLocation(0, 63, 0);
        return world;
    }

    public void clearEntities(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.getType() != EntityType.ITEM_FRAME
                    && entity.getType() != EntityType.PLAYER) {
                entity.remove();
            }
        }
    }

    public boolean unloadWorld(final World world) {
        for (Player player : world.getPlayers()) {
            player.teleport(getNextLobbySpawn());
        }

        for (Player player : world.getPlayers()) {
            player.kickPlayer("");
        }

        clearEntities(world);

        world.setKeepSpawnInMemory(false);

        for (Chunk chunk : world.getLoadedChunks()) {
            world.unloadChunk(chunk);
        }
        boolean ret = Bukkit.unloadWorld(world, false);
        return ret;
    }

    /**
     *
     * @param world the world to be copied.
     * @param newWorld the new world name
     * @return true if done, false on failure.
     */
    public World cloneWorld(World world, String newWorld) {
        return cloneWorld(world, newWorld, true);
    }

    public World cloneWorld(World world, String newWorld, boolean load) {
        File newWorldDir = new File(newWorld);
        if (newWorldDir.exists()) {
            return null;
        }
        try {
            copyFolder(world.getWorldFolder(), newWorldDir);
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
            return null;
        }
        File uidFile = new File(newWorld, "uid.dat");
        uidFile.delete();
        World ret;
        if (load) {
            ret = loadWorld(newWorld);
        } else {
            ret = null;
        }
        return ret;
    }

    public boolean cloneRegions(World origin, String destination) {
        File regionDir = new File(destination, "region");

        if (!regionDir.exists()) {
            return false;
        }
        try {
            delete(regionDir);
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
            return false;
        }
        try {
            copyFolder(new File(origin.getWorldFolder(), "region"), regionDir);
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
            return false;
        }
        return true;
    }

    public boolean eraseWorld(String worldName) {
        return eraseWorld(worldName, true);
    }

    public boolean eraseWorld(String worldName, boolean test) {
        if (test) {
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                unloadWorld(world);
            }
        }
        File worldDir = new File(worldName);
        if (worldDir.exists() && worldDir.isDirectory()) {
            try {
                delete(worldDir);
                return true;
            } catch (IOException ex) {
                plugin.getLogger().severe(ex.toString());
            }
        }
        return false;
    }

    private void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        f.delete();
    }

    private static void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }
            String files[] = src.list();
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyFolder(srcFile, destFile);
            }
        } else {
            if (!FileUtil.copy(src, dest)) {
                System.out.println("Error copying: " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
            }
        }
    }

    public World getLobbyWorld() {
        if (lobbySpawnLocations.isEmpty()) {
            return null;
        }
        return lobbySpawnLocations.get(0).getWorld();
    }

    public void addSpawnLocation(Location spawnPoint) {
        String key = spawnPoint.getBlockX() + "x" + spawnPoint.getBlockY() + "y"
                + spawnPoint.getBlockZ() + "z";
        worlds.set("lobby.spawnpoints." + key + ".x", spawnPoint.getX());
        worlds.set("lobby.spawnpoints." + key + ".y", spawnPoint.getY());
        worlds.set("lobby.spawnpoints." + key + ".z", spawnPoint.getZ());
        worlds.set("lobby.spawnpoints." + key + ".pitch", spawnPoint.getPitch());
        worlds.set("lobby.spawnpoints." + key + ".yaw", spawnPoint.getYaw());
        for (int n = 0; n < lobbySpawnLocations.size(); n++) {
            Location loc = lobbySpawnLocations.get(n);
            if (loc.getBlockX() == spawnPoint.getBlockX()
                    && loc.getBlockY() == spawnPoint.getBlockY()
                    && loc.getBlockZ() == spawnPoint.getBlockZ()) {
                lobbySpawnLocations.remove(n);
                break;
            }
        }
        lobbySpawnLocations.add(spawnPoint);
        worlds.set("lobby.world", spawnPoint.getWorld().getName());
    }

    private void processLobbySpawnList() {
        String worldName = worlds.getString("lobby.world");
        if (worldName == null) {
            return;
        }
        World lobbyWorld = loadWorld(worldName);
        if (lobbyWorld == null) {
            return;
        }
        double x;
        double y;
        double z;
        double pitch;
        double yaw;
        for (String spawnPoint : worlds.getConfigurationSection("lobby.spawnpoints").getKeys(false)) {
            x = worlds.getDouble("lobby.spawnpoints." + spawnPoint + ".x");
            y = worlds.getDouble("lobby.spawnpoints." + spawnPoint + ".y");
            z = worlds.getDouble("lobby.spawnpoints." + spawnPoint + ".z");
            pitch = worlds.getDouble("lobby.spawnpoints." + spawnPoint + ".pitch");
            yaw = worlds.getDouble("lobby.spawnpoints." + spawnPoint + ".yaw");
            lobbySpawnLocations.add(new Location(lobbyWorld, x, y, z, (float) yaw, (float) pitch));
        }

    }

    public List<Location> getLobbySpawnLocations() {
        return lobbySpawnLocations;
    }

    public void clearLobbyInformation() {
        lobbySpawnLocations.clear();
    }

    public Location getNextLobbySpawn() {
        Location ret;
        if (currentLobbySpawnPoint >= lobbySpawnLocations.size()) {
            currentLobbySpawnPoint = 0;
        }
        ret = lobbySpawnLocations.get(currentLobbySpawnPoint);
        currentLobbySpawnPoint++;
        return ret;
    }

    public boolean isOnLobby(Player player) {
        return getLobbyWorld().getName().equals(player.getWorld().getName());
    }

    public void addModificationPoint(Location loc) {
        CuboidSelection cs = restoreAreas.get(loc.getWorld().getName());
        if (cs == null) {
            cs = new CuboidSelection(loc.getWorld(), loc, loc);
        } else {
            Location min = cs.getMinimumPoint();
            Location max = cs.getMaximumPoint();
            if (min.getBlockX() > loc.getBlockX()) {
                min.setX(loc.getBlockX());
            }
            if (min.getBlockY() > loc.getBlockY()) {
                min.setX(loc.getBlockY());
            }
            if (min.getBlockZ() > loc.getBlockZ()) {
                min.setX(loc.getBlockZ());
            }

            if (max.getBlockX() < loc.getBlockX()) {
                max.setX(loc.getBlockX());
            }
            if (max.getBlockY() < loc.getBlockY()) {
                max.setX(loc.getBlockY());
            }
            if (max.getBlockZ() < loc.getBlockZ()) {
                max.setX(loc.getBlockZ());
            }
            cs = new CuboidSelection(loc.getWorld(), min, max);
        }
        _restoreAreas_mutex.lock();
        try {
            restoreAreas.put(loc.getWorld().getName(), cs);
        } finally {
            _restoreAreas_mutex.unlock();
        }
    }

    public void restoreMap(final MapManager.MapData map, final World roomMap) {
        Location min = map.restaurationArea.getMinimumPoint();
        Location max = map.restaurationArea.getMaximumPoint();
        int count = 1;
        final World source = map.world;
        if (source == null) {
            return;
        }

        for (int X = min.getBlockX(); X <= max.getBlockX(); X++) {
            Location areaMin = new Location(source,
                    X, min.getBlockY(), min.getBlockZ());
            Location areaMax = new Location(source,
                    X, max.getBlockY(), max.getBlockZ());
            final Selection sel = new CuboidSelection(source, areaMin, areaMax);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    cloneRegion(plugin, source, roomMap, sel);
                }
            }, count);
            count = count + 2;
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.rm.removeWools(map.world.getName(), roomMap);
            }
        }, count);

    }

    private static void cloneRegion(final Main plugin, World source, World destination, Selection area) {
        Location min = area.getMinimumPoint();
        Location max = area.getMaximumPoint();

        for (int X = min.getBlockX(); X <= max.getBlockX(); X++) {
            for (int Y = min.getBlockY(); Y <= max.getBlockY(); Y++) {
                for (int Z = min.getBlockZ(); Z <= max.getBlockZ(); Z++) {
                    final Block src = source.getBlockAt(X, Y, Z);
                    final Block dst = destination.getBlockAt(X, Y, Z);
                    dst.setTypeIdAndData(src.getTypeId(), src.getData(), false);
                    if (src.getType() == Material.SIGN_POST
                            || src.getType() == Material.WALL_SIGN) {
                        final Sign s = (Sign) src.getState();
                        final Sign d = (Sign) dst.getState();
                        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                            @Override
                            public void run() {
                                d.setLine(0, s.getLine(0));
                                d.setLine(1, s.getLine(1));
                                d.setLine(2, s.getLine(2));
                                d.setLine(3, s.getLine(3));
                                d.update();
                            }
                        }, 1);
                    } else if (src.getType() == Material.CHEST
                            || src.getType() == Material.TRAPPED_CHEST) {

                        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                            @Override
                            public void run() {
                                final Chest s = (Chest) src.getState();
                                final Chest d = (Chest) dst.getState();
                                try {
                                    d.getInventory().setContents(s.getInventory().getContents());
                                } catch (IllegalArgumentException ex) {
                                    // Do nothing.
                                }
                            }
                        }, 1);

                    }
                }
            }
        }
        plugin.wm.clearEntities(destination);
    }

}
