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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Game {

    private final Main plugin;
    private final TreeMap<String, List<Location>> freeTeamPos;
    private final MapManager.Arena arena;
    private final TreeMap<Player, PlayerInfo> players;
    private gameState state;
    private boolean pairTick;
    //private int countDown;
    private final Round round;
    private final TreeMap<Location, Material> blockingMaterials;
    private final TreeMap<Location, Material> towerMaterials;
    
    public gameState getGameState() {
        return state;
    }

    public int getPlayerCount() {
        return players.size();
    }
    
    public String getTeam(Player player) {
        PlayerInfo pi = players.get(player);
        if (pi != null) {
            return pi.team;
        } else {
            return null;
        }
    }

    public MapManager.Arena getArena() {
        return arena;
    }

    public Main getPlugin() {
        return plugin;
    }

    private class PlayerInfo {

        String team;
        Location spawnPoint;
    }

    public enum gameState {

        WAITING_FOR_PLAYERS, IN_GAME
    }

    public Game(Main plugin, String mapName) {
        this.plugin = plugin;
        this.arena = plugin.mapMan.getArena(mapName);
        freeTeamPos = new TreeMap<>();
        for (String team : plugin.mapMan.getArena(mapName).teamSpawnPoints.keySet()) {
            List<Location> locs = new ArrayList<>();
            locs.addAll(plugin.mapMan.getArena(mapName).teamSpawnPoints.get(team));
            freeTeamPos.put(team, locs);
        }
        players = new TreeMap<>(new Main.PlayerComparator());
        state = gameState.WAITING_FOR_PLAYERS;
        round = new Round(this);
        round.setRound(-1);
        blockingMaterials = new TreeMap<>(new Main.LocationComparator());
        towerMaterials = new TreeMap<>(new Main.LocationComparator());
        for (Block block : arena.blockingBlocks) {
            blockingMaterials.put(block.getLocation(), block.getType());
        }
        /*
         for (int i = 1; i < 11; i++) {
         Location loc = new Location(arena.capturePoint.getWorld(), arena.capturePoint.getBlockX(), arena.capturePoint.getBlockY(), arena.capturePoint.getBlockZ());
         loc.setY(loc.getY() - i);
         towerMaterials.put(loc, loc.getBlock().getType());
         }
         */
    }

    /*
     public void restoreBlocks() {
     for (int i = 1; i < 11; i++) {
     Location loc = new Location(arena.capturePoint.getWorld(), arena.capturePoint.getBlockX(), arena.capturePoint.getBlockY(), arena.capturePoint.getBlockZ());
     loc.setY(loc.getY() - i);
     loc.getBlock().setType(towerMaterials.get(loc));
     }
     closeCages();
     }
     */
    
    public boolean isInGame(Player player) {
        return players.containsKey(player);
    }
    
    public boolean addPlayer(Player player, String team) {
        if (state != gameState.WAITING_FOR_PLAYERS) {
            player.sendMessage(ChatColor.RED + "No puedes entrar ahora en este juego.");
            return false;
        }
        List<Location> freeLocations = freeTeamPos.get(team);
        if (freeLocations.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No hay posisiones libres en este equipo");
            return false;
        }

        PlayerInfo pi = new PlayerInfo();
        pi.team = team;
        pi.spawnPoint = freeLocations.remove(0);
        players.put(player, pi);

        plugin.teamMan.disguise(player, team);
        moveToSpawn(player);

        if (players.size() >= arena.maxPlayers) {
            startGame();
        } else if (players.size() >= arena.minPlayers) {
            //startCountDown();
        }
        
        plugin.gameControler.updateSigngs(this);
        return true;
    }

    public boolean addPlayer(Player player) {
        if (state != gameState.WAITING_FOR_PLAYERS) {
            player.sendMessage(ChatColor.RED + "No puedes entrar ahora en este juego.");
            return false;
        }
        int min = arena.maxPlayers;
        String minTeamName = null;
        for (String teamName : freeTeamPos.keySet()) {
            if (!freeTeamPos.get(teamName).isEmpty()) {
                if (min > freeTeamPos.get(teamName).size()) {
                    min = freeTeamPos.get(teamName).size();
                    minTeamName = teamName;
                }
            }
        }

        if (minTeamName != null) {
            addPlayer(player, minTeamName);
            return true;
        }
        return false;
    }

    public void removePlayer(Player player) {
        PlayerInfo pi = players.remove(player);
        plugin.teamMan.getScore(player).setScore(0);
        freeTeamPos.get(pi.team).add(pi.spawnPoint);
        plugin.teamMan.backToNormal(player);
        player.teleport(plugin.mapMan.getLobby());
        if (players.isEmpty()) {
            endGame();
        }
        plugin.gameControler.updateSigngs(this);
    }

    public void removeAllPlayers() {
        List<Player> playerList = new ArrayList();
        playerList.addAll(players.keySet());
        for (Player player : playerList) {
            removePlayer(player);
        }
    }

    /*
    public void startCountDown() {
        countDown = arena.waitSecForPlayers + 1;
    }
    */
    public void messageAll(String message) {
        for (Player player : players.keySet()) {
            player.sendMessage(message);
        }
    }

    public void messageAll(String message, List<Player> involvedPlayers) {
        for (Player player : players.keySet()) {
            if (player.getName().equals(involvedPlayers)) {
                player.sendMessage(message);
            } else {
                player.sendMessage(ChatColor.GOLD + "***" + message + ChatColor.GOLD + "***");
            }
        }
    }

    public void tick() {
        pairTick = !pairTick;

        round.tick();

/*        if (pairTick) {
            if (countDown >= 1) {
                if (state == gameState.WAITING_FOR_PLAYERS) {
                    switch (countDown) {
                        case 20:
                        case 10:
                        case 5:
                        case 4:
                        case 3:
                        case 2:
                        case 1:
                            messageAll(ChatColor.GRAY + "" + ChatColor.ITALIC + "El juego comienza en " + (countDown) + " segundos.");
                    }

                }
                countDown--;
                if (countDown == 0) {
                    startGame();
                }
            }
        }*/
    }

    public void startGame() {
        state = gameState.IN_GAME;
        round.setRound(0);
    }
    
    public void endGame() {
        removeAllPlayers();
        round.setRound(-1);        
        state = gameState.WAITING_FOR_PLAYERS;
    }

    public void giveToAllplayers(ItemStack is) {
        for (Player player : players.keySet()) {
            player.getInventory().addItem(is);
        }
    }

    public void giveEffectToAllPlayers(PotionEffect effect) {
        for (Player player : players.keySet()) {
            player.addPotionEffect(effect, true);
        }
    }

    public void openCages() {
        for (Block block : arena.blockingBlocks) {
            //block.breakNaturally();
            block.setType(Material.AIR);
            playSoundAll(Sound.DOOR_OPEN);
        }
    }

    public void closeCages() {
        for (Block block : arena.blockingBlocks) {
            block.setType(blockingMaterials.get(block.getLocation()));
        }
    }

    public void moveAllToSpawn() {
        for (Player player : players.keySet()) {
            moveToSpawn(player);
        }
    }

    public void moveToSpawn(Player player) {
        PlayerInfo pi = players.get(player);
        if (pi != null) {
            player.teleport(pi.spawnPoint);
            plugin.teamMan.disguise(player, pi.team);
        }
    }

    public void playSoundAll(Sound sound) {
        for (Player player : players.keySet()) {
            player.playSound(arena.capturePoint, sound, 100, 1);
        }
    }

    public void announsePoint() {
        Map<String, Integer> scores = new TreeMap<>();
        for (Player player : players.keySet()) {
            scores.put(player.getDisplayName(), getPlugin().teamMan.getScore(player).getScore());
            player.sendMessage(ChatColor.YELLOW + "Tus puntos actuales: " + getPlugin().teamMan.getScore(player).getScore());
        }
        messageAll(entriesSortedByValues(scores).last().getKey() + " lidera la tabla de puntos.");
    }
    
    public void announseWinner() {
        Map<String, Integer> scores = new TreeMap<>();
        for (Player player : players.keySet()) {
            scores.put(player.getName(), getPlugin().teamMan.getScore(player).getScore());
            player.sendMessage(ChatColor.YELLOW + "Tus puntos actuales: " + getPlugin().teamMan.getScore(player).getScore());
        }
        String winner = null;
        int winnerPoints = 0;
        for (String playerName: scores.keySet()) {
            if (scores.get(playerName) > winnerPoints) {
                winnerPoints = scores.get(playerName);
                winner = playerName;
            }
        }
        if (winner != null) {
            Player player = plugin.getServer().getPlayer(winner);
            if (player != null) {
                messageAll(player.getDisplayName() +  ChatColor.GREEN + ChatColor.BOLD + " ha ganado el juego");
            }
        }
        
        plugin.sbm.updateScores(scores);
    }
    

    static <K, V extends Comparable<? super V>>
            SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
                    @Override
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        return e1.getValue().compareTo(e2.getValue());
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
            
            

}
