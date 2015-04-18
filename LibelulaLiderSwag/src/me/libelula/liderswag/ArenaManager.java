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
package me.libelula.liderswag;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ArenaManager {

    private final Main plugin;
    private final TreeMap<String, Arena> arenas;
    private final ReentrantLock _arenas_mutex;

    public enum misconfig {

        ARENA_AREA, COLISSEUM_AREA, KIT, P1_LOC, P2_LOC, SP_LOC, NOTHING
    }

    public enum QueuePriority {

        LOWEST, LOW, NORMAL, HIGH, HIGHEST
    }

    public class Arena {

        private final String name;
        private final File configFile;
        private final YamlConfiguration yaml;
        private CuboidSelection arenaArea;
        private CuboidSelection colisseumArea;
        private Inventory startingKitInventory;
        private ItemStack kitHelmet;
        private ItemStack kitChestplate;
        private ItemStack kitLeggings;
        private ItemStack kitBoots;
        private Location p1StartPoint;
        private Location p2StartPoint;
        private List<Location> spectatorPoints;
        private TreeMap<Integer, Skull> scoreHeads;
        private boolean enabled;
        private List<Location> joinSigns;
        private int maxPlayers;
        private final TreeMap<QueuePriority, TreeMap<Integer, Player>> queuePlayers;
        private final TreeMap<Player, QueuePriority> playersPriority;
        private final TreeMap<Player, Integer> spectators;
        private final TreeSet<Player> inGame;
        private final ReentrantLock _players_Lock;
        private int nextQueNumber;
        private int incInGameCounter;

        public Arena(String name) {
            this.name = name;
            configFile = new File(plugin.getDataFolder(), "arenas/".concat(name));
            yaml = new YamlConfiguration();
            queuePlayers = new TreeMap<>();
            inGame = new TreeSet<>(new Tools.PlayerComparator());
            queuePlayers.put(QueuePriority.LOWEST, new TreeMap<Integer, Player>());
            queuePlayers.put(QueuePriority.LOW, new TreeMap<Integer, Player>());
            queuePlayers.put(QueuePriority.NORMAL, new TreeMap<Integer, Player>());
            queuePlayers.put(QueuePriority.HIGH, new TreeMap<Integer, Player>());
            queuePlayers.put(QueuePriority.HIGHEST, new TreeMap<Integer, Player>());
            playersPriority = new TreeMap<>(new Tools.PlayerComparator());
            spectators = new TreeMap<>(new Tools.PlayerComparator());
            _players_Lock = new ReentrantLock(true);
        }

        public void updateSigns() {
            for (Location loc : joinSigns) {
                if (SignManager.isSign(loc.getBlock())) {
                    Sign sign = (Sign) loc.getBlock().getState();
                    if (sign.getLine(1).equals(name)) {
                        plugin.sgm.updateJoinSign(sign, this);
                    }
                }
            }
        }

        public List<Location> getJoinSignsLocations() {
            return joinSigns;
        }

        public TreeMap<Integer, Skull> getScoreHeads() {
            return scoreHeads;
        }

        public ItemStack[] getStartingKit() {
            return startingKitInventory.getContents();
        }
        
        public ItemStack getBoots() {
            return kitBoots;
        }

        public ItemStack getLeggings() {
            return kitLeggings;
        }

        public ItemStack getChestplate() {
            return kitChestplate;
        }
        
        public ItemStack getHelmet() {
            return kitHelmet;
        }        
        
        public TreeSet<Player> getInGamePlayers() {
            return inGame;
        }

        public Set<Player> getSpectators() {
            return spectators.keySet();
        }

        public boolean isInsideColisseum(Location loc) {
            return colisseumArea.contains(loc);
        }

        public boolean isInsideArena(Location loc) {
            return arenaArea.contains(loc);
        }

        public int getPlayersCount() {
            return spectators.size() + inGame.size();
        }

        public int getinGameCount() {
            return inGame.size() + incInGameCounter;
        }

        public int getSpectatorCount() {
            return spectators.size();
        }

        public String getName() {
            return name;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public Location getNextSpawnPoint() {
            return spectatorPoints.get(nextQueNumber % (spectatorPoints.size() - 1));
        }

        public Location getP1StartPoint() {
            return p1StartPoint;
        }

        public Location getP2StartPoint() {
            return p2StartPoint;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean hasPlayer(Player player) {
            return spectators.containsKey(player) || inGame.contains(player);
        }

        public boolean addPlayerToGame(Player player) {
            return inGame.add(player);
        }

        public void incInGameDummyPlayerCounter() {
            incInGameCounter++;
        }

        public void clearDummyPlayerCounter() {
            incInGameCounter = 0;
        }

        public void sweep() {
            for (Entity entity : colisseumArea.getWorld().getEntities()) {
                if (entity.getType() != EntityType.PLAYER
                        && entity.getType() != EntityType.ITEM_FRAME) {
                    entity.remove();
                }
            }
        }

        public boolean removePlayerFromGame(Player player) {
            return inGame.remove(player);
        }

        public boolean addPlayerToQueue(Player player, QueuePriority priority) {
            boolean result = false;
            _players_Lock.lock();
            try {
                if (maxPlayers == 0 || spectators.size() > maxPlayers) {

                } else {
                }
                Integer currentPosition = spectators.get(player);
                if (currentPosition != null) {
                    QueuePriority currentPriority = playersPriority.get(player);
                    if (currentPriority != priority) {
                        queuePlayers.get(currentPriority).remove(currentPosition);
                    }
                }
                int playerQueuePossition = nextQueNumber++;
                TreeMap<Integer, Player> possitionPlayer = queuePlayers.get(priority);
                possitionPlayer.put(playerQueuePossition, player);
                playersPriority.put(player, priority);
                spectators.put(player, playerQueuePossition);
                result = true;
            } finally {
                _players_Lock.unlock();
            }
            return result;
        }

        public boolean removePlayerFromSpectator(Player player) {
            boolean result = false;
            _players_Lock.lock();
            try {
                QueuePriority currentPriority = playersPriority.remove(player);
                if (currentPriority != null) {
                    Integer currentPosition = spectators.remove(player);
                    queuePlayers.get(currentPriority).remove(currentPosition);
                    result = true;
                }
            } finally {
                _players_Lock.unlock();
            }
            return result;
        }

        public Player removeFirstInQueue() {
            Player player = null;
            _players_Lock.lock();
            try {
                Map.Entry<Integer, Player> possitionPlayer;
                possitionPlayer = queuePlayers.get(QueuePriority.HIGHEST).firstEntry();
                if (possitionPlayer == null) {
                    possitionPlayer = queuePlayers.get(QueuePriority.HIGH).firstEntry();
                    if (possitionPlayer == null) {
                        possitionPlayer = queuePlayers.get(QueuePriority.NORMAL).firstEntry();
                        if (possitionPlayer == null) {
                            possitionPlayer = queuePlayers.get(QueuePriority.LOW).firstEntry();
                            if (possitionPlayer == null) {
                                possitionPlayer = queuePlayers.get(QueuePriority.LOWEST).firstEntry();
                            }
                        }
                    }
                }
                if (possitionPlayer != null) {
                    player = possitionPlayer.getValue();
                    QueuePriority currentPriority = playersPriority.remove(player);
                    Integer currentPosition = spectators.remove(player);
                    queuePlayers.get(currentPriority).remove(currentPosition);
                }
            } finally {
                _players_Lock.unlock();
            }
            return player;
        }

        public boolean isSpectator(Player player) {
            return spectators.containsKey(player);
        }

        public boolean isPlaying(Player player) {
            return inGame.contains(player);
        }

        public boolean isInArena(Player player) {
            return isSpectator(player) || isPlaying(player);
        }

        public List<Player> getPlayersBehind(Player player) {
            List<Player> playersBehind = new ArrayList<>();
            _players_Lock.lock();
            try {
                QueuePriority priority = playersPriority.get(player);
                if (priority != null) {
                    switch (priority) {
                        case HIGHEST:
                            playersBehind.addAll(queuePlayers.get(QueuePriority.HIGH).values());
                        case HIGH:
                            playersBehind.addAll(queuePlayers.get(QueuePriority.NORMAL).values());
                        case NORMAL:
                            playersBehind.addAll(queuePlayers.get(QueuePriority.LOW).values());
                        case LOW:
                            playersBehind.addAll(queuePlayers.get(QueuePriority.LOWEST).values());
                    }
                }
            } finally {
                _players_Lock.unlock();
            }
            return playersBehind;
        }
        
        public List<Player> getPossitions() {
            List<Player> results = new ArrayList<>();
            _players_Lock.lock();
            try {
                results.addAll(queuePlayers.get(QueuePriority.HIGHEST).values());
                results.addAll(queuePlayers.get(QueuePriority.HIGH).values());
                results.addAll(queuePlayers.get(QueuePriority.NORMAL).values());
                results.addAll(queuePlayers.get(QueuePriority.LOW).values());
                results.addAll(queuePlayers.get(QueuePriority.LOWEST).values());
            } finally {
                _players_Lock.unlock();
            }
            return results;
        }

        private void getEnchantFromConfig(ConfigurationSection enchSect, ItemStack is) {
            if (enchSect != null) {
                for (String enchantName : enchSect.getKeys(false)) {
                    Enchantment e = Enchantment.getByName(enchantName);
                    is.addEnchantment(e, enchSect.getInt(enchantName + ".level"));
                }
            }
        }

        public boolean load() {
            boolean result = false;
            if (configFile.exists()) {
                try {
                    yaml.load(configFile);
                    String worldName = yaml.getString("colisseum-area.world");
                    if (worldName != null) {
                        World world = plugin.getServer().getWorld(worldName);
                        if (world != null) {
                            Location colisseumMin = new Location(world,
                                    yaml.getInt("colisseum-area.min.x"),
                                    yaml.getInt("colisseum-area.min.y"),
                                    yaml.getInt("colisseum-area.min.z"));
                            Location colisseumMax = new Location(world,
                                    yaml.getInt("colisseum-area.max.x"),
                                    yaml.getInt("colisseum-area.max.y"),
                                    yaml.getInt("colisseum-area.max.z"));
                            colisseumArea = new CuboidSelection(world, colisseumMin, colisseumMax);

                            if (yaml.isSet("arena-area")) {
                                Location arenaMin = new Location(world,
                                        yaml.getInt("arena-area.min.x"),
                                        yaml.getInt("arena-area.min.y"),
                                        yaml.getInt("arena-area.min.z"));
                                Location arenaMax = new Location(world,
                                        yaml.getInt("arena-area.max.x"),
                                        yaml.getInt("arena-area.max.y"),
                                        yaml.getInt("arena-area.max.z"));
                                arenaArea = new CuboidSelection(world, arenaMin, arenaMax);
                            }
                            enabled = yaml.getBoolean("enabled");

                            if (yaml.isSet("p1-start-point")) {
                                p1StartPoint = getPreciseLocation("p1-start-point", world);
                            }

                            if (yaml.isSet("p2-start-point")) {
                                p2StartPoint = getPreciseLocation("p2-start-point", world);
                            }

                            {
                                ConfigurationSection spect = yaml.getConfigurationSection("spectator");
                                if (spect != null) {
                                    spectatorPoints = new ArrayList<>();
                                    for (String id : spect.getKeys(false)) {
                                        spectatorPoints.add(getPreciseLocation("spectator." + id, world));
                                    }
                                }
                            }

                            result = true;

                        } else {
                            TreeMap<String, String> repl = new TreeMap<>();
                            repl.put("%WORLD%", worldName);
                            repl.put("%ARENA%", name);
                            plugin.alertAdmins(plugin.lm.getText("cant-load-arena-world", repl));
                        }
                    }

                    ConfigurationSection itemsSection = yaml.getConfigurationSection("kit.inventory");
                    if (itemsSection != null) {

                        startingKitInventory = Bukkit.createInventory(null, InventoryType.PLAYER);
                        for (String position : itemsSection.getKeys(false)) {
                            Material mat = Material.getMaterial(itemsSection.getString(position + ".material"));
                            int amount = itemsSection.getInt(position + ".amount");
                            short durability = (short) itemsSection.getInt(position + ".durability");
                            ItemStack is = new ItemStack(mat, amount, durability);
                            ConfigurationSection enchSect = itemsSection.getConfigurationSection(position + ".enchantment");
                            getEnchantFromConfig(enchSect, is);
                            startingKitInventory.setItem(Integer.parseInt(position), is);
                        }
                    }

                    itemsSection = yaml.getConfigurationSection("kit.armour");
                    if (itemsSection != null) {
                        Material mat;
                        short durability;
                        ConfigurationSection enchSect;
                        mat = Material.getMaterial(itemsSection.getString("boots.material"));
                        durability = (short) itemsSection.getInt("boots.durability");
                        enchSect = itemsSection.getConfigurationSection("boots.enchantment");
                        kitBoots = new ItemStack(mat, 1, durability);
                        getEnchantFromConfig(enchSect, kitBoots);
                        mat = Material.getMaterial(itemsSection.getString("leggings.material"));
                        durability = (short) itemsSection.getInt("leggings.durability");
                        enchSect = itemsSection.getConfigurationSection("leggings.enchantment");
                        kitLeggings = new ItemStack(mat, 1, durability);
                        getEnchantFromConfig(enchSect, kitLeggings);
                        mat = Material.getMaterial(itemsSection.getString("chestplate.material"));
                        durability = (short) itemsSection.getInt("chestplate.durability");
                        enchSect = itemsSection.getConfigurationSection("chestplate.enchantment");
                        kitChestplate = new ItemStack(mat, 1, durability);
                        getEnchantFromConfig(enchSect, kitChestplate);
                        mat = Material.getMaterial(itemsSection.getString("helmet.material"));
                        durability = (short) itemsSection.getInt("helmet.durability");
                        enchSect = itemsSection.getConfigurationSection("helmet.enchantment");
                        kitHelmet = new ItemStack(mat, 1, durability);
                        getEnchantFromConfig(enchSect, kitHelmet);
                    }

                    ConfigurationSection headsScore = yaml.getConfigurationSection("scoreboard.head");

                    if (headsScore != null) {
                        World world = plugin.getServer().getWorld(yaml.getString("scoreboard.heads.world"));
                        scoreHeads = new TreeMap<>();
                        int i = 0;
                        for (String id : headsScore.getKeys(false)) {
                            Location loc = new Location(world, headsScore.getInt(id + ".location.x"),
                                    headsScore.getInt(id + ".location.y"),
                                    headsScore.getInt(id + ".location.z"));

                            BlockFace bf = BlockFace.valueOf(headsScore.getString(id + ".rotation"));

                            loc.getBlock().setType(Material.SKULL);
                            Skull skull = (Skull) loc.getBlock().getState();
                            skull.setSkullType(SkullType.PLAYER);
                            skull.setRotation(bf);
                            loc.getBlock().setType(Material.SKULL);
                            skull.update();
                            scoreHeads.put(i, skull);
                            i++;
                        }
                    }

                    ConfigurationSection joinSignsConf = yaml.getConfigurationSection("join-sign");

                    if (joinSignsConf != null) {
                        joinSigns = new ArrayList<>();
                        for (String id : joinSignsConf.getKeys(false)) {
                            World world = plugin.getServer().getWorld(joinSignsConf.getString(id + ".location.world"));
                            if (world != null) {
                                Location signLoc = new Location(world,
                                        joinSignsConf.getInt(id + ".location.x"),
                                        joinSignsConf.getInt(id + ".location.y"),
                                        joinSignsConf.getInt(id + ".location.z"));
                                if (SignManager.isSign(signLoc.getBlock())) {
                                    joinSigns.add(signLoc);
                                }
                            }
                        }
                    }

                    maxPlayers = yaml.getInt("max-players", 0);

                } catch (IOException | InvalidConfigurationException ex) {
                    plugin.getLogger().severe(ex.getMessage());
                }
            }
            return result;
        }

        private void setPreciseLocation(Location loc, String base) {
            yaml.set(base + ".x", loc.getX());
            yaml.set(base + ".y", loc.getY());
            yaml.set(base + ".z", loc.getZ());
            yaml.set(base + ".yaw", loc.getYaw());
            yaml.set(base + ".pitch", loc.getPitch());
        }

        private Location getPreciseLocation(String base, World world) {
            return new Location(world,
                    yaml.getDouble(base + ".x"),
                    yaml.getDouble(base + ".y"),
                    yaml.getDouble(base + ".z"),
                    (float) yaml.getDouble(base + ".yaw"),
                    (float) yaml.getDouble(base + ".pitch"));
        }

        public boolean save() {
            boolean result = false;
            yaml.set("enabled", enabled);

            if (p1StartPoint != null) {
                setPreciseLocation(p1StartPoint, "p1-start-point");
            }

            if (p2StartPoint != null) {
                setPreciseLocation(p2StartPoint, "p2-start-point");
            }

            if (spectatorPoints != null) {
                int i = 0;
                for (Location spectator : spectatorPoints) {
                    setPreciseLocation(spectator, "spectator." + i);
                    i++;
                }
            }

            if (arenaArea != null) {
                yaml.set("arena-area.world", arenaArea.getWorld().getName());
                yaml.set("arena-area.min.x", arenaArea.getMinimumPoint().getBlockX());
                yaml.set("arena-area.min.y", arenaArea.getMinimumPoint().getBlockY());
                yaml.set("arena-area.min.z", arenaArea.getMinimumPoint().getBlockZ());
                yaml.set("arena-area.max.x", arenaArea.getMaximumPoint().getBlockX());
                yaml.set("arena-area.max.y", arenaArea.getMaximumPoint().getBlockY());
                yaml.set("arena-area.max.z", arenaArea.getMaximumPoint().getBlockZ());
            }

            if (colisseumArea != null) {
                yaml.set("colisseum-area.world", colisseumArea.getWorld().getName());
                yaml.set("colisseum-area.min.x", colisseumArea.getMinimumPoint().getBlockX());
                yaml.set("colisseum-area.min.y", colisseumArea.getMinimumPoint().getBlockY());
                yaml.set("colisseum-area.min.z", colisseumArea.getMinimumPoint().getBlockZ());
                yaml.set("colisseum-area.max.x", colisseumArea.getMaximumPoint().getBlockX());
                yaml.set("colisseum-area.max.y", colisseumArea.getMaximumPoint().getBlockY());
                yaml.set("colisseum-area.max.z", colisseumArea.getMaximumPoint().getBlockZ());
            }

            if (startingKitInventory != null) {
                ItemStack[] content = startingKitInventory.getContents();
                for (int i = 0; i < content.length; i++) {
                    ItemStack is = content[i];
                    if (is != null) {
                        yaml.set("kit.inventory." + i + ".material", is.getType().name());
                        yaml.set("kit.inventory." + i + ".amount", is.getAmount());
                        yaml.set("kit.inventory." + i + ".durability", is.getDurability());
                        for (Enchantment enchantment : is.getEnchantments().keySet()) {
                            yaml.set("kit.inventory." + i + ".enchantment." + enchantment.getName() + ".level", is.getEnchantmentLevel(enchantment));
                        }
                    }
                }
            }
                        
            if (kitBoots != null) {
                yaml.set("kit.armour.boots.material", kitBoots.getType().name());
                yaml.set("kit.armour.boots.durability", kitBoots.getDurability());
                for (Enchantment enchantment : kitBoots.getEnchantments().keySet()) {
                        yaml.set("kit.armour.boots.enchantment." + enchantment.getName() + ".level", 
                                kitBoots.getEnchantmentLevel(enchantment));
                }
            }

            if (kitLeggings != null) {
                yaml.set("kit.armour.leggings.material", kitLeggings.getType().name());
                yaml.set("kit.armour.leggings.durability", kitLeggings.getDurability());
                for (Enchantment enchantment : kitLeggings.getEnchantments().keySet()) {
                        yaml.set("kit.armour.leggings.enchantment." + enchantment.getName() + ".level", 
                                kitLeggings.getEnchantmentLevel(enchantment));
                }
            }
            
            if (kitChestplate != null) {
                yaml.set("kit.armour.chestplate.material", kitChestplate.getType().name());
                yaml.set("kit.armour.chestplate.durability", kitChestplate.getDurability());
                for (Enchantment enchantment : kitChestplate.getEnchantments().keySet()) {
                        yaml.set("kit.armour.chestplate.enchantment." + enchantment.getName() + ".level", 
                                kitChestplate.getEnchantmentLevel(enchantment));
                }
            }
            
            if (kitHelmet != null) {
                yaml.set("kit.armour.helmet.material", kitHelmet.getType().name());
                yaml.set("kit.armour.helmet.durability", kitHelmet.getDurability());
                for (Enchantment enchantment : kitHelmet.getEnchantments().keySet()) {
                        yaml.set("kit.armour.helmet.enchantment." + enchantment.getName() + ".level", 
                                kitHelmet.getEnchantmentLevel(enchantment));
                }
            }
            

            yaml.set("scoreboard.heads", null);
            yaml.set("scoreboard.head", null);
            if (scoreHeads != null) {
                for (int i = 0; i < scoreHeads.size(); i++) {
                    if (i == 0) {
                        yaml.set("scoreboard.heads.world", scoreHeads.get(i).getWorld().getName());
                    }
                    yaml.set("scoreboard.head." + i + ".location.x", scoreHeads.get(i).getLocation().getBlockX());
                    yaml.set("scoreboard.head." + i + ".location.y", scoreHeads.get(i).getLocation().getBlockY());
                    yaml.set("scoreboard.head." + i + ".location.z", scoreHeads.get(i).getLocation().getBlockZ());
                    yaml.set("scoreboard.head." + i + ".rotation", scoreHeads.get(i).getRotation().name());
                }
            }

            if (joinSigns != null) {
                yaml.set("join-sign", null);
                int i = 0;
                for (Location loc : joinSigns) {
                    yaml.set("join-sign." + i + ".location.world", loc.getWorld().getName());
                    yaml.set("join-sign." + i + ".location.x", loc.getBlockX());
                    yaml.set("join-sign." + i + ".location.y", loc.getBlockY());
                    yaml.set("join-sign." + i + ".location.z", loc.getBlockZ());
                    i++;
                }
            }

            if (maxPlayers > 0) {
                yaml.set("max-players", maxPlayers);
            }

            File arenaDir = new File(configFile.getParent());
            arenaDir.mkdirs();
            try {
                yaml.save(configFile);
                result = true;
            } catch (IOException ex) {
                plugin.getLogger().severe(ex.getMessage());
            }
            return result;
        }

        public boolean del() {
            return configFile.delete();
        }

    }

    public ArenaManager(Main plugin) {
        this.plugin = plugin;
        arenas = new TreeMap<>();
        _arenas_mutex = new ReentrantLock(true);
    }

    public void add(String arenaName) {
        Arena arena = new Arena(arenaName);
        _arenas_mutex.lock();
        try {
            arenas.put(arenaName, arena);
        } finally {
            _arenas_mutex.unlock();
        }
    }

    public boolean exists(String arenaName) {
        return arenas.containsKey(arenaName);
    }

    public boolean del(String arenaName) {
        Arena arena;
        _arenas_mutex.lock();
        try {
            arena = arenas.remove(arenaName);
        } finally {
            _arenas_mutex.unlock();
        }
        if (arena != null) {
            arena.del();
        }
        return arena != null;
    }

    public void setColisseumArea(CuboidSelection sel, String arenaName) {
        Arena arena = arenas.get(arenaName);
        arena.colisseumArea = new CuboidSelection(sel.getWorld(), sel.getMinimumPoint(), sel.getMaximumPoint());
    }

    public void setArenaArea(CuboidSelection sel, String arenaName) {
        Arena arena = arenas.get(arenaName);
        arena.arenaArea = new CuboidSelection(sel.getWorld(), sel.getMinimumPoint(), sel.getMaximumPoint());
    }

    public TreeMap<String, Boolean> getList() {
        TreeMap<String, Boolean> result = new TreeMap<>();
        _arenas_mutex.lock();
        try {
            for (String arenaName : arenas.keySet()) {
                Arena arena = arenas.get(arenaName);
                result.put(arenaName, arena.enabled);
            }
        } finally {
            _arenas_mutex.unlock();
        }
        return result;
    }

    public boolean setState(String arenaName, boolean enabled) {
        Arena arena = arenas.get(arenaName);
        if (arena == null || arena.colisseumArea == null
                || arena.arenaArea == null || arena.p1StartPoint == null
                || arena.p2StartPoint == null || arena.spectatorPoints == null
                || arena.startingKitInventory == null) {
            return false;
        } else {
            if (arena.enabled != enabled) {
                if (enabled) {
                    plugin.em.addProtectedArea(arena.colisseumArea);
                    arena.sweep();
                    // Sacar los jujadores fuera.
                } else {
                    plugin.em.removeProtectedArea(arena.colisseumArea);
                }
                arena.enabled = enabled;
            }
        }
        return true;
    }

    public misconfig checkConfiguration(String arenaName) {
        Arena arena = arenas.get(arenaName);

        if (arena == null) {
            return null;
        }

        if (arena.colisseumArea == null) {
            return misconfig.COLISSEUM_AREA;
        }

        if (arena.arenaArea == null) {
            return misconfig.ARENA_AREA;
        }

        if (arena.p1StartPoint == null) {
            return misconfig.P1_LOC;
        }

        if (arena.p2StartPoint == null) {
            return misconfig.P2_LOC;
        }

        if (arena.spectatorPoints == null) {
            return misconfig.SP_LOC;
        }

        if (arena.startingKitInventory == null) {
            return misconfig.KIT;
        }

        return misconfig.NOTHING;
    }

    public String getArenaName(Location loc) {
        String result = null;
        _arenas_mutex.lock();
        try {
            for (String arenaName : arenas.keySet()) {
                Arena arena = arenas.get(arenaName);
                if (arena.colisseumArea != null
                        && arena.colisseumArea.contains(loc)) {
                    result = arenaName;
                    break;
                }
            }
        } finally {
            _arenas_mutex.unlock();
        }
        return result;
    }

    public void setStratingKit(PlayerInventory inventory, String arenaName) {
        Arena arena = arenas.get(arenaName);
        arena.startingKitInventory = Bukkit.createInventory(null, InventoryType.PLAYER);
        arena.startingKitInventory.setContents(inventory.getContents());
        arena.kitBoots = inventory.getBoots();
        arena.kitChestplate = inventory.getChestplate();
        arena.kitHelmet = inventory.getHelmet();
        arena.kitLeggings = inventory.getLeggings();
    }

    public void saveAll() {
        _arenas_mutex.lock();
        try {
            for (String arenaName : arenas.keySet()) {
                Arena arena = arenas.get(arenaName);
                arena.save();
            }
        } finally {
            _arenas_mutex.unlock();
        }
    }

    public void loadAll() {
        _arenas_mutex.lock();
        try {
            arenas.clear();
            File arenaDir = new File(plugin.getDataFolder(), "arenas");
            if (arenaDir.exists()) {
                for (File file : arenaDir.listFiles()) {
                    if (file.isFile()) {
                        Arena arena = new Arena(file.getName());
                        arena.load();
                        arenas.put(arena.name, arena);
                        if (arena.enabled) {
                            arena.enabled = false;
                            setState(arena.name, true);
                        }
                    }
                }
            }
        } finally {
            _arenas_mutex.unlock();
        }
    }

    public boolean isColisseumSet(String arenaName) {
        Arena arena = arenas.get(arenaName);
        return arena.colisseumArea != null;
    }

    public boolean isInsideColisseum(CuboidSelection cs, String arenaName) {
        Arena arena = arenas.get(arenaName);
        return arena.colisseumArea.contains(cs.getMaximumPoint())
                && arena.colisseumArea.contains(cs.getMinimumPoint());
    }

    public boolean isInsideArena(Location loc, String arenaName) {
        Arena arena = arenas.get(arenaName);
        if (arena.arenaArea != null) {
            return arena.arenaArea.contains(loc);
        } else {
            return false;
        }
    }

    public boolean isInsideColisseum(Location loc, String arenaName) {
        Arena arena = arenas.get(arenaName);
        return arena.colisseumArea.contains(loc);
    }

    public void setPlayerStartPoint(String arenaName, Location loc, int playerId) {
        Arena arena = arenas.get(arenaName);
        switch (playerId) {
            case 1:
                arena.p1StartPoint = loc;
            case 2:
                arena.p2StartPoint = loc;
        }
    }

    public void addSpectatorSpawnPoint(String arenaName, Location loc) {
        Arena arena = arenas.get(arenaName);
        if (arena.spectatorPoints == null) {
            arena.spectatorPoints = new ArrayList<>();
        }
        arena.spectatorPoints.add(loc);
    }

    public void clearSpectatorSpawnPoints(String arenaName) {
        Arena arena = arenas.get(arenaName);
        arena.spectatorPoints = null;
    }

    public List<Location> getSpectatorSpawnPoints(String arenaName) {
        Arena arena = arenas.get(arenaName);
        return arena.spectatorPoints;
    }

    public void addScoreHead(Skull head, String arenaName) {

        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            if (arena.scoreHeads == null) {
                arena.scoreHeads = new TreeMap<>();
            }
            arena.scoreHeads.put(arena.scoreHeads.size(), head);
        }
    }

    public boolean delScoreHead(Skull head, String arenaName) {
        boolean result = true;
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            if (arena.scoreHeads != null && !arena.scoreHeads.isEmpty()) {
                Location lastLoc = arena.scoreHeads.get(arena.scoreHeads.size() - 1).getLocation();
                if (Tools.compareLocationBlocks(lastLoc, head.getLocation()) != 0) {
                    result = false;
                } else {
                    arena.scoreHeads.remove(arena.scoreHeads.size() - 1);
                    if (arena.scoreHeads.isEmpty()) {
                        arena.scoreHeads = null;
                    }
                }
            }
        }
        return result;
    }

    public boolean addJoinSign(String arenaName, Location loc) {
        boolean result = false;
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            if (arena.joinSigns == null) {
                arena.joinSigns = new ArrayList<>();
            }
            arena.joinSigns.add(loc);
        }
        return result;
    }

    public boolean delJoinSign(Location loc) {
        boolean result = false;
        for (Arena arena : arenas.values()) {
            if (arena.joinSigns.remove(loc)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean setMaxSpectators(String arenaName, int maxSpectators) {
        boolean result = false;
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            arena.maxPlayers = maxSpectators;
            result = true;
        }
        return result;
    }

    public int getMaxPlayers(String arenaName) {
        int result = 0;
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            result = arena.maxPlayers;
        }
        return result;
    }

    public int getPlayerCount(String arenaName) {
        int result = 0;
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            result = arena.queuePlayers.size() + arena.inGame.size();
        }
        return result;
    }

    public boolean isEnabled(String arenaName) {
        boolean result = false;
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            result = arena.enabled;
        }
        return result;
    }

    public Arena getArena(String arenaName) {
        return arenas.get(arenaName);
    }

}
