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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class SignManager {

    private final Main plugin;
    String configuredFirstLine;

    public SignManager(Main plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configuredFirstLine = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("signs.first-line"));
    }

    public void checkForJoinSignCreation(final SignChangeEvent e) {
        final String firstLine = ChatColor.stripColor(e.getLine(0));
        if (ChatColor.stripColor(configuredFirstLine).equalsIgnoreCase(firstLine)) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    updateSign(e.getBlock());
                }
            }, 2);
        }
    }

    public void checkForJoin(Sign e, Player player) {
        final String firstLine = ChatColor.stripColor(e.getLine(0));
        if (ChatColor.stripColor(configuredFirstLine).equalsIgnoreCase(firstLine)) {
            plugin.gm.joinGame(player, e.getLine(1));
            updateSign(e.getBlock());
        }

    }

    public void updateSign(Block block) {
        final Sign sign = (Sign) block.getState();
        sign.setLine(0, configuredFirstLine);
        sign.setLine(2, plugin.gm.getCurrentPlayers(sign.getLine(1)) + "/"
                + plugin.gm.getMaxPlayers(sign.getLine(1)));
        sign.update();
    }

}
