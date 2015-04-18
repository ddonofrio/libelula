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
package me.libelula.climber;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Commands implements CommandExecutor {

    private final Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String dummy, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        switch (command.getName()) {
            case "lcl":
                switch (args.length) {
                    case 1:
                        plugin.gameControler.addPlayer(player, args[0]);
                        break;
                    case 2:
                        plugin.gameControler.addPlayer(player, args[0], args[1]);
                        break;
                    case 3: {
                        plugin.gameControler.addPlayer(player, args[0], args[1]);
                    }
                }
                break;
            case "lclsetup":
                if (player == null) {
                    sender.sendMessage(plugin.getText("NONPLAYER_INGAME"));
                    return false;
                }

                if (args.length == 1 && args[0].equals("setlobby")) {
                    plugin.mapMan.setLobby(player.getLocation());
                    sender.sendMessage(plugin.getText("LOBBY_SET"));
                    sender.sendMessage(plugin.getText("CREATE_ARENA"));
                    return true;
                }

                if (args.length < 2) {
                    if (args.length == 1) {
                        switch (args[0]) {
                            case "signs":
                                plugin.sbm.setScoreboardStart(player);
                                sender.sendMessage(ChatColor.GREEN + "Golpea todos los carteles en órden.");
                                sender.sendMessage(ChatColor.GOLD + "Y luego /lclsetup signfinish");
                                return true;
                            case "signsfinish":
                                plugin.sbm.setScoreboardFinish();
                                sender.sendMessage(ChatColor.GOLD + "listo.");
                                return true;
                            case "startsigns":
                                plugin.gameControler.setSignsStart(player);
                                sender.sendMessage(ChatColor.GREEN + "Golpea todos los carteles en órden.");
                                sender.sendMessage(ChatColor.GOLD + "Y luego /lclsetup startsignfinish");
                                return true;
                            case "startsignsfinish":
                                plugin.gameControler.setSignsFinish();
                                sender.sendMessage(ChatColor.GOLD + "listo.");
                                return true;
                        }
                    }
                    sender.sendMessage(ChatColor.RED + "¡Número incorrecto de parámetros!");
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    case "create":
                        if (plugin.mapMan.create(args[1]) == MapManager.result.ALLREADY_EXISTS) {
                            sender.sendMessage(ChatColor.RED + "Esa arena ya existe.");
                        } else {
                            plugin.mapMan.setWorld(args[1], player.getWorld());
                            sender.sendMessage(ChatColor.GREEN + "Arena " + args[1]
                                    + " creada, ahora crea un área de worldguard que la proteja y escribe "
                                    + ChatColor.GOLD + "/lclsetup setbounds <nombre de la área> <nombre de la región>");
                        }
                        break;
                    case "setbounds":
                        if (args.length != 3) {
                            sender.sendMessage(ChatColor.RED + "¡Número incorrecto de parámetros!");
                            return true;
                        }
                        switch (plugin.mapMan.setProtectedRegion(args[1], args[2])) {
                            case DONT_EXISTS:
                                sender.sendMessage(ChatColor.RED + "Esa arena no existe");
                                return true;
                            case REGION_DONT_EXISTS:
                                sender.sendMessage(ChatColor.RED + "Esa area de WorldGuard no existe");
                                return true;
                            case OK:
                                sender.sendMessage(ChatColor.GREEN + "Arena " + args[1]
                                        + " creada y sus límites configurados, ahora ponte sobre el punto de captura y escribe:"
                                        + ChatColor.GOLD + "/lclsetup setcapture <nombre de la arena>");
                                break;
                            default:
                                sender.sendMessage(ChatColor.RED + "Error inesperado");
                                return true;
                        }
                        break;
                    case "setcapture":
                        switch (plugin.mapMan.setCapture(args[1], player.getLocation())) {
                            case WORLD_MISSSMATCH:
                                sender.sendMessage(ChatColor.RED + "El punto de captura no puede estar en un mundo diferente al que se creó la arena.");
                                return true;
                            case DONT_EXISTS:
                                sender.sendMessage(ChatColor.RED + "Esa arena no existe");
                            case OK:
                                sender.sendMessage(ChatColor.GREEN + "Arena " + args[1]
                                        + " creada y límites y punto de captura configurados, ahora ponte sobre el punto de aparición de un equipo y escribe:"
                                        + ChatColor.GOLD + "/lclsetup setspawn <nombre del equipo>");
                                break;
                            default:
                                sender.sendMessage(ChatColor.RED + "Error inesperado");
                                return true;

                        }
                        break;
                    case "setspawn":
                        switch (plugin.mapMan.setSpawn(args[1], player.getLocation())) {
                            case TEAM_DONT_EXISTS:
                                sender.sendMessage(ChatColor.RED + "Ese equipo no existe.");
                                sender.sendMessage(ChatColor.GOLD + "Los posibles equipos son: " + plugin.teamMan.toString());
                                return true;
                            case NOT_IN_ARENA:
                                sender.sendMessage(ChatColor.RED + "El punto dónde estás no está dentro de una arena.");
                                return true;
                            case OK:
                                sender.sendMessage(ChatColor.GREEN + "Spawn " + args[1] + " configurado.");
                                sender.sendMessage(ChatColor.GOLD + "Continua haciendo esto hasta configurar todos los puntos."
                                        + " Al menos 2 equipos, luego escribe:" + "/lclsetup block <nombre de la área>");
                                break;
                            default:
                                sender.sendMessage(ChatColor.RED + "Error inesperado");
                                return true;
                        }
                        break;
                    case "block":
                        if (args.length < 2) {
                            sender.sendMessage(ChatColor.RED + "¡Número incorrecto de parámetros!");
                        }

                        if (plugin.mapMan.setBlockingBlocks(args[1], player) == MapManager.result.DONT_EXISTS) {
                            sender.sendMessage(ChatColor.RED + "Esa arena no existe");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Golpea todos los bloques que se deban quitar cuando de comienzo cada ronda.");
                        sender.sendMessage(ChatColor.GOLD + "Y luego /lclsetup finish <nombre de la área>");
                        break;
                    case "finish":
                        switch (plugin.mapMan.finish(args[1], player)) {
                            case DONT_EXISTS:
                                sender.sendMessage(ChatColor.RED + "Esa arena no existe");
                                return true;
                            case NOT_SETTING_UP:
                                sender.sendMessage(ChatColor.RED + "finish solo se usa después de /lclsetup block");
                                return true;
                            case OK:
                                sender.sendMessage(ChatColor.GOLD + "Arena creada con éxito.");
                                break;
                            default:
                                sender.sendMessage(ChatColor.RED + "Error inesperado");
                                return true;
                        }
                        break;
                    case "enable":
                        switch (plugin.mapMan.enable(args[1])) {
                            case LOBBY_NOT_SET:
                                sender.sendMessage(ChatColor.RED + "No se ha especificado el lobby del juego.");
                                return true;
                            case DONT_EXISTS:
                                sender.sendMessage(ChatColor.RED + "Esa arena no existe");
                                return true;
                            case UNCONFIG_TEAM:
                                sender.sendMessage(ChatColor.RED + "No se han configurado los equipos.");
                                return true;
                            case ONLY_ONE_TEAM:
                                sender.sendMessage(ChatColor.RED + "Como mínimo debe haber 2 equipos.");
                                return true;
                            case UNCONFIG_AREA:
                                sender.sendMessage(ChatColor.RED + "El área de la arena no está configurado.");
                                return true;
                            case UNCONFIG_CAPPOINT:
                                sender.sendMessage(ChatColor.RED + "no se ha configurado el punto de captura.");
                                return true;
                            case OK:
                                sender.sendMessage(ChatColor.GOLD + "Arena activada y esperando jugadores.");
                                break;
                            default:
                                sender.sendMessage(ChatColor.RED + "Error inesperado");
                                return true;
                        }
                        break;
                    case "disable":
                        switch (plugin.mapMan.disable(args[1])) {
                            case DONT_EXISTS:
                                sender.sendMessage(ChatColor.RED + "Esa arena no existe");
                                return true;
                            case NOT_ENABLED:
                                sender.sendMessage(ChatColor.RED + "Esa arena no estaba habilitada");
                                return true;
                            case OK:
                                sender.sendMessage(ChatColor.GOLD + "Arena desactivada.");
                                break;
                            default:
                                sender.sendMessage(ChatColor.RED + "Error inesperado");
                                return true;
                        }
                        break;
                }
        }

        return true;
    }

}
