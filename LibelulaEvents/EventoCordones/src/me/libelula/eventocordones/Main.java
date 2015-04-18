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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Main extends JavaPlugin {

    public final ArenaManager am;
    public final CommandManager cm;
    public WorldEditPlugin we;
    public final PlayerManager pm;
    public final GameManager gm;
    public final SignManager sm;
    public final EventManager em;
    public Location spawn;
    public String prefix;

    public Main() {
        this.am = new ArenaManager(this);
        this.cm = new CommandManager(this);
        this.pm = new PlayerManager(this);
        this.gm = new GameManager(this);
        this.sm = new SignManager(this);
        this.em = new EventManager(this);
    }

    @Override
    public void onEnable() {
        we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        try {
            am.load();
        } catch (IOException | InvalidConfigurationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        gm.load();
        cm.register();
        em.register();
        sm.load();
        prefix = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("prefix"));

        String spawnWorldName = getConfig().getString("spawn.world");
        if (spawnWorldName != null) {
            World spawnWorld = getServer().getWorld(spawnWorldName);
            if (spawnWorld != null) {
                spawn = spawnWorld.getSpawnLocation();
            }
        }
    }

    @Override
    public void onDisable() {
        try {
            am.save();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (spawn != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                player.teleport(spawn);
                pm.backToNormal(player);
            }
        }
    }

}
