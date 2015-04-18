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
package me.libelula.capturethewool;

import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public final class EventManager {

    private final Main plugin;
    private final GameListeners gameEvents;
    private final TreeMap<Player, SetupListeners> playerSetup;

    /**
     *
     */
    public enum SetUpAction {

        RED_WIN_WOOL, BLUE_WIN_WOOL, WOOL_SPAWNER
    }

    private class SetupListeners implements Listener {

        private final SetUpAction action;

        public SetupListeners(SetUpAction action) {
            this.action = action;
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            if (!playerSetup.containsKey(e.getPlayer())) {
                return;
            }
            Location currLoc;
            if (e.getBlock().getType() != Material.WOOL) {
                if (e.getBlock().getType() == Material.MOB_SPAWNER) {
                    Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
                    currLoc = plugin.mm.getWoolSpawnerLocation(e.getBlock().getWorld(), wool.getColor());
                    if (currLoc != null) {
                        plugin.mm.delWoolSpawner(e.getBlock());
                        plugin.lm.sendMessage("spawner-deleted", e.getPlayer());
                        return;
                    }
                }
                plugin.lm.sendMessage("not-a-wool", e.getPlayer());
                e.setCancelled(true);
                return;
            }

            Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
            currLoc = plugin.mm.getBlueWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
            if (currLoc != null) {
                plugin.lm.sendMessage("cappoint-deleted", e.getPlayer());
                plugin.mm.delBlueWoolWinPoint(e.getBlock());
                return;
            }

            currLoc = plugin.mm.getRedWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
            if (currLoc != null) {
                plugin.lm.sendMessage("cappoint-deleted", e.getPlayer());
                plugin.mm.delRedWoolWinPoint(e.getBlock());
                return;
            }

        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            if (!playerSetup.containsKey(e.getPlayer())) {
                return;
            }
            if (e.getBlock().getType() != Material.WOOL) {
                plugin.lm.sendMessage("not-a-wool", e.getPlayer());
                e.setCancelled(true);
                return;
            }
            Wool wool = new Wool(e.getBlock().getType(), e.getBlock().getData());
            Location currLoc;
            if (action == SetUpAction.BLUE_WIN_WOOL || action == SetUpAction.RED_WIN_WOOL) {
                currLoc = plugin.mm.getBlueWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.lm.getText("woolwin-already-blueteam")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
                currLoc = plugin.mm.getRedWoolWinLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.lm.getText("woolwin-already-redteam")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
            } else {
                currLoc = plugin.mm.getWoolSpawnerLocation(e.getBlock().getWorld(), wool.getColor());
                if (currLoc != null) {
                    e.getPlayer().sendMessage(plugin.lm.getText("spawner-already-exists")
                            .replace("%X%", currLoc.getBlockX() + "").replace("%Y%", currLoc.getBlockY() + "")
                            .replace("%Z%", currLoc.getBlockZ() + ""));
                    e.setCancelled(true);
                    return;
                }
            }

            switch (action) {
                case BLUE_WIN_WOOL:
                    if (plugin.mm.addBlueWoolWinPoint(e.getBlock())) {
                        plugin.lm.sendMessage("blue-wool-winpoint-placed", e.getPlayer());
                    }
                    break;
                case RED_WIN_WOOL:
                    if (plugin.mm.addRedWoolWinPoint(e.getBlock())) {
                        plugin.lm.sendMessage("red-wool-winpoint-placed", e.getPlayer());
                    }
                    break;
                case WOOL_SPAWNER:
                    if (plugin.mm.addwoolSpawner(e.getBlock())) {
                        e.getPlayer().sendMessage(plugin.lm.getText("spawner-placed")
                                .replace("%WOOL%", wool.getColor().toString()));
                    }
                    break;
            }
        }

    }

    private class GameListeners implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void PlayerBucketEmptyEvent(PlayerBucketEmptyEvent e) {
            plugin.gm.events.cancelUseBukketOnProtectedAreas(e);

        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeatherChange(WeatherChangeEvent e) {
            plugin.gm.ajustWeather(e);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerMove(PlayerMoveEvent e) {
            plugin.gm.denyEnterToProhibitedZone(e);
            if (!e.isCancelled()) {
                if (plugin.mm.isMap(e.getPlayer().getWorld())) {
                    plugin.mm.announceAreaBoundering(e);
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onRespawn(PlayerRespawnEvent e) {
            String roomName = plugin.rm.getRoom(e.getPlayer().getWorld());
            if (roomName != null) {
                TeamManager.TeamId ti = plugin.pm.getTeamId(e.getPlayer());
                if (ti == null) {
                    return;
                }
                switch (ti) {
                    case RED:
                        e.setRespawnLocation(plugin.gm.getRedSpawn(roomName));
                        plugin.pm.disguise(e.getPlayer(), TeamManager.TeamId.RED);
                        break;
                    case BLUE:
                        e.setRespawnLocation(plugin.gm.getBlueSpawn(roomName));
                        plugin.pm.disguise(e.getPlayer(), TeamManager.TeamId.BLUE);
                        break;
                    default:
                        return;
                }
                String mapName = plugin.rm.getCurrentMap(roomName);
                if (plugin.mm.getKitarmour(mapName)) {
                    ItemStack air = new ItemStack(Material.AIR);
                    e.getPlayer().getInventory().setBoots(air);
                    e.getPlayer().getInventory().setChestplate(air);
                    e.getPlayer().getInventory().setHelmet(air);
                    e.getPlayer().getInventory().setLeggings(air);
                }
                Inventory startingKit = plugin.mm.getKit(mapName);
                if (startingKit != null) {
                    e.getPlayer().getInventory().setContents(startingKit.getContents());
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onDeath(PlayerDeathEvent e) {
            plugin.tm.manageDeath(e);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onSignChange(SignChangeEvent e) {
            plugin.sm.checkForGameInPost(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onNameTag(AsyncPlayerReceiveNameTagEvent e) {
            Player player = e.getNamedPlayer();
            e.setTag(plugin.pm.getChatColor(player) + player.getName());
        }

        @EventHandler(ignoreCancelled = true)
        public void onItemSpawnEvent(ItemSpawnEvent e) {
            plugin.tm.onArmourDrop(e);
            if (!e.isCancelled()) {
                if (plugin.rm.isInGame(e.getEntity().getWorld())) {
                    if (plugin.rm.isProhibited(e.getEntity())) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
            if (plugin.pm.getTeamId(e.getPlayer()) != null) {
                if (!plugin.hasPermission(e.getPlayer(), "ingame-extra-cmds")) {
                    String cmd = e.getMessage().split(" ")[0].replaceFirst("/", "");
                    if (!plugin.cm.isAllowedInGameCmd(cmd)) {
                        e.setCancelled(true);
                        String errorMessage = ChatColor.RED + "/" + cmd + " " + plugin.lm.getText("disabled") + ".";
                        plugin.lm.sendMessage(errorMessage, e.getPlayer());
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerChat(AsyncPlayerChatEvent e) {
            plugin.tm.playerChat(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
        public void onPlayerInteract(PlayerInteractEvent e) {

            plugin.tm.cancelSpectator(e);

            if (e.isCancelled()) {
                return;
            }

            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                    && e.getClickedBlock().getType() == Material.WORKBENCH
                    && !e.getPlayer().isSneaking()) {
                if (plugin.pm.getTeamId(e.getPlayer()) != null) {
                    e.setCancelled(true);
                    e.getPlayer().openWorkbench(null, true);
                }

            }

            if (e.isCancelled()) {
                return;
            }

            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                    || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (e.getClickedBlock().getType() == Material.WALL_SIGN
                        || e.getClickedBlock().getType() == Material.SIGN_POST) {
                    plugin.sm.checkForPlayerJoin(e);

                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerDrop(PlayerDropItemEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
            if (e.isCancelled() && e.getEntity() instanceof Player) {
                TeamManager.TeamId teamId = plugin.pm.getTeamId((Player) e.getEntity());
                if (teamId != null && teamId != TeamManager.TeamId.SPECTATOR) {
                    e.setCancelled(false);
                }
            }

            if (!e.isCancelled()) {
                plugin.tm.cancelSpectatorOrSameTeam(e);
            }
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onPlayerFish(PlayerFishEvent e) {
            plugin.tm.cancelSameTeam(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
        public void onInventoryOpenEvent(InventoryOpenEvent e) {
            plugin.gm.cancelProtectedChest(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onInventoryClick(InventoryClickEvent e) {
            plugin.tm.cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.gm.checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerTeleport(PlayerTeleportEvent e) {
            if (!e.getFrom().getWorld().getName().equals(e.getTo().getWorld().getName())) {
                if (plugin.rm.isInGame(e.getTo().getWorld())) { // Getting in a game
                    final Player player = e.getPlayer();
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.gm.movePlayerTo(player, TeamManager.TeamId.SPECTATOR);
                        }
                    }, 5);
                } else {
                    if (plugin.rm.isInGame(e.getFrom().getWorld())) {
                        plugin.gm.playerLeftGame(e.getPlayer());
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent e) {
            if (plugin.rm.isInGame(e.getPlayer().getWorld())) {
                e.getPlayer().teleport(plugin.wm.getNextLobbySpawn());
            }
            e.setQuitMessage("");
            e.getPlayer().teleport(plugin.wm.getNextLobbySpawn());
            String leftMessage = plugin.lm.getText("left-message")
                    .replace("%PLAYER%", e.getPlayer().getDisplayName());
            for (Player player : plugin.wm.getLobbyWorld().getPlayers()) {
                plugin.lm.sendMessage(leftMessage, player);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onJoinEvent(final PlayerJoinEvent e) {
            World lobbyWorld = plugin.wm.getLobbyWorld();
            if (lobbyWorld == null) {
                if (plugin.hasPermission(e.getPlayer(), "setup")) {
                    final Player player = e.getPlayer();
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.lm.sendText("unconfigured-lobby", player);
                        }
                    }, 30);
                }
            } else {
                e.setJoinMessage("");

                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        e.getPlayer().teleport(plugin.wm.getNextLobbySpawn());
                    }
                }, 10);

                String joinMessage = plugin.lm.getText("join-message")
                        .replace("%PLAYER%", e.getPlayer().getDisplayName());
                for (Player player : plugin.wm.getLobbyWorld().getPlayers()) {
                    plugin.lm.sendMessage(joinMessage, player);
                }
                if (plugin.pm.getTeamId(e.getPlayer()) != null) {
                    plugin.gm.playerLeftGame(e.getPlayer());
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockPlaceEvent(BlockPlaceEvent e) {
            plugin.tm.cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.gm.events.cancelEditProtectedAreas(e);
            }
            if (e.isCancelled()) {
                plugin.gm.checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockBreakEvent(BlockBreakEvent e) {
            plugin.tm.cancelSpectator(e);
            plugin.gm.events.cancelEditProtectedAreas(e);
            if (!e.isCancelled()) {
                if (plugin.pm.getTeamId(e.getPlayer()) != null) {
                    for (Player other : e.getPlayer().getWorld().getPlayers()) {
                        if (other.getName().equals(e.getPlayer().getName())) {
                            continue;
                        }
                        if (other.getLocation().getBlockX() == e.getBlock().getLocation().getBlockX()
                                && other.getLocation().getBlockY() >= e.getBlock().getLocation().getBlockY()
                                && other.getLocation().getBlockY() < e.getBlock().getLocation().getBlockY() + 2
                                && other.getLocation().getBlockZ() == e.getBlock().getLocation().getBlockZ()
                                && e.getBlock().getType().isSolid()){

                            
                            
                            String spleafText = plugin.lm.getText("block-spleaf");

                            plugin.lm.sendVerbatimTextToWorld(
                                    spleafText.replace("%DAMAGER%",
                                            plugin.pm.getChatColor(e.getPlayer()) + e.getPlayer().getName())
                                    .replace("%VICTIM%",
                                            plugin.pm.getChatColor(other) + other.getName()), other.getWorld(), null);
                            break;
                        }
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPlayerPickupItem(PlayerPickupItemEvent e) {
            plugin.tm.cancelSpectator(e);
            if (!e.isCancelled()) {
                plugin.gm.checkTarget(e);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityTarget(EntityTargetEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onBlockDamage(BlockDamageEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onEntityDamage(EntityDamageEvent e
        ) {
            plugin.tm.cancelSpectator(e);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onFoodLevelChange(FoodLevelChangeEvent e) {
            plugin.tm.cancelSpectator(e);
            if (e.getEntity() instanceof Player == true && !e.isCancelled()) {
                Player player = (Player) e.getEntity();
                TeamManager.TeamId ti = plugin.pm.getTeamId(player);
                if (ti != null && player.getFoodLevel() > e.getFoodLevel()) {
                    if ((Math.random() * ((10) + 1)) > 4) {
                        e.setCancelled(true);
                    }
                }
            }
        }

    }

    public EventManager(Main plugin) {
        this.plugin = plugin;
        gameEvents = new GameListeners();
        playerSetup = new TreeMap<>(new Tools.PlayerComparator());
        registerGameEvents();
    }

    public void registerGameEvents() {
        plugin.getServer().getPluginManager().registerEvents(gameEvents, plugin);
    }

    public void registerSetupEvents(Player player, SetUpAction action) {
        unregisterSetUpEvents(player);
        SetupListeners sl = new SetupListeners(action);
        plugin.getServer().getPluginManager().registerEvents(sl, plugin);
        playerSetup.put(player, sl);
    }

    public void unregisterGameEvents() {
        HandlerList.unregisterAll(gameEvents);
    }

    public void unregisterSetUpEvents(Player player) {
        SetupListeners sl = playerSetup.remove(player);
        if (sl != null) {
            HandlerList.unregisterAll(sl);
        }
    }
}
