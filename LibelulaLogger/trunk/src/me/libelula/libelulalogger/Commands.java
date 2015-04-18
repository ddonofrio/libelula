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
package me.libelula.libelulalogger;

import com.sk89q.worldedit.bukkit.selections.Selection;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Class Commands of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Commands implements CommandExecutor {

    private final LibelulaLogger plugin;

    public Commands(LibelulaLogger plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {

        plugin.getCommand("libelulalogger").setExecutor(this);
        plugin.getCommand("whomadethis").setExecutor(this);
        plugin.getCommand("/whoeditedthisarea").setExecutor(this);
        plugin.getCommand("whoeditedthisarea").setExecutor(this);
        plugin.getCommand("blockrestore").setExecutor(this);
        plugin.getCommand("undoedited").setExecutor(this);
        plugin.getCommand("/undoedited").setExecutor(this);
        plugin.getCommand("redoedited").setExecutor(this);
        plugin.getCommand("/redoedited").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {

        if (!(sender instanceof Player)) {
            if (!command.getName().equals("libelulalogger")) {
                sender.sendMessage("This command must be used by a player in game.");
                return true;
            }
        }

        switch (command.getName()) {
            case "libelulalogger":
                if (args.length == 0) {
                    sender.sendMessage(plugin.getPluginFullDescription());
                    sender.sendMessage("Posible commands are:");
                    sender.sendMessage(ChatColor.GREEN + "/ll config" + ChatColor.RESET + " - List configuration keys.");
                    sender.sendMessage(ChatColor.GREEN + "/whomadethis" + ChatColor.RESET + " - Activates the block by block Discovery Tool.");
                    sender.sendMessage(ChatColor.GREEN + "/blockrestore " + ChatColor.RESET + " - Activates the block by block restore tool.");
                    sender.sendMessage(ChatColor.GREEN + "/whoeditedthisarea [radius]" + ChatColor.RESET + " - Shows who edited current area.");
                    sender.sendMessage(ChatColor.GREEN + "//whoeditedthisarea " + ChatColor.RESET + " - The same but with a WE selection.");
                    sender.sendMessage(ChatColor.GREEN + "/undoedited [playername] [radius]" + ChatColor.RESET + " - Undo given player editions from current area.");
                    sender.sendMessage(ChatColor.GREEN + "//undoedited [playername]" + ChatColor.RESET + " - The same but with a WE selection.");
                    sender.sendMessage(ChatColor.GREEN + "/redoedited [playername] [radius]" + ChatColor.RESET + " - Redo given player editions from current area.");
                    sender.sendMessage(ChatColor.GREEN + "//redoedited [playername]" + ChatColor.RESET + " - The same but with a WE selection.");
                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
                    sender.sendMessage(plugin.getPluginFullDescription());
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    case "config":
                        processLibelulaConfigCommand(sender, args);
                        return true;
                    case "reload":
                        if (args.length == 1) {
                            plugin.config.reload();
                            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
                            return true;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "whomadethis":
                if (args.length == 0) {
                    plugin.toolbox.giveDiscoveryTool((Player) sender);
                    return true;
                }
                break;
            case "/whoeditedthisarea":
                return processWhoEditedThisAreaCmd(args, sender);
            case "whoeditedthisarea":
                return processWhoEditedThisAreaByRadCmd(args, sender);
            case "blockrestore":
                if (args.length == 0) {
                    if (sender instanceof Player) {
                        plugin.toolbox.giveRestoreTool((Player) sender);
                    } else {
                        sender.sendMessage("This command must be used by a player in game.");
                    }
                    return true;
                }
                break;
            case "undoedited":
                return processEditedByRadiusCmd(args, sender, true);
            case "/undoedited":
                return processEditedCmd(args, sender, true);
            case "redoedited":
                return processEditedByRadiusCmd(args, sender, false);
            case "/redoedited":
                return processEditedCmd(args, sender, false);
            default:
                throw new NotImplementedException();
        }
        return false;
    }

    void processLibelulaConfigCommand(CommandSender sender, String[] args) {
        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {

            for (String configLine : plugin.config.toString().split("\\|")) {
                sender.sendMessage(ChatColor.GREEN + configLine);
            }
            return;
        }

        switch (args[1].toLowerCase()) {
            case "reload":
                if (args.length == 2) {
                    plugin.config.reload();
                    return;
                }
            case "set":
                if (args.length > 3) {
                    String value = args[3];
                    for (int i = 4; i < args.length; i++) {
                        value = value.concat(",").concat(args[i]);
                    }
                    plugin.config.setValue(args[2], value, sender);
                    return;
                }
            case "del":
                if (args.length == 3) {
                    plugin.config.delValue(args[2], sender);
                    return;
                }
        }
        sender.sendMessage(ChatColor.RED + "Incorrect usage of the config command.");
    }

    private boolean validateArea(int area, CommandSender sender) {
        if (area <= 4000000) {
            sender.sendMessage(ChatColor.AQUA + "Quering for " + area + " selected blocks...");
        } else {
            sender.sendMessage(ChatColor.RED + "You tried to query for " + area + " selected blocks...");
            sender.sendMessage(ChatColor.RED + "Current maximum max amount is configured to 4 millons, please perform multiple queries.");
            return false;
        }
        if (area > 500000) {
            sender.sendMessage(ChatColor.AQUA + "This may take some time, please be patience...");
        }
        return true;
    }

    private Selection getWESelection(CommandSender sender) {
        if (plugin.we == null) {
            sender.sendMessage(ChatColor.RED + "This command must be used with WordlEdit and it is not installed.");
            sender.sendMessage(ChatColor.RED + "Instead you can use the radius version of this command by typing it with a single slash.");
            return null;
        }

        Selection selection = plugin.we.getSelection((Player) sender);
        if (selection == null) {
            sender.sendMessage(ChatColor.RED + "Make a region selection first.");
        }

        return selection;
    }

    boolean processWhoEditedThisAreaByRadCmd(String[] args, CommandSender sender) {
        int radius;
        int area;
        if (args.length != 1) {
            return false;
        }

        try {
            radius = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "The radius must be a positive integer number.");
            return true;
        }
        if (radius < 1) {
            sender.sendMessage(ChatColor.RED + "The value of the radius must be 1 at least.");
        }
        area = (int) Math.pow(radius, 3);

        if (!validateArea(area, sender)) {
            return true;
        }

        Player player = (Player) sender;
        plugin.meode.asyncQuerySellection(player.getLocation(), radius, player, null);

        return true;
    }

    boolean processWhoEditedThisAreaCmd(String[] args, CommandSender sender) {
        if (args.length == 0) {

            Selection selection = getWESelection(sender);

            if (selection == null) {
                return true;
            } else {

                if (!validateArea(selection.getArea(), sender)) {
                    return true;
                }

                plugin.meode.asyncQuerySellection(selection.getMinimumPoint(),
                        selection.getMaximumPoint(), (Player) sender, null);
                return true;
            }
        }
        return false;
    }

    boolean processEditedCmd(String[] args, CommandSender sender, boolean undo) {
        if (args.length != 1) {
            return false;
        }

        String playerName = args[0];

        if (!plugin.getServer().getOfflinePlayer(playerName).hasPlayedBefore()) {
            if (plugin.getServer().getPlayer(playerName) == null) {
                sender.sendMessage(ChatColor.RED + "The player \"".concat(playerName).concat("\" never played on this server."));
                return true;
            }
        }

        Selection selection = getWESelection(sender);
        if (selection == null) {
            return true;
        }

        if (!validateArea(selection.getArea(), sender)) {
            return true;
        }

        plugin.meode.asyncEditSellection(selection.getMinimumPoint(),
                selection.getMaximumPoint(), (Player) sender, playerName, undo);

        return true;
    }

    private boolean processEditedByRadiusCmd(String[] args, CommandSender sender, boolean undo) {
        if (args.length < 2 || args.length > 3) {
            return false;
        }

        String playerName = args[0];

        if (!plugin.getServer().getOfflinePlayer(playerName).hasPlayedBefore()) {
            if (plugin.getServer().getPlayer(playerName) == null) {
                sender.sendMessage(ChatColor.RED + "The player \"".concat(playerName).concat("\" never played on this server."));
                return true;
            }
        }

        int radius;
        try {
            radius = Integer.parseInt(args[1]);
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Use a integer positive number for radius.");
            return true;
        }

        if (radius < 1) {
            sender.sendMessage(ChatColor.RED + "You have to specify a radius or use the double slashed command with worldedit selections.");
            return true;
        }

        int area = (int) Math.pow(radius, 3);

        if (!validateArea(area, sender)) {
            return true;
        }

        Player player = (Player) sender;
        plugin.meode.asyncEditSellection(player.getLocation(), radius, (Player) sender, playerName, undo);

        return true;
    }
}
