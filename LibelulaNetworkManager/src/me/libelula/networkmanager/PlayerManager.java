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

import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class PlayerManager {

    private final Main plugin;
    private final TreeMap<String, PlayerInfo> players;
    private final ReentrantLock _players_mutex;

    public PlayerManager(Main plugin) {
        this.plugin = plugin;
        players = new TreeMap<>();
        _players_mutex = new ReentrantLock(true);
    }

    public class PlayerInfo {

        private final ServerManager.Server server;
        private final String name;
        private final String DisplayName;
        private PlayerInfo lastPrivateChatContact;
        private long lastTimeChatCmd;
        private final long loggedIn;
        private int totalPlayedMs;
        private boolean banned;
        private String banReason;

        public PlayerInfo(ServerManager.Server server, String name, String CustomName) {
            this.server = server;
            this.name = name;
            this.DisplayName = CustomName;
            lastPrivateChatContact = null;
            lastTimeChatCmd = 0;
            loggedIn = new Date().getTime();
        }

        public String getBanReason() {
            return banReason;
        }

        public boolean isBanned() {
            return banned;
        }

        public void setBanReason(String banReason) {
            this.banReason = banReason;
        }

        public void setBanned(boolean banned) {
            this.banned = banned;
        }
        
        public void setTotalPlayedMs(int totalPlayedMs) {
            this.totalPlayedMs = totalPlayedMs;
        }

        public int getTotalPlayedMs() {
            return totalPlayedMs;
        }

        public long getLoggedInTime() {
            return loggedIn;
        }

        public String getName() {
            return name;
        }

        public ServerManager.Server getServer() {
            return server;
        }

        public String getDisplayName() {
            return DisplayName;
        }

        public PlayerInfo getLastPrivateChatContact() {
            return lastPrivateChatContact;
        }

        public void sendMessage(Player source, String message) {
            String[] args = {source.getName(), getName(), source.getDisplayName(), message};
            server.sendMessage(plugin.cm.formatMessage(CommunicationManager.MessageType.WHISPER, args));
            PlayerInfo sourcePi = plugin.pm.getPlayerInfo(source.getName());
            if (sourcePi != null) {
                sourcePi.lastPrivateChatContact = this;
                this.lastPrivateChatContact = sourcePi;
            }
        }

        private void setLastPrivateChatContact(PlayerInfo lastPrivateChatContact) {
            this.lastPrivateChatContact = lastPrivateChatContact;
        }

        public void sendNotification(String message) {
            String[] args = {getName(), message};
            server.sendMessage(plugin.cm.formatMessage(CommunicationManager.MessageType.NOTIFY_PLAYER, args));
        }

    }

    public void chatProcess(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        PlayerInfo pi = players.get(player.getName().toLowerCase());
        if (pi != null) {
            long now = new Date().getTime();
            if (pi.lastTimeChatCmd != 0) {
                int delay = plugin.config.getChatDelay(player);
                int timeToWait = (int) ((pi.lastTimeChatCmd + delay) - now) / 1000;
                if (timeToWait > 0) {
                    e.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED
                            + "Debido a tu rango debes esperar " + timeToWait + " segundos para hablar.");
                } else {
                    pi.lastTimeChatCmd = now;
                }
            } else {
                pi.lastTimeChatCmd = now;
            }
        }
        if (!e.isCancelled()) {
            plugin.chat.processChat(e);
        }
    }

    public void start() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            addPlayer(player);
        }
    }

    public void addPlayer(Player player) {
        _players_mutex.lock();
        try {
            PlayerInfo pi = new PlayerInfo(plugin.sm.getThisServer(),
                    player.getName(), player.getDisplayName());
            players.put(player.getName().toLowerCase(), pi);
            String[] args = {player.getName(), player.getDisplayName()};
            plugin.cm.broadCast(plugin.cm.formatMessage(CommunicationManager.MessageType.ENTER, args));
        } finally {
            _players_mutex.unlock();
        }
    }

    public void addRemotePlayer(ServerManager.Server server, String playerName, String playerDisplayName) {
        PlayerInfo pi = new PlayerInfo(server, playerName, playerDisplayName);
        _players_mutex.lock();
        try {
            players.put(playerName.toLowerCase(), pi);
        } finally {
            _players_mutex.unlock();
        }
    }

    public PlayerInfo getPlayerInfo(String playerName) {
        PlayerInfo pi = null;
        _players_mutex.lock();
        try {
            pi = players.get(playerName.toLowerCase());
        } finally {
            _players_mutex.unlock();
        }
        return pi;
    }

    public void processRemoteMessage(ServerManager.Server server, Player player,
            String sender, String senderDisplayName, String message) {
        player.sendMessage(ChatColor.YELLOW + "["
                + ChatColor.GRAY + ChatColor.ITALIC + "{"
                + server.getServerId() + "} " + ChatColor.YELLOW + "<"
                + senderDisplayName + ChatColor.YELLOW + "> -> yo] "
                + ChatColor.WHITE + message);
        PlayerInfo senderPi = getPlayerInfo(sender);
        if (senderPi != null) {
            senderPi.sendNotification(ChatColor.YELLOW + "[yo -> "
                    + ChatColor.GRAY + ChatColor.ITALIC + "{"
                    + plugin.sm.getServerId() + "} " + ChatColor.YELLOW
                    + player.getDisplayName() + ChatColor.YELLOW + "] "
                    + ChatColor.WHITE + message);
            PlayerInfo recieverPi = getPlayerInfo(player.getName());
            if (recieverPi != null) {
                recieverPi.setLastPrivateChatContact(senderPi);
                senderPi.setLastPrivateChatContact(recieverPi);
            }
        }

    }

    public boolean isConnectedToThisServer(String playerName) {
        boolean isOnLine = false;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                isOnLine = true;
                break;
            }
        }

        return isOnLine;
    }

    public Player getOnlinePlayer(String playerName) {
        Player result = null;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                result = player;
                break;
            }
        }

        return result;
    }

    public void sendSyncMessage(final CommandSender cs, final String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            cs.sendMessage(message);
        });
    }

    public void sendSyncMessage(final CommandSender cs, final String message, final boolean prefix) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (prefix) {
                cs.sendMessage(plugin.getPrefix() + message);
            } else {
                cs.sendMessage(message);
            }
        });
    }

    public void playerLeft(Player player) {
        _players_mutex.lock();
        try {
            PlayerInfo pi = players.remove(player.getName());
            if (pi != null) {
                pi.setTotalPlayedMs((int)(new Date().getTime() - pi.getLoggedInTime()));
                if (player.isBanned()) {
                    pi.setBanned(true);
                    pi.setBanReason(plugin.essentials.getBanReason(player.getName()));
                }
                if (pi.isBanned() || pi.getTotalPlayedMs() > 60000) {
                    plugin.xrm.storePlayerInfo(pi);
                }
            }
        } finally {
            _players_mutex.unlock();
        }

    }

    public void syncPlayerKick(final Player player, final String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.kickPlayer(message);
        });
    }

    public void announce(PlayerJoinEvent e) {
        if (e.getPlayer().hasPermission("lnm.announce")) {
            enterExitAnnounce(e.getPlayer(), true);
        }
        e.setJoinMessage("");
    }

    public void announce(PlayerQuitEvent e) {
        if (e.getPlayer().hasPermission("lnm.announce")) {
            enterExitAnnounce(e.getPlayer(), false);
        }
        e.setQuitMessage("");
    }

    private void enterExitAnnounce(Player player, boolean enter) {
        String announce;
        if (player.hasPermission("lnm.staff-member")) {
            announce = "El miembro del staff #PLAYER#";
        } else {
            announce = "El jugador #PLAYER#";
        }
        if (enter) {
            announce = announce + ChatColor.YELLOW + " ha llegado.";
        } else {
            announce = announce + ChatColor.YELLOW + " se ha ido.";
        }
        final String announceFinal = announce;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String announString = announceFinal.replace("#PLAYER#", player.getDisplayName());
            for (Player player1 : plugin.getServer().getOnlinePlayers()) {
                player1.sendMessage(plugin.getPrefix() + announString);
            }
            plugin.getLogger().info(ChatColor.stripColor(announString));
        }, 10);
    }
}
