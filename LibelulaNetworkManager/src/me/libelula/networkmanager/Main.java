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
package me.libelula.networkmanager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class Main extends JavaPlugin implements CommandExecutor {

    public final CommunicationManager cm;
    public final ServerManager sm;
    public final PlayerManager pm;
    public final EventManager em;
    public final ConfigurationManager config;
    public final CommandManager cmd;
    public final ChatManager chat;
    public final SharedCommandManager scm;
    public final EssentialsManager essentials;
    public final XmlRpcManager xrm;

    private String prefix;

    public Main() throws MalformedURLException, 
            InvalidConfigurationException, FileNotFoundException {
        cm = new CommunicationManager(this);
        sm = new ServerManager(this);
        pm = new PlayerManager(this);
        em = new EventManager(this);
        config = new ConfigurationManager(this);
        cmd = new CommandManager(this);
        chat = new ChatManager(this);
        scm = new SharedCommandManager(this);
        essentials = new EssentialsManager(this);
        xrm = new XmlRpcManager(this);
    }

    @Override
    public void onEnable() {
        sm.load();
        config.init();
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        try {
            cm.startServer(getServer().getPort());
        } catch (SocketException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        pm.start();
        em.register();
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
        cmd.register();
        scm.init();
        for (Player player:getServer().getOnlinePlayers()) {
            pm.addPlayer(player);
            getLogger().log(Level.INFO, "Added player {0} to the database.", player.getName());
        }
    }

    @Override
    public void onDisable() {
        cm.stopServer();
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        cm.sendMessage(cmnd.getName() + "\0" + cs.getName() + "\0" + args.toString(), new InetSocketAddress("localhost", getServer().getPort()));
        return true;
    }

    public void alert(String message) {
        String prefixedMessage = prefix + ChatColor.RED + "(alert) " + message;
        getServer().getConsoleSender().sendMessage(prefixedMessage);
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission("lnm.receive-alerts")) {
                player.sendMessage(prefixedMessage);
            }
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public void teleportToServer(final Player player, final String serverName) {
        final Main plugin = this;
        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {

                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);
                getLogger().info(player.getName() + " -> " + serverName);
                try {
                    out.writeUTF("Connect");
                    out.writeUTF(serverName); // Target Server
                } catch (IOException dummy) {
                    // Can never happen
                }
                player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

            }
        });

    }

}
