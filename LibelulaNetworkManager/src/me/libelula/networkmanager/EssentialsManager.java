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
 *            This file is part of LibelulaNetworkManager plugin.
 *
 *  LibelulaNetworkManager is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibelulaNetworkManager is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibelulaNetworkManager. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package me.libelula.networkmanager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class EssentialsManager {
    private final Main plugin;

    public EssentialsManager(Main plugin) {
        this.plugin = plugin;
    }
    
    public String getBanReason(String playerName) {
        String banReason = null;
        YamlConfiguration userData = getUserData(playerName);
        if (userData != null) {
            banReason = ChatColor.stripColor(userData.getString("ban.reason"));
        }
        return banReason;
    }
    
    private YamlConfiguration getUserData(String playerName) {
        YamlConfiguration yc = null;
        String filePath = "./plugins/Essentials/userdata/" + playerName.toLowerCase() +".yml";
        File configFile = new File(filePath);
        if (configFile.exists()) {
            yc = new YamlConfiguration();
            try {
                yc.load(configFile);
            } catch (IOException | InvalidConfigurationException ex) {
                Logger.getLogger(EssentialsManager.class.getName()).log(Level.SEVERE, null, ex);
            }           
        }
        return yc;
    }
    
}
