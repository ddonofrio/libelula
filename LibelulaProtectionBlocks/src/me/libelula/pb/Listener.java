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

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import static org.bukkit.block.BlockFace.DOWN;
import static org.bukkit.block.BlockFace.NORTH;
import static org.bukkit.block.BlockFace.WEST;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class Listener of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Listener implements org.bukkit.event.Listener {

    private final LibelulaProtectionBlocks plugin;

    public Listener(LibelulaProtectionBlocks plugin) {
        this.plugin = plugin;
    }

    private class PbSizes {

        int length;
        int height;
        int width;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getPlayer().getItemInHand().getItemMeta().getLore() != null
                && e.getPlayer().getItemInHand().getItemMeta().getLore().size() == 3
                && e.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null) {
            ItemMeta dtMeta = e.getPlayer().getItemInHand().getItemMeta();
            int valuesFound = 0;
            if (dtMeta.getDisplayName().matches("(.*)\\s(\\d+)\\sx\\s(\\d+)\\sx\\s(\\d+)(.*)")) {
                valuesFound = 3;
            } else if (dtMeta.getDisplayName().matches("(.*)\\s(\\d+)\\sx\\s" + '\u221E' + "\\sx\\s(\\d+)(.*)")) {
                valuesFound = 2;
            }
            if (valuesFound != 0) {
                TaskManager.protectionBlockPlaced(e, valuesFound, plugin);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(ItemSpawnEvent e) {
        if (plugin.pbs.removeDropEventCancellation(e.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (plugin.pbs.matches(e.getBlock())) {
            ProtectionBlocks.PSBlocks pbInfo = plugin.pbs.get(e.getBlock().getLocation());
            if (pbInfo.hidden) {
                return;
            }
            if (pbInfo.region.isOwner(e.getPlayer().getName())
                    || e.getPlayer().isOp() || e.getPlayer().hasPermission("pb.break.others")) {
                if (plugin.pbs.removeProtectionBlock(e.getBlock().getLocation(), e.getPlayer())) {
                    e.getPlayer().sendMessage(ChatColor.GREEN + plugin.i18n.getText("protection_block_removed"));
                }
            } else {
                plugin.pbs.addDropEventCancellation(e.getBlock().getLocation(), e.getBlock().getDrops().size());
                TaskManager.restoreBlock(e.getBlock().getLocation(), e.getBlock().getType(), e.getBlock().getData(), plugin);
                e.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("only_owner_can"));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignEdit(SignChangeEvent e) {
        if (e.getLine(0).toLowerCase().startsWith("[lpb]")) {
            PbSizes pbSizes = parseSize(e.getLine(1));
            if (pbSizes == null) {
                return;
            }

            if (!e.getPlayer().hasPermission("pb.shop.create") && !e.getPlayer().isOp()) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
                return;
            }

            double price;
            try {
                price = Double.parseDouble(e.getLine(2).replace("$", ""));
            } catch (NumberFormatException ex) {
                e.setLine(2, ChatColor.STRIKETHROUGH + e.getLine(2));
                e.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("invalid_price"));
                return;
            }

            Material material = Material.getMaterial(e.getLine(3).toUpperCase());
            if (material == null || !material.isBlock() || material.hasGravity() || !ProtectionController.isMaterialSuitable(new ItemStack(material))) {
                e.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("invalid_material"));
                e.setLine(3, ChatColor.STRIKETHROUGH + e.getLine(3));
                return;
            }

            if (((pbSizes.length & 1) == 0) || ((pbSizes.width & 1) == 0) || ((pbSizes.height & 1) == 0)) {
                e.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("values_must_be_odd"));
                e.setLine(1, ChatColor.STRIKETHROUGH + e.getLine(1));
                return;
            }

            if (plugin.eco == null) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("vault_not_found"));
                return;
            }

            e.setLine(1, pbSizes.length + " x " + pbSizes.height + " x " + pbSizes.width);
            e.setLine(2, "$ " + price);
            e.setLine(3, material.name());
            e.getPlayer().sendMessage(ChatColor.GREEN + plugin.i18n.getText("shop_created"));

        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerUse(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.eco == null) {
                return;
            }
            if (event.getClickedBlock().getType() == Material.WALL_SIGN
                    || event.getClickedBlock().getType() == Material.SIGN_POST) {
                Sign e = (Sign) event.getClickedBlock().getState();
                if (!e.getLine(0).toLowerCase().startsWith("[lpb]")) {
                    return;
                }
                PbSizes pbSizes = parseSize(e.getLine(1));
                if (pbSizes == null) {
                    return;
                }
                double price;
                try {
                    price = Double.parseDouble(e.getLine(2).replace("$", ""));
                } catch (NumberFormatException ex) {
                    return;
                }

                Material material = Material.getMaterial(e.getLine(3).toUpperCase());
                if (material == null || !material.isBlock() || material.hasGravity() || !ProtectionController.isMaterialSuitable(new ItemStack(material))) {
                    return;
                }

                if (((pbSizes.length & 1) == 0) || ((pbSizes.width & 1) == 0) || ((pbSizes.height & 1) == 0)) {
                    return;
                }

                if (plugin.eco.getBalance(event.getPlayer().getName()) < price) {
                    event.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("not_enough_money"));
                    return;
                }

                if (event.getPlayer().getItemInHand().getType() != Material.AIR) {
                    event.getPlayer().sendMessage(ChatColor.RED + plugin.i18n.getText("not_empty_hand"));
                    return;
                }

                event.getPlayer().setItemInHand(new ItemStack(material));
                String[] flags = e.getLine(0).split("\\+");
                boolean result;
                if (flags.length > 1) {
                    
                    result = plugin.pc.createPBFromItemsInHand(event.getPlayer(), pbSizes.length, pbSizes.height, pbSizes.width, flags[1]);
                } else {
                    result = plugin.pc.createPBFromItemsInHand(event.getPlayer(), pbSizes.length, pbSizes.height, pbSizes.width);
                }
                if (result) {
                    plugin.eco.withdrawPlayer(event.getPlayer().getName(), price);
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Cost: " + e.getLine(2));
                    plugin.getLogger().log(Level.INFO, "The player {0} has bought {1} ({2}) {3}",
                            new Object[]{event.getPlayer().getName(), e.getLine(3), e.getLine(1), e.getLine(2)});
                }
            }
        }
    }

    private PbSizes parseSize(String line) {
        PbSizes pbSizes = new PbSizes();
        if (!line.matches("(.*)\\s*(\\d+)\\s*x\\s*(\\d+)\\s*x\\s*(\\d+)(.*)")) {
            return null;
        }
        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(line);
        m.find();
        pbSizes.length = Integer.parseInt(m.group());
        m.find();
        pbSizes.height = Integer.parseInt(m.group());
        m.find();
        pbSizes.width = Integer.parseInt(m.group());
        return pbSizes;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block pushedBlock : e.getBlocks()) {
            if (plugin.pbs.contains(pushedBlock.getLocation())
                    && !plugin.pbs.get(pushedBlock.getLocation()).hidden) {
                e.setCancelled(true);
                return;
            }
        }
        switch (e.getDirection()) {
            case NORTH:
                if (e.getLength() != 0) {
                    if (plugin.pbs.contains(e.getBlocks().get(e.getLength() - 1).getLocation().subtract(0, 0, 1))) {
                        e.setCancelled(true);
                    }
                } else {
                    if (plugin.pbs.contains(e.getBlock().getLocation().subtract(0, 0, 1))) {
                        e.setCancelled(true);
                    }
                }
                break;
            case SOUTH:
                if (e.getLength() != 0) {
                    if (plugin.pbs.contains(e.getBlocks().get(e.getLength() - 1).getLocation().add(0, 0, 1))) {
                        e.setCancelled(true);
                    }
                } else {
                    if (plugin.pbs.contains(e.getBlock().getLocation().add(0, 0, 1))) {
                        e.setCancelled(true);
                    }
                }
                break;
            case WEST:
                if (e.getLength() != 0) {
                    if (plugin.pbs.contains(e.getBlocks().get(e.getLength() - 1).getLocation().subtract(1, 0, 0))) {
                        e.setCancelled(true);
                    }
                } else {
                    if (plugin.pbs.contains(e.getBlock().getLocation().subtract(1, 0, 0))) {
                        e.setCancelled(true);
                    }
                }
                break;
            case EAST:
                if (e.getLength() != 0) {
                    if (plugin.pbs.contains(e.getBlocks().get(e.getLength() - 1).getLocation().add(1, 0, 0))) {
                        e.setCancelled(true);
                    }
                } else {
                    if (plugin.pbs.contains(e.getBlock().getLocation().add(1, 0, 0))) {
                        e.setCancelled(true);
                    }
                }
                break;
            case DOWN:
                if (e.getLength() != 0) {
                    if (plugin.pbs.contains(e.getBlocks().get(e.getLength() - 1).getLocation().subtract(0, 1, 0))) {
                        e.setCancelled(true);
                    }
                } else {
                    if (plugin.pbs.contains(e.getBlock().getLocation().subtract(0, 1, 0))) {
                        e.setCancelled(true);
                    }
                }
                break;
            case UP:
                if (e.getLength() != 0) {
                    if (plugin.pbs.contains(e.getBlocks().get(e.getLength() - 1).getLocation().add(0, 1, 0))) {
                        e.setCancelled(true);
                    }
                } else {
                    if (plugin.pbs.contains(e.getBlock().getLocation().add(0, 1, 0))) {
                        e.setCancelled(true);
                    }
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.isSticky() && plugin.pbs.contains(e.getRetractLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent e) {
        if (e.getPlayer().isBanned()) {
            TaskManager.checkBannedForStones(plugin, e.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().toLowerCase().startsWith("/ban")
                && e.getMessage().split(" ").length >= 2) {
            OfflinePlayer bannedPlayer = plugin.getServer().getOfflinePlayer(e.getMessage().split(" ")[1]);
            if (bannedPlayer != null) {
                TaskManager.checkBannedForStones(plugin, bannedPlayer.getName());
            }
        }
    }
}
