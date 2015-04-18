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
package me.libelula.lobby;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.libelula.lobby.minigames.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class Main extends JavaPlugin {

    public ConfigurationManager cm;
    public PlayerManager pm;
    public XmlRpcManager xr;
    public EventListener el;
    public WorldEditPlugin we;
    public CommandExecutor ce;
    public Menu menu;
    public SharedCommandManager scm;
    public GameManager olmoTower;

    @Override
    public void onEnable() {
        cm = new ConfigurationManager(this);
        pm = new PlayerManager(this);
        xr = new XmlRpcManager(this);
        el = new EventListener(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        ce = new CommandExecutor(this);
        if (!cm.isAltmenu()) {
            menu = new Menu(this);
        } else {
            menu = new AltMenu(this);
        }
        for (Player player:getServer().getOnlinePlayers()) {
            pm.registerUser(player);
            if (!cm.isPremium() && !cm.isFrontServer()) {
                sendMessage(player, "Lobby reiniciado, necesitamos que te identifiques nuevamente.");
            }
        }
        for (Entity entity : getServer().getWorlds().get(0).getEntities()) {
            if (entity.getType() == EntityType.VILLAGER) {
                entity.remove();
            }
        }
        try {
            scm = new SharedCommandManager(this);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        scm.init();
        olmoTower = new GameManager(this);
        olmoTower.loadConfig("olmotower.yml");
        olmoTower.init();
    }

    @Override
    public void onDisable() {

    }

    public void logInfo(String text) {
        if (text == null) {
            text = "null";
        }
        log(text, ChatColor.AQUA + "[INFO] " + ChatColor.RESET);
    }

    public void logWarn(String text) {
        if (text == null) {
            text = "null";
        }
        log(text, ChatColor.YELLOW + "[WARNING] " + ChatColor.RESET);
    }

    public void logErr(String text) {
        if (text == null) {
            text = "null";
        }
        log(text, ChatColor.RED + "[ERROR] " + ChatColor.RESET);
    }

    private void log(String text, String level) {
        String prefix;
        if (cm == null) {
            prefix = "";
        } else {
            prefix = cm.getPluginPrefix();
        }
        getServer().getConsoleSender().sendMessage(prefix
                .concat(level).concat(ChatColor.translateAlternateColorCodes('&', text)));
    }

    public void teleportToServer(final Player player, final String server) {
        final Main plugin = this;
        player.sendMessage(ChatColor.GREEN + "Llevandote a " + server + "...");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try {
                DataOutputStream out = new DataOutputStream(b);
                if (cm.isDebugMode()) {
                    logInfo("Sending player " + player.getName() + " to " + server + ".");
                }
                out.writeUTF("Connect");
                out.writeUTF(server); // Target Server
                out.close();
            } catch (IOException dummy) {
                // Can never happen
                logInfo("WTF! " + dummy);
            }
            try {
                b.close();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            if (cm.isFrontServer()) {
                pm.kickPlayer(player, "¡fallo al llevarte al servidor esperado!", 60);
            }
        });
    }

    public void sendMessage(Player player, final String message) {
        sendMessage((CommandSender) player, message);
    }

    public void sendMessage(final CommandSender cs, final String message) {
        sendMessage(cs, message, 1);
    }

    public void sendMessage(final CommandSender cs, final String message, long later) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            cs.sendMessage(cm.getPluginPrefix()
                    + ChatColor.translateAlternateColorCodes('&', message));
        }, later);
    }

}
