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
package me.libelula.lobby;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class SharedCommandManager {

    private final Main plugin;
    private final RandomAccessFile globalCmds;
    private final YamlConfiguration globalOffset;
    private final ReentrantLock _globals_mutex;
    private final File globalOffsetFile;

    public SharedCommandManager(Main plugin) throws FileNotFoundException {
        this.plugin = plugin;
        File globalCmdFile = new File(plugin.getConfig().getString("global-cmd-source"));
        if (!globalCmdFile.exists()) {
            try {
                globalCmdFile.createNewFile();
            } catch (IOException ex) {
                plugin.logErr(ex.toString());
            }
        }
        this.globalCmds = new RandomAccessFile(globalCmdFile, "r");
        globalOffsetFile = new File(plugin.getDataFolder(), "global-offset.yml");
        globalOffset = new YamlConfiguration();
        try {
            if (globalOffsetFile.exists()) {
                globalOffset.load(globalOffsetFile);
            } else {
                globalOffset.set("global-cmd-offset", globalCmds.length());
                globalOffset.save(globalOffsetFile);
            }
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.logErr(ex.toString());
        }
        this._globals_mutex = new ReentrantLock(true);
    }

    public void init() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                runGlobalCmds();
            } catch (IOException ex) {
                Logger.getLogger(SharedCommandManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, 100, 100);
    }

    public final void runGlobalCmds() throws IOException {
        _globals_mutex.lock();
        try {
            long offset = globalOffset.getLong("global-cmd-offset");
            if (globalCmds.length() > offset) {
                globalCmds.seek(offset);
                String line;
                int i = 1;
                do {
                    line = globalCmds.readLine();
                    if (line != null) {
                        final String lineFinal = line;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.getLogger().info("Executing global CMD: " + lineFinal);
                            plugin.getServer().dispatchCommand(
                                    plugin.getServer().getConsoleSender(), lineFinal);
                        }, i++);
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            if (player.isOp()) {
                                plugin.sendMessage(player, "Ejecutando comando remoto: " + line);
                            }
                        }
                    } else if (i > 1) {
                        offset = globalCmds.length();
                        globalOffset.set("global-cmd-offset", offset);
                        globalOffset.save(globalOffsetFile);
                        break;
                    }
                } while (true);
            }
        } finally {
            _globals_mutex.unlock();
        }
    }

}
