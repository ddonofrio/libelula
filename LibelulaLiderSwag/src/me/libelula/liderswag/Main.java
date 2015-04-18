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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.sql.SQLException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Main extends JavaPlugin {

    public LangManager lm;
    public ArenaManager am;
    public CommandManager cm;
    public WorldEditPlugin we;
    public EventManager em;
    public ScoreManager sm;
    public DBManager dm;
    public SignManager sgm;
    public PlayerManager pm;
    public GameManager gm;
    public Location lobby;

    @Override
    public void onEnable() {
        we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        if (we == null) {
            alertAdmins(lm.getText("we-not-enabled"));
            getPluginLoader().disablePlugin(this);
            return;
        }

        lm = new LangManager(this);
        am = new ArenaManager(this);
        cm = new CommandManager(this);
        em = new EventManager(this);

        try {
            dm = new DBManager(this);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            getLogger().severe(ex.getMessage());
            getPluginLoader().disablePlugin(this);
            return;
        }
        sm = new ScoreManager(this);

        am.loadAll();
        sgm = new SignManager(this);
        pm = new PlayerManager(this);
        gm = new GameManager(this);
        if (!getConfig().isSet("lobby.location")) {
            alertAdmins(lm.getText("main-lobby-not-set"));
        } else {
            String worldName = getConfig().getString("lobby.location.world");
            if (worldName != null) {
                World world = getServer().getWorld(worldName);
                if (world != null) {
                    lobby = Tools.getPreciseLocation(getConfig().getConfigurationSection("lobby.location"), world);
                }
            }
            if (lobby == null) {
                alertAdmins(lm.getText("main-lobby-not-set"));
            }
        }
    }

    @Override
    public void onDisable() {
        am.saveAll();
        gm.removeAllPlayers();
        sm.save();
    }

    public void alertAdmins(String text) {
        getServer().getConsoleSender().sendMessage(text);
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission("lls.admin")) {
                player.sendMessage(text);
            }
        }
    }

}
