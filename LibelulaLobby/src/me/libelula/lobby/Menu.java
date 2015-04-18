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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class Menu {

    private final Main plugin;
    private final Inventory networkMenu;
    private final Inventory technic;
    private final Inventory pvp;
    private final Inventory minigames;
    private final Inventory special;

    public Menu(Main plugin) {
        this.plugin = plugin;
        networkMenu = Bukkit.createInventory(null, 27, ChatColor.AQUA + "¿Dónde quieres jugar hoy?");
        technic = Bukkit.createInventory(null, 36, ChatColor.RED + "Minecraft Técnico");
        pvp = Bukkit.createInventory(null, 36, ChatColor.AQUA + "Player vs Player");
        minigames = Bukkit.createInventory(null, 36, ChatColor.GRAY + "Minijuegos");
        special = Bukkit.createInventory(null, 36, ChatColor.BLUE + "Servidores especiales");
        setNetworkMenu();
        setTechnicMenu();
        setPVPMenu();
        setSpecial();
    }

    private void setNetworkMenu() {
        ItemMeta im;
        List<String> lore = new ArrayList<>();

        ItemStack tecnico = new ItemStack(Material.REDSTONE, 1);
        im = tecnico.getItemMeta();
        im.setDisplayName(ChatColor.RED + "Minecraft Técnico");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA
                + "" + ChatColor.BOLD + "Técnico");
        lore.add(ChatColor.BLUE + "* Survival en distintas modalidades");
        lore.add(ChatColor.BLUE + "* Skyblock");
        lore.add(ChatColor.BLUE + "* Hardcore");
        lore.add(ChatColor.BLUE + "* etc...");
        im.setLore(lore);
        tecnico.setItemMeta(im);
        networkMenu.setItem(10, tecnico);

        ItemStack pvp = new ItemStack(Material.DIAMOND_SWORD, 1);
        im = pvp.getItemMeta();
        im.setDisplayName(ChatColor.AQUA + "Player vs Player");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA
                + "" + ChatColor.BOLD + "PVP");
        lore.add(ChatColor.BLUE + "* Capture the Wool (CTW)");
        lore.add(ChatColor.BLUE + "* Skywars");
        lore.add(ChatColor.BLUE + "* Juegos del hambre");
        lore.add(ChatColor.BLUE + "* etc...");
        im.setLore(lore);
        im.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        pvp.setItemMeta(im);
        networkMenu.setItem(12, pvp);

        ItemStack minigames = new ItemStack(Material.BREWING_STAND_ITEM, 1);
        im = minigames.getItemMeta();
        im.setDisplayName(ChatColor.GRAY + "Minijuegos");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Minijuegos");
        lore.add(ChatColor.BLUE + "* Las últimas novedades.");
        lore.add(ChatColor.BLUE + "* Los clásicos de siempre.");
        lore.add(ChatColor.BLUE + "* TNT Run");
        lore.add(ChatColor.BLUE + "* Build my thing");
        lore.add(ChatColor.BLUE + "* etc...");
        im.setLore(lore);
        minigames.setItemMeta(im);
        networkMenu.setItem(14, minigames);

        ItemStack special = new ItemStack(Material.BEACON, 1);
        im = special.getItemMeta();
        im.setDisplayName(ChatColor.BLUE + "Servidores especiales");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Especial");
        lore.add(ChatColor.BLUE + "* Eventos.");
        lore.add(ChatColor.BLUE + "* Red VIP.");
        lore.add(ChatColor.BLUE + "* Servidores de la 1.8");
        lore.add(ChatColor.BLUE + "* etc...");
        im.setLore(lore);
        special.setItemMeta(im);
        networkMenu.setItem(16, special);
    }

    private void setTechnicMenu() {
        ItemMeta im;
        List<String> lore = new ArrayList<>();

        ItemStack main = new ItemStack(Material.GRASS, 1);
        im = main.getItemMeta();
        im.setDisplayName(ChatColor.GREEN + "main() survival");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "main()");
        lore.add(ChatColor.BLUE + "* Servidor survival");
        lore.add(ChatColor.BLUE + "* Parcelas");
        lore.add(ChatColor.BLUE + "* Nuevos plugins cada semana");
        lore.add(ChatColor.BLUE + "* etc...");
        im.setLore(lore);
        main.setItemMeta(im);
        technic.setItem(10, main);

        ItemStack survivalOne = new ItemStack(Material.GRASS, 2);
        im = survivalOne.getItemMeta();
        im.setDisplayName(ChatColor.GREEN + "Survival ONE");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Survival ONE");
        lore.add(ChatColor.BLUE + "* Servidor survival vanilla");
        lore.add(ChatColor.BLUE + "* Muy pocas reglas");
        lore.add(ChatColor.BLUE + "* Muy pocos plugins");
        im.setLore(lore);
        survivalOne.setItemMeta(im);
        technic.setItem(12, survivalOne);

        ItemStack skyblock = new ItemStack(Material.FEATHER, 1);
        im = skyblock.getItemMeta();
        im.setDisplayName(ChatColor.AQUA + "Skyblock");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Skyblock");
        lore.add(ChatColor.BLUE + "* Servidor exclusivo Skyblock");
        im.setLore(lore);
        skyblock.setItemMeta(im);
        technic.setItem(14, skyblock);

        ItemStack rol = new ItemStack(Material.PAPER, 1);
        im = rol.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Juegos de ROL");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "ROL");
        lore.add(ChatColor.BLUE + "* Factions");
        lore.add(ChatColor.BLUE + "* Supernaturals");
        lore.add(ChatColor.BLUE + "* Vampiros");
        lore.add(ChatColor.BLUE + "* etc...");
        im.setLore(lore);
        rol.setItemMeta(im);
        technic.setItem(16, rol);

        ItemStack back = new ItemStack(Material.FENCE_GATE, 1);
        im = back.getItemMeta();
        im.setDisplayName(ChatColor.GREEN + "Volver al menu principal");
        back.setItemMeta(im);
        technic.setItem(27, back);

    }

    private void setPVPMenu() {
        ItemMeta im;
        List<String> lore = new ArrayList<>();

        ItemStack liderSwag = new ItemStack(Material.IRON_FENCE, 1);
        im = liderSwag.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Lider Swag");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Lider Swag");
        lore.add(ChatColor.BLUE + "* El mejor 1 vs 1");
        lore.add(ChatColor.BLUE + "* Demuestra ser el mejor");
        lore.add(ChatColor.BLUE + "* El ganador queda en la arena");
        im.setLore(lore);
        liderSwag.setItemMeta(im);
        pvp.setItem(10, liderSwag);

        ItemStack ctw = new ItemStack(Material.WOOL, 1);
        im = ctw.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Capture the Wool");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "CTW");
        lore.add(ChatColor.BLUE + "* PVP por equipos");
        lore.add(ChatColor.BLUE + "* Las mejores arenas");
        lore.add(ChatColor.BLUE + "* Estadísticas");
        im.setLore(lore);
        ctw.setItemMeta(im);
        pvp.setItem(12, ctw);

        ItemStack skyWars = new ItemStack(Material.LAVA_BUCKET, 1);
        im = skyWars.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Sky Wars");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Sky Wars");
        lore.add(ChatColor.BLUE + "* El juego del momento");
        lore.add(ChatColor.BLUE + "* Demuestra ser el mejor");
        lore.add(ChatColor.BLUE + "* Mescla de técnico y PVP");
        im.setLore(lore);
        skyWars.setItemMeta(im);
        pvp.setItem(14, skyWars);

        ItemStack towers = new ItemStack(Material.BRICK, 1);
        im = towers.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "The Towers");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "The Towers");
        lore.add(ChatColor.BLUE + "* PVP por equipos");
        lore.add(ChatColor.BLUE + "* Lleva a tu equipo al éxito");
        im.setLore(lore);
        towers.setItemMeta(im);
        pvp.setItem(16, towers);

        ItemStack hungerGames = new ItemStack(Material.CHEST, 1);
        im = hungerGames.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Los juegos del hambre");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Hunger Games");
        lore.add(ChatColor.BLUE + "* Demuestra ser el mejor");
        lore.add(ChatColor.BLUE + "* Sé el último superviviente");
        im.setLore(lore);
        hungerGames.setItemMeta(im);
        pvp.setItem(20, hungerGames);

        ItemStack climbUp = new ItemStack(Material.LADDER, 1);
        im = climbUp.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Climb Up");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Climb Up");
        lore.add(ChatColor.BLUE + "* El juego de killercreeper55");
        lore.add(ChatColor.BLUE + "* Por primera vez en un server");
        lore.add(ChatColor.BLUE + "* Demuestra lo que vales");
        im.setLore(lore);
        climbUp.setItemMeta(im);
        pvp.setItem(22, climbUp);

        ItemStack back = new ItemStack(Material.FENCE_GATE, 1);
        im = back.getItemMeta();
        im.setDisplayName(ChatColor.GREEN + "Volver al menu principal");
        back.setItemMeta(im);
        pvp.setItem(27, back);

    }

    private void setSpecial() {
        ItemMeta im;
        List<String> lore = new ArrayList<>();

        ItemStack creative = new ItemStack(Material.COMMAND, 1);
        im = creative.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Creative");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "Creative");
        lore.add(ChatColor.BLUE + "* Modo de juego creativo");
        lore.add(ChatColor.BLUE + "* Construye lo que quieras");
        lore.add(ChatColor.BLUE + "* Participa en concursos y gana premios");
        im.setLore(lore);
        creative.setItemMeta(im);
        special.setItem(10, creative);

        ItemStack vip = new ItemStack(Material.DIAMOND, 1);
        im = vip.getItemMeta();
        im.setDisplayName(ChatColor.GOLD + "Lobby VIP");
        lore.clear();
        lore.add(ChatColor.GOLD + "Libelula "
                + ChatColor.YELLOW + "Minecraft " + ChatColor.DARK_AQUA + "VIP");
        lore.add(ChatColor.BLUE + "* Exclusivo para VIPs");
        lore.add(ChatColor.BLUE + "* Prueba los juegos antes que nadie");
        lore.add(ChatColor.BLUE + "* Accede durante mantenimientos");
        im.setLore(lore);
        vip.setItemMeta(im);
        special.setItem(12, vip);

        ItemStack back = new ItemStack(Material.FENCE_GATE, 1);
        im = back.getItemMeta();
        im.setDisplayName(ChatColor.GREEN + "Volver al menu principal");
        back.setItemMeta(im);
        special.setItem(27, back);

    }

    public Inventory getNetworkMenu() {
        return networkMenu;
    }

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
        
        if (e.getCurrentItem().getType() == Material.SPONGE) {
            plugin.pm.manegeSpecialClick(player, e.getCurrentItem());
        }
        /*
         Network menu
         */
        if (inventoryName.equals(networkMenu.getName())) {
            switch (e.getCurrentItem().getType()) {
                case REDSTONE:
                    player.openInventory(technic);
                    ret = true;
                    break;
                case DIAMOND_SWORD:
                    player.openInventory(pvp);
                    ret = true;
                    break;
                case BREWING_STAND_ITEM:
                    plugin.sendMessage(player, "Lo sentimos, esta red está actualmente desconectada.");
                    plugin.sendMessage(player, "Puede que esté en mantenimiento, prueba más tarde.");
                    //player.openInventory(minigames);
                    //ret = true;
                    break;
                case BEACON:
                    player.openInventory(special);
                    ret = true;
                    break;
            }
        } else if (inventoryName.equals(technic.getName())) {
            switch (e.getCurrentItem().getType()) {
                case GRASS:
                    switch (e.getCurrentItem().getAmount()) {
                        case 1:
                            plugin.teleportToServer(player, "main");
                            break;
                        case 2:
                            plugin.teleportToServer(player, "survival-one");
                            break;
                    }
                    break;
                case FEATHER:
                    plugin.teleportToServer(player, "skyblock");
                    break;
                case PAPER:
                    plugin.teleportToServer(player, "rol");
                    break;
                case FENCE_GATE:
                    player.openInventory(getNetworkMenu());
                    ret = true;
                    break;

            }
        } else if (inventoryName.equals(pvp.getName())) {
            switch (e.getCurrentItem().getType()) {

                case IRON_FENCE:
                    plugin.teleportToServer(player, "liderswag");
                    break;
                case WOOL:
                    plugin.teleportToServer(player, "CTW");
                    break;
                case LAVA_BUCKET:
                    plugin.teleportToServer(player, "skywars");
                    break;
                case FENCE_GATE:
                    player.openInventory(getNetworkMenu());
                    ret = true;
                    break;
                case LADDER:
                    plugin.teleportToServer(player, "climb-up");
                    break;
                case BRICK:
                    plugin.teleportToServer(player, "the-towers");
                    break;
                default:
                    plugin.sendMessage(player, "Lo sentimos, este servidor está en mantenimiento.");
                    break;
            }
        } else if (inventoryName.equals(special.getName())) {
            switch (e.getCurrentItem().getType()) {
                case COMMAND:
                    plugin.teleportToServer(player, "creative");
                    break;
                case DIAMOND:
                    plugin.sendMessage(player, "Debes ser VIP para entrar aquí.");
                    break;
                case FENCE_GATE:
                    player.openInventory(getNetworkMenu());
                    ret = true;
                    break;
                default:
                    plugin.sendMessage(player, "Lo sentimos, este servidor está en mantenimiento.");
                    break;
            }
        }

        return ret;
    }

}
