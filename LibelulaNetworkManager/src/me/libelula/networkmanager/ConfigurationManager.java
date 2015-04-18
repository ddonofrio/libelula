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
package me.libelula.networkmanager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ConfigurationManager {

    private final Main plugin;
    private final YamlConfiguration config;
    private final TreeMap<String, Integer> permissionChatDelayMs;
    private final TreeMap<String, Integer> permissionTeleportDelayMs;
    private final TreeMap<String, Integer> permissionMaxFriends;
    private final TreeMap<String, Integer> permissionMaxEnemies;
    private final TreeMap<Integer, String> reputationCommand;
    private final TreeMap<Integer, String> reputationPermission;
    private URL xmlrpcURI;
    private int xmlrpcMsTimeOut;
    private String xmlrpcUser;
    private String xmlrpcPassword;
    private boolean debug;

    public ConfigurationManager(Main plugin) {
        this.plugin = plugin;
        permissionChatDelayMs = new TreeMap<>();
        permissionTeleportDelayMs = new TreeMap<>();
        permissionMaxEnemies = new TreeMap<>();
        permissionMaxFriends = new TreeMap<>();
        reputationCommand = new TreeMap<>();
        reputationPermission = new TreeMap<>();
        config = new YamlConfiguration();
    }

    public void init() {
        File defaultConfig = new File(plugin.getDataFolder(), "config.yml");
        if (!defaultConfig.exists()) {
            plugin.saveDefaultConfig();
        }
        try {
            config.load(defaultConfig);
        } catch (IOException | InvalidConfigurationException ex) {
            Logger.getLogger(ConfigurationManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        debug = plugin.getConfig().getBoolean("debug");
        
        ConfigurationSection chatDelay = config.getConfigurationSection("permissions.delays.chat");
        for (String key : chatDelay.getKeys(true)) {
            permissionChatDelayMs.put(key, chatDelay.getInt(key));
        }

        ConfigurationSection teleportDelay = config.getConfigurationSection("permissions.delays.teleport");
        for (String key : teleportDelay.getKeys(true)) {
            permissionTeleportDelayMs.put(key, teleportDelay.getInt(key));
        }

        ConfigurationSection maxFriends = config.getConfigurationSection("permissions.friends");
        for (String key : maxFriends.getKeys(true)) {
            permissionMaxFriends.put(key, maxFriends.getInt(key));
            if (debug) {
                plugin.getLogger().info("DEBUG (f):" + key + "=" + maxFriends.getInt(key));
            }
        }

        ConfigurationSection maxEnemies = config.getConfigurationSection("permissions.enemies");
        for (String key : maxEnemies.getKeys(true)) {
            permissionMaxEnemies.put(key, maxEnemies.getInt(key));
            if (debug) {
                plugin.getLogger().info("DEBUG (e):" + key + "=" + maxEnemies.getInt(key));
            }
        }

        ConfigurationSection promotions = config.getConfigurationSection("permissions.promotions");
        for (String key : promotions.getKeys(true)) {
            Integer points = promotions.getInt(key + ".reputation");
            String command = promotions.getString(key + ".command");
            if (points != 0 && command != null) {
                reputationCommand.put(points, command);
                reputationPermission.put(points, key);
            }
        }
        
        try {
            this.xmlrpcURI = new URL(config.getString("xmlrpc.uri"));
        } catch (MalformedURLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Malformed xmlrpc.uri: {0}", ex.getMessage());
        }

        this.xmlrpcMsTimeOut = config.getInt("xmlrpc.timeout-ms", 3000);
        this.xmlrpcUser = config.getString("xmlrpc.user");
        this.xmlrpcPassword = config.getString("xmlrpc.password");
    }

    public int getChatDelay(Player player) {
        int delay = 0;
        if (!player.hasPermission("lnm.chat-cmds-no-delay")) {
            for (String permission : permissionChatDelayMs.keySet()) {
                if (player.hasPermission(permission)) {
                    delay = permissionChatDelayMs.get(permission);
                    break;
                }
            }
        }
        return delay;
    }

    public int getTeleportDelay(Player player) {
        int delay = 0;
        if (!player.hasPermission("lnm.teleport-no-delay")) {
            for (String permission : permissionTeleportDelayMs.keySet()) {
                if (player.hasPermission(permission)) {
                    delay = permissionTeleportDelayMs.get(permission);
                    break;
                }
            }
        }
        return delay;
    }

    public int getMaxFriendsAllowed(Player player) {
        int limit = -1;
        if (!player.hasPermission("lnm.friendship.unlimited")) {
            for (String permission : permissionMaxFriends.keySet()) {
                if (player.hasPermission(permission)) {
                    limit = permissionMaxFriends.get(permission);
                    break;
                }
            }
        }
        return limit;
    }

    public int getMaxEnemiesAllowed(Player player) {
        int limit = -1;
        if (!player.hasPermission("lnm.enmity.unlimited")) {
            for (String permission : permissionMaxEnemies.keySet()) {
                if (player.hasPermission(permission)) {
                    limit = permissionMaxEnemies.get(permission);
                    break;
                }
            }
        }
        return limit;
    }

    public String getPromotionCommand(int reputation) {
        String ret = null;
        Integer value = 0;
        Integer next = 0;
        for (Iterator<Integer> it = reputationCommand.keySet().iterator(); it.hasNext();) {
            if (it.hasNext()) {
                next = it.next();
            }
            if ((reputation >= value && reputation < next)) {
                ret = reputationCommand.get(value);
                break;
            }
            value = next;
        }
        if (ret == null) {
            if (reputation >= next) {
                ret = reputationCommand .get(value);
            }
        }
        return ret;
    }

    public String getPromotionPermission(int reputation) {
        String ret = null;
        Integer value = 0;
        Integer next = 0;
        for (Iterator<Integer> it = reputationPermission.keySet().iterator(); it.hasNext();) {
            if (it.hasNext()) {
                next = it.next();
            }
            if ((reputation >= value && reputation < next)) {
                ret = reputationPermission.get(value);
                break;
            }
            value = next;
        }
        if (ret == null) {
            if (reputation >= next) {
                ret = reputationPermission.get(value);
            }
        }
        return ret;
    }
    
    public URL getXmlrpcURI() {
        return xmlrpcURI;
    }

    public int getXmlrpcMsTimeOut() {
        return xmlrpcMsTimeOut;
    }

    public String getXmlrpcPassword() {
        return xmlrpcPassword;
    }

    public String getXmlrpcUser() {
        return xmlrpcUser;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
