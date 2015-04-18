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
package me.libelula.libelulalogger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class LogToTextFile of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class LogToTextFile extends BukkitRunnable {

    public enum types {
        SIGN
    }
    private final LibelulaLogger plugin;
    private final types type;
    private final Object[] objects;

    public LogToTextFile(LibelulaLogger plugin, types type, Object[] objects) {
        this.plugin = plugin;
        this.type = type;
        this.objects = objects;
    }

    @Override
    public void run() {
        switch (type) {
            case SIGN:
                if (objects.length != 1) {
                    return;
                }
                if (!(objects[0] instanceof SignChangeEvent)) {
                    return;
                }
                SignChangeEvent e = (SignChangeEvent) objects[0];

                if (plugin.config.ignoreEmptySigns() && e.getLine(0).isEmpty()
                        && e.getLine(1).isEmpty() && e.getLine(2).isEmpty()
                        && e.getLine(3).isEmpty()) {
                    return;
                }

                String logLine = e.getPlayer().getName() + "\t"
                        + e.getBlock().getLocation().getBlockX() + "\t"
                        + e.getBlock().getLocation().getBlockY() + "\t"
                        + e.getBlock().getLocation().getBlockZ() + "\t"
                        + "(" + e.getBlock().getLocation().getWorld().getName() + ")" + "\t"
                        + "\"" + e.getLine(0) + "\"\t\"" + e.getLine(1) + "\"\t\"" + e.getLine(2) + "\"\t\"" + e.getLine(3) + "\"\t";
                
                if (plugin.config.getLogTypeForSigns() == Configuration.logType.INTERNAL ||
                        plugin.config.getLogTypeForSigns() == Configuration.logType.BOTH) {
                    plugin.getLogger().info("New sign placed: " + logLine.replace("\t", " "));
                } 
                if (plugin.config.getLogTypeForSigns() == Configuration.logType.EXTERNAL ||
                        plugin.config.getLogTypeForSigns() == Configuration.logType.BOTH)  {
                    try {
                        try (PrintWriter signsLog = new PrintWriter(
                                new BufferedWriter(
                                new FileWriter(plugin.getDataFolder() + "/" + plugin.config.getSignsExternalLogFileName(), true)))) {
                            signsLog.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                                    + "\t" + logLine);
                            signsLog.close();
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().severe(ex.toString());
                    }
                }
        }
    }
}
