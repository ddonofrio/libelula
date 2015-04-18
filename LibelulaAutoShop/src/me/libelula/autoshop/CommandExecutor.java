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

package me.libelula.autoshop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class CommandExecutor implements org.bukkit.command.CommandExecutor {

    private final Main plugin;

    public CommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getCommand("autoshop").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        boolean result = true;
        switch (cmnd.getName()) {
            case "autoshop":
                if (!(cs instanceof Player)) {
                    cs.sendMessage(plugin.getDescription().getFullName());
                    cs.sendMessage(plugin.getDescription().getDescription());
                    cs.sendMessage(plugin.getDescription().getWebsite());
                } else if (args.length < 1) {
                    result = false;
                } else if (!args[0].equalsIgnoreCase("on")
                        && !args[0].equalsIgnoreCase("off")) {
                    result = false;
                } else {
                    Player player = (Player) cs;
                    if (args[0].equalsIgnoreCase("on")) {
                        if (plugin.isPlayerCreatingShops(player)) {
                            plugin.sendMessage(cs, "&4You are already creating shops!");
                            plugin.sendMessage(cs, "use: /autoshop off instead.");
                        } else {
                            switch (args.length){
                                case 1:
                                    plugin.addShopCreator(player, 0, 0);
                                    break;
                                case 2:
                                    plugin.addShopCreator(player, Integer.parseInt(args[1]), 0);
                                    break;
                                case 3:
                                    plugin.addShopCreator(player, 0, Float.parseFloat(args[2]));
                                    break;
                            }
                            plugin.sendMessage(cs, "Break signs with items for creating Admin Shops.");

                        }
                    } else if (args[0].equalsIgnoreCase("off") && args.length == 1) {
                        if (!plugin.isPlayerCreatingShops(player)) {
                            plugin.sendMessage(cs, "&4You are not creating shops!");
                            plugin.sendMessage(cs, "use: /autoshop on instead.");
                        } else {
                            plugin.removeShopCreator(player);
                            plugin.sendMessage(cs, "Auto Shop Creation disabled.");
                        }                        
                    }
                }
                break;
        }
        return result;
    }

}
