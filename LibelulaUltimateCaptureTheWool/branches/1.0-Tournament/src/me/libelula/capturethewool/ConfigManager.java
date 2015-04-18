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
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 * 
 */

public class ConfigManager {

    private final Main plugin;
    private final FileConfiguration config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        plugin.saveDefaultConfig();
        load(false);
    }

    public void load() {
        load(true);
    }
    
    public void persists() {
        plugin.saveConfig();
    }

    private void load(boolean reload) {
        if (reload) {
            plugin.reloadConfig();
        }
        
        validateSignText(getSignFirstLine(), "signs.first-line-text", "ctw");
                validateSignText(getSignFirstLineReplacement(), "signs.first-line-text-replacement",
                "&1LIBELULA&4CTW");
        validateSignText(getTextForInvalidRooms(), "signs.on-invalid-room-replacement",
                "&4INVALID ROOM");
        validateSignText(getTextForInvalidMaps(), "signs.on-invalid-map-replacement",
                "&4INVALID MAP");
        File defaultMapFile = new File(plugin.getDataFolder(), "defaultmap.yml");
        if (!defaultMapFile.exists()) {
            plugin.saveResource("defaultmap.yml", false);
        }
    }

    private void validateSignText(String text, String key, String defaultValue) {
        if (text.length() < 1 || text.length() > 16) {
            plugin.getLogger().warning("Config value \"".concat(key).concat("\" is incorrect."));
            config.set(key, defaultValue);
            plugin.getLogger().info("Config value \"".concat(key).concat("\" has been changed to \"")
                    .concat(defaultValue).concat("\"."));
        }

    }

    public String getSignFirstLine() {
        return config.getString("signs.first-line-text");
    }

    public String getSignFirstLineReplacement() {
        return config.getString("signs.first-line-text-replacement");
    }
    
    public String getTextForInvalidRooms() {
        return config.getString("signs.on-invalid-room-replacement");
    }

    public String getTextForInvalidMaps() {
        return config.getString("signs.on-invalid-map-replacement");
    }
    
    public String getTextForDisabledMaps() {
        return config.getString("signs.on-disabled-map");
    }
    
    public boolean implementSpawnCmd() {
        return config.getBoolean("implement-spawn-cmd", false);
    }
    
    public boolean isTournament() {
        return plugin.getConfig().getBoolean("tournament-mode");
    }

}
