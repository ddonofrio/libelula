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
package me.libelula.liderswag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class SignManager {

    private final Main plugin;
    public String joinSignFirstLine;

    public SignManager(Main plugin) {
        this.plugin = plugin;
        joinSignFirstLine = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("signs.first-line-text-replacement"));
    }

    public void checkUpdateJoin(SignChangeEvent e) {

        if (e.getLine(0).equalsIgnoreCase(plugin.getConfig().getString("signs.first-line-text"))) {
            e.setLine(0, joinSignFirstLine);
            String arenaName = e.getLine(1);
            final ArenaManager.Arena arena = plugin.am.getArena(arenaName);
            if (arena != null) {
                plugin.am.addJoinSign(arenaName, e.getBlock().getLocation());
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            arena.updateSigns();
                        }
                    }, 2);
            } else {
                e.setLine(2, ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("signs.on-invalid-arena-replacement")));
            }
        }
    }

    public void updateJoinSign(Sign sign, ArenaManager.Arena arena) {
        String maxPlayers = "" + arena.getMaxPlayers();
        if ("0".equals(maxPlayers)) {
            maxPlayers = Tools.Chars.infinity;
        }
        sign.setLine(2, ChatColor.DARK_PURPLE + "" + arena.getPlayersCount()
                + " / " + maxPlayers);
        if (arena.isEnabled()) {
            sign.setLine(3, ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("signs.on-enabled-arena")));
        } else {
            sign.setLine(3, ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("signs.on-disabled-arena")));
        }
        sign.update();
    }

    public boolean checkJoin(PlayerInteractEvent e) {
        boolean result = false;
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (isSign(e.getClickedBlock())) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                if (sign.getLine(0).equals(plugin.sgm.joinSignFirstLine)) {
                    e.setCancelled(true);
                    ArenaManager.Arena arena = plugin.am.getArena(sign.getLine(1));
                    plugin.gm.addPlayerToSpectators(e.getPlayer(), arena);
                    result = true;
                }
            }
        }
        return result;
    }
    
    static boolean isSign(Block block) {
        return block.getType() == Material.WALL_SIGN
                    || block.getType() == Material.SIGN_POST;
    }
    
    public void updateJoinSigns(String arenaName) {
        ArenaManager.Arena arena = plugin.am.getArena(arenaName);
        for (Location loc : arena.getJoinSignsLocations()) {
            if (isSign(loc.getBlock())) {
                Sign sign = (Sign) loc.getBlock().getState();
                updateJoinSign(sign, arena);
            }
        }
    }

}
