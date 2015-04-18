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

import java.util.TreeSet;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class SyncTask of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class SyncTask extends BukkitRunnable {

    private final Object[] objects;
    private final Plugin plugin;
    private final TaskType taskType;

    public enum TaskType {

        SEND_MESSAGE, RESTORE_BLOCK, RESTORE_CHUNK
    }

    public SyncTask(TaskType tasktype, Object[] objects, Plugin plugin) {
        this.objects = objects;
        this.plugin = plugin;
        this.taskType = tasktype;
    }

    public void sendSyncMessageToPlayer(String playerName, String message) {
        Object[] o = {playerName, message};
        new SyncTask(SyncTask.TaskType.SEND_MESSAGE, o, plugin).runTask(plugin);
    }

    public void callChunkRestore(TreeSet<Store.BlockEvent> chunk, boolean undo, int schedule) {
        Object[] o = {chunk, undo};
        new SyncTask(SyncTask.TaskType.RESTORE_CHUNK, o, plugin).runTaskLater(plugin, schedule);
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
            case SEND_MESSAGE:
                if (objects.length == 2) {
                    if (objects[0] instanceof String
                            && objects[1] instanceof String) {
                        Player player = plugin.getServer().getPlayer((String) objects[0]);
                        if (player != null) {
                            player.sendMessage((String) objects[1]);
                        }
                    }
                }
                break;
            case RESTORE_BLOCK:
                if (objects.length == 2) {
                    if ((objects[0] instanceof String || objects[0] == null)
                            && objects[1] instanceof Store.BlockEvent) {
                        Player player = null;
                        if (objects[0] != null) {
                            player = plugin.getServer().getPlayer((String) objects[0]);
                        }
                        MEODE.setBlockBEValue(player, (Store.BlockEvent) objects[1]);
                    }
                }
                break;
            case RESTORE_CHUNK:
                if (objects.length != 2) {
                    return;
                }
                if (objects[1] instanceof Boolean) {
                    MEODE.restoreChunk(plugin, (TreeSet<Store.BlockEvent>) objects[0], (boolean) objects[1]);
                }
                break;
        }
    }
}
