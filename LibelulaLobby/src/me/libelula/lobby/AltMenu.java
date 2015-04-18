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

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class AltMenu extends Menu {

    private final Main plugin;
    private final Inventory networkMenu;

    public AltMenu(Main plugin) {
        super(plugin);
        this.plugin = plugin;
        networkMenu = Bukkit.createInventory(null, 27, ChatColor.AQUA + "¿Dónde quieres jugar hoy?");
        setNetworkMenu();
    }

    private void setNetworkMenu() {
        ItemMeta im;
        List<String> lore = new ArrayList<>();
        ItemStack survival = new ItemStack(Material.GRASS, 1);
        im = survival.getItemMeta();
        im.setDisplayName(ChatColor.GREEN + "Survival");
        lore.clear();
        lore.add(ChatColor.GOLD + "Mine One "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Survival");
        lore.add(ChatColor.BLUE + "* Servidor survival");
        lore.add(ChatColor.BLUE + "* Parcelas");
        lore.add(ChatColor.BLUE + "* etc...");
        im.setLore(lore);
        survival.setItemMeta(im);
        networkMenu.setItem(10, survival);

        ItemStack skyWars = new ItemStack(Material.LAVA_BUCKET, 1);
        im = skyWars.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Sky Wars");
        lore.clear();
        lore.add(ChatColor.GOLD + "Mine One "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Sky Wars");
        lore.add(ChatColor.BLUE + "* El juego del momento");
        lore.add(ChatColor.BLUE + "* Demuestra ser el mejor");
        lore.add(ChatColor.BLUE + "* Mescla de técnico y PVP");
        im.setLore(lore);
        skyWars.setItemMeta(im);
        networkMenu.setItem(12, skyWars);

        ItemStack climbUp = new ItemStack(Material.LADDER, 1);
        im = climbUp.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Climb Up");
        lore.clear();
        lore.add(ChatColor.GOLD + "Mine One "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Climb Up");
        lore.add(ChatColor.BLUE + "* El juego de killercreeper55");
        lore.add(ChatColor.BLUE + "* Por primera vez en un server");
        lore.add(ChatColor.BLUE + "* Demuestra lo que vales");
        im.setLore(lore);
        climbUp.setItemMeta(im);
        networkMenu.setItem(14, climbUp);

        ItemStack ctw = new ItemStack(Material.WOOL, 1);
        im = ctw.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Capture the Wool");
        lore.clear();
        lore.add(ChatColor.GOLD + "Mine One "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "CTW");
        lore.add(ChatColor.BLUE + "* PVP por equipos");
        lore.add(ChatColor.BLUE + "* Las mejores arenas");
        lore.add(ChatColor.BLUE + "* Estadísticas");
        im.setLore(lore);
        ctw.setItemMeta(im);
//        networkMenu.setItem(16, ctw); La niña esta no quiere CTW! :(

    }

    @Override
    public Inventory getNetworkMenu() {
        return networkMenu;
    }

    @Override
    public boolean processClick(InventoryClickEvent e) {
        boolean ret = false;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) {
            return false;
        } else if (e.getCurrentItem().getType() == Material.AIR) {
            return true;
        }
        Player player = (Player) e.getWhoClicked();
        String inventoryName = e.getInventory().getName();
        e.setCancelled(true);
        /*
         Network menu
         */
        if (inventoryName.equals(networkMenu.getName())) {
            switch (e.getCurrentItem().getType()) {
                case GRASS:
                    plugin.sendMessage(player, "Lo sentimos, este servidor está en mantenimiento.");
                    break;
                case LAVA_BUCKET:
                    plugin.teleportToServer(player, "skywars");
                    break;                    
                case LADDER:
                    plugin.teleportToServer(player, "climb-up");
                    break;
                case WOOL:
                    plugin.teleportToServer(player, "CTW");
                    break;
                default:
                    plugin.sendMessage(player, "Lo sentimos, este servidor está en mantenimiento.");
                    break;
                    
            }
        }
        return ret;
    }

}
