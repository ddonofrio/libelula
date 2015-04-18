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

package me.libelula.atlantis;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Main extends JavaPlugin implements Listener {

    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void playerMove(PlayerMoveEvent e) {
        if (e.getTo().getBlockY() < 0) {
            if (e.getPlayer().getWorld().getName().equals("sea")) {
                this.getServer().dispatchCommand(e.getPlayer(), "spawn");
            }
        }
    }
    
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new AtlantisGenerator(this);
    }
    
}
