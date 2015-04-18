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
package me.libelula.eventocordones;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ArenaManager {

    private final Main plugin;
    private TreeMap<String, Arena> arenas;

    public class Arena {

        public class Team {

            private String name;
            private Location spawn;
            private CuboidSelection portal;
            private TreeSet<String> playerNames;
            private ChatColor color;
            private Inventory startingKitInventory;
            private ItemStack kitHelmet;
            private ItemStack kitChestplate;
            private ItemStack kitLeggings;
            private ItemStack kitBoots;

            private void save(ConfigurationSection cs) {
                cs.createSection(name);
                if (spawn != null) {
                    setLocation(cs.createSection(name + ".spawn"), spawn);
                }
                if (portal != null) {
                    setLocation(cs.createSection(name + ".portal.min"), portal.getMinimumPoint(), false);
                    setLocation(cs.createSection(name + ".portal.max"), portal.getMaximumPoint(), false);
                }
                if (color != null) {
                    cs.set(name + ".chat-color", color.name());
                }

                if (startingKitInventory != null) {
                    ItemStack[] content = startingKitInventory.getContents();
                    for (int i = 0; i < content.length; i++) {
                        ItemStack is = content[i];
                        if (is != null) {
                            cs.set(name + ".kit.inventory." + i + ".material", is.getType().name());
                            cs.set(name + ".kit.inventory." + i + ".amount", is.getAmount());
                            cs.set(name + ".kit.inventory." + i + ".durability", is.getDurability());
                            for (Enchantment enchantment : is.getEnchantments().keySet()) {
                                cs.set(name + ".kit.inventory." + i + ".enchantment." + enchantment.getName() + ".level", is.getEnchantmentLevel(enchantment));
                            }
                        }
                    }
                }

                if (kitBoots != null) {
                    cs.set(name + ".kit.armour.boots.material", kitBoots.getType().name());
                    cs.set(name + ".kit.armour.boots.durability", kitBoots.getDurability());
                    for (Enchantment enchantment : kitBoots.getEnchantments().keySet()) {
                        cs.set(name + ".kit.armour.boots.enchantment." + enchantment.getName() + ".level",
                                kitBoots.getEnchantmentLevel(enchantment));
                    }
                }

                if (kitLeggings != null) {
                    cs.set(name + ".kit.armour.leggings.material", kitLeggings.getType().name());
                    cs.set(name + ".kit.armour.leggings.durability", kitLeggings.getDurability());
                    for (Enchantment enchantment : kitLeggings.getEnchantments().keySet()) {
                        cs.set(name + ".kit.armour.leggings.enchantment." + enchantment.getName() + ".level",
                                kitLeggings.getEnchantmentLevel(enchantment));
                    }
                }

                if (kitChestplate != null) {
                    cs.set(name + ".kit.armour.chestplate.material", kitChestplate.getType().name());
                    cs.set(name + ".kit.armour.chestplate.durability", kitChestplate.getDurability());
                    for (Enchantment enchantment : kitChestplate.getEnchantments().keySet()) {
                        cs.set(name + ".kit.armour.chestplate.enchantment." + enchantment.getName() + ".level",
                                kitChestplate.getEnchantmentLevel(enchantment));
                    }
                }

                if (kitHelmet != null) {
                    cs.set(name + ".kit.armour.helmet.material", kitHelmet.getType().name());
                    cs.set(name + ".kit.armour.helmet.durability", kitHelmet.getDurability());
                    for (Enchantment enchantment : kitHelmet.getEnchantments().keySet()) {
                        cs.set(name + ".kit.armour.helmet.enchantment." + enchantment.getName() + ".level",
                                kitHelmet.getEnchantmentLevel(enchantment));
                    }
                }

                if (spectatorSpawn != null) {
                    setLocation(cs.createSection(name + ".spectator-spawn"), spectatorSpawn);
                }
            }

            private void getEnchantFromConfig(ConfigurationSection enchSect, ItemStack is) {
                if (enchSect != null) {
                    for (String enchantName : enchSect.getKeys(false)) {
                        Enchantment e = Enchantment.getByName(enchantName);
                        is.addUnsafeEnchantment(e, enchSect.getInt(enchantName + ".level"));
                    }
                }
            }

            private void load(ConfigurationSection cs) {
                this.name = cs.getName();
                if (cs.isSet("spawn")) {
                    this.spawn = getLocation(cs.getConfigurationSection("spawn"));
                }
                if (cs.isSet("portal")) {
                    Location min = getLocation(cs.getConfigurationSection("portal.min"), false);
                    Location max = getLocation(cs.getConfigurationSection("portal.max"), false);

                    if (max != null && min != null) {
                        this.portal = new CuboidSelection(min.getWorld(), min, max);
                    }
                }
                String colorName = cs.getString("chat-color");
                if (colorName != null) {
                    this.color = ChatColor.valueOf(colorName);
                }

                ConfigurationSection itemsSection = cs.getConfigurationSection("kit.inventory");
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

                itemsSection = cs.getConfigurationSection("kit.armour");
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
                
                if (cs.isSet("spectator-spawn")) {
                    setSpectatorSpawn(getLocation(cs.getConfigurationSection("spectator-spawn")));
                }
            }

            public ItemStack getKitBoots() {
                return kitBoots;
            }

            public ItemStack getKitChestplate() {
                return kitChestplate;
            }

            public ItemStack getKitHelmet() {
                return kitHelmet;
            }

            public ItemStack getKitLeggings() {
                return kitLeggings;
            }

            public Inventory getStartingKitInventory() {
                return startingKitInventory;
            }

            public void setKitBoots(ItemStack kitBoots) {
                this.kitBoots = kitBoots;
            }

            public void setKitChestplate(ItemStack kitChestplate) {
                this.kitChestplate = kitChestplate;
            }

            public void setKitHelmet(ItemStack kitHelmet) {
                this.kitHelmet = kitHelmet;
            }

            public void setKitLeggings(ItemStack kitLeggings) {
                this.kitLeggings = kitLeggings;
            }

            public void setStartingKitInventory(Inventory startingKitInventory) {
                this.startingKitInventory = startingKitInventory;
            }

            public void setColor(ChatColor color) {
                this.color = color;
            }

            public ChatColor getColor() {
                return color;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public Location getSpawn() {
                return spawn;
            }

            public void setSpawn(Location spawn) {
                this.spawn = spawn;
            }

            public CuboidSelection getPortal() {
                return portal;
            }

            public void setPortal(CuboidSelection portal) {
                this.portal = portal;
            }

            public boolean hasPlayer(Player player) {
                return playerNames.contains(player.getName());
            }

            public int getPlayerCount() {
                if (playerNames != null) {
                    return playerNames.size();
                } else {
                    return 0;
                }
            }

            public void addPlayer(Player player) {
                if (playerNames == null) {
                    playerNames = new TreeSet<>();
                }
                playerNames.add(player.getName());
            }

            public void removePlayer(Player player) {
                playerNames.remove(player.getName());
            }

        }

        private String name;
        private Location spawn;
        private int minPlayers;
        private int maxPlayers;
        private TreeMap<String, Team> teams;
        private Location spectatorSpawn;
        private boolean inGame;

        public void setSpectatorSpawn(Location spectatorSpawn) {
            this.spectatorSpawn = spectatorSpawn;
        }

        public Location getSpectatorSpawn() {
            return spectatorSpawn;
        }

        public Team getNewTeam() {
            return new Team();
        }

        public String getName() {
            return name;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public int getMinPlayers() {
            return minPlayers;
        }

        public Location getSpawn() {
            return spawn;
        }

        public Collection<Team> getTeams() {
            return teams.values();
        }

        public Team getTeam(String name) {
            return teams.get(name);
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }

        public void setMinPlayers(int minPlayers) {
            this.minPlayers = minPlayers;
        }

        public void setSpawn(Location spawn) {
            this.spawn = spawn;
        }

        public void setInGame(boolean inGame) {
            this.inGame = inGame;
        }

        public boolean isInGame() {
            return inGame;
        }

        public void save() throws IOException {
            YamlConfiguration arenaConfig = new YamlConfiguration();
            if (spawn != null) {
                setLocation(arenaConfig.createSection("spawn"), spawn);
            }
            arenaConfig.set("min-players", minPlayers);
            arenaConfig.set("max-players", maxPlayers);
            if (teams != null) {
                ConfigurationSection teamConfig = arenaConfig.createSection("teams");
                for (Team team : teams.values()) {
                    team.save(teamConfig);
                }
            }
            File arenasDir = new File(plugin.getDataFolder(), "arenas");
            arenasDir.mkdirs();
            File arenaFile = new File(arenasDir, name.concat(".yml"));
            arenaConfig.save(arenaFile);
        }

        public void load(File arenaFile) throws IOException,
                FileNotFoundException, InvalidConfigurationException {
            YamlConfiguration arenaConfig = new YamlConfiguration();
            arenaConfig.load(arenaFile);

            this.name = arenaFile.getName().substring(0, arenaFile.getName().indexOf("."));
            this.minPlayers = arenaConfig.getInt("min-players");
            this.maxPlayers = arenaConfig.getInt("max-players");
            if (arenaConfig.isSet("spawn")) {
                this.spawn = getLocation(arenaConfig.getConfigurationSection("spawn"));
            }
            ConfigurationSection teamConfig = arenaConfig.getConfigurationSection("teams");
            if (teamConfig != null) {
                this.teams = new TreeMap<>();
                for (String teamName : teamConfig.getKeys(false)) {
                    Team team = new Team();
                    team.load(teamConfig.getConfigurationSection(teamName));
                    teams.put(teamName, team);
                }
            }
        }
    }

    public ArenaManager(Main plugin) {
        this.plugin = plugin;
    }

    public void load() throws IOException, FileNotFoundException, InvalidConfigurationException {
        arenas = new TreeMap<>();
        File arenaDir = new File(plugin.getDataFolder(), "arenas");
        if (arenaDir.exists()) {
            for (File file : arenaDir.listFiles()) {
                if (file.isFile()) {
                    Arena arena = new Arena();
                    arena.load(file);
                    arenas.put(arena.name, arena);
                }
            }
        }
    }

    public void save() throws IOException {
        for (Arena arena : arenas.values()) {
            arena.save();
        }
    }

    private void setLocation(ConfigurationSection yaml, Location loc) {
        setLocation(yaml, loc, true);
    }

    private void setLocation(ConfigurationSection yaml, Location loc, boolean isPlayerLocation) {
        yaml.set("world", loc.getWorld().getName());
        if (isPlayerLocation) {
            yaml.set("x", loc.getX());
            yaml.set("y", loc.getY());
            yaml.set("z", loc.getZ());
            yaml.set("yaw", loc.getYaw());
            yaml.set("pitch", loc.getPitch());
        } else {
            yaml.set("x", loc.getBlockX());
            yaml.set("y", loc.getBlockY());
            yaml.set("z", loc.getBlockZ());
        }
    }

    private Location getLocation(ConfigurationSection yaml) {
        return getLocation(yaml, true);
    }

    private Location getLocation(ConfigurationSection yaml, boolean isPlayerLocation) {
        if (yaml == null) {
            return null;
        }
        Location ret = null;
        String worldName = yaml.getString("world");
        if (worldName != null) {
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                if (isPlayerLocation) {
                    ret = new Location(world,
                            yaml.getDouble("x"),
                            yaml.getDouble("y"),
                            yaml.getDouble("z"),
                            (float) yaml.getDouble("yaw"),
                            (float) yaml.getDouble("pitch"));
                } else {
                    ret = new Location(world,
                            yaml.getInt("x"),
                            yaml.getInt("y"),
                            yaml.getInt("z"));
                }
            }
        }
        return ret;
    }

    public void addArena(Location loc) {
        Arena arena = new Arena();
        arena.setName(loc.getWorld().getName());
        arena.setSpawn(loc);
        arenas.put(loc.getWorld().getName(), arena);
    }

    public boolean removeArena(World world) {
        return arenas.remove(world.getName()) != null;
    }

    public boolean isArena(World world) {
        return arenas.containsKey(world.getName());
    }

    public Arena.Team addTeam(World world, String teamName, ChatColor color) {
        Arena.Team team = null;
        Arena arena = arenas.get(world.getName());
        if (arena != null) {
            team = arena.getNewTeam();
            team.setName(teamName);
            team.setColor(color);
            if (arena.teams == null) {
                arena.teams = new TreeMap<>();
            }
            arena.teams.put(teamName, team);
        }
        return team;
    }

    public boolean removeTeam(World world, String teamName) {
        boolean ret = false;
        Arena arena = arenas.get(world.getName());
        if (arena != null) {
            if (arena.teams != null) {
                ret = arena.teams.remove(teamName) != null;
            }
        }
        return ret;
    }

    public Arena.Team getTeam(World world, String teamName) {
        Arena.Team team = null;
        Arena arena = arenas.get(world.getName());
        if (arena != null) {
            team = arena.teams.get(teamName);
        }
        return team;
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public void setMinPlayers(World world, int playerCount) {
        Arena arena = arenas.get(world.getName());
        if (arena != null) {
            arena.setMinPlayers(playerCount);
        }
    }

    public void setMaxPlayers(World world, int playerCount) {
        Arena arena = arenas.get(world.getName());
        if (arena != null) {
            arena.setMaxPlayers(playerCount);
        }
    }

}
