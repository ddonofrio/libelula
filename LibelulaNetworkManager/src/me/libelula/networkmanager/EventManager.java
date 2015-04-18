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

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class EventManager implements Listener {

    private final Main plugin;

    public EventManager(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (plugin.getConfig().getBoolean("survival-server")) {
            me.libelula.survival.EventManager survivalEventManager 
                    = new me.libelula.survival.EventManager(plugin);
            plugin.getServer().getPluginManager().registerEvents(survivalEventManager, plugin);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        plugin.pm.addPlayer(e.getPlayer());
        plugin.pm.announce(e);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.pm.announce(e);
        plugin.pm.playerLeft(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        plugin.pm.chatProcess(e);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCommandEvent(PlayerCommandPreprocessEvent e) {
        String[] splitedCommand = e.getMessage().split(" ");
        String commandName = splitedCommand[0].toLowerCase();
        switch (commandName) {
            case "/w":
            case "/t":
            case "/whisper":
            case "/tell":
                if (e.getPlayer().hasPermission("lnm.msg")
                        && splitedCommand.length >= 3) {
                    String playerName = splitedCommand[1];
                    if (!plugin.pm.isConnectedToThisServer(playerName)) {
                        PlayerManager.PlayerInfo pi = plugin.pm.getPlayerInfo(playerName);
                        if (pi != null) {
                            e.setCancelled(true);
                            pi.sendMessage(e.getPlayer(), e.getMessage().substring(e.getMessage().indexOf(splitedCommand[2])));
                        }
                    }
                }
                break;
            case "/r":
                if (e.getPlayer().hasPermission("lnm.msg")
                        && splitedCommand.length >= 2) {
                    PlayerManager.PlayerInfo senderInfo = plugin.pm.getPlayerInfo(e.getPlayer().getName());
                    if (senderInfo != null) {
                        PlayerManager.PlayerInfo recieverInfo = senderInfo.getLastPrivateChatContact();
                        if (recieverInfo != null) {
                            if (!plugin.pm.isConnectedToThisServer(recieverInfo.getName())) {
                                recieverInfo.sendMessage(e.getPlayer(), e.getMessage().substring(e.getMessage().indexOf(splitedCommand[1])));
                                e.setCancelled(true);
                            }
                        }
                    }
                }
                break;                
        }
    }

}
