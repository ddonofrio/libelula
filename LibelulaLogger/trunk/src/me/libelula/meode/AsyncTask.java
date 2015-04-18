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

import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class AsyncTask of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class AsyncTask extends BukkitRunnable {

    private final Object[] objects;
    private final Plugin plugin;
    private final TaskType taskType;

    public AsyncTask(TaskType tasktype, Object[] objects, Plugin plugin) {
        this.objects = objects;
        this.plugin = plugin;
        this.taskType = tasktype;
    }

    public enum TaskType {

        SAVE_EVENTS, QUERY_EVENT, RESTORE_BLOCK, QUERY_SELECTION, EDIT_SELECTION, LOAD_FILTER
    }

    @Override
    public void run() {
        if (objects == null) {
            return;
        }
        if (objects.length == 0) {
            return;
        }
        if (plugin == null) {
            return;
        }

        switch (taskType) {
            case SAVE_EVENTS:
                if (objects.length != 2) {
                    return;
                }
                if (objects[0] instanceof RAMStore && objects[1] instanceof HDStore) {
                    RAMStore rams = (RAMStore) objects[0];
                    HDStore hds = (HDStore) objects[1];
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
                break;
            case QUERY_EVENT:
                if (objects.length != 6) {
                    return;
                }
                if (objects[0] instanceof MEODE
                        && objects[1] instanceof RAMStore
                        && objects[2] instanceof HDStore
                        && objects[3] instanceof Location
                        && objects[4] instanceof String
                        && objects[5] instanceof Boolean) {

                    MEODE.syncTellBlockInfo((MEODE) objects[0],
                            (RAMStore) objects[1],
                            (HDStore) objects[2],
                            (Location) objects[3],
                            (String) objects[4],
                            (boolean) objects[5]);
                }
                break;
            case RESTORE_BLOCK:

                if (objects.length != 3) {
                    return;
                }

                if (objects[0] instanceof MEODE
                        && objects[1] instanceof Location
                        && (objects[2] instanceof String || objects[2] == null)) {
                    try {
                        MEODE.syncRestoreBlock(plugin,
                                (MEODE) objects[0],
                                (Location) objects[1],
                                (String) objects[2]);
                    } catch (Exception ex) {
                        plugin.getLogger().severe(ex.toString());
                    }
                }
                break;
            case QUERY_SELECTION:
                if (objects.length != 5) {
                    return;
                }
                if (objects[0] instanceof MEODE
                        && objects[1] instanceof Location
                        && objects[2] instanceof Location
                        && objects[3] instanceof Player
                        && (objects[4] instanceof String || objects[4] == null)) {
                    MEODE meode = (MEODE) objects[0];
                    meode.syncQuerySellection((Location) objects[1],
                            (Location) objects[2],
                            (Player) objects[3],
                            (String) objects[4]);
                }
                break;
            case EDIT_SELECTION:
                if (objects.length != 6) {
                    return;
                }
                if (objects[0] instanceof MEODE
                        && objects[1] instanceof Location
                        && objects[2] instanceof Location
                        && objects[3] instanceof Player
                        && (objects[4] instanceof String || objects[4] == null)
                        && objects[5] instanceof Boolean) {
                    MEODE meode = (MEODE) objects[0];
                    meode.syncEditSellection((Location) objects[1],
                            (Location) objects[2],
                            (Player) objects[3],
                            (String) objects[4],
                            (boolean) objects[5]);
                }
                break;
            case LOAD_FILTER:
                if (objects[0] instanceof HDStore) {
                    HDStore hds = (HDStore) objects[0];
                    hds.loadBloomFilter();
                    plugin.getLogger().info("MEODE Database loaded.");
                }
                break;
        }
    }
}
