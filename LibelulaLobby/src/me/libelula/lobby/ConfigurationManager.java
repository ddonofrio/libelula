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
package me.libelula.lobby;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class ConfigurationManager {

    private class MiniGameConfig {


    }

    private final FileConfiguration config;
    private final Main plugin;
    private String pluginPrefix;
    private URL xmlrpcURI;
    private int xmlrpcMsTimeOut;
    private boolean premium;
    private boolean allowPremiumOnNonpremium;
    private String premiumKickMessage;
    private boolean frontServer;
    private int tarpittingMs;
    private boolean tarpittingActive;
    private String xmlrpcUser;
    private String xmlrpcPassword;
    private boolean debugMode;
    private String nonPremiumUnregisteredMsg;
    private String lobbyServerName;
    private int tarpittingPenaltySeconds;
    private String tarpittingPenaltyMessage;
    private ArrayList<Location> spawnPoints;
    private int playerMaxAllowedHeight;
    private int playerFlyBlocksFromZero;
    private int msBetweenChat;
    private Location zeroPoint;
    private final List<CuboidSelection> silenceAreas;
    private final List<CuboidSelection> interactAreas;
    private final List<CuboidSelection> editionAreas;
    private final TreeSet<String> opPlayers;
    private boolean altmenu;

    public ConfigurationManager(Main plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        plugin.saveDefaultConfig();
        silenceAreas = new ArrayList<>();
        interactAreas = new ArrayList<>();
        editionAreas = new ArrayList<>();
        opPlayers = new TreeSet<>();
        loadConfig();
    }

    public void saveConfig() {
        try {
            saveSpawns();
            saveAreas();
        } catch (IOException ex) {
            Logger.getLogger(ConfigurationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        plugin.saveConfig();
    }

    public final void loadConfig() {
        this.pluginPrefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix"));

        try {
            this.xmlrpcURI = new URL(config.getString("xmlrpc.uri"));
        } catch (MalformedURLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Malformed xmlrpc.uri: {0}", ex.getMessage());
        }

        this.xmlrpcMsTimeOut = config.getInt("xmlrpc.timeout-ms", 3000);

        this.premium = !config.getBoolean("non-premium.active", false);
        this.allowPremiumOnNonpremium = config.getBoolean("non-premium.allow-premium", false);
        this.premiumKickMessage = config.getString("non-premium.premium-kick-message");
        this.frontServer = config.getBoolean("front-server.active", false);
        this.tarpittingMs = config.getInt("front-server.tarpitting.time-ms");
        this.tarpittingActive = config.getBoolean("front-server.tarpitting.active");
        this.xmlrpcUser = config.getString("xmlrpc.user");
        this.xmlrpcPassword = config.getString("xmlrpc.password");
        this.debugMode = config.getBoolean("debug");
        this.nonPremiumUnregisteredMsg = config.getString("non-premium.unregistered-kick-message");
        this.lobbyServerName = config.getString("front-server.lobby-server");
        this.tarpittingPenaltySeconds = config.getInt("front-server.tarpitting.penalty-seconds");
        this.tarpittingPenaltyMessage = config.getString("front-server.tarpitting.penalty-message");
        this.spawnPoints = loadSpawns();
        this.zeroPoint = getLocation(config.getConfigurationSection("map.zero-point"));
        this.playerMaxAllowedHeight = config.getInt("player-limits.max-height");
        this.playerFlyBlocksFromZero = config.getInt("player-limits.fly-blocks");
        this.msBetweenChat = config.getInt("player-limits.ms-between-chat");
        this.altmenu = config.getBoolean("altmenu", false);
        this.opPlayers.addAll(config.getStringList("op-players"));
        loadAreas();

        if (debugMode) {
            plugin.logInfo("Configuration loaded.");
            plugin.logInfo("xmlrpc.timeout-ms: " + xmlrpcMsTimeOut);
            plugin.logInfo("non-premium.active: " + !premium);
            plugin.logInfo("non-premium.allow-premium: " + allowPremiumOnNonpremium);
            plugin.logInfo("non-premium.premium-kick-message: " + premiumKickMessage);
            plugin.logInfo("front-server.active: " + frontServer);
            plugin.logInfo("front-server.tarpitting.time-ms: " + tarpittingMs);
            plugin.logInfo("front-server.tarpitting.active: " + tarpittingActive);
            plugin.logInfo("xmlrpc.user: " + xmlrpcUser);
            plugin.logInfo("xmlrpc.password: " + (xmlrpcPassword == null ? "Not set!" : "<Hidden>"));
            plugin.logInfo("debug: " + debugMode);
            plugin.logInfo("non-premium.unregistered-kick-message: " + nonPremiumUnregisteredMsg);
            plugin.logInfo("front-server.lobby-server: " + lobbyServerName);
            plugin.logInfo("front-server.tarpitting.penalty-seconds: " + tarpittingPenaltySeconds);
            plugin.logInfo("front-server.tarpitting.penalty-message: " + tarpittingPenaltyMessage);
            plugin.logInfo("spawnPoints: " + spawnPoints);
            plugin.logInfo("map.zero-point: " + zeroPoint);
            plugin.logInfo("player-limits.max-height: " + playerMaxAllowedHeight);
            plugin.logInfo("player-limits.fly-blocks: " + playerFlyBlocksFromZero);
            plugin.logInfo("player-limits.ms-between-chat: " + msBetweenChat);
            plugin.logInfo("alt-menu: " + altmenu);
            plugin.logInfo("op-players: " + opPlayers);

            silenceAreas.stream().forEach((area) -> {
                plugin.logInfo("silence area: " + area.getMinimumPoint() + " -> " + area.getMaximumPoint());
            });
            interactAreas.stream().forEach((area) -> {
                plugin.logInfo("interact area: " + area.getMinimumPoint() + " -> " + area.getMaximumPoint());
            });
            editionAreas.stream().forEach((area) -> {
                plugin.logInfo("edition area: " + area.getMinimumPoint() + " -> " + area.getMaximumPoint());
            });
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

    private void saveSpawns() throws IOException {
        YamlConfiguration spawns = new YamlConfiguration();
        int spawnId = 0;
        for (Location spawn : spawnPoints) {
            spawns.set(spawnId + ".world", "dummy");
            setLocation(spawns.getConfigurationSection("" + spawnId), spawn);
            spawnId++;
        }
        File spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        spawns.save(spawnFile);
    }

    private ArrayList<Location> loadSpawns() {
        ArrayList<Location> spawns = new ArrayList<>();
        File spawnFile = new File(plugin.getDataFolder(), "spawn.yml");

        if (!spawnFile.exists()) {
            plugin.saveResource("spawn.yml", true);
        }

        FileConfiguration spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);

        for (String key : spawnConfig.getKeys(false)) {
            String worldName = spawnConfig.getString(key + ".world");
            if (worldName == null) {
                continue;
            }
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.logErr("Invalid world configured @spawn.yml[" + key + "]: " + worldName);
                continue;
            }
            spawns.add(getLocation(spawnConfig.getConfigurationSection(key)));
        }

        if (spawns.isEmpty()) {
            plugin.logWarn("No spawn point defined!");
            Location defaulSpawnPoint
                    = new Location(plugin.getServer().getWorlds().get(0), 0, 0, 0);
            spawns.add(defaulSpawnPoint);
        }
        return spawns;
    }

    public String getPluginPrefix() {
        return pluginPrefix;
    }

    public URL getXmlrpcURI() {
        return xmlrpcURI;
    }

    public int getXmlrpcMsTimeOut() {
        return xmlrpcMsTimeOut;
    }

    public boolean isPremium() {
        return premium;
    }

    public boolean isAllowedPremiumOnNonpremium() {
        return allowPremiumOnNonpremium;
    }

    public String getPremiumKickMessage() {
        return premiumKickMessage;
    }

    public boolean isAllowPremiumOnNonpremium() {
        return allowPremiumOnNonpremium;
    }

    public boolean isFrontServer() {
        return frontServer;
    }

    public int getTarpittingMs() {
        return tarpittingMs;
    }

    public boolean isTarpittingActive() {
        return tarpittingActive;
    }

    public String getXmlrpcPassword() {
        return xmlrpcPassword;
    }

    public String getXmlrpcUser() {
        return xmlrpcUser;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public String getNonPremiumUnregisteredMsg() {
        return nonPremiumUnregisteredMsg;
    }

    public String getLobbyServerName() {
        return lobbyServerName;
    }

    public int getTarpittingPenaltySeconds() {
        return tarpittingPenaltySeconds;
    }

    public String getTarpittingPenaltyMessage() {
        return tarpittingPenaltyMessage;
    }

    public ArrayList<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public int getPlayerFlyBlocksFromZero() {
        return playerFlyBlocksFromZero;
    }

    public int getPlayerMaxAllowedHeight() {
        return playerMaxAllowedHeight;
    }

    public Location getZeroPoint() {
        return zeroPoint;
    }

    public List<String> getBookPages(String book) {
        List<String> bookPages = new ArrayList<>();

        for (String page : plugin.getConfig().getConfigurationSection(book + "-book.pages").getKeys(false)) {
            try {
                Integer.parseInt(page);
            } catch (NumberFormatException ex) {
                continue;
            }
            String textPage = "";
            for (String line : plugin.getConfig().getConfigurationSection(book + "-book.pages." + page).getKeys(false)) {
                if (line != null) {
                    String text = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(book + "-book.pages." + page + "." + line));
                    textPage = textPage.concat(text).concat("\n");
                }
            }
            bookPages.add(textPage);
        }
        return bookPages;
    }

    public void addSpawnPoints(Location spawn) {
        this.spawnPoints.add(spawn);
    }

    public void setZeroPoint(Location zeroPoint) {
        this.zeroPoint = zeroPoint;
        setLocation(config.getConfigurationSection("map.zero-point"), zeroPoint);
    }

    private void loadAreas() {

        File areasFile = new File(plugin.getDataFolder(), "areas.yml");

        if (!areasFile.exists()) {
            plugin.saveResource("areas.yml", true);
        }

        FileConfiguration areasConfig = YamlConfiguration.loadConfiguration(areasFile);

        if (areasConfig.isSet("silence")) {
            silenceAreas.clear();
            for (String key : areasConfig.getConfigurationSection("silence").getKeys(false)) {
                Location from = getLocation(areasConfig.getConfigurationSection("silence" + "." + key + ".from"));
                Location to = getLocation(areasConfig.getConfigurationSection("silence" + "." + key + ".to"));
                silenceAreas.add(new CuboidSelection(from.getWorld(), from, to));
            }
        }

        if (areasConfig.isSet("interact")) {
            interactAreas.clear();
            areasConfig.getConfigurationSection("interact").getKeys(false).stream().forEach((key) -> {
                Location from = getLocation(areasConfig.getConfigurationSection("interact" + "." + key + ".from"));
                Location to = getLocation(areasConfig.getConfigurationSection("interact" + "." + key + ".to"));
                interactAreas.add(new CuboidSelection(from.getWorld(), from, to));
            });
        }

        if (areasConfig.isSet("edition")) {
            editionAreas.clear();
            areasConfig.getConfigurationSection("edition").getKeys(false).stream().forEach((key) -> {
                Location from = getLocation(areasConfig.getConfigurationSection("edition" + "." + key + ".from"));
                Location to = getLocation(areasConfig.getConfigurationSection("edition" + "." + key + ".to"));
                editionAreas.add(new CuboidSelection(from.getWorld(), from, to));
            });
        }

    }

    private void saveAreas() throws IOException {
        YamlConfiguration areas = new YamlConfiguration();
        int areaId = 0;
        for (CuboidSelection area : silenceAreas) {
            areas.set("silence." + areaId + ".from.world", "dummy");
            areas.set("silence." + areaId + ".to.world", "dummy");
            setLocation(areas.getConfigurationSection("silence." + areaId + ".from"), area.getMinimumPoint());
            setLocation(areas.getConfigurationSection("silence." + areaId + ".to"), area.getMaximumPoint());
            areaId++;
        }
        areaId = 0;
        for (CuboidSelection area : interactAreas) {
            areas.set("interact." + areaId + ".from.world", "dummy");
            areas.set("interact." + areaId + ".to.world", "dummy");
            setLocation(areas.getConfigurationSection("interact." + areaId + ".from"), area.getMinimumPoint());
            setLocation(areas.getConfigurationSection("interact." + areaId + ".to"), area.getMaximumPoint());
            areaId++;
        }
        areaId = 0;
        for (CuboidSelection area : editionAreas) {
            areas.set("edition." + areaId + ".from.world", "dummy");
            areas.set("edition." + areaId + ".to.world", "dummy");
            setLocation(areas.getConfigurationSection("edition." + areaId + ".from"), area.getMinimumPoint());
            setLocation(areas.getConfigurationSection("edition." + areaId + ".to"), area.getMaximumPoint());
            areaId++;
        }
        File areasFile = new File(plugin.getDataFolder(), "areas.yml");
        areas.save(areasFile);

    }

    public void addSilencedArea(CuboidSelection area) {
        silenceAreas.add(area);
    }

    public void addInteractArea(CuboidSelection area) {
        interactAreas.add(area);
    }

    public void addEditionArea(CuboidSelection area) {
        editionAreas.add(area);
    }

    public List<CuboidSelection> getSilencedAreas() {
        return silenceAreas;
    }

    public List<CuboidSelection> getEditionAreas() {
        return editionAreas;
    }

    public List<CuboidSelection> getInteractionAreas() {
        return interactAreas;
    }

    public int getMsBetweenChat() {
        return msBetweenChat;
    }

    public boolean isAltmenu() {
        return altmenu;
    }

    public boolean isOpAllowed(Player player) {
        return opPlayers.contains(player.getName().toLowerCase());
    }
}
