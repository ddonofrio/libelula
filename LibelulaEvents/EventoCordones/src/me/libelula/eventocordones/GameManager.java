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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class GameManager {

    private final Main plugin;
    private final TreeMap<String, Game> games;
    private final TreeMap<String, Game> players;
    private final ReentrantLock _game_mutex;
    private static final String check = "\u2714";
    private static final String cross = "\u2715";

    private class Game {

        List<String> currentPlayers;
        boolean started;
        private final ArenaManager.Arena arena;
        private Scoreboard board;
        private final TreeMap<String, Integer> objetives;
        private final World world;
        private boolean win;
        private final List<Player> winners;

        public Game(ArenaManager.Arena arena) {
            this.arena = arena;
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            objetives = new TreeMap<>();
            winners = new ArrayList<>();
            world = plugin.getServer().getWorld(arena.getName());
            currentPlayers = new ArrayList<>();
            for (ArenaManager.Arena.Team team : arena.getTeams()) {
                objetives.put(team.getName(), 0);
            }
        }

        public void addPoint(ArenaManager.Arena.Team team) {
            Integer currentScore = objetives.get(team.getName());
            if (currentScore == null) {
                currentScore = 1;
            } else {
                currentScore++;
            }
            objetives.put(team.getName(), currentScore);
            if (currentScore >= 2) {
                win = true;
            }
        }

        public List<Player> getWinners() {
            return winners;
        }

        public void addWiner(Player player) {
            winners.add(player);
        }

        public boolean isWin() {
            return win;
        }

        public void updateScoreBoard() {
            Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = newBoard.registerNewObjective("scores", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName("Equipos");
            OfflinePlayer op;
            for (String teamName : objetives.keySet()) {
                String values;
                ArenaManager.Arena.Team team = arena.getTeam(teamName);
                switch (objetives.get(teamName)) {
                    case 1:
                        values = ChatColor.GREEN + check + " " + ChatColor.RED + cross;
                        break;
                    case 2:
                        values = ChatColor.GREEN + check + " " + check;
                        break;
                    default:
                        values = ChatColor.RED + cross + " " + cross;
                        break;
                }
                String name = values + team.getColor() + " " + teamName;
                if (name.length() > 16) {
                    name = name.substring(0, 15);
                }
                op = plugin.getServer().getOfflinePlayer(name);

                //plugin.getLogger().info("Debug: "+ op.getName());
                objective.getScore(op).setScore(team.getPlayerCount());
            }
            board = newBoard;
            for (Player player : world.getPlayers()) {
                player.setScoreboard(newBoard);
            }
        }
    }

    public GameManager(Main plugin) {
        this.plugin = plugin;
        games = new TreeMap<>();
        players = new TreeMap<>();
        _game_mutex = new ReentrantLock(true);
    }

    public void load() {
        for (ArenaManager.Arena arena : plugin.am.getArenas()) {
            if (arena.getTeams() != null) {
                Game game = new Game(arena);
                _game_mutex.lock();
                try {
                    games.put(arena.getName(), game);
                } finally {
                    _game_mutex.unlock();
                }
            }
        }
    }

    public int getMaxPlayers(String gameName) {
        int ret = 0;
        _game_mutex.lock();
        try {
            Game game = games.get(gameName);
            if (game != null) {
                ret = game.arena.getMaxPlayers();
            }
        } finally {
            _game_mutex.unlock();
        }
        return ret;
    }

    public int getCurrentPlayers(String gameName) {
        int ret = 0;
        _game_mutex.lock();
        try {
            Game game = games.get(gameName);
            if (game != null) {
                ret = game.currentPlayers.size();
            }
        } finally {
            _game_mutex.unlock();
        }
        return ret;
    }

    public void joinGame(Player player, String gameName) {
        if (player.hasPermission("ec.game.*")
                || player.hasPermission("ec.game." + gameName)) {
        } else {
            player.sendMessage(ChatColor.RED + "No estás invitado a este juego.");
            return;
        }

        _game_mutex.lock();
        try {
            Game game = games.get(gameName);
            if (game != null) {
                if (game.currentPlayers.size() <= game.arena.getMaxPlayers()
                        || player.hasPermission("ec.override-limit")) {
                    game.currentPlayers.add(player.getName());
                    players.put(player.getName(), game);
                    player.teleport(game.arena.getSpawn());
                    game.updateScoreBoard();
                } else {
                    player.sendMessage(ChatColor.RED + "No queda espacio para jugar en este juego.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Juego inválido.");
            }
        } finally {
            _game_mutex.unlock();
        }

    }

    public void leftGame(Player player) {
        _game_mutex.lock();
        try {
            Game game = players.remove(player.getName());
            if (game != null) {
                game.currentPlayers.remove(player.getName());
                ArenaManager.Arena.Team team = plugin.pm.removeTeam(player);
                if (team != null) {
                    team.removePlayer(player);
                }
                plugin.pm.backToNormal(player);
                game.updateScoreBoard();
            }
        } finally {
            _game_mutex.unlock();
        }
    }

    public boolean isInGame(Player player) {
        return players.containsKey(player.getName());
    }

    public void checkForWin(Player player) {
        final Game game = players.get(player.getName());
        String teamName = plugin.pm.getTeamName(player);
        if (game != null && teamName != null) {
            if (player.getInventory().getBoots() != null
                    && player.getInventory().getChestplate() != null
                    && player.getInventory().getHelmet() != null
                    && player.getInventory().getLeggings() != null
                    && player.getInventory().getBoots().getType() == Material.IRON_BOOTS
                    && player.getInventory().getChestplate().getType() == Material.IRON_CHESTPLATE
                    && player.getInventory().getHelmet().getType() == Material.IRON_HELMET
                    && player.getInventory().getLeggings().getType() == Material.IRON_LEGGINGS) {

                ArenaManager.Arena.Team team = plugin.am.getTeam(player.getWorld(), teamName);
                game.addPoint(team);
                game.updateScoreBoard();
                String winMessage = plugin.prefix + "El jugador " + team.getColor() + player.getName()
                        + ChatColor.YELLOW + " ha logrado un punto para el equipo " + team.getColor() + team.getName()
                        + ChatColor.YELLOW + ".";
                for (Player all : player.getWorld().getPlayers()) {
                    all.sendMessage(winMessage);
                }
                plugin.gm.leftGame(player);
                player.teleport(game.arena.getSpectatorSpawn());

                game.addWiner(player);

                if (game.isWin()) {
                    winMessage = plugin.prefix + "¡Juego terminado!";
                    String bestPlayers = plugin.prefix + "Los mejores jugadores:";
                    for (Player best : game.getWinners()) {
                        bestPlayers = bestPlayers + " " + best.getDisplayName();
                        if (best.getWorld().getName().equals(game.world.getName())) {
                            best.teleport(game.arena.getTeams().iterator().next().getSpawn().add(0, 1, 0));
                        }
                    }
                    for (Player all : player.getWorld().getPlayers()) {
                        all.sendMessage(winMessage);
                        all.sendMessage(bestPlayers);
                        player.teleport(player.getLocation().add(0, 1, 0));
                        player.setAllowFlight(true);
                        player.setFlying(true);
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            restartGame(game);
                        }

                        private void restartGame(Game game) {
                            for (Player player : game.world.getPlayers()) {
                                plugin.gm.leftGame(player);
                                player.teleport(plugin.spawn);
                            }
                            Game newgame = new Game(plugin.am.getArena(game.arena.getName()));
                            games.put(newgame.arena.getName(), newgame);
                        }
                        
                    }, 20*10);
                } else {
                    player.sendMessage(plugin.prefix + "Espera aquí hasta que termine la partida.");
                }
            }
        }
    }

    public void checkPortalUse(Player player) {
        Game game = games.get(player.getWorld().getName());
        if (game != null && game.arena.getTeams() != null) {
            for (ArenaManager.Arena.Team team : game.arena.getTeams()) {
                if (team.getPortal() != null) {
                    if (team.getPortal().contains(player.getLocation())) {
                        for (ArenaManager.Arena.Team other : game.arena.getTeams()) {
                            if (other.getName().equals(team.getName())) {
                                continue;
                            }
                            if (other.getPlayerCount() < team.getPlayerCount()) {
                                player.sendMessage(plugin.prefix + "Entra en otro equipo que tenga menos jugadores.");
                                return;
                            }
                        }
                        _game_mutex.lock();
                        try {
                            plugin.pm.setTeam(player, team);
                            team.addPlayer(player);
                            game.updateScoreBoard();
                        } finally {
                            _game_mutex.unlock();
                        }
                        plugin.pm.setStuff(player);
                        player.teleport(team.getSpawn());

                    }
                }
            }

        }
    }
}
