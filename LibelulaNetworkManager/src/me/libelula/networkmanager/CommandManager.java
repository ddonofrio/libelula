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
package me.libelula.networkmanager;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import org.bukkit.ChatColor;
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

    public CommandManager(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getCommand("lobby").setExecutor(this);
        plugin.getCommand("universo").setExecutor(this);
        plugin.getCommand("sendallto").setExecutor(this);
        plugin.getCommand("network").setExecutor(this);
        plugin.getCommand("global").setExecutor(this);

        plugin.getCommand("ctw").setExecutor(this);
        plugin.getCommand("creative").setExecutor(this);
        plugin.getCommand("liderswag").setExecutor(this);
        plugin.getCommand("skyblock").setExecutor(this);
        plugin.getCommand("survival-one").setExecutor(this);
//        plugin.getCommand("minejump").setExecutor(this);
//        plugin.getCommand("minigames").setExecutor(this);
//        plugin.getCommand("pvp").setExecutor(this);
//        plugin.getCommand("rol").setExecutor(this);

    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        boolean ret = true;
        switch (cmnd.getName()) {
            case "universo":
                if (!plugin.getConfig().getBoolean("register-jump-cmds")) {
                    cs.sendMessage(plugin.getPrefix()
                            + ChatColor.RED + "Desde aquí no puedes usar ese comando.");
                    return true;
                }
                if (args.length == 0) {
                    plugin.sm.showServerListTo(cs);
                    return true;
                } else if (args.length != 1) {
                    return false;
                }
                plugin.sm.tryToJump(cs, args[0]);
                break;
            case "lobby":
                plugin.teleportToServer((Player) cs, "lobby-nopremium");
                plugin.teleportToServer((Player) cs, "lobby-premium");
                break;
            case "sendallto":
                if (args.length != 1) {
                    ret = false;
                } else {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        if (!player.hasPermission("lnm.prevent-moved")) {
                            plugin.teleportToServer(player, args[0]);
                        }
                    }
                }
                break;
            case "network": {
                String commandLine = "";
                for (String arg : args) {
                    commandLine = commandLine.concat(arg).concat(" ");
                }
                plugin.sm.runCommandOnNetwork(cs, commandLine);
            }

            break;
            case "global":
                if (!cs.isOp()) {
                    plugin.pm.sendSyncMessage(cs, "No puedes ejecutar este comando.");
                    return true;
                }
                if (!plugin.getConfig().getBoolean("global-cmd-allowed")) {
                    plugin.pm.sendSyncMessage(cs, "No puedes ejecutar este comando en este servidor.");
                    return true;
                }
                if (args.length != 0) {
                    String toRun = Arrays.toString(args).replace(",", "");
                    toRun = toRun.substring(1, toRun.length() - 1);
                    if (toRun.startsWith("/")) {
                        toRun = toRun.substring(1);
                    }

                    try (PrintWriter output = new PrintWriter(
                            new FileWriter(plugin.getConfig().getString("global-cmd-source"), true))) {
                        output.printf("%s\n", toRun);
                        plugin.pm.sendSyncMessage(cs, "Enviado a Global: '/" + toRun + "'", true);
                    } catch (Exception e) {
                        plugin.pm.sendSyncMessage(cs, "Error al enviar a Global: '/" + toRun + "': "
                                + e.getMessage(), true);
                    }
                } else {
                    plugin.pm.sendSyncMessage(cs, "Uso: /global [comando] <parámetros>");
                    plugin.pm.sendSyncMessage(cs, "Ejemplo: /global broadcast No olvides seguirnos en Twitter @libelulame");
                }
                break;
            case "ctw":
            case "creative":
            case "liderswag":
            case "skyblock":
            case "survival-one":
                if (!plugin.getConfig().getBoolean("register-jump-cmds")) {
                    cs.sendMessage(plugin.getPrefix()
                            + ChatColor.RED + "Desde aquí no puedes usar ese comando.");
                } else {
                    plugin.teleportToServer((Player) cs, cmnd.getName());
                }
                break;
        }

        return ret;
    }

    private void showParamErrorMsg(CommandSender cs) {
        cs.sendMessage(plugin.getPrefix() + ChatColor.RED + "Error en parámetros.");
    }

    private String help(String cmd) {
        String ret = plugin.getPrefix();
        switch (cmd) {
            case "friend":
                ret = ret + "El comando /friend acepta los siguientes parámetros:\n"
                        + "/friend <nombre> - Solicita amistad.\n"
                        + "/friend <nombre> accept - Acepta la amistad.\n"
                        + "/friend <nombre> deny - Rechaza la amistad.\n"
                        + "/friend <nombre> remove - elimina un amigo existente.";
                break;
            case "enemy":
                ret = ret + "El comando /enemy acepta los siguientes parámetros:\n"
                        + "/enemy <nombre> - añades el jugador a tu lista de enemigos.\n"
                        + "/enemy <nombre> remove - quitas el jugador a de lista de enemigos.";
                break;
            case "strike":
                ret = ret + "El comando /strike acepta los siguientes parámetros:\n"
                        + "/strike <nombre> <breve motivo de sanción> <puntos a quitar>\n"
                        + "Ejemplo: /strike <molestoman> <moletar a los demás jugadores> <2>\n";
        }
        return ret;
    }
}
