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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class ProtectionController of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class ProtectionController {

    private final LibelulaProtectionBlocks plugin;

    public ProtectionController(LibelulaProtectionBlocks plugin) {
        this.plugin = plugin;
    }

    public static boolean isMaterialSuitable(ItemStack pb) {
        if (!pb.getType().isSolid()) {
            return false;
        }

        if (pb.getType().hasGravity()) {
            return false;
        }

        if (pb.getType().isEdible()) {
            return false;
        }

        switch (pb.getType()) {
            case DIRT:
            case GRASS:
            case ICE:
            case SNOW:
            case SNOW_BLOCK:
            case CACTUS:
            case PISTON_BASE:
            case PISTON_EXTENSION:
            case PISTON_MOVING_PIECE:
            case PISTON_STICKY_BASE:
            case FURNACE:
            case MYCEL:
            case LEAVES:
            case IRON_PLATE:
            case GOLD_PLATE:
                return false;
            default:
                break;
        }
        return true;
    }

    public boolean createPBFromItemsInHand(Player player, int x, int y, int z) {
        return createPBFromItemsInHand(player, x, y, z, null);
    }

    public boolean createPBFromItemsInHand(Player player, int x, int y, int z, String flags) {
        if (!player.getItemInHand().getType().isBlock()) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("pbs_must_be_created_from_a_block"));
            return false;
        }
        if (x == 0 || z == 0) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("x_z_cannot_be_zero"));
            return false;
        }

        if (player.getItemInHand().getAmount() != 1) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("only_one_block_in_hand"));
            return false;
        }

        if (!isMaterialSuitable(player.getItemInHand())) {
            player.sendMessage(ChatColor.RED + plugin.i18n.getText("block_not_suitable"));
            return false;
        }

        ItemMeta dtMeta = player.getItemInHand().getItemMeta();
        String yText = (y == 0 ? Character.toString('\u221E') : "" + y);
        dtMeta.setDisplayName(plugin.i18n.getText("protection") + ": " + x + " x " + yText + " x " + z + " (" + plugin.i18n.getText("blocks") + ")");
        List<String> lores = new ArrayList<>();
        if (flags == null) {
            lores.add("Libelula Protection Blocks");
        } else {
            if (flags.contains("f")) {
                lores.add("+Fence");
            }
        }
        lores.add(plugin.i18n.getText("created_by").concat(" ").concat(player.getName()));
        String hash = getFullHashFromValues(x, y, z, player.getItemInHand().getTypeId());
        lores.add(hash);
        dtMeta.setLore(lores);
        player.getItemInHand().setItemMeta(dtMeta);
        addAvailableId(hash);
        return true;
    }

    public ItemStack getItemStack(ProtectionBlocks.PSBlocks psb) {
        ItemStack is = new ItemStack(psb.material, 1, psb.materialData);
        ItemMeta dtMeta = is.getItemMeta();
        dtMeta.setDisplayName(psb.name);
        dtMeta.setLore(psb.lore);
        is.setItemMeta(dtMeta);
        return is;
    }

    public void addAvailableId(String hash) {
        TaskManager.addAvailableID(hash, plugin);
    }

    public void removeAvailableId(String hash) {
        TaskManager.removeAvailableID(hash, plugin);
    }

    public boolean containsSync(String hash) {
        try {
            return plugin.sql.isAvailableHashStored(hash);
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.toString());
            return true;
        }
    }

    public static String getFullHashFromValues(int x, int y, int z, int material) {
        return getFullHashFromValues(x, y, z, material, 0);
    }

    public static String getFullHashFromValues(int x, int y, int z, int material, int inc) {
        String hash = getHashFromValues(x, y, z, material)
                + Long.toHexString(new Date().getTime() + inc);
        return hash;
    }

    public static String getHashFromValues(int x, int y, int z, int material) {
        return Integer.toHexString((x * 1 + y * 2 + z * 3 + material * 4));
    }
}
