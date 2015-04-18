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

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class CommandManager implements CommandExecutor {

    private final Main plugin;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getCommand("arena").setExecutor(this);
        plugin.getCommand("team").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        Player player = (Player) cs;
        boolean ret = false;
        switch (cmnd.getName()) {
            case "arena":
                if (args.length == 1) {
                    ret = true;
                    switch (args[0].toLowerCase()) {
                        case "add":
                            plugin.am.addArena(player.getLocation());
                            player.sendMessage(ChatColor.GREEN + "Arena " + player.getWorld().getName() + " añadida.");
                            break;
                        case "save":
                            try {
                                plugin.am.save();
                                player.sendMessage(ChatColor.GREEN + "Configuración guardada.");
                            } catch (IOException ex) {
                                Logger.getLogger(CommandManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;
                        case "load":
                            try {
                                plugin.am.load();
                                player.sendMessage(ChatColor.GREEN + "Configuración recargada.");
                            } catch (IOException | InvalidConfigurationException ex) {
                                Logger.getLogger(CommandManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;
                        case "remove":
                            if (plugin.am.removeArena(player.getWorld())) {
                                player.sendMessage(ChatColor.GREEN + "Arena eliminada.");
                            } else {
                                player.sendMessage(ChatColor.RED + "Este mundo no es una arena.");
                            }
                            break;
                        case "spectator":
                            ArenaManager.Arena arena = plugin.am.getArena(player.getWorld().getName());
                            if (arena != null) {
                                arena.setSpectatorSpawn(player.getLocation());
                                player.sendMessage(ChatColor.GREEN + "Spawn de espectadores añadido.");
                            } else {
                                player.sendMessage(ChatColor.RED + "Este mundo no es una arena.");
                            }
                            break;
                        default:
                            ret = false;
                            break;
                    }
                } else if (args.length == 2) {
                    ret = true;
                    switch (args[0].toLowerCase()) {
                        case "min-players":
                            if (plugin.am.isArena(player.getWorld())) {
                                plugin.am.setMinPlayers(player.getWorld(), Integer.parseInt(args[1]));
                                player.sendMessage(ChatColor.GREEN + "Número mínimo de jugadores configrado a " + args[1]);
                            } else {
                                player.sendMessage(ChatColor.RED + "Este mundo no es una arena.");
                            }
                            break;
                        case "max-players":
                            if (plugin.am.isArena(player.getWorld())) {
                                plugin.am.setMaxPlayers(player.getWorld(), Integer.parseInt(args[1]));
                                player.sendMessage(ChatColor.GREEN + "Número máximo de jugadores configrado a " + args[1]);
                            } else {
                                player.sendMessage(ChatColor.RED + "Este mundo no es una arena.");
                            }
                            break;
                        default:
                            ret = false;
                            break;
                    }
                }
                break;
            case "team":
                if (plugin.am.isArena(player.getWorld())) {

                    if (args.length >= 1) {
                        ret = true;
                        switch (args[0].toLowerCase()) {
                            case "add":
                                if (args.length == 3) {
                                    if (plugin.am.addTeam(player.getWorld(), args[1],
                                            ChatColor.valueOf(args[2])) != null) {
                                        player.sendMessage(ChatColor.GREEN + "Equipo " + ChatColor.valueOf(args[2])
                                                + args[1] + ChatColor.GREEN + " añadido.");
                                    }
                                } else {
                                    ret = false;
                                }
                                break;
                            case "setportal":
                                if (args.length == 2) {
                                    ArenaManager.Arena.Team team = plugin.am.getTeam(player.getWorld(), args[1]);
                                    if (team != null) {
                                        Selection sel = plugin.we.getSelection(player);
                                        CuboidSelection cuboidSelection = new CuboidSelection(sel.getWorld(),
                                                sel.getMinimumPoint().subtract(1, 1, 1),
                                                sel.getMaximumPoint().add(1, 1, 1));
                                        team.setPortal(cuboidSelection);
                                        player.sendMessage(ChatColor.GREEN + "Portal añadido.");
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Team no encontrado.");
                                    }
                                } else {
                                    ret = false;
                                }
                                break;

                            case "setspawn":
                                if (args.length == 2) {
                                    ArenaManager.Arena.Team team = plugin.am.getTeam(player.getWorld(), args[1]);
                                    if (team != null) {
                                        team.setSpawn(player.getLocation());
                                        player.sendMessage(ChatColor.GREEN + "Punto de aparición añadido para el equipo " + team.getColor() + team.getName());
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Team no encontrado.");
                                    }
                                } else {
                                    ret = false;
                                }
                                break;

                            case "setkit":
                                if (args.length == 2) {
                                    ArenaManager.Arena.Team team = plugin.am.getTeam(player.getWorld(), args[1]);
                                    if (team != null) {
                                        team.setKitBoots(player.getInventory().getBoots());
                                        team.setKitChestplate(player.getInventory().getChestplate());
                                        team.setKitHelmet(player.getInventory().getHelmet());
                                        team.setKitLeggings(player.getInventory().getLeggings());
                                        team.setStartingKitInventory(player.getInventory());
                                        player.sendMessage(ChatColor.GREEN + "Kit inicial añadido al equipo " + team.getColor() + team.getName());
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Team no encontrado.");
                                    }
                                } else {
                                    ret = false;
                                }
                                break;
                            default:
                                ret = false;
                                break;
                        }

                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Este mundo no es una arena.");
                }
        }
        return ret;
    }

}
