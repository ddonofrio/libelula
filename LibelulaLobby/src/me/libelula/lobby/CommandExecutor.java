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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class CommandExecutor implements org.bukkit.command.CommandExecutor {

    private final Main plugin;

    public CommandExecutor(Main plugin) {
        this.plugin = plugin;
        register();
    }

    private void register() {
        plugin.getCommand("addspawnpoint").setExecutor(this);
        plugin.getCommand("changepassword").setExecutor(this);
        plugin.getCommand("definearea").setExecutor(this);
        plugin.getCommand("help").setExecutor(this);
        plugin.getCommand("lobby-save").setExecutor(this);
        plugin.getCommand("lobby").setExecutor(this);
        plugin.getCommand("login").setExecutor(this);
        plugin.getCommand("logout").setExecutor(this);
        plugin.getCommand("register").setExecutor(this);
        plugin.getCommand("setzeropoint").setExecutor(this);
        plugin.getCommand("spawn").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String dummy, String[] args) {
        Player player = null;
        if (cs instanceof Player) {
            player = (Player) cs;
        }
        switch (cmnd.getName()) {
            case "addspawnpoint":
                if (cs.hasPermission("lobby.admin") && player != null) {
                    plugin.cm.addSpawnPoints(player.getLocation());
                }
                break;
            case "changepassword":
                plugin.sendMessage(cs, "Para cambiar tu contraseña debes hacerlo desde la web: http://www.libelula.me/?page_id=187&module=settings");
                break;
            case "definearea":
                if (player != null) {
                    CuboidSelection cuboidSelection = (CuboidSelection) plugin.we.getSelection(player);
                    if (cuboidSelection == null) {
                        plugin.sendMessage(cs, "&4¡El área está vacía!");
                    } else {
                        if (args.length == 1) {
                            switch (args[0]) {
                                case "silence":
                                    plugin.cm.addSilencedArea(cuboidSelection);
                                    break;
                                case "interact":
                                    plugin.cm.addInteractArea(cuboidSelection);
                                    break;
                                case "edition":
                                    plugin.cm.addEditionArea(cuboidSelection);
                                    break;
                                default:
                                    plugin.sendMessage(cs, "&4Nombre de area incorrecto.");
                                    plugin.sendMessage(cs, "Uso: /defineArea [silence|interact|edition]");
                                    
                            }
                        } else {
                            plugin.sendMessage(cs, "&4Error en parámetros.");
                            plugin.sendMessage(cs, "Uso: /defineArea [silence|interact|edition]");

                        }
                    }
                }
                break;
            case "help":
                if (player != null) {
                    if (!plugin.pm.isLogged(player)) {
                        plugin.sendMessage(cs, "&4Debes escribir /login y tu contraseña");
                        plugin.sendMessage(cs, "Ejemplo: /login libélula");
                    } else {
                        plugin.sendMessage(cs, "Utiliza la brújula para seleccionar dónde jugar.");
                        plugin.sendMessage(cs, "O bien explora este maravilloso lobby con tus amigos.");
                    }
                }
                break;
            case "lobby-save":
                if (cs.hasPermission("lobby.admin")) {
                    plugin.cm.saveConfig();
                    plugin.sendMessage(cs, "All configuration were saved successfully.");
                }
                break;
            case "lobby":
                plugin.sendMessage(cs, "Esto es el lobby ¿que esperabas?");
                plugin.sendMessage(cs, "Para ir al centro del lobby solo escribe /spawn");
                break;
            case "login":
                if (player != null) {
                    if (plugin.pm.isLogged(player)) {
                        plugin.sendMessage(cs, "&4Ya habías ingresado, no hace falta que uses login de nuevo.");
                    } else {
                        if (args.length != 1) {
                            plugin.sendMessage(cs, "&4Debes escribir /login y tu contraseña");
                            plugin.sendMessage(cs, "Ejemplo: /login libélula");
                        } else {
                            if (plugin.pm.requestLogIn(player, args[0])) {
                                plugin.sendMessage(cs, "Verificando tus credenciales...");
                            }
                        }
                    }
                }
                break;
            case "logout":
                if (player != null) {
                    if (!plugin.pm.isLogged(player)) {
                        plugin.sendMessage(cs, "&4Debes escribir /login y tu contraseña");
                        plugin.sendMessage(cs, "Ejemplo: /login libélula");
                    } else {
                        plugin.pm.removePlayerFromList(player);
                        player.kickPlayer("¡Hasta pronto colega!");
                    }
                }
                break;
            case "register":
                plugin.sendMessage(cs, "&4El registro on-line está desactivado para evitar jugadores molestos.");
                plugin.sendMessage(cs, "Si realmente quieres jugar en esta comunidad deberás ingresar en http://libelula.me y registrarte ahí.");
                plugin.sendMessage(cs, "Solo podrás jugar 24 horas después de tu registro, así que ¡date prisa!");
                break;
            case "setzeropoint":
                if (player != null) {
                    plugin.cm.setZeroPoint(player.getLocation());
                    player.getLocation().getWorld()
                            .setSpawnLocation(player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ());
                } else {
                    plugin.cm.setZeroPoint(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                }
                plugin.sendMessage(cs, "Punto zero fijado.");
                break;
            case "spawn":
                plugin.pm.spawn(player);
                break;
        }
        return true;
    }

}
