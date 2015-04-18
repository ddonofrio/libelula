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

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.block.Block;

/**
 * Class Auxiliary of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Auxiliary {

    private Auxiliary() {
        throw new AssertionError();
    }

    /**
     *
     * @param text Text to be centered.
     * @param len The lenght of the returned string.
     * @return A text centered string.
     */
    public static String centerString(String text, int len) {
        String string = String.format("%" + len + "s%s%" + len + "s", "", text, "");
        return string.substring((string.length() / 2) - (len / 2),
                (string.length() / 2) - (len / 2) + len);
    }

    public static Block getOtherBedBlock(Block block) {
        //BigInteger data = BigInteger.valueOf(block.getData());
        /* 
         * 0x0: Head is pointing south
         * 0x1: Head is pointing west
         * 0x2: Head is pointing north
         * 0x3: Head is pointing east
         * 0x8: (bit flag) - When 0, the foot of the bed. When 1, the head of the bed.
         */
        int direction = Integer.valueOf(block.getData()) % 4;
        boolean head = (block.getData() >> 3) == 1;

        switch (direction) {
            case 0:
                return block.getRelative(0, 0, head ? -1 : 1);
            case 1:
                return block.getRelative(head ? 1 : -1, 0, 0);
            case 2:
                return (block.getRelative(0, 0, head ? 1 : -1));
            case 3:
                return (block.getRelative(head ? -1 : 1, 0, 0));
        }

        return null;
    }

    public static byte getOtherDoorBlockData(Block block) {
        byte result = block.getData();
        if ((block.getData() >> 3) == 1) {
            result = (byte) (result | (0 << 3));
        } else {
            result = (byte) (result | (1 << 3));            
        }
        return result;
    }
    
    public static byte getOtherBedBlockData(Block block) {
        int bitIndex = 3;
        byte result = block.getData();
        if ((block.getData() >> bitIndex) == 1) {
            result |= (0 << bitIndex);
        } else {
            result |= (1 << bitIndex);
        }        
        return result;
    }
    

    public static Block getOtherDoorBlock(Block block) {
        // 0x8: If this bit is set, this is the top half of a door (else the lower half).
        return block.getRelative(0,
                ((block.getData() >> 3) == 1) ? -1 : 1, 0);
    }

    public static short minutesFromMidnight(long dateTime) {
        Date date = new Date(dateTime - dateTime % (24 * 60 * 60 * 1000));
        return (short) (60 + (dateTime - date.getTime()) / 1000 / 60);
    }

    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        Set<T> keys = new HashSet<T>();
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }
}
