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
import java.util.Comparator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Menkeres
 */
public class Main extends JavaPlugin {

    static public class PlayerComparator implements Comparator<Player> {

        @Override
        public int compare(Player o1, Player o2) {
            return o1.getName().compareTo(o2.getName());
        }

    }
    static class LocationComparator implements Comparator<Location> {

        @Override
        public int compare(Location o1, Location o2) {
            int diff = o1.getWorld().getName().compareTo(o2.getWorld().getName());
            if (diff == 0) {
                diff = o1.getBlockX() - o2.getBlockX();
                if (diff == 0) {
                    diff = o1.getBlockY() - o2.getBlockY();
                    if (diff == 0) {
                        diff = o1.getBlockZ() - o2.getBlockZ();
                    }
                }
            }
            return diff;
        }
    }
    
    public WorldGuardManager wgm;
    public TeamManager teamMan;
    public MapManager mapMan;
    public GameControler gameControler;
    public ScoreBoardManager sbm;
    public SQLiteManager sql;
    private FileConfiguration lang;

    @Override
    public void onEnable() {
        config();
        wgm = new WorldGuardManager(this);
        teamMan = new TeamManager(this);
        mapMan = new MapManager(this);
        sbm = new ScoreBoardManager(this);
        sql = new SQLiteManager(this);
        Commands commands = new Commands(this);
        getCommand("lclsetup").setExecutor(commands);
        getCommand("lcl").setExecutor(commands);
        gameControler = new GameControler(this);
        lang = new YamlConfiguration();
        try {
            lang.load(new File(getDataFolder(), "lang.yml"));
        } catch (IOException | InvalidConfigurationException ex) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "lang.yml cannot be read." );
        }
    }

    @Override
    public void onDisable() {
        gameControler.playersLeaveAll();
        mapMan.persist();
        sql.closeConnection();
    }
    
    public void startGame(String arenaName) {

    }

    public void stopGame(String arenaName) {

    }

    public void alert(ChatColor color, String message) {
        ConsoleCommandSender cs = getServer().getConsoleSender();
        cs.sendMessage(color + message);
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(color + message);
            }
        }
    }

    private void config() {
        File teams = new File(getDataFolder(), "teams.yml");
        if (!teams.exists()) {
            saveResource("teams.yml", false);
        }
        File lang = new File(getDataFolder(), "lang.yml");
        if (!lang.exists()) {
            saveResource("lang.yml", false);
        }
        saveDefaultConfig();
    }
    
    public String getText(String label) {
        return ChatColor.translateAlternateColorCodes('&', lang.getString("PREFIX")) + 
                ChatColor.translateAlternateColorCodes('&', lang.getString(label));
    }

}
