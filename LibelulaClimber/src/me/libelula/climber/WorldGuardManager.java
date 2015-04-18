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
package me.libelula.climber;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import java.util.Comparator;
import java.util.logging.Level;
import org.bukkit.entity.Player;

/**
 * Class WorldGuardManager of the wgPlugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class WorldGuardManager {

    private final Plugin plugin;
    private WorldGuardPlugin wgp;

    public static class FlagComparator implements Comparator<Flag<?>> {

        @Override
        public int compare(Flag<?> o1, Flag<?> o2) {
            return o1.toString().compareTo(o2.toString());
        }
    }

    public WorldGuardManager(Plugin plugin) {
        this.plugin = plugin;
        initialize();
    }

    public boolean isWorldGuardActive() {
        if (wgp != null) {
            return true;
        } else {
            return false;
        }
    }

    private void initialize() {
        Plugin wgPlugin;
        wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        if (wgPlugin == null || !(wgPlugin instanceof WorldGuardPlugin)) {
            this.wgp = null;
            return;
        }
        wgp = (WorldGuardPlugin) wgPlugin;
    }

    /**
     *
     * @param world
     * @return map of regions, with keys being region IDs (lowercase)
     */
    public Map<String, ProtectedRegion> getRegions(World world) {
        return wgp.getRegionManager(world).getRegions();
    }

    public boolean isEnabled() {
        if (wgp != null) {
            return wgp.isInitialized() && wgp.isEnabled();
        } else {
            return false;
        }
    }

    /**
     * Gets the region manager for a world.
     *
     * @param world
     * @return the region manager or null if regions are not enabled
     */
    public RegionManager getRegionManager(World world) {
        return wgp.getRegionManager(world);
    }

    public LocalPlayer wrapPlayer(Player player) {
        return wgp.wrapPlayer(player);
    }

    /**
     * returns a calculated ProtectedCuboidRegion from given params.
     *
     * @param loc Location of the protection block
     * @param length The lenght of the region
     * @param height The height of the region
     * @param width The width of the reguion
     * @param playerName The Owner of the region.
     * @return ProtectedCuboidRegion
     */
    public ProtectedCuboidRegion getPBregion(Location loc, int length, int height, int width, String playerName) {

        BlockVector min = new BlockVector(loc.getBlockX() - ((length - 1) / 2),
                0,
                loc.getBlockZ() - ((width - 1) / 2));
        BlockVector max = new BlockVector(loc.getBlockX() + ((length - 1) / 2),
                255,
                loc.getBlockZ() + ((width - 1) / 2));

        if (height != 0) {
            min = min.setY(loc.getBlockY() - ((height - 1) / 2)).toBlockVector();
            max = max.setY(loc.getBlockY() + ((height - 1) / 2)).toBlockVector();
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion("ps"
                + loc.getBlockX() + "x"
                + loc.getBlockY() + "y"
                + loc.getBlockZ() + "z", min, max);

        DefaultDomain dd = new DefaultDomain();
        dd.addPlayer(playerName);
        region.setOwners(dd);
        return region;
    }

    public ProtectedRegion getRealApplicableRegion(Location loc) {
        ProtectedRegion region = null;
        for (ProtectedRegion pr : wgp.getRegionManager(loc.getWorld()).getApplicableRegions(loc)) {
            if (region == null || region.getPriority() < pr.getPriority()) {
                region = pr;
            }
        }
        return region;
    }

    /**
     * Adds a list of members to a region.
     *
     * @param world World were the region is.
     * @param regionName The ID of the region.
     * @param playerNames Players to be added.
     * @return true on succes, false on failure.
     */
    public boolean addMembers(World world, String regionName, String[] playerNames) {
        RegionManager rm = wgp.getRegionManager(world);
        if (rm == null) {
            return false;
        }
        ProtectedRegion region = rm.getRegion(regionName);
        if (region == null) {
            return false;
        }
        DefaultDomain members = region.getMembers();
        for (String playerName : playerNames) {
            members.addPlayer(playerName);
        }
        region.setMembers(members);
        try {
            rm.save();
        } catch (ProtectionDatabaseException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected failure saving WorldGuard configuration: {0}", ex.toString());
            return false;
        }
        return true;
    }

    /**
     * Removes a list of members from a region.
     *
     * @param world World were the region is.
     * @param regionName The ID of the region.
     * @param playerNames Players to be added.
     * @return true on succes, false on failure.
     */
    public boolean removeMembers(World world, String regionName, String[] playerNames) {
        RegionManager rm = wgp.getRegionManager(world);
        if (rm == null) {
            return false;
        }
        ProtectedRegion region = rm.getRegion(regionName);
        if (region == null) {
            return false;
        }
        DefaultDomain members = region.getMembers();
        for (String playerName : playerNames) {
            members.removePlayer(playerName);
        }
        region.setMembers(members);
        try {
            rm.save();
        } catch (ProtectionDatabaseException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected failure saving WorldGuard configuration: {0}", ex.toString());
            return false;
        }
        return true;
    }

    public boolean removeProtection(World world, ProtectedRegion pr) {
        RegionManager rm = wgp.getRegionManager(world);
        rm.removeRegion(pr.getId());
        try {
            rm.save();
        } catch (ProtectionDatabaseException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected failure saving WorldGuard configuration: {0}", ex.toString());
            return false;
        }
        return true;
    }
    
    public ProtectedRegion getRegionByName(World world, String name) {
        return wgp.getRegionManager(world).getRegion(name);
    }
}
