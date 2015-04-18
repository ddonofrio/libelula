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
/*
 *            This file is part of LibelulaLobby plugin.
 *
 *  LibelulaLobby is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibelulaLobby is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibelulaLobby. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.lobby.minigames;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;

import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class GameManager {

    private class PlayerStuff {

        private final Inventory previousInventory;
        private final Player player;
        private final GameMode previousGameMode;

        public PlayerStuff(Player player) {
            this.previousInventory = Bukkit.createInventory(player, InventoryType.PLAYER);
            this.previousInventory.setContents(player.getInventory().getContents());
            this.previousGameMode = player.getGameMode();
            this.player = player;
        }

        public Player getPlayer() {
            return player;
        }

        public Inventory getPreviousInventory() {
            return previousInventory;
        }

        public GameMode getPreviousGameMode() {
            return previousGameMode;
        }

    }

    private final Plugin plugin;
    private final TreeMap<String, PlayerStuff> players;
    private final ReentrantLock _players_mutex;
    private final ConfigurationManager config;

    public GameManager(Plugin plugin) {
        this.plugin = plugin;
        this.players = new TreeMap<>();
        this.config = new ConfigurationManager(plugin);
        _players_mutex = new ReentrantLock();
    }

    public void loadConfig(String configFileName) {
        config.loadConfig(configFileName);
    }

    public void init() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                boolean inGame = isInGame(player);
                boolean inGameArea = isGameArea(player.getLocation());
                if (!inGame && inGameArea) {
                    addPlayer(player);
                } else if (inGame && !inGameArea) {
                    removePlayer(player);
                } else if (inGame && inGameArea) {
                    if (config.getWinArea().contains(player.getLocation())) {
                        spawn(player);
                        player.sendMessage(config.getAnnounceWin());
                        String announceBroadcast = config.getAnnounceWinBroadcast().replace(
                                "%PLAYER%", player.getName());
                        for (Player other : plugin.getServer().getOnlinePlayers()) {
                            if (!other.getName().equals(player.getName())) {
                                other.sendMessage(announceBroadcast);
                            }
                        }
                        plugin.getLogger().info(ChatColor.stripColor(announceBroadcast));
                    }
                }
            }
        }, 21, 21);
    }

    public boolean isGameArea(Location loc) {
        boolean result = false;
        for (CuboidSelection area : config.getAreas()) {
            if (area.contains(loc)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean isInGame(Player player) {
        return players.containsKey(player.getName());
    }

    private void addPlayer(Player player) {
        PlayerStuff stuff = new PlayerStuff(player);
        _players_mutex.lock();
        try {
            players.put(player.getName(), stuff);
        } finally {
            _players_mutex.unlock();
        }
        player.setGameMode(config.getGamemode());
        player.sendMessage(config.getAreaAnnounceIn());
        player.getInventory().setContents(config.getKit().getContents());
    }

    public void removePlayer(Player player) {
        PlayerStuff stuff = null;
        _players_mutex.lock();
        try {
            stuff = players.remove(player.getName());
        } finally {
            _players_mutex.unlock();
        }
        if (stuff != null) {
            player.setGameMode(stuff.getPreviousGameMode());
            player.sendMessage(config.getAreaAnnounceOut());
            player.getInventory().setContents(stuff.getPreviousInventory().getContents());
            player.setFireTicks(0);
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        }
    }

    public void spawn(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.teleport(config.getSpawnPoint());
        },5);
    }
}
