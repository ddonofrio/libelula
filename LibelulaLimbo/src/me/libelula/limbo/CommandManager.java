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
package me.libelula.limbo;

import java.util.TreeSet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class CommandManager implements CommandExecutor {

    private final Main plugin;
    private final TreeSet<String> allowedInGameCmds;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        allowedInGameCmds = new TreeSet<>();
    }

    public void register() {
        plugin.getCommand("lobby").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        Player player;
        if (cs instanceof Player) {
            player = (Player) cs;
        } else {
            player = null;
        }
        switch (cmnd.getName()) {
            case "lobby":
                plugin.teleportToServer(player, "lobby");
                return true;
        }
        return false;

    }
}
