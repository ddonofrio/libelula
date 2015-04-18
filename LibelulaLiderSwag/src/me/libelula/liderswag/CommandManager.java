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
package me.libelula.liderswag;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
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
        register();
    }

    private void register() {
        plugin.saveResource("plugin.yml", true);
        File file = new File(plugin.getDataFolder(), "plugin.yml");
        YamlConfiguration pluginYml = new YamlConfiguration();
        try {
            pluginYml.load(file);
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe(ex.toString());
            plugin.getPluginLoader().disablePlugin(plugin);
            return;
        }
        file.delete();
        for (String commandName : pluginYml.getConfigurationSection("commands").getKeys(false)) {
            plugin.getCommand(commandName).setExecutor(this);
            allowedInGameCmds.add(commandName);
        }
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        Player player;
        if (cs instanceof Player) {
            player = (Player) cs;
        } else {
            player = null;
        }

        switch (cmnd.getName()) {
            case "llsleave":
                if (player != null) {
                    if (plugin.gm.isPlayerInArena(player)) {
                        plugin.gm.removePlayer(player);
                    } else {
                        cs.sendMessage(plugin.lm.getText("not-in-arena-cmd"));
                    }
                } else {
                    cs.sendMessage(plugin.lm.getText("not-in-game-cmd"));
                }
                break;
            case "llssetup":
                if (player == null) {
                    cs.sendMessage(plugin.lm.getText("not-in-game-cmd"));
                } else {
                    if (args.length > 0) {
                        switch (args[0]) {
                            case "arena":
                                if (args.length > 1) {
                                    switch (args[1]) {
                                        case "add":
                                            processArenaAddCmd(cs, args);
                                            break;
                                        case "config":
                                            processArenaConfigCmd(cs, args);
                                            break;
                                        case "del":
                                            processArenaDelCmd(cs, args);
                                            break;
                                        case "list":
                                            processArenaListCmd(cs, args);
                                            break;
                                        case "enable":
                                            processArenaEnableCmd(cs, args);
                                            break;
                                        case "disable":
                                            processArenaDisableCmd(cs, args);
                                            break;
                                        default:
                                            plugin.lm.sendTexts(cs, "commands.llssetup-arena");
                                    }
                                } else {
                                    plugin.lm.sendTexts(cs, "commands.llssetup-arena");
                                }
                                break;
                            case "player":
                                if (args.length > 1) {
                                    switch (args[1]) {
                                        case "kit":
                                            processPlayerKitCmd(player, args);
                                            break;
                                        case "pos1":
                                            processPlayerPosCmd(player, 1);
                                            break;
                                        case "pos2":
                                            processPlayerPosCmd(player, 2);
                                            break;
                                        case "spect":
                                            processPlayerSpectCmd(player, args);
                                            break;
                                        default:
                                            plugin.lm.sendTexts(cs, "commands.llssetup-player");
                                    }
                                } else {
                                    plugin.lm.sendTexts(cs, "commands.llssetup-player");
                                }
                                break;
                            case "score":
                                if (args.length > 1) {
                                    switch (args[1]) {
                                        case "head":
                                        case "heads":
                                            processScoreHeadsCmd(player, args);
                                            break;
                                        case "sign":
                                        case "signs":
                                            processScoreSignsCmd(player, args);
                                            break;
                                        case "finish":
                                            processScoreFinish(player, args);
                                            break;
                                        default:
                                            plugin.lm.sendTexts(cs, "commands.llssetup-score");
                                    }
                                } else {
                                    plugin.lm.sendTexts(cs, "commands.llssetup-score");
                                }
                                break;
                            case "lobby":
                                if (args.length == 1) {
                                    plugin.lobby = player.getLocation();
                                    plugin.getConfig().set("lobby.location.world", player.getLocation().getWorld().getName());
                                    plugin.getConfig().set("lobby.location.x", player.getLocation().getX());
                                    plugin.getConfig().set("lobby.location.y", player.getLocation().getY());
                                    plugin.getConfig().set("lobby.location.z", player.getLocation().getZ());
                                    plugin.getConfig().set("lobby.location.yaw", player.getLocation().getYaw());
                                    plugin.getConfig().set("lobby.location.pitch", player.getLocation().getPitch());
                                    plugin.saveConfig();
                                    cs.sendMessage(plugin.lm.getText("cmd-success"));
                                } else {
                                    plugin.lm.sendTexts(cs, "commands.llssetup-lobby");
                                }
                                break;
                            default:
                                plugin.lm.sendTexts(cs, "commands.llssetup");
                        }
                    } else {
                        plugin.lm.sendTexts(cs, "commands.llssetup");
                    }
                }
                break;
        }

        return true;
    }

    private void processArenaAddCmd(CommandSender cs, String[] args) {
        if (args.length != 3) {
            plugin.lm.sendTexts(cs, "commands.llssetup-arena-add");
        } else {
            String arenaName = args[2];
            if (arenaName.length() > 16) {
                cs.sendMessage(plugin.lm.getText("invalid-arena-name"));
            } else {
                if (plugin.am.exists(arenaName)) {
                    cs.sendMessage(plugin.lm.getText("arena-already-exists"));
                } else {
                    plugin.am.add(arenaName);
                    cs.sendMessage(plugin.lm.getText("cmd-success"));
                }
            }
        }
    }

    private void processArenaConfigCmd(CommandSender cs, String[] args) {
        if (args.length != 4) {
            plugin.lm.sendTexts(cs, "commands.llssetup-arena-config");
        } else {
            String arenaName = args[3];
            switch (args[2]) {
                case "colisseum":
                    if (!plugin.am.exists(arenaName)) {
                        cs.sendMessage(plugin.lm.getText("arena-dont-exists"));
                    } else {
                        Selection sel = plugin.we.getSelection((Player) cs);
                        if (sel == null) {
                            cs.sendMessage(plugin.lm.getText("area-not-selected"));
                        } else {
                            plugin.am.setColisseumArea((CuboidSelection) sel, arenaName);
                            cs.sendMessage(plugin.lm.getText("cmd-success"));
                        }
                    }
                    break;
                case "area":
                    if (!plugin.am.exists(arenaName)) {
                        cs.sendMessage(plugin.lm.getText("arena-dont-exists"));
                    } else {
                        Selection sel = plugin.we.getSelection((Player) cs);
                        if (sel == null) {
                            cs.sendMessage(plugin.lm.getText("area-not-selected"));
                        } else {
                            if (plugin.am.isColisseumSet(arenaName)) {
                                if (plugin.am.isInsideColisseum((CuboidSelection) sel, arenaName)) {
                                    plugin.am.setArenaArea((CuboidSelection) sel, arenaName);
                                    cs.sendMessage(plugin.lm.getText("cmd-success"));
                                } else {
                                    cs.sendMessage(plugin.lm.getText("arena-not-in-colisseum"));
                                }
                            } else {
                                cs.sendMessage(plugin.lm.getText("colisseum-must-be-set"));
                            }
                        }
                    }
                    break;
                default:
                    plugin.lm.sendTexts(cs, "commands.llssetup-arena-config");
                    break;
            }
        }

    }

    private void processArenaDelCmd(CommandSender cs, String[] args) {
        if (args.length != 3) {
            plugin.lm.sendTexts(cs, "commands.llssetup-arena-del");
        } else {
            String arenaName = args[2];
            if (!plugin.am.exists(arenaName)) {
                cs.sendMessage(plugin.lm.getText("arena-dont-exists"));
            } else {
                plugin.am.del(arenaName);
                cs.sendMessage(plugin.lm.getText("cmd-success"));
            }
        }
    }

    private void processArenaListCmd(CommandSender cs, String[] args) {
        if (args.length != 2) {
            plugin.lm.sendTexts(cs, "commands.llssetup-arena-list");
        } else {
            TreeMap<String, Boolean> arenas = plugin.am.getList();
            if (arenas.isEmpty()) {
                cs.sendMessage(plugin.lm.getText("no-arenas"));
            } else {
                cs.sendMessage(plugin.lm.getText("arena-list"));
                for (String arena : arenas.keySet()) {
                    String text = "* " + arena + ": ";
                    boolean enabled = arenas.get(arena);
                    if (enabled) {
                        text = text + ChatColor.GREEN + "[" + plugin.lm.getTranslatedText("enabled") + "]";
                    } else {
                        text = text + ChatColor.RED + "[" + plugin.lm.getTranslatedText("disabled") + "]";
                    }
                    cs.sendMessage(text);
                }
            }
        }
    }

    private void processArenaEnableCmd(CommandSender cs, String[] args) {
        if (args.length != 3) {
            plugin.lm.sendTexts(cs, "commands.llssetup-arena-enable");
        } else {
            String arenaName = args[2];
            if (!plugin.am.exists(arenaName)) {
                cs.sendMessage(plugin.lm.getText("arena-dont-exists"));
            } else {
                switch (plugin.am.checkConfiguration(arenaName)) {
                    case COLISSEUM_AREA:
                        cs.sendMessage(plugin.lm.getText("misscfg-colisseum-area"));
                        break;
                    case ARENA_AREA:
                        cs.sendMessage(plugin.lm.getText("misscfg-arena-area"));
                        break;
                    case KIT:
                        cs.sendMessage(plugin.lm.getText("misscfg-player-kit"));
                        break;
                    case P1_LOC:
                        cs.sendMessage(plugin.lm.getText("misscfg-p1-loc"));
                        break;
                    case P2_LOC:
                        cs.sendMessage(plugin.lm.getText("misscfg-p2-loc"));
                        break;
                    case SP_LOC:
                        cs.sendMessage(plugin.lm.getText("misscfg-spec-loc"));
                        break;
                    case NOTHING:
                        plugin.am.setState(arenaName, true);
                        cs.sendMessage(plugin.lm.getText("cmd-success"));
                        plugin.sgm.updateJoinSigns(arenaName);
                        break;
                }
            }
        }
    }

    private void processArenaDisableCmd(CommandSender cs, String[] args) {
        if (args.length != 3) {
            plugin.lm.sendTexts(cs, "commands.llssetup-arena-disable");
        } else {
            String arenaName = args[2];
            if (!plugin.am.exists(arenaName)) {
                cs.sendMessage(plugin.lm.getText("arena-dont-exists"));
            } else {
                plugin.am.setState(arenaName, false);
                plugin.sgm.updateJoinSigns(arenaName);
                cs.sendMessage(plugin.lm.getText("cmd-success"));
            }
        }
    }

    private void processPlayerKitCmd(Player player, String[] args) {
        String arenaName = plugin.am.getArenaName(player.getLocation());
        if (arenaName != null) {
            plugin.am.setStratingKit(player.getInventory(), arenaName);
            player.sendMessage(plugin.lm.getText("cmd-success"));
        } else {
            player.sendMessage(plugin.lm.getText("not-inside-arena-region"));
        }
    }

    private void processPlayerPosCmd(Player player, int PlayerId) {
        String arenaName = plugin.am.getArenaName(player.getLocation());
        if (arenaName != null) {
            if (plugin.am.isInsideArena(player.getLocation(), arenaName)) {
                plugin.am.setPlayerStartPoint(arenaName, player.getLocation(), PlayerId);
                player.sendMessage(plugin.lm.getText("cmd-success"));
            } else {
                player.sendMessage(plugin.lm.getText("spawn-points-not-in-arena"));
            }
        } else {
            player.sendMessage(plugin.lm.getText("not-inside-arena-region"));
        }
    }

    private void processPlayerSpectCmd(Player player, String[] args) {
        if (args.length != 3) {
            plugin.lm.sendTexts(player, "commands.llssetup-player-spect");
        } else {
            String arenaName = plugin.am.getArenaName(player.getLocation());
            switch (args[2]) {
                case "add":
                    if (arenaName != null) {
                        if (!plugin.am.isInsideArena(player.getLocation(), arenaName)
                                && plugin.am.isInsideColisseum(player.getLocation(), arenaName)) {
                            plugin.am.addSpectatorSpawnPoint(arenaName, player.getLocation());
                            player.sendMessage(plugin.lm.getText("cmd-success"));
                        } else {
                            player.sendMessage(plugin.lm.getText("not-inside-colisseum-region"));
                        }
                    } else {
                        player.sendMessage(plugin.lm.getText("not-inside-arena-region"));
                    }
                    break;
                case "clear":
                    if (arenaName != null) {
                        plugin.am.clearSpectatorSpawnPoints(arenaName);
                        player.sendMessage(plugin.lm.getText("cmd-success"));
                    } else {
                        player.sendMessage(plugin.lm.getText("not-inside-arena-region"));
                    }
                    break;
                case "list":
                    if (arenaName != null) {
                        List<Location> spawns = plugin.am.getSpectatorSpawnPoints(arenaName);
                        if (spawns == null || spawns.isEmpty()) {
                            player.sendMessage(plugin.lm.getText("no-spect-spawn-points"));
                        } else {
                            player.sendMessage(plugin.lm.getText("spect-spawn-points"));
                            for (Location loc : spawns) {
                                player.sendMessage("* " + loc.toString());
                            }
                        }
                    } else {
                        player.sendMessage(plugin.lm.getText("not-inside-arena-region"));
                    }
                    break;
                default:
                    plugin.lm.sendTexts(player, "commands.llssetup-player-spect");
            }
        }
    }

    private void processScoreHeadsCmd(Player player, String[] args) {
        if (args.length != 3) {
            plugin.lm.sendTexts(player, "commands.llssetup-score-heads");
        } else {
            String arenaName = args[2];
            if (plugin.am.exists(arenaName)) {
                player.sendMessage(plugin.lm.getText("heads-setup"));
                player.sendMessage(plugin.lm.getText("listen-setup-finish"));
                plugin.em.addSetUpListerners(player, EventManager.setupEvents.HEADS, arenaName);
            } else {
                player.sendMessage(plugin.lm.getText("arena-dont-exists"));
            }
        }
    }

    private void processScoreSignsCmd(Player player, String[] args) {

    }

    private void processScoreFinish(Player player, String[] args) {
        if (args.length != 1) {
            plugin.em.removeSetUpListerners(player);
            player.sendMessage(plugin.lm.getText("cmd-success"));
        } else {
            plugin.lm.sendTexts(player, "commands.llssetup-score-finish");
        }
    }

}
