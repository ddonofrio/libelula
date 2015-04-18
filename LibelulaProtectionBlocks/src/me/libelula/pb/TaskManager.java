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
package me.libelula.pb;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class TaskManager of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class TaskManager extends BukkitRunnable {

    private final LibelulaProtectionBlocks plugin;
    private final Task task;
    private final Object[] objects;

    private enum Task {

        CHECK_FOR_BANNED, CHECK_PLACED_BLOCK, DEL_AVLB_ID_DB, DISABLE_OLDPS, IMPORT,
        INS_AVLB_ID_DB, INS_PSB_INTO_DB, LOAD_PBS, NEW_PROTECTION,
        REGISTER_COMMANDS, REMOVE_PROTECTION_BLOCK, RESTORE_BLOCK,
        UPDATE_PSBLOCKS, PUT_FENCE
    }

    private TaskManager(LibelulaProtectionBlocks plugin, Task task, Object[] objects) {
        this.plugin = plugin;
        this.task = task;
        this.objects = objects;
    }

    @Override
    public void run() {
        switch (task) {
            case DISABLE_OLDPS:
                if (objects.length == 1 && objects[0] instanceof Plugin) {
                    disableOldFashionedPluginSync((Plugin) objects[0]);
                    new TaskManager(plugin, Task.REGISTER_COMMANDS, null).runTask(plugin);
                }
                break;
            case REGISTER_COMMANDS:
                new Commands(plugin);
                break;
            case IMPORT:
                importFromPSWhenWGIsEnabled(plugin);
                break;
            case LOAD_PBS:
                try {
                    plugin.sql.loadPSBlocks(plugin.pbs);
                } catch (Exception ex) {
                    plugin.getLogger().severe("Error importing Protection Blocks from DB: ".concat(ex.toString()));
                }
                break;
            case INS_PSB_INTO_DB:
                if (objects.length == 1 && objects[0] instanceof ProtectionBlocks.PSBlocks) {
                    try {
                        plugin.sql.insertPSBlocks((ProtectionBlocks.PSBlocks) objects[0]);
                    } catch (SQLException ex) {
                        plugin.getLogger().severe("Error inserting Protection Blocks into DB: ".concat(ex.toString()));
                    }
                }
                break;
            case INS_AVLB_ID_DB:
                if (objects.length == 1 && objects[0] instanceof String) {
                    try {
                        plugin.sql.insertAvailableIDs((String) objects[0]);
                    } catch (SQLException ex) {
                        plugin.getLogger().severe("Error inserting Available ID into DB: ".concat(ex.toString()));
                    }
                }
                break;
            case CHECK_PLACED_BLOCK:
                if (objects.length == 6
                        && objects[0] instanceof ItemMeta
                        && objects[1] instanceof String
                        && objects[2] instanceof Material
                        && objects[3] instanceof Byte
                        && objects[4] instanceof Location
                        && objects[5] instanceof Integer) {
                    checkProtectionBlockPlaced((ItemMeta) objects[0], (String) objects[1],
                            (Material) objects[2], (Byte) objects[3], (Location) objects[4], (int) objects[5]);
                }
                break;
            case REMOVE_PROTECTION_BLOCK:
                if (objects.length == 1 && objects[0] instanceof Location) {
                    try {
                        plugin.sql.removePSBlocks((Location) objects[0]);
                    } catch (SQLException ex) {
                        plugin.getLogger().severe("Error removing Protection Blocks into DB: ".concat(ex.toString()));
                    }
                }
                break;
            case NEW_PROTECTION:
                if (objects.length == 6
                        && objects[0] instanceof ProtectedCuboidRegion
                        && objects[1] instanceof Location
                        && objects[2] instanceof String
                        && objects[3] instanceof ItemMeta
                        && objects[4] instanceof Material
                        && objects[5] instanceof Byte) {
                    Player player = plugin.getServer().getPlayer((String) objects[2]);
                    if (player == null) {
                        return;
                    }
                    plugin.pbs.newBlock((ProtectedCuboidRegion) objects[0], (Location) objects[1], player,
                            (ItemMeta) objects[3], (Material) objects[4], (Byte) objects[5]);
                }
                break;
            case DEL_AVLB_ID_DB:
                if (objects.length == 1 && objects[0] instanceof String) {
                    try {
                        plugin.sql.delAvailableIDs((String) objects[0]);
                    } catch (SQLException ex) {
                        plugin.getLogger().severe("Error deleting Available ID from DB: ".concat(ex.toString()));
                    }
                }
                break;
            case RESTORE_BLOCK:
                if (objects.length == 3 && objects[0] instanceof Location
                        && objects[1] instanceof Material
                        && objects[2] instanceof Byte) {
                    Location loc = (Location) objects[0];
                    Material mat = (Material) objects[1];
                    Byte data = (Byte) objects[2];
                    loc.getBlock().setTypeIdAndData(mat.getId(), data, true);
                }
                break;
            case UPDATE_PSBLOCKS:
                if (objects.length == 1 && objects[0] instanceof ProtectionBlocks.PSBlocks) {
                    try {
                        plugin.sql.updatePSBlocks((ProtectionBlocks.PSBlocks) objects[0]);
                    } catch (SQLException ex) {
                        plugin.getLogger().severe("Error updating Protection Block on DB: ".concat(ex.toString()));
                    }
                }
                break;
            case CHECK_FOR_BANNED:
                if (objects.length == 1 && objects[0] instanceof String) {
                    String playerName = (String) objects[0];
                    ProtectionBlocks.PSBlocks[] protectionList = plugin.pbs.getOwnedPSList(playerName);
                    if (protectionList.length > 0) {
                        plugin.getLogger().log(Level.INFO, "{0} {1} {2} {3} {4}", new Object[]{plugin.i18n.getText("banned_player"), playerName, plugin.i18n.getText("is_owner_of"), protectionList.length, plugin.i18n.getText("placed_protections")});
                        plugin.getLogger().log(Level.INFO, "{0} {1}", new Object[]{plugin.i18n.getText("remove_all_ps"), playerName});

                        for (Player onLinePlayer : plugin.getServer().getOnlinePlayers()) {
                            if (onLinePlayer.isOp()) {
                                onLinePlayer.sendMessage(ChatColor.YELLOW
                                        + plugin.i18n.getText("banned_player") + " " + playerName + " "
                                        + plugin.i18n.getText("is_owner_of") + " "
                                        + protectionList.length + " " + plugin.i18n.getText("placed_protections"));
                                onLinePlayer.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("remove_all_ps") + " " + playerName);
                            }

                        }

                    }
                }
                break;
            case PUT_FENCE:
                if (objects.length == 2 && objects[0] instanceof World
                        && objects[1] instanceof TreeSet) {
                    FlagsProcessor.putFence(plugin, (World) objects[0], (TreeSet<BlockVector>) objects[1]);
                }
                break;
        }
    }

    public static void disablePSAndLoadCommands(Plugin oldPS, LibelulaProtectionBlocks plugin) {
        Object[] objects = {oldPS};
        new TaskManager(plugin, Task.DISABLE_OLDPS, objects).runTask(plugin);
    }

    public static void registerCommands(LibelulaProtectionBlocks plugin) {
        new TaskManager(plugin, Task.REGISTER_COMMANDS, null).runTaskLater(plugin, 20);
    }

    private void disableOldFashionedPluginSync(Plugin oldPS) {
        if (oldPS.isEnabled()) {
            plugin.getServer().getPluginManager().disablePlugin(oldPS);
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW
                    + "Incompatible old fashioned ProtectionStones plugin disabled!");
        } else {
            plugin.getLogger().info("Old fashioned ProtectionStones plugin found but disabled.");
        }
    }

    public static void importFromPSWhenWGIsEnabled(LibelulaProtectionBlocks plugin) {
        if (plugin.wgm.isEnabled()) {
            Importer.importFromPS(plugin);
        } else {
            new TaskManager(plugin, Task.IMPORT, null).runTaskLater(plugin, 10);
        }
    }

    public static void loadProtectionBlocks(LibelulaProtectionBlocks plugin) {
        new TaskManager(plugin, Task.LOAD_PBS, null).runTaskAsynchronously(plugin);
    }

    public static void addPSBlock(ProtectionBlocks.PSBlocks psb, LibelulaProtectionBlocks plugin) {
        Object[] objects = {psb};
        new TaskManager(plugin, Task.INS_PSB_INTO_DB, objects).runTaskAsynchronously(plugin);
    }

    public static void addAvailableID(String hash, LibelulaProtectionBlocks plugin) {
        Object[] objects = {hash};
        new TaskManager(plugin, Task.INS_AVLB_ID_DB, objects).runTaskAsynchronously(plugin);
    }

    public static void removeAvailableID(String hash, LibelulaProtectionBlocks plugin) {
        Object[] objects = {hash};
        new TaskManager(plugin, Task.DEL_AVLB_ID_DB, objects).runTaskAsynchronously(plugin);
    }

    public static void protectionBlockPlaced(BlockPlaceEvent e, int values, LibelulaProtectionBlocks plugin) {
        ItemMeta dtMeta = e.getPlayer().getItemInHand().getItemMeta();
        String playerName = e.getPlayer().getName();
        Material material = e.getBlock().getType();
        Byte materialData = e.getBlock().getData();
        Location location = e.getBlock().getLocation();
        Object[] objects = {dtMeta, playerName, material, materialData, location, values};
        new TaskManager(plugin, Task.CHECK_PLACED_BLOCK, objects).runTaskAsynchronously(plugin);
    }

    private void checkProtectionBlockPlaced(ItemMeta dtMeta, String playerName,
            Material material, Byte materialData, Location location, int values) {
        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(dtMeta.getDisplayName());
        int x;
        int y;
        int z;
        if (values == 2) {
            m.find();
            x = Integer.parseInt(m.group());
            y = 0;
            m.find();
            z = Integer.parseInt(m.group());
        } else if (values == 3) {
            m.find();
            x = Integer.parseInt(m.group());
            m.find();
            y = Integer.parseInt(m.group());
            m.find();
            z = Integer.parseInt(m.group());
        } else {
            return;
        }
        if (dtMeta.getLore().size() != 3) {
            return;
        }
        String hash = ProtectionController.getHashFromValues(x, y, z, material.getId());
        if (!dtMeta.getLore().get(2).startsWith(hash)) {
            plugin.getLogger().log(Level.WARNING, "{0} " + plugin.i18n.getText("ruined_stone") + " ({1})",
                    new Object[]{playerName, hash});
            return;
        }

        if (!plugin.pc.containsSync(dtMeta.getLore().get(2))) {
            plugin.getLogger().log(Level.WARNING, "{0} " + plugin.i18n.getText("missing_stone") + " ({1})",
                    new Object[]{playerName, dtMeta.getLore().get(2)});
            return;
        }

        ProtectedCuboidRegion cuboidRegion = plugin.wgm.getPBregion(location, x, y, z, playerName);

        cuboidRegion.setFlags(plugin.config.getFlags(playerName));

        Object[] objects1 = {cuboidRegion, location, playerName, dtMeta, material, materialData};
        new TaskManager(plugin, Task.NEW_PROTECTION, objects1).runTask(plugin);

    }

    public static void removeProtectionBlock(Location location, LibelulaProtectionBlocks plugin) {
        Object[] objects = {location};
        new TaskManager(plugin, Task.REMOVE_PROTECTION_BLOCK, objects).runTaskAsynchronously(plugin);
    }

    public static void restoreBlock(Location loc, Material mat, Byte data, LibelulaProtectionBlocks plugin) {
        Object[] objects = {loc, mat, data};
        new TaskManager(plugin, Task.RESTORE_BLOCK, objects).runTask(plugin);
    }

    public static void updatePSBlocks(ProtectionBlocks.PSBlocks psb, LibelulaProtectionBlocks plugin) {
        Object[] objects = {psb};
        new TaskManager(plugin, Task.UPDATE_PSBLOCKS, objects).runTask(plugin);
    }

    public static void checkBannedForStones(LibelulaProtectionBlocks plugin, String playerName) {
        if (!plugin.bannedAdvicedPlayers.contains(playerName)) {
            plugin.bannedAdvicedPlayers.add(playerName);
            Object[] objects = {playerName};
            new TaskManager(plugin, Task.CHECK_FOR_BANNED, objects).runTask(plugin);
        }
    }

    public static void putFence(LibelulaProtectionBlocks plugin, World world, TreeSet<BlockVector> blockVectors) {
        Object[] objects = {world, blockVectors};
        new TaskManager(plugin, Task.PUT_FENCE, objects).runTask(plugin);
    }
}
