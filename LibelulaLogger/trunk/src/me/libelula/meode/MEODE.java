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
package me.libelula.meode;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import static org.bukkit.Material.BED_BLOCK;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

/**
 * Class MEODE of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class MEODE extends Store {

    private final int maxDbSizeMB;
    private final int maxEventInRAM;
    private final Plugin plugin;
    private final HDStore hds;
    private final RAMStore rams;
    public boolean debug;
    public SyncTask sync;
    List<Player> adminQuering;
    private final Lock _adminQuery_mutex;
    public static int dbEngineVersion = 1;

    /**
     *
     * @param dbPath Path to DB directory.
     * @param maxDbSizeMB Maximum amount of MB to store in disk before rotating
     * storage information.
     * @param maxEventInRAM Maximum amount of events in RAM before saving to
     * disk.
     * @param sendStats If it is true, anonymous statistic usage will be sent to
     * developer.
     * @param plugin main plugin.
     */
    public MEODE(String dbPath, int maxDbSizeMB, int maxEventInRAM, Plugin plugin, boolean debug) throws Exception {
        this.maxDbSizeMB = maxDbSizeMB;
        this.maxEventInRAM = maxEventInRAM;
        this.plugin = plugin;
        this.debug = debug;
        hds = new HDStore(dbPath, maxDbSizeMB, plugin);
        hds.debug = debug;
        rams = new RAMStore(plugin, hds);
        sync = new SyncTask(null, null, plugin);
        this.adminQuering = new ArrayList<>();
        _adminQuery_mutex = new ReentrantLock(true);
    }

    public boolean hasItBeenChanged(Block block) {
        return rams.filterContains(block.getLocation())
                || hds.filterContains(block.getLocation());
    }

    public void addEvent(BlockBreakEvent e) {
        rams.storeEvent(e);
        if (rams.getEventsInMemory() >= maxEventInRAM) {
            hds.peristRamAsynchronously(rams);
        }
    }

    public void addEvent(BlockPlaceEvent e) {
        rams.storeEvent(e);
        if (rams.getEventsInMemory() >= maxEventInRAM) {
            hds.peristRamAsynchronously(rams);
        }
    }

    public void persistRamSynchronously() {
        int eventsToSave = rams.getEventsInMemory();
        try {
            hds.peristRamSynchronously(rams);
            if (hds.debug) {
                plugin.getLogger().log(Level.INFO, "{0} events were stored in disk.", eventsToSave);
            }

        } catch (Exception ex) {
            plugin.getLogger().severe(ex.toString());
            plugin.getLogger().log(Level.SEVERE, "{0} events were lost.", eventsToSave);

        }
    }

    public void asyncTellBlockInfo(Block block, Player admin, Boolean placed) {
        if (!hasItBeenChanged(block)) {
            admin.sendMessage(ChatColor.RED + "There is no records about this block.");
        } else {
            Object objects[] = {this, rams, hds, block.getLocation(), admin.getName(), placed};
            new AsyncTask(AsyncTask.TaskType.QUERY_EVENT, objects, plugin).runTaskAsynchronously(plugin);
        }
    }

    public static void syncTellBlockInfo(MEODE meode, RAMStore rams, HDStore hds, Location loc, String adminName, boolean placed) {
        String result;

        result = rams.query(loc, placed);
        if (result != null) {
            meode.sync.sendSyncMessageToPlayer(adminName, ChatColor.GREEN + result);
        } else {
            try {
                result = hds.query(loc, placed);
            } catch (IOException | ClassNotFoundException | ParseException ex) {
                meode.sync.sendSyncMessageToPlayer(adminName, ChatColor.RED + "Internal server error while performing query.");
                meode.plugin.getLogger().severe(ex.toString());
                return;
            }
            if (result != null) {
                meode.sync.sendSyncMessageToPlayer(adminName, ChatColor.GREEN + result);
            } else {
                meode.sync.sendSyncMessageToPlayer(adminName, ChatColor.RED + "Information about this block is no longer available.");
            }
        }
    }

    public void asyncRestoreBlock(Block block, Player admin) {
        if (hasItBeenChanged(block)) {
            String adminName;
            if (admin != null) {
                adminName = admin.getName();
            } else {
                adminName = null;
            }
            Object objects[] = {this, block.getLocation(), adminName};
            new AsyncTask(AsyncTask.TaskType.RESTORE_BLOCK, objects, plugin).runTaskAsynchronously(plugin);
        }
    }

    public static void syncRestoreBlock(Plugin plugin, MEODE meode, Location loc, String adminName)
            throws IOException, ClassNotFoundException, ParseException {

        BlockEvent be = meode.rams.getLastBlockEvent(loc, false);
        if (be == null) {
            be = meode.hds.getLastBlockEvent(loc, false);
        }
        if (be != null) {

            Object[] objects = {adminName, be};
            new SyncTask(SyncTask.TaskType.RESTORE_BLOCK, objects, plugin).runTask(plugin);
        }
    }

    public void asyncQuerySellection(Location position, int radius, Player admin, String playerName) {
        Location locMin = new Location(position.getWorld(), position.getBlockX(), position.getBlockY(), position.getBlockZ());
        Location locMax = new Location(position.getWorld(), position.getBlockX(), position.getBlockY(), position.getBlockZ());
        locMin.subtract(radius, radius, radius);
        locMax.add(radius, radius, radius);

        asyncQuerySellection(locMin, locMax, admin, playerName);
    }

    public void asyncQuerySellection(Location locMin, Location locMax, Player admin, String playerName) {
        Object[] objects = {this, locMin, locMax, admin, playerName};
        new AsyncTask(AsyncTask.TaskType.QUERY_SELECTION, objects, plugin).runTaskAsynchronously(plugin);
    }

    public TreeSet<BlockEvent> querySellection(MEODE meode, Location locMin, Location locMax, String playerName) {
        TreeSet<BlockEvent> blockEventSet = rams.getBlockEvents(locMin, locMax, playerName);
        TreeSet<BlockEvent> hdBlockEventSet = null;
        try {
            hdBlockEventSet = hds.getBlockEvents(locMin, locMax, blockEventSet, playerName);
//        } catch (IOException | ClassNotFoundException | ParseException ex) {
        } catch (Exception ex) {
            plugin.getLogger().warning("MEODE failure on querySellection():".concat(ex.toString()));
        }
        if (hdBlockEventSet != null) {
            blockEventSet.addAll(hdBlockEventSet);
        }
        return blockEventSet;
    }

    public void syncQuerySellection(Location locMin, Location locMax, Player admin, String playerName) {
        _adminQuery_mutex.lock();
        boolean isAdminQuerying = adminQuering.contains(admin);
        _adminQuery_mutex.unlock();
        if (isAdminQuerying) {
            sync.sendSyncMessageToPlayer(admin.getName(),
                    ChatColor.RED + "You must wait current query finished before launching a new one.");
            return;
        } else {
            _adminQuery_mutex.lock();
            adminQuering.add(admin);
            _adminQuery_mutex.unlock();
        }
        TreeSet<BlockEvent> blockEventSet = querySellection(this, locMin, locMax, playerName);
        TreeMap<String, Long> playerChange = new TreeMap<>();
        for (Iterator<BlockEvent> it = blockEventSet.iterator(); it.hasNext();) {
            BlockEvent be = it.next();
            playerChange.put(be.playerName, be.eventTime);
        }
        for (Map.Entry<String, Long> entry : playerChange.entrySet()) {
            sync.sendSyncMessageToPlayer(admin.getName(), ChatColor.GREEN + entry.getKey()
                    + ": " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(entry.getValue()));
        }
        if (playerChange.isEmpty()) {
            sync.sendSyncMessageToPlayer(admin.getName(),
                    ChatColor.RED + "No records where found in this area");
        }
        _adminQuery_mutex.lock();
        adminQuering.remove(admin);
        _adminQuery_mutex.unlock();
    }

    public void asyncEditSellection(Location position, int radius, Player admin, String playerName, boolean undo) {
        Location locMin = new Location(position.getWorld(), position.getBlockX(), position.getBlockY(), position.getBlockZ());
        Location locMax = new Location(position.getWorld(), position.getBlockX(), position.getBlockY(), position.getBlockZ());
        locMin.subtract(radius, radius, radius);
        locMax.add(radius, radius, radius);
        asyncEditSellection(locMin, locMax, admin, playerName, undo);
    }

    public void asyncEditSellection(Location locMin, Location locMax, Player admin, String playerName, boolean undo) {

        Object[] objects = {this, locMin, locMax, admin, playerName, undo};
        new AsyncTask(AsyncTask.TaskType.EDIT_SELECTION, objects, plugin).runTaskAsynchronously(plugin);
    }

    public void syncEditSellection(Location locMin, Location locMax, Player admin, String playerName, boolean undo) {
        _adminQuery_mutex.lock();
        boolean isAdminQuerying = adminQuering.contains(admin);
        _adminQuery_mutex.unlock();
        if (isAdminQuerying) {
            sync.sendSyncMessageToPlayer(admin.getName(),
                    ChatColor.RED + "You must wait current query finished before launching a new one.");
            return;
        } else {
            _adminQuery_mutex.lock();
            adminQuering.add(admin);
            _adminQuery_mutex.unlock();
        }

        TreeSet<BlockEvent> blockEventSet = querySellection(this, locMin, locMax, playerName);
        TreeSet<BlockEvent> chunk = new TreeSet<>(new BlockEventsTimeComparator());
        int schedule = 0;
        for (Iterator<BlockEvent> it = blockEventSet.iterator(); it.hasNext();) {
            BlockEvent be = it.next();
            chunk.add(be);
            if (chunk.size() >= 50) {
                schedule++;
                sync.callChunkRestore(chunk, undo, schedule);
                chunk = new TreeSet<>(new BlockEventsTimeComparator());
            }
        }
        sync.callChunkRestore(chunk, undo, schedule + 1);
        sync.sendSyncMessageToPlayer(admin.getName(), ChatColor.AQUA + " " + blockEventSet.size() + (undo ? " blocks changes has been undone." : " blocks changes has been redone"));
        _adminQuery_mutex.lock();
        adminQuering.remove(admin);
        _adminQuery_mutex.unlock();
    }

    public static void restoreChunk(Plugin plugin, TreeSet<BlockEvent> chunk, boolean undo) {
        if (chunk.isEmpty()) {
            return;
        }
        World w = chunk.first().location.getWorld();

        if (undo) {
            for (Iterator<BlockEvent> it = chunk.iterator(); it.hasNext();) {
                BlockEvent be = it.next();
                Location loc = be.location;
                if (be.placed) {
                    loc.getBlock().setType(Material.AIR);
                } else {
                    setBlockBEValue(null, be);
                }
            }
        } else {
            for (Iterator<BlockEvent> it = chunk.descendingIterator(); it.hasNext();) {
                BlockEvent be = it.next();
                Location loc = be.location;
                if (be.placed) {
                    setBlockBEValue(null, be);
                } else {
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    private static void updateAditionalData(Block block, String aditionalData) {
        if (block.getType() == Material.WALL_SIGN
                || block.getType() == Material.SIGN_POST) {
            if (aditionalData != null) {
                Sign sign = (Sign) block.getState();
                sign.setLine(0, aditionalData.substring(0, 14));
                sign.setLine(1, aditionalData.substring(15, 29));
                sign.setLine(2, aditionalData.substring(30, 44));
                sign.setLine(3, aditionalData.substring(45, 59));
                sign.update(true);
            }
        }
    }

    public static void setBlockBEValue(Player admin, BlockEvent be) {

        Block block = be.location.getBlock();

        block.setTypeIdAndData(be.blockTypeID, be.blockData, true);
        updateAditionalData(block, be.aditionalData);

        switch (block.getType()) {
            case WOODEN_DOOR:
            case IRON_DOOR_BLOCK:
                Auxiliary.getOtherDoorBlock(block).setTypeIdAndData(block.getTypeId(), Auxiliary.getOtherDoorBlockData(block), true);
                break;
            case BED_BLOCK:
                Auxiliary.getOtherBedBlock(block).setTypeIdAndData(block.getTypeId(), Auxiliary.getOtherBedBlockData(block), true);
                break;
            case WALL_SIGN:
            case SIGN_POST:
                updateAditionalData(block, be.aditionalData);
        }
        if (admin != null) {
            admin.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + 
                    new SimpleDateFormat("yyyy-MM-dd HH:mm").format(be.eventTime)
                    .concat(" ").concat(be.playerName)
                    .concat(be.placed ? " placed " : " removed ")
                    .concat(Material.getMaterial(be.blockTypeID).toString()));
        }
    }
}
