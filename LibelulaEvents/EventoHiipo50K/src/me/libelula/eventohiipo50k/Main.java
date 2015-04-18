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

package me.libelula.eventohiipo50k;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Main extends JavaPlugin {
    
    String winner;
    private final Listener listener;

    public Main() {
        winner = null;
        listener = new Listener(this);
    }

    @Override
    public void onEnable() {
        listener.register();
    }
    
    public void broadcast(String message) {
        String text = ChatColor.BLUE + "["
            + ChatColor.GOLD + "Libelula"+ChatColor.BLUE + "]"
            + ChatColor.YELLOW + " " + message;
        for (Player player : getServer().getOnlinePlayers()) {
            player.sendMessage(text);
        }
    }

    
}
