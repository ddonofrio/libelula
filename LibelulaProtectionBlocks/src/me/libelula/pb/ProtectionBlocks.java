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

import com.sk89q.worldedit.commands.UtilityCommands;
import com.sk89q.worldguard.bukkit.FlagStateManager;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import static org.bukkit.Material.JACK_O_LANTERN;
import static org.bukkit.Material.PUMPKIN;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class ProtectionBlocks of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class ProtectionBlocks {

    private final LibelulaProtectionBlocks plugin;
    private TreeMap<Material, Integer> compacPSDB;
    /**
     * Regular expression for matching new and old PS region names.
     */
    public static String regionIdRegexp = "^ps(-?[0-9]+)x(-?[0-9]+)y(-?[0-9]+)z";

    private class RegionManagerComparator implements Comparator<RegionManager> {

        @Override
        public int compare(RegionManager o1, RegionManager o2) {
            return o1.hashCode() - o2.hashCode();
        }
    }

    private class LocationComparator implements Comparator<Location> {

        @Override
        public int compare(Location o1, Location o2) {
            int resp;
            resp = o1.getWorld().getUID().compareTo(o2.getWorld().getUID());
            if (resp == 0) {
                resp = o1.getBlockX() - o2.getBlockX();
                if (resp == 0) {
                    resp = o1.getBlockY() - o2.getBlockY();
                    if (resp == 0) {
                        resp = o1.getBlockZ() - o2.getBlockZ();
                    }
                }
            }
            return resp;
        }
    }

    public class PSBlocks implements Comparable<PSBlocks> {

        Location location;
        ProtectedRegion region;
        Material material;
        boolean hidden;
        String name;
        List<String> lore;
        Byte materialData;
        int secondsFromEpoch;

        @Override
        public String toString() {
            String resp;
            if (location != null) {
                resp = this.location.getWorld().getName() + " "
                        + this.location.getBlockX() + " "
                        + this.location.getBlockY() + " "
                        + this.location.getBlockZ() + " ";
            } else {
                resp = "<NULL Location>";
            }
            if (material != null) {
                resp = resp + this.material.name() + " ";
            } else {
                resp = resp + "<NULL Material>";
            }
            resp = resp + this.hidden;

            return resp;
        }

        @Override
        public int compareTo(PSBlocks o) {
            return new LocationComparator().compare(o.location, location);
        }
    }
    private TreeMap<Location, PSBlocks> protectionBlockMap;
    private TreeMap<Location, Integer> dropCancellationSet;
    private final Lock _protectionBlock_mutex;
    private final Lock _dropCancellationSet_mutex;

    public ProtectionBlocks(LibelulaProtectionBlocks plugin) {
        protectionBlockMap = new TreeMap<>(new LocationComparator());
        dropCancellationSet = new TreeMap<>(new LocationComparator());
        _protectionBlock_mutex = new ReentrantLock(true);
        _dropCancellationSet_mutex = new ReentrantLock(true);
        this.compacPSDB = new TreeMap<>();
        this.plugin = plugin;
    }

    public void addDropEventCancellation(Location loc, int quantity) {
        _dropCancellationSet_mutex.lock();
        try {
            dropCancellationSet.put(loc, quantity);
        } finally {
            _dropCancellationSet_mutex.unlock();
        }
    }

    /**
     *
     * @param loc Location
     * @return true if the event location where marked for cancellation.
     */
    public boolean removeDropEventCancellation(Location loc) {
        boolean ret;
        _dropCancellationSet_mutex.lock();
        try {
            ret = dropCancellationSet.containsKey(loc);
            if (ret) {
                int remains = dropCancellationSet.remove(loc);
                if (remains > 0) {
                    dropCancellationSet.put(loc, remains);
                }
            }
        } finally {
            _dropCancellationSet_mutex.unlock();
        }
        return ret;
    }

    public void addProtectionBlock(Location location, ProtectedRegion region,
            Material material, boolean hidden, String name, List<String> lore,
            byte materialData, int secondsFromEpoch, boolean insert) {
        PSBlocks psb = new PSBlocks();
        psb.location = location;
        psb.region = region;
        psb.material = material;
        psb.hidden = hidden;
        psb.name = name;
        psb.lore = lore;
        psb.materialData = materialData;
        psb.secondsFromEpoch = secondsFromEpoch;
        _protectionBlock_mutex.lock();
        try {
            protectionBlockMap.put(location, psb);
        } finally {
            _protectionBlock_mutex.unlock();
        }
        if (insert) {
            TaskManager.addPSBlock(psb, plugin);
        }
    }

    public boolean removeProtectionBlock(Location location) {
        boolean resp;
        PSBlocks pbs = protectionBlockMap.get(location);
        _protectionBlock_mutex.lock();
        try {
            if (protectionBlockMap.remove(location) != null) {
                TaskManager.removeProtectionBlock(location, plugin);
                plugin.wgm.removeProtection(location.getWorld(), pbs.region);
                resp = true;
            } else {
                resp = false;
            }
        } finally {
            _protectionBlock_mutex.unlock();
        }
        return resp;
    }

    
    public boolean removeProtectionBlock(Location location, Player player) {
        PSBlocks pbs = protectionBlockMap.get(location);
        boolean resp = removeProtectionBlock(location);
        if (resp) {
            addDropEventCancellation(location, location.getBlock().getDrops().size());
        }

        ItemStack protectionBlock = plugin.pc.getItemStack(pbs);
        plugin.pc.addAvailableId(protectionBlock.getItemMeta().getLore().get(2));
        if (!player.getInventory().addItem(protectionBlock).isEmpty()) {
            location.getWorld().dropItem(location, protectionBlock);
        }
        return resp;
    }

    public void addProtectionBlock(PSBlocks psb) {
        _protectionBlock_mutex.lock();
        try {
            protectionBlockMap.put(psb.location, psb);
        } finally {
            _protectionBlock_mutex.unlock();
        }
        TaskManager.addPSBlock(psb, plugin);
    }

    public void addProtectionBlock(Location location, ProtectedRegion region,
            Material material, boolean hidden, String name, List<String> lore, Byte materialData,
            int secondsFromEpoch) {
        addProtectionBlock(location, region, material, hidden, name, lore, materialData, secondsFromEpoch, true);
    }

    public void load() {
        TaskManager.loadProtectionBlocks(plugin);
    }

    /**
     *
     * @param location
     * @return true if the specified location contains a protection stone.
     */
    public boolean contains(Location location) {
        return protectionBlockMap.containsKey(location);
    }

    /**
     *
     * @return the number of protection blocks.
     */
    public int size() {
        return protectionBlockMap.size();
    }

    public PSBlocks get(Location location) {
        return protectionBlockMap.get(location);
    }

    public boolean matches(Block block) {
        PSBlocks psb = get(block.getLocation());
        if (psb == null) {
            return false;
        }
        if (block.getType() != psb.material) {
            switch (block.getType()) {
                case REDSTONE_LAMP_ON:
                    if (psb.material == Material.REDSTONE_LAMP_OFF) {
                        return true;
                    }
                case GLOWING_REDSTONE_ORE:
                    if (psb.material == Material.REDSTONE_ORE) {
                        return true;
                    }
            }
        } else {
            switch (block.getType()) {
                case PUMPKIN:
                case JACK_O_LANTERN:
                    return true;
                default:
                    if (psb.materialData == block.getData()) {
                        return true;
                    }
                    break;
            }

        }
        return false;
    }

    public void addOldPsBlock(Material material, int size) {
        compacPSDB.put(material, size);
    }

    public TreeMap<Material, Integer> getoldPSs() {
        return compacPSDB;
    }

    public boolean oldPScontainsBlock(Material material) {
        return compacPSDB.containsKey(material);
    }

    public int oldPSgetSizeFor(Material material) {
        return compacPSDB.get(material);
    }

    public void oldPSPlace(BlockPlaceEvent e) {
    }

    public void newBlock(ProtectedCuboidRegion regionToProtect, Location location, Player player, ItemMeta dtMeta, Material material, Byte materialData) {

        RegionManager rm = plugin.wgm.getRegionManager(location.getWorld());

        if (rm.overlapsUnownedRegion(regionToProtect, plugin.wgm.wrapPlayer(player))) {
            returnBlock(location, player, dtMeta);
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("overlaps"));
            return;
        }

        if (plugin.config.ignoredWorldContains(location.getWorld())) {
            returnBlock(location, player, dtMeta);
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("ignored_world"));
            return;
        }

        if (protectionBlockMap.containsKey(location)) {
            returnBlock(location, player, dtMeta);
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("protection_over"));
            return;
        }

        int calculatedPriority = -1;
        for (ProtectedRegion pr : rm.getApplicableRegions(regionToProtect)) {
            if (pr.getPriority() > calculatedPriority) {
                calculatedPriority = pr.getPriority();
            }
        }

        regionToProtect.setPriority(calculatedPriority + 1);

        rm.addRegion(regionToProtect);
        player.sendMessage(ChatColor.GREEN + plugin.i18n.getText("pb_activated"));

        addProtectionBlock(location, regionToProtect, material, false,
                dtMeta.getDisplayName(), dtMeta.getLore(), materialData,
                (int) new Date().getTime() / 1000);

        try {
            rm.save();
        } catch (ProtectionDatabaseException ex) {
            Logger.getLogger(ProtectionBlocks.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            player.setItemInHand(new ItemStack(Material.AIR));
        }
        plugin.pc.removeAvailableId(dtMeta.getLore().get(2));
        new FlagsProcessor(plugin, location).runTaskAsynchronously(plugin);
    }

    private void returnBlock(Location location, Player player, ItemMeta dtMeta) {
        ItemStack is = new ItemStack(location.getBlock().getType(), 1, location.getBlock().getData());
        is.setItemMeta(dtMeta);
        if (player.getItemInHand().getType() == Material.AIR) {
            player.setItemInHand(is);
        } else if (player.getGameMode() == GameMode.CREATIVE) {
            // return nothing.
        } else {
            location.getWorld().dropItem(location, is);
        }
        location.getBlock().setType(Material.AIR);
    }

    public PSBlocks getPs(Location loc) {
        PSBlocks psb = null;
        ProtectedRegion region = plugin.wgm.getRealApplicableRegion(loc);
        if (region != null && region.getId().matches(regionIdRegexp)) {
            Pattern p = Pattern.compile("-?\\d+");
            Matcher m = p.matcher(region.getId());
            m.find();
            int x = Integer.parseInt(m.group());
            m.find();
            int y = Integer.parseInt(m.group());
            m.find();
            int z = Integer.parseInt(m.group());
            Location psLoc = new Location(loc.getWorld(), x, y, z);
            if (protectionBlockMap.containsKey(psLoc)) {
                psb = protectionBlockMap.get(psLoc);
            }
        }
        return psb;
    }

    @SuppressWarnings("unchecked")
    public void setFlag(Player player, Flag flag, String value) {
        PSBlocks psb = getPs(player.getLocation());
        if (psb == null) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_in_ps_area"));
            return;
        }
        if (!validatePlayerPermission(player, psb)) {
            return;
        }
        
        if (value == null) {
            psb.region.setFlag(flag, null);
            return;
        }
        switch (value.toLowerCase()) {
            case "allow":
                psb.region.setFlag(flag, StateFlag.State.ALLOW);
                break;
            case "deny":
                psb.region.setFlag(flag, StateFlag.State.DENY);
                break;
            default:
            switch (flag.getName().toLowerCase()) {
                case "farewell":
                case "greeting":
                    psb.region.setFlag(flag, value);
                default:
                    return;
            }
        }
    }

    private boolean validatePlayerPermission(Player player, PSBlocks psb) {
        if (psb.region.isOwner(player.getName()) || player.isOp()) {
            return true;
        }
        player.sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
        return false;
    }

    public void hide(Player player) {
        PSBlocks psb = getPs(player.getLocation());
        if (psb == null) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_in_ps_area"));
            return;
        }
        if (psb.hidden) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("already_hidden"));
            return;
        }
        if (!validatePlayerPermission(player, psb)) {
            return;
        }
        psb.location.getBlock().setType(Material.AIR);
        psb.hidden = true;
        TaskManager.updatePSBlocks(psb, plugin);
    }

    public void unhide(Player player, boolean force) {
        PSBlocks psb = getPs(player.getLocation());
        if (psb == null) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_in_ps_area"));
            return;
        }
        if (!psb.hidden && !force) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_hidden"));
            if (player.hasPermission("pb.unhide.force")) {
                player.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("not_hidden_force"));
            }
            return;
        }

        if (!validatePlayerPermission(player, psb)) {
            return;
        }

        psb.location.getBlock().setType(psb.material);
        if (psb.hidden) {
            psb.hidden = false;
            TaskManager.updatePSBlocks(psb, plugin);
        }
    }

    public PSBlocks addMember(Player player, String[] members) {
        ProtectionBlocks.PSBlocks psb = getPs(player.getLocation());
        if (psb == null) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_in_ps_area"));
            return null;
        }
        if (!psb.region.isOwner(player.getName()) && !player.hasPermission("pb.addmember.others")) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_owned_by_you"));
            return null;
        }
        plugin.wgm.addMembers(player.getWorld(), psb.region.getId(), members);
        return psb;
    }

    public PSBlocks removeMember(Player player, String[] members) {
        ProtectionBlocks.PSBlocks psb = getPs(player.getLocation());
        if (psb == null) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_in_ps_area"));
            return null;
        }
        if (!psb.region.isOwner(player.getName()) && !player.hasPermission("pb.removemember.others")) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_owned_by_you"));
            return null;
        }
        plugin.wgm.removeMembers(player.getWorld(), psb.region.getId(), members);
        return psb;
    }

    /**
     * Gets PSBlocks list owned by a player.
     *
     * @param owner
     * @return A list of Protection blocks owned by given player.
     */
    public PSBlocks[] getOwnedPSList(String owner) {
        TreeSet<PSBlocks> psbResult = new TreeSet<>();
        _protectionBlock_mutex.lock();
        try {
            for (Map.Entry<Location, PSBlocks> psb : protectionBlockMap.entrySet()) {
                if (psb.getValue().region.isOwner(owner.toLowerCase())) {
                    psbResult.add(psb.getValue());
                }
            }
        } finally {
            _protectionBlock_mutex.unlock();
        }
        return psbResult.toArray(new PSBlocks[0]);
    }

    public void removeAllPB(String playerName) {
        PSBlocks[] psbs = getOwnedPSList(playerName);
        TreeSet<RegionManager> rmsToSave = new TreeSet<>(new RegionManagerComparator());

        int qtty = psbs.length;
        for (PSBlocks psb : psbs) {
            RegionManager rm = plugin.wgm.getRegionManager(psb.location.getWorld());
            rm.removeRegion(psb.region.getId());
            TaskManager.removeProtectionBlock(psb.location, plugin);
            rmsToSave.add(rm);
            _protectionBlock_mutex.lock();
            try {
                protectionBlockMap.remove(psb.location);
            } finally {
                _protectionBlock_mutex.unlock();
            }
        }
        for (RegionManager rm : rmsToSave) {
            try {
                rm.save();
            } catch (Exception ex) {
                plugin.getLogger().severe(ex.toString());
            }
        }
        if (qtty > 0) {
            plugin.getLogger().log(Level.INFO, "Erased: {0} protection blocks owned by {1}", new Object[]{qtty, playerName});
        } else {
            plugin.getLogger().log(Level.INFO, "{0} has no protection blocks.", playerName);

        }
    }
}
