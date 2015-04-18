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

import java.util.TreeMap;
import me.confuser.barapi.BarAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class PlayerManager {

    private final Main plugin;
    private final TreeMap<Player, PlayerGroup> players;
    private final TreeMap<Player, PlayerStuff> playerStuffMap;

    private class PlayerStuff {

        boolean allowFlight;
        GameMode gameMode;
        Inventory inventory;

        public PlayerStuff(Player player) {
            allowFlight = player.getAllowFlight();
            gameMode = player.getGameMode();
            inventory = Bukkit.createInventory(player, InventoryType.PLAYER);
            inventory.setContents(player.getInventory().getContents());
        }

    }

    public enum PlayerGroup {

        SPECTATOR, PLAYER
    }

    public PlayerManager(Main plugin) {
        this.plugin = plugin;
        players = new TreeMap<>(new Tools.PlayerComparator());
        playerStuffMap = new TreeMap<>(new Tools.PlayerComparator());
    }

    public ArenaManager.QueuePriority getPriority(Player player) {
        if (player.hasPermission("lls.priority-highest")) {
            return ArenaManager.QueuePriority.HIGHEST;
        } else if (player.hasPermission("lls.priority-high")) {
            return ArenaManager.QueuePriority.HIGH;
        } else if (player.hasPermission("lls.priority-normal")) {
            return ArenaManager.QueuePriority.NORMAL;
        } else if (player.hasPermission("lls.priority-low")) {
            return ArenaManager.QueuePriority.LOW;
        } else {
            return ArenaManager.QueuePriority.LOWEST;
        }
    }

    public void setSpectator(Player player) {
        if (!players.containsKey(player)) {
            playerStuffMap.put(player, new PlayerStuff(player));
        }
        players.put(player, PlayerManager.PlayerGroup.SPECTATOR);
        clearInventory(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFireTicks(0);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        plugin.sm.removeScoreboard(player);
    }

    public void setInGame(Player player) {
        players.put(player, PlayerManager.PlayerGroup.PLAYER);
        setGameMode(player);
        plugin.sm.setScoreboard(player);
        tingTingSound(player);
    }

    public void setGameMode(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
    }

    public boolean isSpectator(Player player) {
        PlayerGroup group = players.get(player);
        return group != null && group == PlayerGroup.SPECTATOR;
    }

    public boolean isInGame(Player player) {
        PlayerGroup group = players.get(player);
        return group != null && group == PlayerGroup.PLAYER;
    }

    public void backToNormal(Player player) {
        PlayerStuff playerStuff = playerStuffMap.remove(player);
        players.remove(player);
        if (!playerStuff.allowFlight && player.isFlying()) {
            player.setFlying(false);
        }
        player.setAllowFlight(playerStuff.allowFlight);
        player.setGameMode(playerStuff.gameMode);
        player.getInventory().setContents(playerStuff.inventory.getContents());
        player.updateInventory();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        plugin.sm.removeScoreboard(player);
        BarAPI.removeBar(player);
    }

    public static void clearInventory(Player player) {
        ItemStack air = new ItemStack(Material.AIR);
        player.getInventory().clear();
        player.getInventory().setBoots(air);
        player.getInventory().setChestplate(air);
        player.getInventory().setHelmet(air);
        player.getInventory().setLeggings(air);
        player.updateInventory();
    }

    public void tingTingSound(final Player player) {

        for (int i = 5; i <= 30; i += 5) {

            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    player.playSound(player.getLocation(), Sound.GLASS, 10, 10);
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 10, 10);
                    player.playSound(player.getLocation(), Sound.GLASS, 10, 10);
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 10, 10);
                    player.playSound(player.getLocation(), Sound.GLASS, 10, 10);
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 10, 10);
                }
             ;}, i);
        }

    }

}
