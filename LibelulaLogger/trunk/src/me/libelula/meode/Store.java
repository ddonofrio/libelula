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
import java.util.Comparator;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

/**
 * Class Store of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Store {

    protected class BlockEvent implements Comparable<BlockEvent> {

        boolean placed;
        int blockTypeID;
        Location location;
        byte blockData;
        long eventTime;
        String playerName;
        String aditionalData;

        public BlockEvent() {
            playerName = null;
            aditionalData = null;
        }

        @Override
        public String toString() {
            return "placed:" + placed + ","
                    + "blockTypeID:" + blockTypeID + ", "
                    + "blockPosX:" + location.getBlockX() + ", "
                    + "blockPosY:" + location.getBlockY() + ", "
                    + "blockPosZ:" + location.getBlockZ() + ", "
                    + "blockPosWorldID:" + location.getWorld().getName() + ", "
                    + "blockData:" + (int) blockData + ", "
                    + "eventTime:" + eventTime + ", "
                    + "playerName:" + playerName + ", "
                    + "aditionalData:" + aditionalData;
        }

        @Override
        public int compareTo(BlockEvent o) {
            if (location.getBlockY() != o.location.getBlockY()) {
                return location.getBlockY() - o.location.getBlockY();
            }

            if (!location.getWorld().equals(o.location.getWorld())) {
                return location.getWorld().getUID().compareTo(o.location.getWorld().getUID());
            }

            if (placed != o.placed) {
                return o.placed ? 1 : -1;
            }

            if (location.getBlockZ() != o.location.getBlockZ()) {
                return location.getBlockZ() - o.location.getBlockZ();
            }

            return (int) (eventTime - o.eventTime);
        }
    }

    public class BlockEventsTimeComparator implements Comparator<BlockEvent> {

        @Override
        public int compare(BlockEvent o1, BlockEvent o2) {
            if (o1.location.equals(o2.location)) {
                return (int) (o2.eventTime - o1.eventTime);
            } else {
                if (!o1.location.getWorld().equals(o2.location.getWorld())) {
                    return o1.location.getWorld().getUID().compareTo(o2.location.getWorld().getUID());
                }
                if (o1.location.getBlockX() != o2.location.getBlockX()) {
                    return o1.location.getBlockX() - o2.location.getBlockX();
                }
                if (o1.location.getBlockY() != o2.location.getBlockY()) {
                    return o1.location.getBlockY() - o2.location.getBlockY();
                }
                return o1.location.getBlockZ() - o2.location.getBlockZ();
            }
        }
    }

    public class LocationComparator implements Comparator<Location> {

        @Override
        public int compare(Location o1, Location o2) {
            if (!o1.getWorld().equals(o2.getWorld())) {
                return o1.getWorld().getUID().compareTo(o2.getWorld().getUID());
            }
            if (o1.getBlockX() != o2.getBlockX()) {
                return o1.getBlockX() - o2.getBlockX();
            }
            if (o1.getBlockY() != o2.getBlockY()) {
                return o1.getBlockY() - o2.getBlockY();
            }
            return o1.getBlockZ() - o2.getBlockZ();
        }
    }
    public static int rowSize = 17;

    public static String getTableName(long worldHalfUID, int Z) {
        return Long.toHexString(worldHalfUID).concat("-" + Z % 32);
    }

    public static String getTableName(String worldName, int Z) {
        return worldName.concat("-" + Z % 32);
    }

    public static String getRowName(int Y, boolean placed) {
        return Y + (placed ? ".p" : ".r");
    }

    public static String getSuperTableName(long eventTime) {
        return new SimpleDateFormat("yyyy-MM-dd").format(eventTime);
    }

    public static World getWorld(Server server, long leastSignBits) {
        for (Iterator<World> it = server.getWorlds().iterator(); it.hasNext();) {
            World world = it.next();
            if (world.getUID().getLeastSignificantBits() == leastSignBits) {
                return world;
            }
        }
        return null;
    }
}
