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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import me.confuser.barapi.BarAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class GameManager {

    private class Game {

        long endTime;
    }

    private final Main plugin;
    private final TreeMap<Player, ArenaManager.Arena> players;
    private final ReentrantLock _players_Lock;
    private final TreeMap<String, HeraldData> spectatorMessages;
    private final TreeMap<String, HeraldData> inGameMessages;
    private final TreeMap<String, Game> games;

    private class HeraldData {

        String message;
        long time;

        public HeraldData(String message, long time) {
            this.message = message;
            this.time = time;
        }
    }

    public GameManager(final Main plugin) {
        this.plugin = plugin;
        players = new TreeMap<>(new Tools.PlayerComparator());
        _players_Lock = new ReentrantLock(true);
        spectatorMessages = new TreeMap<>();
        inGameMessages = new TreeMap<>();
        for (String arenaName : plugin.am.getList().keySet()) {
            ArenaManager.Arena arena = plugin.am.getArena(arenaName);
            if (arena.isEnabled()) {
                clearHeads(arena);
            }
        }
        games = new TreeMap<>();
        checkIntrudersRoutine();
        gameRoutine();
    }

    private void setInventory(Player player, ArenaManager.Arena arena) {
        player.getInventory().setContents(arena.getStartingKit());
        player.getInventory().setBoots(arena.getBoots());
        player.getInventory().setLeggings(arena.getLeggings());
        player.getInventory().setChestplate(arena.getChestplate());
        player.getInventory().setHelmet(arena.getHelmet());
    }

    public void addPlayerToGame(Player player, ArenaManager.Arena arena) {
        _players_Lock.lock();
        try {
            arena.removePlayerFromSpectator(player);
            plugin.pm.setInGame(player);
            teleportPlayerIntoArena(player, arena);
            arena.addPlayerToGame(player);
        } finally {
            _players_Lock.unlock();
        }
    }

    public void turnSpectator(Player player) {
        ArenaManager.Arena arena = players.get(player);
        if (arena != null) {
            arena.removePlayerFromGame(player);
            addPlayerToSpectators(player, arena);
        }
    }

    public void removePlayer(Player player) {
        _players_Lock.lock();
        try {
            ArenaManager.Arena arena = players.remove(player);
            if (arena != null) {
                if (arena.isPlaying(player)) {
                    arena.removePlayerFromGame(player);
                    plugin.pm.backToNormal(player);
                    player.teleport(plugin.lobby);
                } else {
                    removePlayerFromSpectator(player, arena);
                }
            }
        } finally {
            _players_Lock.unlock();
        }
    }

    private void removePlayerFromSpectator(Player player, ArenaManager.Arena arena) {
        players.remove(player);
        arena.removePlayerFromSpectator(player);
        plugin.pm.backToNormal(player);
        player.teleport(plugin.lobby);
    }

    public void removeAllPlayers() {
        _players_Lock.lock();
        try {
            for (Player player : players.keySet()) {
                ArenaManager.Arena arena = players.get(player);
                if (arena != null) {
                    arena.removePlayerFromSpectator(player);
                }
                plugin.pm.backToNormal(player);
                player.teleport(plugin.lobby);
            }
            players.clear();
        } finally {
            _players_Lock.unlock();
        }
    }

    public void addPlayerToSpectators(Player player, ArenaManager.Arena arena) {
        if (arena != null) {
            if (arena.isEnabled()) {
                _players_Lock.lock();
                List<Player> overTakenPlayers;
                ArenaManager.QueuePriority priority = plugin.pm.getPriority(player);
                try {
                    arena.addPlayerToQueue(player, priority);
                    players.put(player, arena);
                    overTakenPlayers = arena.getPlayersBehind(player);
                    player.teleport(arena.getNextSpawnPoint());
                    plugin.pm.setSpectator(player);

                } finally {
                    _players_Lock.unlock();
                }
                String overtakingMessage;
                switch (priority) {
                    case HIGHEST:
                        overtakingMessage = plugin.lm.getText("overtaken-by-highest");
                        break;
                    case HIGH:
                        overtakingMessage = plugin.lm.getText("overtaken-by-high");
                        break;
                    case NORMAL:
                        overtakingMessage = plugin.lm.getText("overtaken-by-normal");
                        break;
                    default:
                        overtakingMessage = plugin.lm.getText("overtaken-by-low");
                        break;
                }
                overtakingMessage = overtakingMessage.replace("%PLAYER%", player.getDisplayName());
                for (Player overTaken : overTakenPlayers) {
                    overTaken.sendMessage(overtakingMessage);
                }
            } else {
                plugin.lm.sendText(player, "arena-is-disabled");
            }
        } else {
            plugin.lm.sendText(player, "arena-does-not-exists");
        }
    }

    public void controlPlayerMovement(PlayerMoveEvent e) {
        ArenaManager.Arena arena = players.get(e.getPlayer());
        if (arena != null) {
            Player player = e.getPlayer();
            if (arena.isSpectator(player)) {
                if (!arena.isInsideColisseum(e.getTo())) {
                    if (arena.isInsideColisseum(e.getFrom())) {
                        e.setCancelled(true);
                        player.teleport(e.getFrom());
                        if (!player.isFlying() && e.getTo().getY() < e.getFrom().getY()) {
                            player.setFlying(true);
                        }
                    } else {
                        player.teleport(arena.getNextSpawnPoint());
                    }
                    plugin.lm.sendText(player, "spectator-cant-exit");
                } else if (arena.isInsideArena(e.getTo())) {
                    if (!arena.isInsideArena(e.getFrom())) {
                        e.setCancelled(true);
                        player.teleport(e.getFrom());
                        if (!player.isFlying() && e.getTo().getY() < e.getFrom().getY()) {
                            
                            player.setFlying(true);
                        }
                    } else {
                        player.teleport(arena.getNextSpawnPoint());
                    }
                    plugin.lm.sendText(player, "spectator-cant-getin-arena");
                }
            } else if (arena.isPlaying(player)) {
                if (!arena.isInsideArena(e.getTo())) {
                    e.setCancelled(true);
                    if (arena.isInsideArena(e.getFrom())) {
                        player.teleport(e.getFrom());
                    } else {
                        teleportPlayerIntoArena(player, arena);
                    }
                    plugin.lm.sendText(player, "player-outside-arena");
                }
            }
        }
    }

    public boolean isPlayerInArena(Player player) {
        return players.containsKey(player);
    }

    private void checkIntrudersRoutine() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                final TreeMap<Location, Player> playerLocations = new TreeMap<>(new Tools.LocationBlockComparator());
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    playerLocations.put(player.getLocation(), player);
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        for (String arenaName : plugin.am.getList().keySet()) {
                            final ArenaManager.Arena arena = plugin.am.getArena(arenaName);
                            if (arena.isEnabled()) {
                                for (Location loc : playerLocations.keySet()) {
                                    final Player player = playerLocations.get(loc);
                                    if (arena.isInsideColisseum(loc) && !arena.hasPlayer(player)) {

                                        Bukkit.getScheduler().runTask(plugin, new Runnable() {

                                            @Override
                                            public void run() {
                                                if (!player.hasPermission("lls.intruder")) {
                                                    player.teleport(plugin.lobby);
                                                }
                                            }
                                        ;
                                    }

                                
                            
                        
                    
                );
            }
        } }
                        }
                    }
                 ;});

            }
         ;}, 30, 30);
    }

    private void herald(PlayerManager.PlayerGroup group, String message, ArenaManager.Arena arena) {
        HeraldData hd;
        if (group == PlayerManager.PlayerGroup.SPECTATOR) {
            hd = spectatorMessages.get(arena.getName());
        } else {
            hd = inGameMessages.get(arena.getName());
        }
        if (hd != null && hd.message.equals(message)) {
            long now = new Date().getTime();
            if (now < hd.time + 10000) {
                return;
            }
        }
        hd = new HeraldData(message, new Date().getTime());
        if (group == PlayerManager.PlayerGroup.SPECTATOR) {
            spectatorMessages.put(arena.getName(), hd);
            for (Player player : arena.getSpectators()) {
                player.sendMessage(message);
            }
        } else {
            inGameMessages.put(arena.getName(), hd);
            for (Player player : arena.getInGamePlayers()) {
                player.sendMessage(message);
            }
        }
    }

    public void teleportPlayerIntoArena(Player player, ArenaManager.Arena arena) {
        TreeSet<Player> inGamePlayers = arena.getInGamePlayers();
        if (inGamePlayers.isEmpty()) {
            player.teleport(arena.getP1StartPoint());
        } else {
            Player inGame = arena.getInGamePlayers().iterator().next();
            if (inGame.getLocation().distance(arena.getP1StartPoint())
                    < inGame.getLocation().distance(arena.getP2StartPoint())) {
                player.teleport(arena.getP2StartPoint());
            } else {
                player.teleport(arena.getP1StartPoint());
            }
        }
    }

    private void gameRoutine() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (String arenaName : plugin.am.getList().keySet()) {
                    ArenaManager.Arena arena = plugin.am.getArena(arenaName);
                    if (!arena.isEnabled()) {
                        continue;
                    }
                    Game game = games.get(arenaName);
                    TreeSet<Player> inGame = arena.getInGamePlayers();
                    if (inGame.size() == 2) {
                        if (game != null && game.endTime > 0) {
                            if (new Date().getTime() > game.endTime) {
                                List<Player> players = new ArrayList<>();
                                players.addAll(arena.getInGamePlayers());
                                for (Player player : players) {
                                    turnSpectator(player);
                                }
                                herald(PlayerManager.PlayerGroup.SPECTATOR, plugin.lm.getText("time-out"), arena);
                                game.endTime = -1;
                            }
                        }
                        continue;
                    }
                    if (arena.getSpectatorCount() + arena.getinGameCount() < 2) {
                        herald(PlayerManager.PlayerGroup.SPECTATOR, plugin.lm.getText("waiting-for-players"), arena);
                    } else {
                        while (arena.getinGameCount() < 2 && arena.getSpectatorCount() >= 1) {
                            Player nextPlayer = arena.removeFirstInQueue();
                            addPlayerToGame(nextPlayer, arena);
                            nextPlayer.sendMessage(plugin.lm.getText("fight"));
                            if (arena.getinGameCount() == 2) {
                                gameStart(arena);
                            }
                        }
                    }
                }
            }
         ;}, 20, 20);
    }

    public void gameOver(Player looser) {
        final ArenaManager.Arena arena = players.get(looser);

        TreeMap<String, String> dict = new TreeMap<>();
        dict.put("%PLAYER%", looser.getDisplayName());
        herald(PlayerManager.PlayerGroup.SPECTATOR, plugin.lm.getText("lider-defeated", dict), arena);
        clearHeads(arena);
        if (arena != null) {
            arena.incInGameDummyPlayerCounter();
            turnSpectator(looser);
            TreeSet<Player> inGamePlayers = arena.getInGamePlayers();
            if (inGamePlayers.isEmpty()) {
                herald(PlayerManager.PlayerGroup.SPECTATOR, plugin.lm.getText("game-draw"), arena);
            } else {
                final Player winner = arena.getInGamePlayers().iterator().next();
                dict.put("%PLAYER%", winner.getDisplayName());
                herald(PlayerManager.PlayerGroup.SPECTATOR, plugin.lm.getText("player-win-game", dict), arena);
                winner.setFireTicks(0);
                for (PotionEffect effect : winner.getActivePotionEffects()) {
                    winner.removePotionEffect(effect.getType());
                }
                plugin.sm.playReminigHealthPointsEffect(winner);
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        arena.clearDummyPlayerCounter();
                        plugin.pm.setInGame(winner);
                    }
                 ;}, 20 * 8);
            }
        }
    }

    public void teleportToSpectator(Player player) {
        ArenaManager.Arena arena = players.get(player);
        if (arena != null) {
            player.teleport(arena.getNextSpawnPoint());
        }
    }

    public void gameStart(ArenaManager.Arena arena) {
        arena.sweep();
        for (Player player : arena.getInGamePlayers()) {
            plugin.pm.setGameMode(player);
            setInventory(player, arena);
            BarAPI.setMessage(player, plugin.lm.getText("time-left", false), 120);
        }
        final List<Player> queue = arena.getPossitions();
        for (Player player : arena.getSpectators()) {
            BarAPI.setMessage(player, plugin.lm.getText("time-left", false), 120);
        }
        Game game = new Game();
        game.endTime = new Date().getTime() + 120 * 1000;
        games.put(arena.getName(), game);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                int i = 0;
                for (Player player : queue) {
                    if (i == 0) {
                        plugin.lm.sendText(player, "you-are-next");
                    } else if (i == 1) {
                        player.sendMessage(plugin.lm.getText("possition-left").replace("%POSITION%", "" + i));
                    } else {
                        player.sendMessage(plugin.lm.getText("possitions-left").replace("%POSITION%", "" + i));
                    }
                    i++;
                }
            }
         ;}, 20);
    }

    public final void clearHeads(ArenaManager.Arena arena) {
        TreeMap<Integer, Skull> skulls = arena.getScoreHeads();
        if (skulls != null) {
            for (Skull skull : skulls.values()) {
                skull.getBlock().setType(Material.AIR);
            }
        }
    }

}
