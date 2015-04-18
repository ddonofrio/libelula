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
package me.libelula.capturethewool;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public final class TournamentManager {

    private final class Team {

        private String name;
        private String captain;
        private List<String> members;
    }

    private final class Room {

        String name;
        Team blue;
        Team red;
    }

    private final Main plugin;
    private final TreeMap<String, Team> teams;
    private final ReentrantLock _teams_mutex;
    private final TreeMap<String, Room> rooms;

    public TournamentManager(Main plugin) {
        this.plugin = plugin;
        File teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        teams = new TreeMap<>();
        rooms = new TreeMap<>();
        _teams_mutex = new ReentrantLock(true);

        if (!teamsFile.exists()) {
            plugin.getConfig().set("tournament-mode", false);
            plugin.alert("Disablig tournament-mode due to teams.yml does not exists.");
        } else {
            YamlConfiguration teamsConfig = new YamlConfiguration();
            try {
                teamsConfig.load(teamsFile);
                for (String key : teamsConfig.getKeys(false)) {
                    String teamName = teamsConfig.getString(key + ".name");
                    String teamCaptain = teamsConfig.getString(key + ".captain");
                    List<String> teamMembers = teamsConfig.getStringList(key + ".members");
                    setCaptain(teamName, teamCaptain);
                    setMembers(teamName, teamMembers);
                }
            } catch (IOException | InvalidConfigurationException ex) {
                Logger.getLogger(TournamentManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Team getTeam(String teamName) {
        Team result = teams.get(teamName);
        if (result == null) {
            result = new Team();
            result.name = teamName;
            teams.put(teamName, result);
        }
        return result;
    }

    public void setCaptain(String teamName, String captain) {
        _teams_mutex.lock();
        try {
            getTeam(teamName).captain = captain;
        } finally {
            _teams_mutex.unlock();
        }
    }

    public void setMembers(String teamName, List<String> members) {
        _teams_mutex.lock();
        try {
            getTeam(teamName).members = members;
        } finally {
            _teams_mutex.unlock();
        }
    }

    public void addMember(String teamName, String member) {
        _teams_mutex.lock();
        try {
            getTeam(teamName).members.add(member);
        } finally {
            _teams_mutex.unlock();
        }
    }

    public void assignTeamRoomTeam(String teamName, String roomName, final TeamManager.TeamId teamId) {
        _teams_mutex.lock();
        final Team team;
        try {
            team = teams.get(teamName);
        } finally {
            _teams_mutex.unlock();
        }
        if (team != null) {
            final World roomWorld = plugin.rm.getCurrentWorld(roomName);
            if (roomWorld != null) {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
//                        for (String playerName : team.members) {
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            if (player.getName().equalsIgnoreCase(team.captain)) {
                                player.teleport(roomWorld.getSpawnLocation());
                                continue;
                            }
                            for (String memberName : team.members) {
                                if (player.getName().equalsIgnoreCase(memberName)) {
                                    player.teleport(roomWorld.getSpawnLocation());
                                }
                            }
//                            }
                        }
                    }
                });
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        for (Player player : roomWorld.getPlayers()) {
                            if (player.getName().equalsIgnoreCase(team.captain)) {
                                plugin.gm.joinInTeam(player, teamId);
                                continue;
                            }
                            for (String memberName : team.members) {
                                if (player.getName().equalsIgnoreCase(memberName)) {
                                    plugin.gm.joinInTeam(player, teamId);
                                }
                            }
                        }
                    }
                }, 20);
                plugin.lm.sendMessageToWorld(teamName + " ha sido asigando a la sala " + roomName, plugin.wm.getLobbyWorld(), null);
                Room room = rooms.get(roomName);
                if (room == null) {
                    room = new Room();
                    room.name = roomName;
                    rooms.put(roomName, room);
                }
                if (teamId == TeamManager.TeamId.BLUE) {
                    room.blue = team;
                } else {
                    room.red = team;
                }

            } else {
                plugin.alert(roomName + " does not exits.");
            }
        } else {
            plugin.getLogger().info("Debug" + teams.keySet().toString());
            plugin.alert(teamName + " does not exits.");
        }
    }
    
    public void winner(String roomName, TeamManager.TeamId teamId) {
        Room room= rooms.remove(roomName);
        Team winner;
        Team looser;
        if (teamId == TeamManager.TeamId.BLUE) {
            winner = room.blue;
            looser = room.red;
        } else {
            winner = room.red;
            looser = room.blue;
        }
        String text="El equipo " + winner.name + " gana a " + looser.name + " en la sala " + roomName;
        plugin.lm.sendMessageToWorld(text, plugin.wm.getLobbyWorld(), null);
        plugin.alert(text);
    }
}
