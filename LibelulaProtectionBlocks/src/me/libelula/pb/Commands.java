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

import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class CommandManager of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Commands implements CommandExecutor {

    private final LibelulaProtectionBlocks plugin;

    public Commands(LibelulaProtectionBlocks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        Player player = getPlayer(cs);
        ProtectionBlocks.PSBlocks psb;
       
        
        
        if (args.length == 0) {
            switch (cmnd.getName()) {
                case "ps":
                    if (player == null || player.hasPermission("pb.version")) {
                        cs.sendMessage(ChatColor.YELLOW + plugin.getPluginVersion());
                    }
                    showCommnandsHelp(cs);
                    break;
                default:
                    cs.sendMessage(plugin.i18n.getText("unknown_command"));
            }
        } else {
            if (cmnd.getName().equals("ps")) {
                switch (args[0].toLowerCase()) {
                    case "help":
                        showCommnandsHelp(cs);
                        break;
                    case "version":
                        if (args.length != 1) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            return true;
                        }
                        if (player == null || player.hasPermission("pb.version")) {
                            cs.sendMessage(ChatColor.YELLOW + plugin.getPluginVersion());
                        } else {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
                        }
                        break;
                    case "create":
                        if (player == null) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                            return true;
                        }
                        if (!player.hasPermission("pb.create")) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
                            return true;
                        }
                        if (args.length != 2 && args.length != 4) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command2"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command3"));
                            return true;
                        }
                        int length;
                        int height;
                        int width;
                        try {
                            length = Integer.parseInt(args[1]);
                            if (args.length == 4) {
                                height = Integer.parseInt(args[2]);
                                width = Integer.parseInt(args[3]);
                            } else {
                                height = length;
                                width = length;
                            }
                        } catch (NumberFormatException ex) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command2"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command3"));
                            return true;
                        }
                        if (length <= 0 || width <= 0) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("values_must_greater"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command2"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command3"));
                            return true;
                        }
                        if (((length & 1) == 0) || ((width & 1) == 0) || (height != 0 && ((height & 1) == 0))) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("values_must_be_odd"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command2"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command3"));
                            return true;
                        }
                        if (plugin.pc.createPBFromItemsInHand(player, length, height, width)) {
                            cs.sendMessage(ChatColor.GREEN + plugin.i18n.getText("pbs_has_been_created"));
                            return true;
                        }
                        break;
                    case "hide":
                        if (player == null) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                            return true;
                        }
                        if (args.length == 1) {
                            plugin.pbs.hide(player);
                        } else {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_hide_command"));
                        }
                        break;
                    case "unhide":
                        if (player == null) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                            return true;
                        }
                        if (args.length == 1) {
                            plugin.pbs.unhide(player, false);
                        } else if (args.length == 2 && args[1].toLowerCase().equalsIgnoreCase("force")) {
                            if (player.hasPermission("pb.unhide.force")) {
                                plugin.pbs.unhide(player, true);
                            } else {
                                cs.sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
                            }
                        } else {
                            if (player.hasPermission("pb.unhide.force")) {
                                cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_unhide_force_command"));
                            } else {
                                cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_unhide_command"));
                            }
                        }
                        break;
                    case "add":
                        if (player == null) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                            return true;
                        }
                        if (args.length == 1) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_add_command"));
                            return true;
                        }

                        psb = plugin.pbs.addMember(player,
                                Arrays.copyOfRange(args, 1, args.length));
                        if (psb != null) {
                            showMemberList(cs, psb);
                        }
                        break;
                    case "del":
                    case "delete":
                    case "remove":
                        if (player == null) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                            return true;
                        }
                        if (args.length == 1) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_del_command"));
                            return true;
                        }
                        psb = plugin.pbs.removeMember(player,
                                Arrays.copyOfRange(args, 1, args.length));
                        if (psb != null) {
                            showMemberList(cs, psb);
                        }
                        break;
                    case "flag":
                        if (args.length == 1 || args.length == 2 && args[1].equalsIgnoreCase("list")) {
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("available_flags_are") + " "
                                    + ChatColor.YELLOW + plugin.config.getPlayerConfigurableFlags().toString());
                        } else {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("flag_deprecated"));
                        }
                        break;
                    case "info":
                        if (player == null) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                            return true;
                        }
                        if (args.length != 1) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_info_command"));
                            return true;

                        }
                        psb = plugin.pbs.getPs(player.getLocation());
                        if (psb == null) {
                            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_in_ps_area"));
                            return true;
                        }

                        if (!psb.region.isMember(player.getName()) && !player.hasPermission("pb.addmember.others")) {
                            player.sendMessage(ChatColor.RED + plugin.i18n.getText("not_owned_by_you"));
                            return true;
                        }
                        cs.sendMessage(ChatColor.YELLOW + psb.name + " - " + psb.region.getId());
                        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("priority") + ": " + psb.region.getPriority());
                        showMemberList(cs, psb);
                        cs.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Flags:");
                        for (Map.Entry<Flag<?>, Object> flag : psb.region.getFlags().entrySet()) {
                            cs.sendMessage(ChatColor.YELLOW + "  * " + flag.getKey().getName() + ": " + flag.getValue().toString());
                        }
                        break;
                    case "remove-all-ps":
                        if (args.length != 2) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_removeall_command"));
                            return true;
                        }
                        if (player != null && !player.hasPermission("pb.remove.all")) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
                            return true;
                        }
                        plugin.pbs.removeAllPB(args[1]);
                        break;
                    case "reload":
                        if (args.length != 1) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_reload_command"));
                            return true;
                        }
                        if (player != null && !player.hasPermission("pb.reload")) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
                            return true;
                        }
                        plugin.config.reload();
                        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("config_reloaded"));
                        break;
                    case "+fence":
                        if (player == null) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                            return true;
                        }
                        if (player != null && !player.hasPermission("pb.modifyflags")) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("you_dont_have_permissions"));
                            return true;
                        }
                        if (args.length != 1) {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                            return true;
                        }

                        if (player.getItemInHand().getItemMeta().getLore() != null
                                && player.getItemInHand().getItemMeta().getLore().size() == 3
                                && player.getItemInHand().getItemMeta().getDisplayName() != null) {
                            cs.sendMessage(ChatColor.GREEN + plugin.i18n.getText("fence_added"));
                            List <String> lore = player.getItemInHand().getItemMeta().getLore();
                            lore.set(0, "+Fence");
                            ItemMeta dtMeta = player.getItemInHand().getItemMeta();
                            dtMeta.setLore(lore);
                            player.getItemInHand().setItemMeta(dtMeta);
                        } else {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("not_a_protection_block"));
                            return true;
                        }
                        break;
                    default:
                        if (plugin.config.isPlayerFlag(args[0])) {
                            if (player == null) {
                                cs.sendMessage(ChatColor.RED + plugin.i18n.getText("in_game"));
                                return true;
                            }

                            String value = "";
                            if (args.length > 1) {
                                for (String part : Arrays.copyOfRange(args, 1, args.length)) {
                                    value = value.concat(part) + " ";
                                }
                                value = value.substring(0, value.length() - 1);
                            } else {
                                value = null;
                            }
                            plugin.pbs.setFlag(player, DefaultFlag.fuzzyMatchFlag(args[0].toLowerCase()), value);
                        } else {
                            cs.sendMessage(ChatColor.RED + plugin.i18n.getText("incorrect_parameters"));
                        }
                }
            }
        }

        return true;
    }

    private Player getPlayer(CommandSender cs) {
        Player player = null;
        if (cs instanceof Player) {
            player = (Player) cs;
        }
        return player;
    }

    private void showCommnandsHelp(CommandSender cs) {
        Player player = getPlayer(cs);

        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("use_ps_help_command"));
        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("list_only_allowed_commands"));
        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_add_command"));
        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_del_command"));
        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_hide_command"));
        if (player == null || player.hasPermission("pb.unhide.force")) {
            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_unhide_force_command"));
        } else {
            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_unhide_command"));
        }

        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_flag_list_command"));
        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_flag_command"));
        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_info_command"));

        if (player == null || player.hasPermission("pb.reload")) {
            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_reload_command"));
        }

        if (player == null || player.hasPermission("pb.create")) {
            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_create_command"));
            cs.sendMessage(ChatColor.YELLOW + "- " + plugin.i18n.getText("ps_create_command2"));
            cs.sendMessage(ChatColor.YELLOW + "- " + plugin.i18n.getText("ps_create_command3"));
        }

        if (player == null || player.hasPermission("pb.version")) {
            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_version_command"));
        }

        if (player == null || player.hasPermission("pb.remove.all")) {
            cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("ps_removeall_command"));
        }


    }

    private void showMemberList(CommandSender cs, ProtectionBlocks.PSBlocks psb) {
        cs.sendMessage(ChatColor.YELLOW + plugin.i18n.getText("member_list_title"));
        cs.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + plugin.i18n.getText("Owners") + ":");
        for (String owner : psb.region.getOwners().getPlayers()) {
            cs.sendMessage(ChatColor.YELLOW + "  * " + owner);
        }
        cs.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + plugin.i18n.getText("Members") + ":");
        for (String member : psb.region.getMembers().getPlayers()) {
            cs.sendMessage(ChatColor.YELLOW + "  * " + member);
        }
    }
}
