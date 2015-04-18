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

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;
import org.kitteh.tag.TagAPI;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class TeamManager {

    private class Score {

        private final Scoreboard scoreboard;
        private final Objective objective;
        public final TreeMap<String, org.bukkit.scoreboard.Team> scoreTeams;

        public Score() {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            objective = scoreboard.registerNewObjective("Players", "Score");
            objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            objective.setDisplayName("Mejores jugadores");
            scoreTeams = new TreeMap<>();

            for (String teamName : teams.keySet()) {
                org.bukkit.scoreboard.Team sbTeam = scoreboard.registerNewTeam(teamName);
                sbTeam.setCanSeeFriendlyInvisibles(true);
                sbTeam.setAllowFriendlyFire(false);
                sbTeam.setPrefix(teams.get(teamName).chatColor + "");
                scoreTeams.put(teamName, sbTeam);
            }
        }

        public org.bukkit.scoreboard.Score getScore(Player player) {
            return objective.getScore(player);
        }

        public Scoreboard getScoreBoard() {
            return scoreboard;
        }
    }

    private class Team implements Comparable<Team> {

        public String name;
        public String tshirtName;
        public Color tshirtColor;
        public ChatColor chatColor;
        public DyeColor dye;

        public Team(String name, String tshirtName, Color tshirtColor, ChatColor chatColor, DyeColor dye) {
            this.name = name;
            this.tshirtName = tshirtName;
            this.tshirtColor = tshirtColor;
            this.chatColor = chatColor;
            this.dye = dye;
        }

        @Override
        public int compareTo(Team o) {
            return this.name.compareTo(o.name);
        }
    }

    private class Listener implements org.bukkit.event.Listener {

        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onNameTag(AsyncPlayerReceiveNameTagEvent event) {
            Team team = disguisedPlayers.get(event.getNamedPlayer());
            if (team != null) {
                event.setTag(team.chatColor + event.getNamedPlayer().getName());
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onArmorSlot(InventoryClickEvent event) {
            if (event.getSlotType().equals(InventoryType.SlotType.ARMOR)
                    && !event.getCurrentItem().getType().equals(Material.AIR)) {
                if (disguisedPlayers.containsKey((Player) event.getWhoClicked())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private final TreeMap<String, Team> teams;
    private final Main plugin;
    private TreeMap<Player, Team> disguisedPlayers;
    private final Lock _disguisedPlayers_mutex;
    private final Score score;

    @Override
    public String toString() {
        String ret = "";
        for (Team team : teams.values()) {
            ret = ret.concat(team.chatColor + team.name + " ");
        }
        if (!ret.isEmpty()) {
            ret = ret.replace(ret.substring(ret.length() - 1), "");
        }
        return ret;
    }

    public TeamManager(Main plugin) {
        this.plugin = plugin;
        teams = new TreeMap<>();
        disguisedPlayers = new TreeMap<>(new Main.PlayerComparator());
        _disguisedPlayers_mutex = new ReentrantLock(true);

        plugin.getServer().getPluginManager().registerEvents(new Listener(), plugin);

        FileConfiguration teamConfig = new YamlConfiguration();
        try {
            teamConfig.load(new File(plugin.getDataFolder(), "teams.yml"));
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe(ex.toString());
        }
        for (String teamName : teamConfig.getKeys(false)) {
            String colorInText = teamName + "." + "color";
            Color tshirtColor = null;
            ChatColor chatColor = null;
            DyeColor dye = null;
            switch (teamConfig.getString(teamName + "." + "color")) {
                case "RED":
                    tshirtColor = Color.RED;
                    chatColor = ChatColor.DARK_RED;
                    dye = DyeColor.RED;
                    break;
                case "BLUE":
                    tshirtColor = Color.BLUE;
                    chatColor = ChatColor.BLUE;
                    dye = DyeColor.BLUE;
                    break;
                case "YELLOW":
                    tshirtColor = Color.YELLOW;
                    chatColor = ChatColor.YELLOW;
                    dye = DyeColor.YELLOW;
                    break;
                case "GREEN":
                    tshirtColor = Color.GREEN;
                    chatColor = ChatColor.GREEN;
                    dye = DyeColor.GREEN;
                    break;
                case "BLACK":
                    tshirtColor = Color.BLACK;
                    chatColor = ChatColor.BLACK;
                    dye = DyeColor.BLACK;
                    break;
                case "WHITE":
                    tshirtColor = Color.WHITE;
                    chatColor = ChatColor.WHITE;
                    dye = DyeColor.WHITE;
                    break;
                case "MAGENTA":
                    tshirtColor = Color.fromRGB(255, 0, 255);
                    chatColor = ChatColor.LIGHT_PURPLE;
                    dye = DyeColor.MAGENTA;
                    break;
                case "CYAN":
                    tshirtColor = Color.fromRGB(0, 255, 255);
                    chatColor = ChatColor.DARK_AQUA;
                    dye = DyeColor.CYAN;
                    break;
                default:
                    plugin.getLogger().severe("Invalid configured color at teams.yml: ".concat(colorInText));
            }
            if (tshirtColor != null) {
                String tshirtName = teamConfig.getString(teamName + ".tshirt-name");
                teams.put(teamName, new Team(teamName, tshirtName, tshirtColor, chatColor, dye));
            }
        }
        score = new Score();
    }
    
    public DyeColor getDyeColor(Player player) {
        return disguisedPlayers.get(player).dye;
    }

    public boolean disguise(Player player, String teamName) {
        if (!isValid(teamName)) {
            return false;
        }
        Team team = teams.get(teamName);
        ItemStack tshirt = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta tShirtMeta = (LeatherArmorMeta) tshirt.getItemMeta();
        tShirtMeta.setColor(team.tshirtColor);
        tShirtMeta.setDisplayName(team.chatColor + team.tshirtName);
        backToNormal(player, false);
        player.setDisplayName(team.chatColor + player.getName());
        tshirt.setItemMeta(tShirtMeta);
        player.getInventory().setChestplate(tshirt);
        player.setGameMode(GameMode.ADVENTURE);
        TagAPI.refreshPlayer(player);
        _disguisedPlayers_mutex.lock();
        try {
            disguisedPlayers.put(player, team);
        } finally {
            _disguisedPlayers_mutex.unlock();
        }
        score.scoreTeams.get(teamName).addPlayer(player);
        updateScoreboards();
        return true;
    }

    public void backToNormal(Player player) {
        backToNormal(player, true);
    }

    private void backToNormal(Player player, boolean reset) {
        _disguisedPlayers_mutex.lock();
        Team team = null;
        try {
            team = disguisedPlayers.remove(player);
        } finally {
            _disguisedPlayers_mutex.unlock();
        }
        ItemStack air = new ItemStack(Material.AIR);
        player.setFoodLevel(20);
        player.setHealth(20);
        player.setFlying(false);
        player.getInventory().clear();
        player.getInventory().setBoots(air);
        player.getInventory().setHelmet(air);
        player.getInventory().setLeggings(air);
        if (team != null) {
            score.scoreTeams.get(team.name).removePlayer(player);           
        }
        
        for (PotionEffect effect : player.getActivePotionEffects())
            player.removePotionEffect(effect.getType());
        
        if (reset == true) {
            player.setDisplayName(player.getName());
            player.setGameMode(GameMode.SURVIVAL);
            TagAPI.refreshPlayer(player);
            player.getInventory().setChestplate(air);
            updateScoreboards();
        }
    }

    public boolean isValid(String teamName) {
        return teams.containsKey(teamName);
    }

    public org.bukkit.scoreboard.Score getScore(Player player) {
        return score.getScore(player);
    }

    public void updateScoreboards() {
        _disguisedPlayers_mutex.lock();
        try {
            for (Player player : disguisedPlayers.keySet()) {
                getScore(player).setScore(getScore(player).getScore());
                score.scoreTeams.get(disguisedPlayers.get(player).name).removePlayer(player);
                player.setScoreboard(score.getScoreBoard());
                score.scoreTeams.get(disguisedPlayers.get(player).name).addPlayer(player);
                player.setScoreboard(score.getScoreBoard());
                
            }
        } finally {
            _disguisedPlayers_mutex.unlock();
        }
    }

}
