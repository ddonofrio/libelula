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
package me.libelula.pb;

import com.sk89q.worldedit.BlockVector;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.TreeSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class FlagsProcessor of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class FlagsProcessor extends BukkitRunnable {
    
    private class BlockVectorComparator implements Comparator<BlockVector> {
        
        @Override
        public int compare(BlockVector o1, BlockVector o2) {
            int resp;
            resp = o1.getBlockX() - o2.getBlockX();
            if (resp == 0) {
                resp = o1.getBlockY() - o2.getBlockY();
                if (resp == 0) {
                    resp = o1.getBlockZ() - o2.getBlockZ();
                }
            }
            return resp;
        }
    }
    private final LibelulaProtectionBlocks plugin;
    private final Location location;
    
    public FlagsProcessor(LibelulaProtectionBlocks plugin, Location location) {
        this.location = location;
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        ProtectionBlocks.PSBlocks psb = plugin.pbs.get(location);
        if (psb == null) {
            return;
        }
        String[] flags = psb.lore.get(0).split("\\+");
        if (flags.length == 0) {
            return;
        }
        for (String flag : flags) {
            if (flag.equals("Fence")) {
                TreeSet<BlockVector> blockVectors = new TreeSet<>(new BlockVectorComparator());
                BlockVector bv;
                bv = psb.region.getMinimumPoint().setY(psb.location.getBlockY()).toBlockVector();
                bv.setY(psb.location.getBlockY());
                for (int x = psb.region.getMinimumPoint().getBlockX(); x <= psb.region.getMaximumPoint().getBlockX(); x++) {
                    blockVectors.add(new BlockVector(bv.setX(x)));
                }
                for (int z = psb.region.getMinimumPoint().getBlockZ(); z <= psb.region.getMaximumPoint().getBlockZ(); z++) {
                    blockVectors.add(new BlockVector(bv.setZ(z)));
                }
                
                bv = psb.region.getMaximumPoint().setY(psb.location.getBlockY()).toBlockVector();
                
                for (int x = psb.region.getMaximumPoint().getBlockX(); x >= psb.region.getMinimumPoint().getBlockX(); x--) {
                    blockVectors.add(new BlockVector(bv.setX(x)));
                }
                for (int z = psb.region.getMaximumPoint().getBlockZ(); z >= psb.region.getMinimumPoint().getBlockZ(); z--) {
                    blockVectors.add(new BlockVector(bv.setZ(z)));
                }
                TaskManager.putFence(plugin, psb.location.getWorld(), blockVectors);                
                psb.lore.set(0, "Libelula Protection Blocks");
                try {
                    plugin.sql.updatePSBlockInfo(psb);
                } catch (SQLException ex) {
                    plugin.getLogger().severe(ex.toString());
                }
            }
        }
        
    }
    
    public static void putFence(LibelulaProtectionBlocks plugin, World world, TreeSet<BlockVector> blockVectors) {
        for (BlockVector bv : blockVectors) {
            Location loc = new Location(world, bv.getBlockX(), bv.getBlockY(), bv.getBlockZ());
            if (loc.getBlock().getType() == Material.AIR) {
                loc.getBlock().setType(Material.FENCE);
            }
        }
    }
}
