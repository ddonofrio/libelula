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

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public class GameManager {

    int counter;

    public class Events {

        private boolean isProhibitedLocation(Location location, TeamManager.TeamId ti, Game game) {
            boolean ret = false;
            if (ti != null && ti != TeamManager.TeamId.SPECTATOR) {
                switch (ti) {
                    case BLUE:
                        for (Selection sel : game.bluePhoibitedAreas) {
                            if (sel.contains(location)) {
                                ret = true;
                                break;
                            }
                        }
                        break;
                    case RED:
                        for (Selection sel : game.redPhoibitedAreas) {
                            if (sel.contains(location)) {
                                ret = true;
                                break;
                            }
                        }
                        break;
                }
            }
            return ret;
        }

        public void cancelEditProtectedAreas(BlockPlaceEvent e) {
            Game game = worldGame.get(e.getBlock().getWorld());
            if (game != null) {
                if (isProtected(e.getBlock(), game)) {
                    e.setCancelled(true);
                } else {
                    TeamManager.TeamId ti = plugin.pm.getTeamId(e.getPlayer());
                    if (isProhibitedLocation(e.getBlock().getLocation(), ti, game)) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        public void cancelEditProtectedAreas(BlockBreakEvent e) {
            Game game = worldGame.get(e.getBlock().getWorld());
            if (game != null) {
                if (isProtected(e.getBlock(), game)) {
                    e.setCancelled(true);
                } else {
                    TeamManager.TeamId ti = plugin.pm.getTeamId(e.getPlayer());
                    if (isProhibitedLocation(e.getBlock().getLocation(), ti, game)) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        public void cancelUseBukketOnProtectedAreas(PlayerBucketEmptyEvent e) {
            Game game = worldGame.get(e.getBlockClicked().getWorld());
            if (game != null) {
                if (isProtected(e.getBlockClicked(), game)) {
                    e.setCancelled(true);
                } else {
                    TeamManager.TeamId ti = plugin.pm.getTeamId(e.getPlayer());
                    if (isProhibitedLocation(e.getBlockClicked().getLocation(), ti, game)) {
                        e.setCancelled(true);
                    }
                }
            }
        }

        private boolean isProtected(Block block, Game game) {
            boolean ret = false;
            Location loc = block.getLocation();
            if (block.getType() == Material.MOB_SPAWNER) {
                ret = true;
            } else {
                if (game.restaurationArea != null && !game.restaurationArea.contains(loc)) {
                    ret = true;
                } else {
                    for (Selection sel : game.mapData.protectedAreas) {
                        loc.setWorld(sel.getWorld());
                        if (sel.contains(loc)) {
                            ret = true;
                            break;
                        }
                    }
                }

            }
            return ret;
        }
    }

    /**
     * Game information.
     */
    private class Target {

        TeamManager.TeamId team;
        DyeColor color;
        Location location;
        boolean completed;
    }

    public enum GameState {

        IN_GAME, FINISHED, WAITING_FOR_PLAYERS, NOT_IN_GAME
    }

    protected class Game {

        String roomName;
        int redPlayers;
        int bluePlayers;
        MapManager.MapData mapData;
        World world;
        TreeMap<Location, Target> targets;
        BukkitTask bt;
        int step;
        final TreeSet<Selection> bluePhoibitedAreas;
        final TreeSet<Selection> redPhoibitedAreas;
        private Selection restaurationArea;
        private Scoreboard board;
        private GameState state;

        public Game() {
            bluePhoibitedAreas = new TreeSet<>(new Tools.SelectionComparator());
            redPhoibitedAreas = new TreeSet<>(new Tools.SelectionComparator());
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            state = GameState.IN_GAME;
        }
    }

    private final Main plugin;
    private final TreeMap<String, Game> games;
    private final TreeMap<World, Game> worldGame;
    public final Events events;
    private final String decorator;

    public GameManager(Main plugin) {
        this.plugin = plugin;
        games = new TreeMap<>();
        events = new Events();
        worldGame = new TreeMap<>(new Tools.WorldComparator());
        decorator = plugin.getConfig().getString("message-decorator");

        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                spawnWool(games);
            }
        }, 300, 300);

        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                controlPlayers();
            }
        }, 40, 40);
    }

    private void controlPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.pm.getTeamId(player) == null) {
                if (plugin.mm.isMap(player.getWorld())
                        && !player.hasPermission("ctw.admin")) {
                    plugin.getLogger().log(Level.INFO, "Unexpected event: Player {0} has no team and was on {1}",
                            new Object[]{player.getName(), player.getWorld().getName()});
                    player.teleport(plugin.wm.getNextLobbySpawn());
                }
            } else {
                if (!plugin.rm.isInGame(player.getWorld())) {
                    plugin.getLogger().log(Level.INFO, "Unexpected event: Player {0} has team and was on {1}",
                            new Object[]{player.getName(), player.getWorld().getName()});
                    plugin.pm.clearTeam(player);
                }
            }
        }
    }

    public void movePlayerToRoom(Player player, String roomName) {
        World targetWorld = plugin.rm.getCurrentWorld(roomName);
        if (targetWorld != null) {
            player.teleport(targetWorld.getSpawnLocation());
        } else {
            plugin.lm.sendMessage("room-has-no-map", player);
        }
    }

    public boolean joinInTeam(Player player, TeamManager.TeamId teamId) {
        if (teamId == TeamManager.TeamId.SPECTATOR || plugin.hasPermission(player, "choseteam")) {
            movePlayerTo(player, teamId);
        } else {
            plugin.lm.sendMessage("not-teamselect-perm", player);
        }
        return true;
    }

    /**
     * Moves a player into a new team.
     *
     * @param player the player who must be moved into the new team.
     * @param teamId Id of the team where player must be put or null for random.
     */
    public void movePlayerTo(final Player player, TeamManager.TeamId teamId) {
        final String roomName = plugin.rm.getRoom(player.getWorld());
        if (roomName != null) {
            Game game = games.get(roomName);
            if (game == null) {
                plugin.getLogger().log(Level.WARNING, "Improvising non-created game: {0} (please report)", roomName);
                game = addGame(roomName);
            }

            if (teamId != TeamManager.TeamId.SPECTATOR && !plugin.hasPermission(player, "override-limit")
                    && getPlayersIn(roomName) >= game.mapData.maxPlayers) {
                plugin.lm.sendMessage("no-free-slots", player);
                return;
            }

            TeamManager.TeamId prevTeam = plugin.pm.getTeamId(player);
            if (prevTeam != null) {
                switch (prevTeam) {
                    case BLUE:
                        game.bluePlayers--;
                        break;
                    case RED:
                        game.redPlayers--;
                        break;
                }
            }

            final String advert;
            if (teamId == null) {
                if (game.redPlayers <= game.bluePlayers) {
                    teamId = TeamManager.TeamId.RED;
                } else {
                    teamId = TeamManager.TeamId.BLUE;
                }
            }

            switch (teamId) {
                case BLUE:
                    game.bluePlayers++;
                    advert = plugin.lm.getText("player-join-blue");
                    break;
                case RED:
                    advert = plugin.lm.getText("player-join-red");
                    game.redPlayers++;
                    break;
                default:
                    advert = plugin.lm.getText("player-join-spect");
            }
            plugin.pm.addPlayerTo(player, teamId);

            if (plugin.db != null) {
                final String playerName = player.getName();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.db.addEvent(playerName, "JOIN|" + roomName + "|" + advert);
                    }
                });
            }

            player.sendMessage(advert.replace("%PLAYER%", player.getName()));
            plugin.sm.updateSigns(roomName);
            takeToSpawn(player);
            player.setScoreboard(game.board);

            if (teamId != TeamManager.TeamId.SPECTATOR) {
                if (game.mapData.kitArmour) {
                    ItemStack air = new ItemStack(Material.AIR);
                    player.getInventory().setBoots(air);
                    player.getInventory().setChestplate(air);
                    player.getInventory().setHelmet(air);
                    player.getInventory().setLeggings(air);
                }

                if (game.mapData.kitInv != null) {
                    player.getInventory().setContents(game.mapData.kitInv.getContents());
                }
            }
        }
    }

    public void playerLeftGame(Player player) {
        final String roomName = plugin.rm.getRoom(player.getWorld());
        TeamManager.TeamId teamId = plugin.pm.getTeamId(player);
        if (roomName != null && teamId != null) {
            Game game = games.get(roomName);
            if (game == null) {
                plugin.getLogger().log(Level.WARNING, "Improvising non-created game: {0} (please report)", roomName);
                game = addGame(roomName);
            }
            switch (teamId) {
                case BLUE:
                    if (game.bluePlayers > 0) {
                        game.bluePlayers--;
                    }
                    break;
                case RED:
                    if (game.redPlayers > 0) {
                        game.redPlayers--;
                    }
                    break;
            }
        }

        plugin.lm.sendVerbatimTextToWorld(plugin.lm.getText("player-left-map")
                .replace("%PLAYER%", plugin.pm.getChatColor(player) + player.getName()), player.getWorld(), player);

        if (plugin.db != null && roomName != null) {
            final String playerName = player.getName();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.db.addEvent(playerName, "LEFT|" + roomName);
                }
            });
        }

        plugin.pm.clearTeam(player);
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            other.showPlayer(player);
        }
        if (roomName != null) {
            plugin.sm.updateSigns(roomName);
        }
    }

    public int getPlayersIn(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return 0;
        } else {
            return game.bluePlayers + game.redPlayers;
        }
    }

    public void checkForSpectator(Player player) {

        for (Player spectator : player.getWorld().getPlayers()) {
            if (plugin.pm.getTeamId(spectator) != TeamManager.TeamId.SPECTATOR) {
                continue;
            }
            if (player.getLocation().distance(spectator.getLocation()) < 4) {
                spectator.teleport(spectator.getLocation().add(0, 5, 0));
                spectator.setFlying(true);
            }
        }
    }

    public void denyEnterToProhibitedZone(PlayerMoveEvent e) {
        TeamManager.TeamId ti = plugin.pm.getTeamId(e.getPlayer());
        if (ti == null || ti == TeamManager.TeamId.SPECTATOR) {
            return;
        }
        String roomName = plugin.rm.getRoom(e.getPlayer().getWorld());
        if (roomName != null) {
            Game game = games.get(roomName);
            if (game != null) {
                switch (ti) {
                    case BLUE:
                        for (Selection sel : game.bluePhoibitedAreas) {
                            if (!sel.contains(e.getTo())) {
                                continue;
                            }
                            if (sel.contains(e.getFrom())) {
                                e.getPlayer().teleport(getBlueSpawn(roomName));
                            } else {
                                e.setCancelled(true);
                                e.getPlayer().teleport(e.getFrom());
                            }
                        }
                        checkForSpectator(e.getPlayer());
                        break;
                    case RED:
                        for (Selection sel : game.redPhoibitedAreas) {
                            if (!sel.contains(e.getTo())) {
                                continue;
                            }
                            if (sel.contains(e.getFrom())) {
                                e.getPlayer().teleport(getRedSpawn(roomName));
                            } else {
                                e.setCancelled(true);
                                e.getPlayer().teleport(e.getFrom());
                            }
                        }
                        checkForSpectator(e.getPlayer());
                        break;
                }
            }
        }
    }

    public Location getRedSpawn(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return null;
        }
        return new Location(game.world, game.mapData.redSpawn.getBlockX(),
                game.mapData.redSpawn.getBlockY(), game.mapData.redSpawn.getBlockZ());
    }

    public Location getBlueSpawn(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return null;
        }
        return new Location(game.world, game.mapData.blueSpawn.getBlockX(),
                game.mapData.blueSpawn.getBlockY(), game.mapData.blueSpawn.getBlockZ());
    }

    public GameState getState(String roomName) {
        Game game = games.get(roomName);
        if (game == null) {
            return GameState.NOT_IN_GAME;
        } else {
            return game.state;
        }
    }

    /**
     *
     * @param roomName
     * @return Game
     */
    protected Game addGame(String roomName) {
        Game game = new Game();
        game.roomName = roomName;

        game.mapData = plugin.mm.getMapData(plugin.rm.getCurrentMap(roomName));
        game.world = plugin.rm.getCurrentWorld(roomName);
        games.put(roomName, game);
        worldGame.put(game.world, game);
        game.targets = new TreeMap<>(new Tools.LocationBlockComparator());

        for (String color : game.mapData.redWoolWinPoints.keySet()) {
            Target t = new Target();
            t.color = DyeColor.valueOf(color);
            Location tempLoc = game.mapData.redWoolWinPoints.get(color);
            t.location = new Location(game.world, tempLoc.getBlockX(),
                    tempLoc.getBlockY(), tempLoc.getBlockZ());
            t.team = TeamManager.TeamId.RED;
            game.targets.put(t.location, t);
        }

        for (String color : game.mapData.blueWoolWinPoints.keySet()) {
            Target t = new Target();
            t.color = DyeColor.valueOf(color);
            Location tempLoc = game.mapData.blueWoolWinPoints.get(color);
            t.location = new Location(game.world, tempLoc.getBlockX(),
                    tempLoc.getBlockY(), tempLoc.getBlockZ());
            t.team = TeamManager.TeamId.BLUE;
            game.targets.put(t.location, t);
        }

        if (game.mapData.blueInaccessibleAreas != null) {
            for (Selection sel : game.mapData.blueInaccessibleAreas) {
                game.bluePhoibitedAreas.add(new CuboidSelection(game.world, sel.getNativeMinimumPoint(),
                        sel.getNativeMaximumPoint()));
            }
        }

        if (game.mapData.redInaccessibleAreas != null) {
            for (Selection sel : game.mapData.redInaccessibleAreas) {
                game.redPhoibitedAreas.add(new CuboidSelection(game.world, sel.getNativeMinimumPoint(),
                        sel.getNativeMaximumPoint()));
            }
        }

        if (game.mapData.restaurationArea != null) {
            game.restaurationArea = new CuboidSelection(game.world,
                    game.mapData.restaurationArea.getNativeMinimumPoint(),
                    game.mapData.restaurationArea.getNativeMaximumPoint());
        }

        updateScoreBoard(game);

        if (game.mapData.weather.fixed) {
            game.world.setStorm(game.mapData.weather.storm);
        }

        return game;
    }

    public void removeGame(String roomName) {
        games.remove(roomName);
    }

    public void takeToSpawn(Player player) {
        Game game = worldGame.get(player.getWorld());
        if (game != null) {
            TeamManager.TeamId teamId = plugin.pm.getTeamId(player);
            Location spawn;
            if (teamId != null) {
                switch (teamId) {
                    case BLUE:
                        spawn = game.mapData.blueSpawn;
                        break;
                    case RED:
                        spawn = game.mapData.redSpawn;
                        break;
                    default:
                        spawn = game.mapData.mapSpawn;
                }
                spawn.setWorld(game.world);
                player.teleport(spawn);
            }
        }
    }

    public void checkTarget(InventoryClickEvent e) {
        checkTarget((Player) e.getWhoClicked(), e.getCurrentItem());
    }

    public void checkTarget(PlayerPickupItemEvent e) {
        checkTarget(e.getPlayer(), e.getItem().getItemStack());
    }

    public void cancelProtectedChest(InventoryOpenEvent e) {
        Player player = (Player) e.getPlayer();
        Game game = worldGame.get(player.getWorld());
        if (game != null && (e.getInventory().getHolder() instanceof Chest
                || e.getInventory().getHolder() instanceof DoubleChest)) {
            TeamManager.TeamId teamId = plugin.pm.getTeamId(player);
            Location chestLocation;
            if (e.getInventory().getHolder() instanceof Chest) {
                Chest chest = (Chest) e.getInventory().getHolder();
                chestLocation = chest.getLocation();
            } else {
                DoubleChest chest = (DoubleChest) e.getInventory().getHolder();
                chestLocation = chest.getLocation();
            }
            switch (teamId) {
                case BLUE:
                    for (Selection sel : game.bluePhoibitedAreas) {
                        if (sel.contains(chestLocation)) {
                            e.setCancelled(true);
                            break;
                        }
                    }
                    break;
                case RED:
                    for (Selection sel : game.redPhoibitedAreas) {
                        if (sel.contains(chestLocation)) {
                            e.setCancelled(true);
                            break;
                        }
                    }
                    break;
            }
        }
    }

    public void checkTarget(Player player, ItemStack is) {
        Game game = worldGame.get(player.getWorld());
        if (game != null) {

            if (player.getInventory().containsAtLeast(is, 1)) {
                return;
            }
            if (is == null) {
                return;
            }
            if (is.getType() == Material.WOOL) {
                Wool wool = new Wool(is.getTypeId(), is.getData().getData());
                final String message = plugin.lm.getText("wool-pickup-message")
                        .replace("%PLAYER%", plugin.pm.getChatColor(player) + player.getName())
                        .replace("%WOOL%", Tools.toChatColor(wool.getColor()) + wool.getColor().name());
                switch (plugin.pm.getTeamId(player)) {
                    case BLUE:
                        for (String colorName : game.mapData.blueWoolWinPoints.keySet()) {
                            if (colorName.equals(wool.getColor().name())) {
                                plugin.lm.sendVerbatimMessageToTeam(message, player);
                                break;
                            }
                        }
                        break;
                    case RED:
                        for (String colorName : game.mapData.redWoolWinPoints.keySet()) {
                            if (colorName.equals(wool.getColor().name())) {
                                plugin.lm.sendVerbatimMessageToTeam(message, player);
                                break;
                            }
                        }
                        break;
                }

                if (plugin.db != null) {
                    final String playerName = player.getName();
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.db.addEvent(playerName, "WOOL-PICKUP|" + message);
                        }
                    });
                }

            }
        }
    }

    public void advanceGame(World world) {
        Game game = worldGame.get(world);
        if (game != null) {
            game.step = 66;
            startNewRound(game);
        }
    }

    public void checkTarget(BlockPlaceEvent e) {
        final Game game = worldGame.get(e.getBlock().getWorld());
        if (game != null) {
            final Target t = game.targets.get(e.getBlock().getLocation());

            if (t != null) {
                if (e.getBlock().getType() == Material.WOOL) {
                    Wool wool = new Wool(e.getBlock().getType(),
                            e.getBlock().getData());

                    if (wool.getColor() == t.color && t.team
                            == plugin.pm.getTeamId(e.getPlayer())) {

                        e.setCancelled(false);
                        t.completed = true;

                        if (!decorator.isEmpty()) {
                            plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                                    + decorator, e.getBlock().getWorld(), null);
                        }

                        final String winText = plugin.lm.getText("win-wool-placed")
                                .replace("%PLAYER%", e.getPlayer().getName())
                                .replace("%WOOL%", t.color.toString());
                        plugin.lm.sendVerbatimTextToWorld(winText, e.getBlock().getWorld(), null);
                        checkForWin(game);

                        if (!decorator.isEmpty()) {
                            plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                                    + decorator, e.getBlock().getWorld(), null);
                        }

                        Tools.firework(plugin, e.getBlock().getLocation(),
                                wool.getColor().getColor(), wool.getColor().getColor(), wool.getColor().getColor(),
                                FireworkEffect.Type.BALL_LARGE);
                        updateScoreBoard(game);
                        if (plugin.db != null) {
                            final String playerName = e.getPlayer().getName();
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    plugin.db.addEvent(playerName, "WOOL-CAPTURE|" + t.color.toString() + "|" + game.roomName + "|" + winText);
                                    plugin.db.incScore(playerName, plugin.scores.capture);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private void updateScoreBoard(Game game) {
        List<Target> redTarget = new ArrayList<>();
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = newBoard.registerNewObjective("wools", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(plugin.lm.getText("Wools"));
        int score = 3 + game.targets.size();
        OfflinePlayer op;
        op = plugin.getServer().getOfflinePlayer(plugin.lm.getText("Blue-Team"));
        objective.getScore(op).setScore(score--);
        for (Target target : game.targets.values()) {
            if (target.team == TeamManager.TeamId.RED) {
                redTarget.add(target);
            } else {
                String state;
                if (target.completed) {
                    state = ChatColor.GREEN + Tools.Chars.check;
                } else {
                    state = ChatColor.RED + Tools.Chars.cross;
                }
                String lineName = state + Tools.toChatColor(target.color) + " "
                        + Tools.Chars.wool + " " + ChatColor.WHITE
                        + target.color.toString();

                if (lineName.length() > 16) {
                    lineName = lineName.substring(0, 15);
                }

                op = plugin.getServer().getOfflinePlayer(lineName);
                objective.getScore(op).setScore(score--);
            }
        }
        op = plugin.getServer().getOfflinePlayer(ChatColor.AQUA + " ");
        objective.getScore(op).setScore(score--);
        op = plugin.getServer().getOfflinePlayer(plugin.lm.getText("Red-Team"));
        objective.getScore(op).setScore(score--);

        for (Target target : redTarget) {
            String state;
            if (target.completed) {
                state = ChatColor.GREEN + Tools.Chars.check;
            } else {
                state = ChatColor.RED + Tools.Chars.cross;
            }
            String lineName = state + Tools.toChatColor(target.color) + " "
                    + Tools.Chars.wool + " " + ChatColor.WHITE
                    + target.color.toString();

            if (lineName.length() > 16) {
                lineName = lineName.substring(0, 15);
            }

            op = plugin.getServer().getOfflinePlayer(lineName);
            objective.getScore(op).setScore(score--);
        }
        game.board = newBoard;
        for (Player player : game.world.getPlayers()) {
            player.setScoreboard(newBoard);
        }
    }

    private void checkForWin(Game game) {
        boolean redComplete = true;
        boolean blueComplete = true;
        for (Target target : game.targets.values()) {
            if (target.completed == false) {
                if (target.team == TeamManager.TeamId.BLUE) {
                    blueComplete = false;
                } else {
                    redComplete = false;
                }
            }
        }
        if (redComplete) {
            if (!decorator.isEmpty()) {
                plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                        + decorator, game.world, null);
            }
            plugin.lm.sendVerbatimTextToWorld(plugin.lm.getText("red-win-game"), game.world, null);
            
            if (plugin.cf.isTournament()) {
                plugin.trm.winner(game.roomName, TeamManager.TeamId.RED);
            }
            
            if (!decorator.isEmpty()) {
                plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                        + decorator, game.world, null);
            }
            game.step = 0;
            startNewRound(game);
        } else if (blueComplete) {
            if (!decorator.isEmpty()) {
                plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                        + decorator, game.world, null);
            }
            plugin.lm.sendVerbatimTextToWorld(plugin.lm.getText("blue-win-game"), game.world, null);
            
            if (plugin.cf.isTournament()) {
                plugin.trm.winner(game.roomName, TeamManager.TeamId.BLUE);
            }

            
            if (!decorator.isEmpty()) {
                plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                        + decorator, game.world, null);
            }
            game.step = 0;
            startNewRound(game);
        }
    }

    public void ajustWeather(WeatherChangeEvent e) {
        Game game = worldGame.get(e.getWorld());
        if (game != null) {
            if (game.mapData.weather.fixed) {
                if (e.toWeatherState() != game.mapData.weather.storm) {
                    e.getWorld().setStorm(game.mapData.weather.storm);
                }
            }
        }
    }

    private void startNewRound(final Game game) {
        game.state = GameState.FINISHED;

        for (Player player : game.world.getPlayers()) {
            if (plugin.getConfig().getBoolean("keep-teams-on-win")) {
                player.setGameMode(GameMode.ADVENTURE);
                plugin.pm.clearInventory(player);
                plugin.pm.setFalseSpectator(player);
                player.setAllowFlight(true);
                player.setFlying(true);
                if (plugin.getConfig().getBoolean("user-fireworks-on-win")) {
                    Tools.firework(plugin, player.getLocation(),
                            Color.GREEN, Color.RED, Color.BLUE,
                            FireworkEffect.Type.BALL_LARGE);
                }
                if (!player.isOnGround()) {
                    player.teleport(player.getLocation().add(0, 0.5, 0));
                }
            } else {
                plugin.pm.addPlayerTo(player, TeamManager.TeamId.SPECTATOR);
            }
        }

        counter++;
        game.bt = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    switch (game.step) {
                        case 5:
                            plugin.lm.sendMessageToWorld("thirty-seconds-to-start", game.world, null);
                            break;
                        case 30:
                            plugin.lm.sendMessageToWorld("fifteen-seconds-to-start", game.world, null);
                            break;
                        case 66:
                            plugin.lm.sendVerbatimTextToWorld(plugin.lm.getText("next-game-starts-in-five"), game.world, null);
                            break;
                        case 68:
                            plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                                    + "----------x 4 x----------", game.world, null);
                            break;
                        case 70:
                            plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                                    + "----------x 3 x----------", game.world, null);
                            break;
                        case 72:
                            plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                                    + "----------x 2 x----------", game.world, null);
                            break;
                        case 74:
                            plugin.lm.sendVerbatimTextToWorld(ChatColor.GOLD + "" + ChatColor.BOLD
                                    + "----------x 1 x----------", game.world, null);
                            break;
                        case 76:

                            final TreeMap<Player, TeamManager.TeamId> currentTeams = new TreeMap<>(new Tools.PlayerComparator());
                            plugin.rm.swapMap(game.roomName);
                            for (Player player : game.world.getPlayers()) {
                                TeamManager.TeamId teamId = plugin.pm.getTeamId(player);
                                currentTeams.put(player, teamId);
                                player.teleport(plugin.rm.getCurrentWorld(game.roomName).getSpawnLocation());
                            }

                            if (!plugin.cf.isTournament()) {
                                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        for (Player player : currentTeams.keySet()) {
                                            plugin.gm.movePlayerTo(player, currentTeams.get(player));
                                        }
                                    }
                                }, 10);
                            } else {
                                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        for (Player player : currentTeams.keySet()) {
                                            player.teleport(plugin.wm.getNextLobbySpawn());
                                        }
                                    }
                                }, 10);                                
                            }
                            plugin.gm.removeGame(game.roomName);
                            Game newGame = plugin.gm.addGame(game.roomName);
                            newGame.state = GameState.IN_GAME;
                            break;
                        case 77:
                            plugin.lm.sendMessageToWorld("starting-new-game", game.world, null);
                            break;
                        case 80:
                            break;
                        case 81:
                            game.bt.cancel();
                    }
                } finally {
                    game.step++;
                }
            }
        }, 10, 10);
    }

    private void spawnWool(TreeMap<String, Game> games) {
        for (Game game : games.values()) {
            if (game.mapData.woolSpawners != null) {
                for (String woolColor : game.mapData.woolSpawners.keySet()) {
                    DyeColor dyeColor = DyeColor.valueOf(woolColor);
                    Wool wool = new Wool(dyeColor);
                    ItemStack stack = wool.toItemStack(1);
                    Location loc = new Location(game.world,
                            game.mapData.woolSpawners.get(woolColor).getBlockX(),
                            game.mapData.woolSpawners.get(woolColor).getBlockY(),
                            game.mapData.woolSpawners.get(woolColor).getBlockZ());
                    for (Player player : game.world.getPlayers()) {
                        if (player.getLocation().distance(loc) <= 6
                                && !plugin.pm.isSpectator(player)) {
                            game.world.dropItem(loc, stack);
                        }
                    }
                }
            }
        }
    }
}
