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

import com.sk89q.worldedit.bukkit.selections.Selection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public class Tools {

    public static class Chars {

        public static String check = "\u2714";
        public static String cross = "\u2715";
        public static String wool = "\u2752";
        public static String invisible = "\u200B";
    }

    public static class PlayerComparator implements Comparator<Player> {

        @Override
        public int compare(Player o1, Player o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class LocationBlockComparator implements Comparator<Location> {

        @Override
        public int compare(Location o1, Location o2) {
            int result = o1.getBlockX() - o2.getBlockX();
            if (result == 0) {
                result = o1.getBlockY() - o2.getBlockY();
                if (result == 0) {
                    result = o1.getBlockZ() - o2.getBlockZ();
                    if (result == 0) {
                        result = o1.getWorld().getName().compareTo(o2.getWorld().getName());
                    }
                }
            }

            return result;
        }

    }

    public static class WorldComparator implements Comparator<World> {

        @Override
        public int compare(World o1, World o2) {
            return o1.getName().compareTo(o2.getName());
        }

    }

    public static class SelectionComparator implements Comparator<Selection> {

        @Override
        public int compare(Selection o1, Selection o2) {
            Location o1Max = o1.getMaximumPoint();
            Location o1Min = o1.getMinimumPoint();
            Location o2Max = o2.getMaximumPoint();
            Location o2Min = o2.getMinimumPoint();
            int result;
            result = o1Max.getBlockX() - o2Max.getBlockX();
            if (result == 0) {
                result = o1Max.getBlockY() - o2Max.getBlockY();
                if (result == 0) {
                    result = o1Max.getBlockZ() - o2Max.getBlockZ();
                    if (result == 0) {
                        result = o1Min.getBlockX() - o2Min.getBlockX();
                        if (result == 0) {
                            result = o1Min.getBlockY() - o2Min.getBlockY();
                            if (result == 0) {
                                result = o1Min.getBlockZ() - o2Min.getBlockZ();
                                if (result == 0) {
                                    result = o1.getWorld().getName().compareTo(o2.getWorld().getName());
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
    }

    static void firework(Main plugin, final Location loc,
            final Color color1, final Color color2, final Color color3,
            final FireworkEffect.Type type) {
        final World world = loc.getWorld();
        new org.bukkit.scheduler.BukkitRunnable() {

            @Override
            public void run() {

                for (int i = -2; i < 3; i++) {
                    org.bukkit.entity.Firework firework = world.spawn(new org.bukkit.Location(loc.getWorld(), loc.getX() + (i * 5), loc.getY(), loc.getZ()), org.bukkit.entity.Firework.class);
                    org.bukkit.inventory.meta.FireworkMeta data = firework.getFireworkMeta();
                    data.addEffects(org.bukkit.FireworkEffect.builder()
                            .withColor(color1).withColor(color2).withColor(color3).with(type)
                            .trail(new java.util.Random().nextBoolean()).flicker(new java.util.Random().nextBoolean()).build());
                    data.setPower(new java.util.Random().nextInt(2) + 2);
                    firework.setFireworkMeta(data);
                }
            }
        }.runTaskLater(plugin, 10);
    }

    public static ChatColor toChatColor(DyeColor color) {
        ChatColor result;

        switch (color) {
            case BLACK:
                result = ChatColor.BLACK;
                break;
            case BLUE:
                result = ChatColor.DARK_BLUE;
                break;
            case BROWN:
                result = ChatColor.GOLD;
                break;
            case CYAN:
                result = ChatColor.AQUA;
                break;
            case GRAY:
                result = ChatColor.GRAY;
                break;
            case GREEN:
                result = ChatColor.DARK_GREEN;
                break;
            case LIGHT_BLUE:
                result = ChatColor.BLUE;
                break;
            case LIME:
                result = ChatColor.GREEN;
                break;
            case MAGENTA:
                result = ChatColor.DARK_PURPLE;
                break;
            case ORANGE:
                result = ChatColor.RED;
                break;
            case PINK:
                result = ChatColor.DARK_PURPLE;
                break;
            case PURPLE:
                result = ChatColor.LIGHT_PURPLE;
                break;
            case RED:
                result = ChatColor.DARK_RED;
                break;
            case SILVER:
                result = ChatColor.GRAY;
                break;
            case WHITE:
                result = ChatColor.WHITE;
                break;
            case YELLOW:
                result = ChatColor.YELLOW;
                break;
            default:
                result = ChatColor.WHITE;
        }
        return result;
    }

    public static String randomIdentifier() {
        // class variable
        final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";

        final java.util.Random rand = new java.util.Random();

// consider using a Map<String,Boolean> to say whether the identifier is being used or not 
        final Set<String> identifiers = new HashSet<String>();
        StringBuilder builder = new StringBuilder();
        while (builder.toString().length() == 0) {
            int length = rand.nextInt(5) + 5;
            for (int i = 0; i < length; i++) {
                builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
            }
            if (identifiers.contains(builder.toString())) {
                builder = new StringBuilder();
            }
        }
        return builder.toString();
    }

    public static String toString(Selection sel) {
        Location min = sel.getMinimumPoint();
        Location max = sel.getMaximumPoint();
        return "X:" + min.getBlockX() + ", Y:" + min.getBlockY()
                + ", Z:" + min.getBlockZ() + " -> X:" + max.getBlockX()
                + ", Y:" + max.getBlockY() + ", Z:" + max.getBlockZ();
    }

}
