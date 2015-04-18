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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class CommunicationManager {

    public enum MessageType {

        CHAT, WHISPER, NOTIFY_PLAYER, ENTER, LEFT, FRIEND_REQUEST,
        FRIEND_REQUEST_ANSWER, RUN_COMMAND

    }

    private class Server extends Thread {

        public boolean running;
        private DatagramSocket serverSocket;
        private final byte[] receiveData;

        public Server() {
            receiveData = new byte[1024];
            running = true;
        }

        public void setPort(int port) throws SocketException {
            serverSocket = new DatagramSocket(port);
        }

        @Override
        public void run() {

            while (running) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    serverSocket.receive(receivePacket);
                    if (plugin.getConfig().getBoolean("debug")) {
                        plugin.getLogger().log(Level.INFO, "DEBUG: msg size={0}", receivePacket.getLength());
                    }
                    String recievedText = new String(receivePacket.getData()).substring(0, receivePacket.getLength());

                    String command[] = recievedText.split("\0");
                    ServerManager.Server remoteServer;

                    if (command.length < 1) {
                        continue;
                    }

                    if (plugin.getConfig().getBoolean("debug")) {
                        String messageList = "";
                        for (String part : command) {
                            messageList = messageList + "\"" + part + "\", ";
                        }

                        plugin.getLogger().log(Level.INFO, "<Debug> Message received: {0}", messageList);
                    }

                    switch (command[0]) {
                        case "ENTER":
                            if (command.length != 4) {
                                continue;
                            }
                            // 1 - server ID
                            // 2 - player.getName() 
                            // 3 - player.getDisplayName()
                            remoteServer = plugin.sm.getServer(command[1]);
                            if (remoteServer != null) {
                                plugin.pm.addRemotePlayer(remoteServer, command[2], command[3]);
                                if (plugin.getConfig().getBoolean("debug")) {
                                    plugin.getLogger().log(Level.INFO, "Player {0} added to our list.", command[2]);
                                }

                            } else {
                                plugin.getLogger().log(Level.SEVERE, "Receiving messages from unconfigured remote server:{0}", command[1]);
                            }
                            break;

                        case "CHAT":
                            if (command.length != 5) {
                                continue;
                            }
                            // 1 - server ID
                            // 2 - player.getName()
                            // 3 - player.getDisplayName()
                            // 4 - message

                            break;
                        case "WHISPER":
                            if (command.length != 6) {
                                continue;
                            }

                            // 1 - server ID
                            // 2 - sender.getName()
                            // 3 - reciever.getName()
                            // 4 - sender.getDisplayName()
                            // 5 - message
                            remoteServer = plugin.sm.getServer(command[1]);
                            if (remoteServer != null) {
                                PlayerManager.PlayerInfo pi = plugin.pm.getPlayerInfo(command[2]);
                                if (pi == null || !pi.getServer().getServerId().equals(remoteServer.getServerId())) {
                                    plugin.pm.addRemotePlayer(remoteServer, command[2], command[4]);
                                }

                                {
                                    final ServerManager.Server server = remoteServer;
                                    final String recieverName = command[3];
                                    final String senderName = command[2];
                                    final String senderDisplayName = command[4];
                                    final String message = command[5];

                                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                        @Override
                                        public void run() {
                                            Player reciever = null;
                                            for (Player player : plugin.getServer().getOnlinePlayers()) {
                                                if (player.getName().equalsIgnoreCase(recieverName)) {
                                                    reciever = player;
                                                    break;
                                                }
                                            }
                                            if (reciever != null) {
                                                plugin.pm.processRemoteMessage(server,
                                                        reciever, senderName, senderDisplayName, message);

                                            } else {
                                                String args[] = {senderName, plugin.getPrefix() + ChatColor.RED + "Jugador \"" + recieverName + "\" no encontrado."};
                                                sendMessage(formatMessage(MessageType.NOTIFY_PLAYER, args),
                                                        server.getAddress());
                                            }
                                        }
                                    });
                                }

                            } else {
                                plugin.getLogger().log(Level.SEVERE, "Receiving messages from unconfigured remote server:{0}", command[1]);
                            }
                            break;
                        case "NOTIFY_PLAYER":
                            if (command.length != 4) {
                                continue;
                            }
                            // 1 - server ID
                            // 2 - reciever.getName()
                            // 3 - message
                             {
                                final String recieverName = command[2];
                                final String message = command[3];

                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        Player reciever = null;
                                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                                            if (player.getName().equalsIgnoreCase(recieverName)) {
                                                reciever = player;
                                                break;
                                            }
                                        }
                                        if (reciever != null) {
                                            reciever.sendMessage(message);
                                        }
                                    }
                                });
                            }
                            break;
                        case "FRIEND_REQUEST":
                            if (command.length != 4) {
                                continue;
                            }
                            // 1 - server ID
                            // 2 - reciever.getName()
                            // 3 - sender.getName()
                             {
                                final ServerManager.Server server = plugin.sm.getServer(command[1]);;
                                final String recieverName = command[2];
                                final String senderName = command[3];

                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        Player reciever = null;
                                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                                            if (player.getName().equalsIgnoreCase(recieverName)) {
                                                reciever = player;
                                                break;
                                            }
                                        }
                                        if (reciever != null) {
                                            PlayerManager.PlayerInfo pi = plugin.pm.getPlayerInfo(recieverName);
                                            if (pi != null) {
//                                                plugin.pm.localyProcessAddFriendRequest(reciever, pi, senderName);
                                            } else {
                                                String args[] = {senderName, plugin.getPrefix() + ChatColor.RED + "Jugador \"" + recieverName + "\" no encontrado."};
                                                sendMessage(formatMessage(MessageType.NOTIFY_PLAYER, args),
                                                        server.getAddress());
                                            }
                                        } else {

                                        }
                                    }
                                });
                            }
                            break;
                        case "FRIEND_REQUEST_ANSWER":
                            if (command.length != 5) {
                                continue;
                            }
                            // 1 - server ID
                            // 2 - requester.getName()
                            // 3 - answeringPlayer.getName()
                            // 4 - answer: ACCEPT/DENY
                             {
                                final ServerManager.Server server = plugin.sm.getServer(command[1]);;
                                final String requesterName = command[2];
                                final String answeringPlayerName = command[3];
                                final boolean hasAccepted = command[4].equals("ACCEPT");

                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        Player requester = plugin.pm.getOnlinePlayer(requesterName);
                                        if (requester != null) {
                                            PlayerManager.PlayerInfo pi = plugin.pm.getPlayerInfo(requesterName);
                                            if (pi != null) {
                                                if (hasAccepted) {
                                                    requester.sendMessage(plugin.getPrefix() + "Ahora " + answeringPlayerName + " es tu amigo.");
                                                    requester.sendMessage(plugin.getPrefix() + "Se ha incrementado tu reputación en 1 punto.");
                                                } else {
                                                    requester.sendMessage(plugin.getPrefix() + "El jugador " + answeringPlayerName + " no desea ser tu amigo en este momento.");
                                                }
                                            } else {
                                                if (hasAccepted) {
                                                    String args[] = {answeringPlayerName, plugin.getPrefix()
                                                        + ChatColor.RED + "Error enviando amistad a la otra parte, es posible que él no sea tu amigo."};
                                                    sendMessage(formatMessage(MessageType.NOTIFY_PLAYER, args),
                                                            server.getAddress());
                                                }
                                            }
                                        } else {
                                            if (hasAccepted) {
                                                String args[] = {answeringPlayerName, plugin.getPrefix()
                                                    + ChatColor.RED + "Error enviando amistad a la otra parte, es posible que él no sea tu amigo."};
                                                sendMessage(formatMessage(MessageType.NOTIFY_PLAYER, args),
                                                        server.getAddress());
                                            }
                                        }
                                    }
                                });
                            }
                            break;
                        case "RUN_COMMAND":
                            if (command.length != 4) {
                                continue;
                            }
                            // 1 - server ID
                            // 2 - sender.getName()
                            // 3 - Command
                             {
                                final ServerManager.Server server = plugin.sm.getServer(command[1]);;
                                final String sender = command[2];
                                final String commandLine = command[3];
                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        plugin.getLogger().log(Level.INFO,
                                                "RPC: Server={0} | sender={1} | cmd={2}",
                                                new Object[]{server.getName(), sender, commandLine});
                                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandLine);
                                                
                                        String args[] = {sender, plugin.getPrefix()
                                                    + ChatColor.GREEN + plugin.sm.getServerId() + " Enterado."};

                                        sendMessage(formatMessage(MessageType.NOTIFY_PLAYER, args),
                                                        server.getAddress());        
                                    }

                                });
                            }
                            break;

                    }

                    /* responce:
                     InetAddress IPAddress = receivePacket.getAddress();
                     int port = receivePacket.getPort();
                     String capitalizedSentence = sentence.toUpperCase();
                     sendData = capitalizedSentence.getBytes();
                     DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                     serverSocket.send(sendPacket);
                     */
                } catch (IOException ex) {
                    Logger.getLogger(CommunicationManager.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            serverSocket.disconnect();
            serverSocket.close();
        }
    }

    private final Server server;
    private final Main plugin;

    public CommunicationManager(Main plugin) {
        this.plugin = plugin;
        server = new Server();

    }

    public void startServer(int port) throws SocketException {
        server.setPort(port);
        server.start();
    }

    public void stopServer() {
        server.running = false;
        String message = "NOP";

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            DatagramPacket sendPacket;
            sendPacket = new DatagramPacket(message.getBytes(), message.length(),
                    plugin.sm.getInetSocketAddress());
            clientSocket.send(sendPacket);
            clientSocket.close();
        } catch (SocketException ex) {
            Logger.getLogger(CommunicationManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CommunicationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendMessage(final String message, final InetSocketAddress address) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (DatagramSocket clientSocket = new DatagramSocket()) {
                    DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), address);
                    clientSocket.send(sendPacket);
                    clientSocket.close();
                } catch (SocketException ex) {
                    Logger.getLogger(CommunicationManager.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(CommunicationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

    }

    public String queryServer(String message, InetSocketAddress address) throws SocketException,
            UnknownHostException, IOException {
        String ret;
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            byte[] receiveData = new byte[1024];
            DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), address);
            clientSocket.send(sendPacket);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.setSoTimeout(1000);
            clientSocket.receive(receivePacket);
            ret = new String(receivePacket.getData());
            clientSocket.close();
        }
        return ret;
    }

    public String getSting(URL url) throws IOException {
        String configuration = "";
        URLConnection con = url.openConnection();
        con.setConnectTimeout(1000);
        con.setReadTimeout(1000);

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                configuration = configuration + inputLine + "\n";
            }
            in.close();
        }

        return configuration;
    }

    public void broadCast(String message) {
        for (InetSocketAddress address : plugin.sm.getAddresses()) {
            sendMessage(message, address);
        }
    }

    public String formatMessage(MessageType messageType, String[] args) {
        String ret;
        switch (messageType) {
            case ENTER:
                // 1 - server ID
                // 2 - player.getName()
                // 3 - player.getDisplayName()
                ret = "ENTER\0" + plugin.sm.getServerId() + "\0" + args[0] + "\0" + args[1] + "\0";
                break;
            case WHISPER:
                // 1 - server ID
                // 2 - sender.getName()
                // 3 - reciever.getName()
                // 4 - sender.getDisplayName()
                // 5 - message
                ret = "WHISPER\0" + plugin.sm.getServerId() + "\0" + args[0] + "\0" + args[1] + "\0" + args[2] + "\0" + args[3] + "\0";
                break;
            case NOTIFY_PLAYER:
                // 1 - server ID
                // 2 - reciever.getName()
                // 3 - message
                ret = "NOTIFY_PLAYER\0" + plugin.sm.getServerId() + "\0" + args[0] + "\0" + args[1] + "\0";
                break;
            case FRIEND_REQUEST:
                // 1 - server ID
                // 2 - reciever.getName()
                // 3 - sender.getName()
                ret = "FRIEND_REQUEST\0" + plugin.sm.getServerId() + "\0" + args[0] + "\0" + args[1] + "\0";
                break;
            case FRIEND_REQUEST_ANSWER:
                // 1 - server ID
                // 2 - requester.getName()
                // 3 - answeringPlayer.getName()
                // 4 - answer: ACCEPT/DENY
                ret = "FRIEND_REQUEST_ANSWER\0" + plugin.sm.getServerId() + "\0" + args[0] + "\0" + args[1] + "\0" + args[2] + "\0";
                break;
            case RUN_COMMAND:
                // 1 - server ID
                // 2 - sender.getName()
                // 3 - Command
                ret = "RUN_COMMAND\0" + plugin.sm.getServerId() + "\0" + args[0] + "\0" + args[1] + "\0";
            default:
                ret = "NOOP";
                break;
        }
        return ret
                + "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0";
    }

}
