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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ServerManager {

    private final Main plugin;
    private final TreeMap<String, Server> servers;
    private final TreeMap<String, String> aliasServerName;
    private final List<InetSocketAddress> addresses;
    private final ReentrantLock _servers_mutex;

    public class Server {

        private final InetSocketAddress address;
        private final String networkName;
        private final String name;
        private final String alias;

        public Server(InetSocketAddress address, String network, String name,
                String alias) {
            this.address = address;
            this.networkName = network;
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public String getNetwork() {
            return networkName;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public String getServerId() {
            return networkName + "." + name;
        }

        public void sendMessage(String message) {
            plugin.cm.sendMessage(message, address);
        }
    }

    public ServerManager(Main plugin) {
        this.plugin = plugin;
        this.servers = new TreeMap<>();
        this.aliasServerName = new TreeMap<>();
        this._servers_mutex = new ReentrantLock(true);
        addresses = new ArrayList<>();
    }

    public String getServerId() {
        return plugin.getConfig().getString("network-name")
                + "." + plugin.getConfig().getString("server-name");
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress("localhost", plugin.getServer().getPort());
    }

    public void load() {
        _servers_mutex.lock();
        try {
            addresses.clear();
            servers.clear();
            aliasServerName.clear();
            YamlConfiguration serversConfig = new YamlConfiguration();
            File configCache = new File(plugin.getDataFolder(), "netcache.yml");
            try {
                serversConfig.loadFromString(plugin.cm.getSting(new URL(plugin.getConfig().getString("source-uri"))));
                try {
                    serversConfig.save(configCache);
                } catch (IOException ex) {
                    Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (Exception ex) {
                try {
                    if (configCache.exists()) {
                        serversConfig.load(configCache);
                        plugin.getLogger().warning("Unable to read remote file. Using cache.");
                    } else {
                        plugin.getLogger().severe(ex.getMessage());
                        plugin.getLogger().severe("There is no local cache and network global file is unreachable.");
                    }
                } catch (IOException | InvalidConfigurationException ex1) {
                    Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }

            boolean foundMySelf = false;
            for (String remoteNetworkName : serversConfig.getKeys(false)) {
                plugin.getLogger().info("Debug: " + remoteNetworkName);
                ConfigurationSection netSection = serversConfig.getConfigurationSection(remoteNetworkName);
                for (String remoteServerName : netSection.getKeys(false)) {
                    String remoteServerID = remoteNetworkName + "." + remoteServerName;
                    String addressString[] = serversConfig.getString(remoteServerID + "." + "address").split(":");
                    String alias = serversConfig.getString(remoteServerID + "." + "alias");
                    InetSocketAddress address = new InetSocketAddress(addressString[0], Integer.parseInt(addressString[1]));
                    if (remoteServerID.equals(getServerId())) {
                        foundMySelf = true;
                        if (address.getPort() == getInetSocketAddress().getPort()) {
                            address = getInetSocketAddress();
                        } else {
                            plugin.getLogger().warning("Invalid port configuration on global configuration file!");
                        }
                    } else {
                        addresses.add(address);
                    }
                    Server remoteServer = new Server(address, remoteNetworkName, remoteServerName, alias);
                    servers.put(remoteServerID, remoteServer);
                    if (alias != null) {
                        aliasServerName.put(alias, remoteServerName);
                    }
                }
            }
            if (!foundMySelf) {
                plugin.getLogger().warning("Local server is not configured on global configuration.");
            }
            plugin.getLogger().log(Level.INFO, "{0} servers loaded.", servers.size());

        } finally {
            _servers_mutex.unlock();
        }
    }

    public Server getServer(String serverId) {
        return servers.get(serverId);
    }

    public Server getThisServer() {
        return getServer(getServerId());
    }

    public List<InetSocketAddress> getAddresses() {
        return addresses;
    }

    public void showServerListTo(final CommandSender cs) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.pm.sendSyncMessage(cs, ChatColor.GOLD + "Para saltar a los distintos universos escribe: /universo y el nombre de un universo. Los posibles universos son:");
                for (String serverName : aliasServerName.keySet()) {
                    plugin.pm.sendSyncMessage(cs, ChatColor.GOLD + "  * " + serverName);
                }
            }
        });

    }

    public void tryToJump(final CommandSender cs, final String givenServerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Player player;
                if (cs instanceof Player) {
                    player = (Player) cs;
                } else {
                    return;
                }
                String resolvedServer = null;
                for (String serverName : aliasServerName.keySet()) {
                    if (serverName.equalsIgnoreCase(givenServerName)) {
                        resolvedServer = serverName;
                        break;
                    }
                }

                if (resolvedServer != null) {
                    plugin.pm.sendSyncMessage(player, ChatColor.translateAlternateColorCodes('&',
                            "&1[&6Libelula &eNetwork&1] &eLlevándote a " + resolvedServer + "..."));
                    plugin.teleportToServer(player, resolvedServer);
                } else {
                    if (player.hasPermission("lnm.jump-to-hide")) {
                        plugin.pm.sendSyncMessage(player, ChatColor.RED + "Ese universo no está en la lista.");
                        player.sendMessage(ChatColor.GOLD + "Tienes permisos para saltar a universos no listados, tratando de llevarte...");
                        plugin.teleportToServer(player, givenServerName);
                    } else {
                        plugin.pm.sendSyncMessage(player, ChatColor.RED + "Ese universo no existe, escribe /universo para ver la lista.");
                    }
                }
            }
        });
    }

    public void runCommandOnNetwork(final CommandSender cs, final String command) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                String ownNetwork = getThisServer().networkName;
                for (Server server : servers.values()) {
                    if (server.networkName.equals(ownNetwork)) {
                        if (server.getName().equals(getThisServer().getName())) {
                            continue;
                        }
                        runRemoteCommand(server.getName(), cs, command);
                    }
                }
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        });
    }

    public void runRemoteCommand(String serverName, CommandSender cs, String command) {
        String[] args = {serverName, cs.getName(), command};
        Server server = servers.get(serverName);
        if (server != null) {
            server.sendMessage(plugin.cm.formatMessage(CommunicationManager.MessageType.RUN_COMMAND, args));
        }

    }

}
