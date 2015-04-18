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
/*
 *            This file is part of LibelulaLobby plugin.
 *
 *  LibelulaLobby is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibelulaLobby is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibelulaLobby. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.lobby.minigames;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class ConfigurationManager {

    private final List<CuboidSelection> areas;
    private CuboidSelection winArea;
    private Location spawnPoint;
    private String areaAnnounceIn;
    private String areaAnnounceOut;
    private String announceWin;
    private String announceWinBroadcast;
    private GameMode gamemode;
    private String configFileName;
    private Inventory kit;
    private final Plugin plugin;
    private boolean build;

    public ConfigurationManager(Plugin plugin) {
        this.areas = new ArrayList<>();
        this.plugin = plugin;
    }

    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public List<CuboidSelection> getAreas() {
        return areas;
    }

    public String getAreaAnnounceIn() {
        return areaAnnounceIn;
    }

    public String getAreaAnnounceOut() {
        return areaAnnounceOut;
    }

    public GameMode getGamemode() {
        return gamemode;
    }

    public Inventory getKit() {
        return kit;
    }

    public CuboidSelection getWinArea() {
        return winArea;
    }

    public String getAnnounceWin() {
        return announceWin;
    }

    public String getAnnounceWinBroadcast() {
        return announceWinBroadcast;
    }

    public void setKit(Inventory kit) throws IOException {
        File gameConfigFile = new File(plugin.getDataFolder(), configFileName);

        if (!gameConfigFile.exists()) {
            plugin.saveResource(configFileName, true);
        }
        FileConfiguration gameConfig = YamlConfiguration.loadConfiguration(gameConfigFile);
        saveInventory(kit, gameConfig.getConfigurationSection("kit"));
        gameConfig.save(gameConfigFile);
    }

    public final void loadConfig(String configFile) {
        this.configFileName = configFile;
        File gameConfigFile = new File(plugin.getDataFolder(), configFileName);

        if (!gameConfigFile.exists()) {
            plugin.saveResource(configFileName, true);
        }
        FileConfiguration gameConfig = YamlConfiguration.loadConfiguration(gameConfigFile);
        if (gameConfig.isSet("areas")) {
            areas.clear();
            for (String key : gameConfig.getConfigurationSection("areas").getKeys(false)) {
                Location from = getLocation(gameConfig.getConfigurationSection("areas" + "." + key + ".from"));
                Location to = getLocation(gameConfig.getConfigurationSection("areas" + "." + key + ".to"));
                areas.add(new CuboidSelection(from.getWorld(), from, to));
            }
        }
        if (gameConfig.isSet("win-area")) {
            Location from = getLocation(gameConfig.getConfigurationSection("win-area.from"));
            Location to = getLocation(gameConfig.getConfigurationSection("win-area.to"));
            winArea = new CuboidSelection(from.getWorld(), from, to);
        }

        spawnPoint = getLocation(gameConfig.getConfigurationSection("spawn"));
        areaAnnounceIn = ChatColor.translateAlternateColorCodes('&',
                gameConfig.getString("announce-in"));
        areaAnnounceOut = ChatColor.translateAlternateColorCodes('&',
                gameConfig.getString("announce-out"));
        announceWin = ChatColor.translateAlternateColorCodes('&',
                gameConfig.getString("announce-win"));
        announceWinBroadcast = ChatColor.translateAlternateColorCodes('&',
                gameConfig.getString("announce-win-broadcast"));
        gamemode = GameMode.valueOf(gameConfig.getString("game-mode"));
        kit = loadInvetory(gameConfig.getConfigurationSection("kit"));
        build = gameConfig.getBoolean("build");
    }

    private Inventory loadInvetory(ConfigurationSection config) {
        Inventory inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
        for (String position : config.getKeys(false)) {
            Material mat = Material.getMaterial(config.getString(position + ".material"));
            int amount = config.getInt(position + ".amount");
            short durability = (short) config.getInt(position + ".durability");
            ItemStack is = new ItemStack(mat, amount, durability);
            ConfigurationSection enchSect = config.getConfigurationSection(position + ".enchantment");
            if (enchSect != null) {
                for (String enchantName : enchSect.getKeys(false)) {
                    Enchantment e = Enchantment.getByName(enchantName);
                    is.addUnsafeEnchantment(e, enchSect.getInt(enchantName + ".level"));
                }
            }
            inventory.setItem(Integer.parseInt(position), is);
        }

        return inventory;
    }

    private void saveInventory(Inventory inventory, ConfigurationSection config) {
        ItemStack[] content = inventory.getContents();
        for (int i = 0; i < content.length; i++) {
            ItemStack is = content[i];
            if (is != null) {
                config.set(i + ".material", is.getType().name());
                config.set(i + ".amount", is.getAmount());
                config.set(i + ".durability", is.getDurability());
                for (Enchantment enchantment : is.getEnchantments().keySet()) {
                    config.set(i + ".enchantment." + enchantment.getName()
                            + ".level", is.getEnchantmentLevel(enchantment));
                }
            }
        }
    }

    private Location getLocation(ConfigurationSection section) {
        Location result = null;
        int x;
        int y;
        int z;
        float yaw;
        float pitch;
        String worldName = section.getString("world");
        World world = null;
        if (worldName != null) {
            world = plugin.getServer().getWorld(worldName);
        }

        if (world != null) {
            x = section.getInt("x");
            y = section.getInt("y");
            z = section.getInt("z");
            yaw = (float) section.getDouble("yaw");
            pitch = (float) section.getDouble("pitch");
            result = new Location(world, x, y, z, yaw, pitch);
        }
        return result;
    }

    private void setLocation(ConfigurationSection section, Location location) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getBlockX());
        section.set("y", location.getBlockY());
        section.set("z", location.getBlockZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

}
