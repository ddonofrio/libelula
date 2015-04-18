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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import static org.bukkit.Material.BED_BLOCK;
import static org.bukkit.Material.IRON_DOOR_BLOCK;
import static org.bukkit.Material.SIGN_POST;
import static org.bukkit.Material.WALL_SIGN;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

/**
 * Class RAMStore of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class RAMStore extends Store {

    private final Plugin plugin;
    private TreeSet<BlockEvent> blockEvents;
    private TreeSet<Location> ramFilter;
    private final Lock _blockEvents_mutex;
    private final Lock _ramQuery_mutex;

    public RAMStore(Plugin plugin, HDStore hds) {
        this.plugin = plugin;

        blockEvents = new TreeSet<>();
        ramFilter = new TreeSet<>(new LocationComparator());
        _blockEvents_mutex = new ReentrantLock(true);
        _ramQuery_mutex = new ReentrantLock(true);
    }

    public int getEventsInMemory() {
        return blockEvents.size();
    }

    public boolean filterContains(Location loc) {
        return ramFilter.contains(loc);
    }

    public void storeEvent(BlockBreakEvent e) {
        BlockEvent be = getBlockEventData(e.getBlock());
        BlockEvent beAux;
        be.placed = false;
        be.playerName = e.getPlayer().getName();
        Block blockAux;
        switch (e.getBlock().getType()) {
            case SIGN_POST:
            case WALL_SIGN:
                Sign sign = (Sign) e.getBlock().getState();
                be.aditionalData =
                        Auxiliary.centerString(sign.getLine(0), 15)
                        .concat(Auxiliary.centerString(sign.getLine(1), 15))
                        .concat(Auxiliary.centerString(sign.getLine(2), 15))
                        .concat(Auxiliary.centerString(sign.getLine(3), 15));
                break;
            case WOODEN_DOOR:
            case IRON_DOOR_BLOCK:
            case BED_BLOCK:
                if (e.getBlock().getType() == BED_BLOCK) {
                    blockAux = Auxiliary.getOtherBedBlock(e.getBlock());
                } else {
                    blockAux = Auxiliary.getOtherDoorBlock(e.getBlock());
                }
                beAux = getBlockEventData(blockAux);
                if (beAux.blockTypeID != be.blockTypeID) {
                    plugin.getLogger().log(Level.WARNING,
                            "Invalid block @(X={0} Y={1} Z={2}):{3} {4}",
                            new Object[]{be.location.getBlockX(),
                        be.location.getBlockY(),
                        be.location.getBlockZ(),
                        be.location.getWorld().getName(),
                        "A half of a "
                        .concat(e.getBlock().getType().toString())
                        .concat(" will not be logged.")});
                    return;
                }
                beAux.placed = false;
                beAux.playerName = e.getPlayer().getName();
                storeEvent(beAux);
                break;
            default:
                break;
        }
        storeEvent(be);
    }

    public void storeEvent(BlockPlaceEvent e) {
        BlockEvent be = getBlockEventData(e.getBlock());
        be.placed = true;
        be.playerName = e.getPlayer().getName();
        switch (e.getBlock().getType()) {
            case SIGN_POST:
            case WALL_SIGN:
                Sign sign = (Sign) e.getBlock().getState();
                be.aditionalData =
                        Auxiliary.centerString(sign.getLine(0), 15)
                        .concat(Auxiliary.centerString(sign.getLine(1), 15))
                        .concat(Auxiliary.centerString(sign.getLine(2), 15))
                        .concat(Auxiliary.centerString(sign.getLine(3), 15));
                break;
        }

        storeEvent(be);
    }

    private void storeEvent(BlockEvent blockEvent) {
        _blockEvents_mutex.lock();
        try {
            blockEvents.add(blockEvent);
            ramFilter.add(blockEvent.location);
        } finally {
            _blockEvents_mutex.unlock();
        }
    }

    private BlockEvent getBlockEventData(Block block) {
        BlockEvent be = new BlockEvent();
        be.blockData = block.getData();
        be.location = block.getLocation();
        be.blockTypeID = block.getTypeId();
        be.eventTime = new Date().getTime();
        return be;
    }

    public TreeSet<BlockEvent> getBlockEventsAndRotate() {
        TreeSet<BlockEvent> bes = blockEvents;
        _blockEvents_mutex.lock();
        try {
            blockEvents = new TreeSet<>();
            ramFilter.clear();
        } finally {
            _blockEvents_mutex.unlock();
        }
        return bes;
    }

    public String query(Location loc, boolean placed) {
        String result = null;
        BlockEvent be = getLastBlockEvent(loc, placed);
        if (be != null) {
            result = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(be.eventTime)
                    .concat(" ").concat(be.playerName)
                    .concat(be.placed ? " placed " : " removed ")
                    .concat(Material.getMaterial(be.blockTypeID).toString());
        }
        return result;
    }

    private TreeSet<BlockEvent> getExclusiveAccess() {
        _blockEvents_mutex.lock();
        TreeSet<BlockEvent> bes = blockEvents;
        try {
            blockEvents = new TreeSet<>();
        } finally {
            _blockEvents_mutex.unlock();
        }
        return bes;
    }

    private void returnEvents(TreeSet<BlockEvent> ramEvents) {
        _blockEvents_mutex.lock();
        try {
            ramEvents.addAll(blockEvents);
            blockEvents = ramEvents;
        } finally {
            _blockEvents_mutex.unlock();
        }
    }

    public BlockEvent getLastBlockEvent(Location loc, boolean placed) {
        BlockEvent result = null;
        if (!ramFilter.contains(loc)) {
            return null;
        }
        _ramQuery_mutex.lock();
        TreeSet<BlockEvent> bes = getExclusiveAccess();

        try {

            for (Iterator<BlockEvent> it = bes.descendingIterator(); it.hasNext();) {
                BlockEvent be = it.next();
                if (be.placed == placed && be.location.equals(loc)) {
                    result = be;
                    break;
                }
            }

        } finally {
            returnEvents(bes);
            _ramQuery_mutex.unlock();
        }
        return result;
    }

    public TreeSet<BlockEvent> getBlockEvents(Location locMin, Location locMax, String playerName) {
        TreeSet<BlockEvent> resultSet = new TreeSet<>();
        TreeSet<BlockEvent> bes = new TreeSet<>();

        _blockEvents_mutex.lock();
        try {
            bes.addAll(blockEvents);
        } finally {
            _blockEvents_mutex.unlock();
        }

        for (Iterator<BlockEvent> it = bes.descendingIterator(); it.hasNext();) {
            BlockEvent be = it.next();
            if (be.location.getWorld().equals(locMin.getWorld())
                    && be.location.getBlockX() >= locMin.getBlockX() && be.location.getBlockX() <= locMax.getBlockX()
                    && be.location.getBlockY() >= locMin.getBlockY() && be.location.getBlockY() <= locMax.getBlockY()
                    && be.location.getBlockZ() >= locMin.getBlockZ() && be.location.getBlockZ() <= locMax.getBlockZ()) {
                if (playerName != null) {
                    if (!playerName.equalsIgnoreCase(be.playerName)) {
                        continue;
                    }
                }
                resultSet.add(be);
            }
        }

        bes.clear();
        return resultSet;
    }
}
