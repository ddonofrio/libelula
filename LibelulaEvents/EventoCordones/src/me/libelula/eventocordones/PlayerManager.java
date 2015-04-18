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
package me.libelula.eventocordones;

import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class PlayerManager {

    private final Main plugin;
    private final TreeMap<String, PlayerInfo> players;

    private class PlayerInfo {

        Player player;
        ArenaManager.Arena.Team team;
        String previousDisplayName;
        ItemStack boots;
        ItemStack helmet;
        ItemStack chetsplate;
        ItemStack leggins;
        
        public void setPlayer(Player player) {
            this.player = player;
        }

        public void setTeam(ArenaManager.Arena.Team team) {
            this.team = team;
        }

    }

    public PlayerManager(Main plugin) {
        this.plugin = plugin;
        players = new TreeMap<>();
    }

    public boolean hasTeam(Player player) {
        return players.containsKey(player.getName());
    }

    public void setTeam(Player player, ArenaManager.Arena.Team team) {
        PlayerInfo pi = new PlayerInfo();
        pi.setPlayer(player);
        pi.setTeam(team);
        players.put(player.getName(), pi);
    }

    public ArenaManager.Arena.Team removeTeam(Player player) {
        ArenaManager.Arena.Team team = null;
        PlayerInfo pi = players.remove(player.getName());
        if (pi != null) {
            if (pi.previousDisplayName != null) {
                player.setDisplayName(pi.previousDisplayName);
                team = pi.team;
            }
        }
        return team;
    }

    public void setStuff(final Player player, boolean reposition) {
        final PlayerInfo pi = players.get(player.getName());
        if (pi != null) {
            if (pi.team.getStartingKitInventory() != null) {
                player.getInventory().setContents(pi.team.getStartingKitInventory().getContents());
            }
            if (pi.team.getKitBoots() != null) {
                if (player.getInventory().getBoots() == null) {
                    player.getInventory().setBoots(pi.team.getKitBoots());
                } else {
                    if (player.getInventory().getBoots().getType() != Material.IRON_BOOTS) {
                        player.getInventory().setBoots(pi.team.getKitBoots());
                    }
                }
            }

            if (pi.team.getKitChestplate() != null) {
                if (player.getInventory().getChestplate() == null
                        || player.getInventory().getChestplate().getType() != Material.IRON_CHESTPLATE) {
                    player.getInventory().setChestplate(pi.team.getKitChestplate());
                }
            }
            if (pi.team.getKitHelmet() != null) {
                if (player.getInventory().getHelmet() == null
                        || player.getInventory().getHelmet().getType() != Material.IRON_HELMET) {
                    player.getInventory().setHelmet(pi.team.getKitHelmet());
                }
            }

            if (pi.team.getKitLeggings() != null) {
                if (player.getInventory().getLeggings() == null
                        || player.getInventory().getLeggings().getType() != Material.IRON_LEGGINGS) {
                    player.getInventory().setLeggings(pi.team.getKitLeggings());
                }
            }

            pi.previousDisplayName = player.getDisplayName() + "";
            player.setDisplayName(pi.team.getColor() + "{" + pi.team.getName() + "}" + player.getDisplayName());
        }
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setFlying(false);
        player.setAllowFlight(false);
        if (reposition) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    player.teleport(pi.team.getSpawn());
                }
            }, 2);
        }

    }
    
    public ItemStack[] getArmour(Player player) {
        PlayerInfo pi = players.get(player.getName());
        if (pi != null) {
        ItemStack[] is = {pi.boots, pi.leggins, pi.chetsplate, pi.helmet };
        return is;
        } else {
            return null;
        }
    }
    
    public void setArmour(Player player) {
        PlayerInfo pi = players.get(player.getName());
        pi.boots = player.getInventory().getBoots();
        pi.chetsplate = player.getInventory().getChestplate();
        pi.helmet = player.getInventory().getHelmet();
        pi.leggins = player.getInventory().getLeggings();
    }

    public void setStuff(Player player) {
        setStuff(player, false);
    }

    public void backToNormal(Player player) {
        player.getInventory().clear();
        ItemStack air = new ItemStack(Material.AIR);
        player.getInventory().setBoots(air);
        player.getInventory().setChestplate(air);
        player.getInventory().setHelmet(air);
        player.getInventory().setLeggings(air);
        player.setFireTicks(0);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public ChatColor getNameTagColor(Player player) {
        PlayerInfo pi = players.get(player.getName());
        if (pi == null) {
            return ChatColor.WHITE;
        } else {
            return pi.team.getColor();
        }
    }

    public String getTeamName(Player player) {
        PlayerInfo pi = players.get(player.getName());
        if (pi != null) {
            return pi.team.getName();
        } else {
            return null;
        }
    }

}
