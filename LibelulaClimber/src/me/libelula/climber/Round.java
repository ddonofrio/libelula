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

import com.sk89q.worldedit.BlockVector;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Round {

    private final Game g;
    private int round;
    private int tickCounter;
    private final List<Integer> reward;
    private boolean allowPVP;
    Main.LocationComparator locationComp;

    private class Listener implements org.bukkit.event.Listener {

        @EventHandler(ignoreCancelled = true)
        public void onQuitEvent(PlayerQuitEvent e) {
            if (g.isInGame(e.getPlayer())) {
                g.removePlayer(e.getPlayer());
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent e) {
            boolean moveIntoArena = false;
            BlockVector min = g.getArena().area.getMinimumPoint();
            BlockVector max = g.getArena().area.getMaximumPoint();
            if (e.getTo().getWorld().getName().equals(g.getArena().capturePoint.getWorld().getName())) {
                if (e.getTo().getBlockX() >= min.getX() && e.getTo().getBlockX() <= max.getBlockX()
                        && e.getTo().getBlockY() >= min.getY() && e.getTo().getBlockY() <= max.getBlockY()
                        && e.getTo().getBlockZ() >= min.getZ() && e.getTo().getBlockZ() <= max.getBlockZ()) {
                    moveIntoArena = true;
                }
            }

            if (g.getTeam(e.getPlayer()) == null) {
                if (moveIntoArena) {
                    if (!e.getPlayer().isOp()) {
                        e.getPlayer().teleport(g.getPlugin().mapMan.getLobby());
                        g.getPlugin().teamMan.backToNormal(e.getPlayer());
                    }
                    return;
                }
            } else {
                if (!moveIntoArena) {
//                    g.getPlugin().getLogger().info("Debug: fuera de Area " +e.getTo() + "  " + g.getArena().area.getMinimumPoint() + " / " + g.getArena().area.getMaximumPoint());
                    g.messageAll(e.getPlayer().getName() + " ha caído fuera de la arena.");
                    g.moveToSpawn(e.getPlayer());
                }
            }

            if (g.getArena().capturePoint.getBlockX() == e.getTo().getBlockX()
                    && g.getArena().capturePoint.getBlockY() == e.getTo().getBlockY()
                    && g.getArena().capturePoint.getBlockZ() == e.getTo().getBlockZ()
                    && g.getArena().capturePoint.getWorld().getName().equals(e.getTo().getWorld().getName())) {
                g.moveAllToSpawn();
                g.messageAll(ChatColor.GOLD + e.getPlayer().getDisplayName() + " captura el punto");
                int score = g.getPlugin().teamMan.getScore(e.getPlayer()).getScore();
                g.getPlugin().teamMan.getScore(e.getPlayer()).setScore(score + reward.get(round));
                g.playSoundAll(Sound.EXPLODE);

                /*
                 Location pointLoc = e.getTo();
                 pointLoc.setY(e.getTo().getY() - round);
                 pointLoc.getBlock().setType(Material.WOOL);
                 pointLoc.getBlock().setData(g.getPlugin().teamMan.getDyeColor(e.getPlayer()).getData());
                 */
                g.announsePoint();
                round++;
                tickCounter = 0;
            }

        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent e) {
            Player player = e.getEntity();

            if (g.getTeam(player) != null) {
                String deathMessage = e.getDeathMessage();
                e.setDeathMessage("");
                e.setDroppedExp(0);
                e.setKeepLevel(true);
                e.getDrops().clear();
                g.moveToSpawn(player);
                g.messageAll(ChatColor.GRAY + deathMessage);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerDamage(EntityDamageByEntityEvent e) {
            Player player;
            if (e.getEntity() instanceof Player) {
                player = (Player) e.getEntity();
                //e.setCancelled(true);
                e.setDamage(0.001);
                return;
            } else {
                return;
            }
            /*
             boolean melee = true;
             boolean headshot = false;
             Player damager = null;
             if (e.getDamager() instanceof Player) {
             damager = (Player) e.getDamager();
             } else if (e.getDamager() instanceof Arrow) {
             final Arrow arrow = (Arrow) e.getDamager();
             if (arrow.getShooter() instanceof Player) {
             damager = (Player) arrow.getShooter();
             melee = false;
             double y = arrow.getLocation().getY();
             double shotY = player.getLocation().getY();
             headshot = y - shotY > 1.35d;
             }
             }

             if (damager == null) {
             return;
             }

             String playerTeam = g.getTeam(player);

             if (playerTeam == null) {
             return;
             }

             String damagerTeam = g.getTeam(damager);

             if (damagerTeam == null) {
             e.setCancelled(true);
             return;
             }

             if (!allowPVP) {
             e.setCancelled(true);
             return;
             }

             boolean dead = e.getDamage() >= player.getHealth();
             if (dead) {
             e.setCancelled(true);
             }
             damageControl(damager, player, melee, headshot, dead, e);
             */
        }
    }

    public Round(Game game) {
        g = game;
        reward = new ArrayList<>();
        reward.add(5);
        reward.add(5);
        reward.add(10);
        reward.add(15);
        reward.add(15);
        reward.add(15);
        reward.add(25);
        reward.add(25);
        reward.add(35);
        reward.add(45);
        reward.add(55);
        reward.add(65);
        reward.add(75);
        reward.add(85);
        reward.add(95);
        reward.add(100);
        reward.add(100);
        locationComp = new Main.LocationComparator();
        g.getPlugin().getServer().getPluginManager().registerEvents(new Round.Listener(), g.getPlugin());
        allowPVP = true;
//        g.getPlugin().getLogger().info("Debug: " + g.getArena().name);
//        g.getPlugin().getLogger().info("Debug: MinPlayer " + g.getArena().minPlayers);
//        g.getPlugin().getLogger().info("Debug: MaxPlayer " + g.getArena().maxPlayers);
//        g.getPlugin().getLogger().info("Debug: Area " + g.getArena().area.getMinimumPoint() + " / " + g.getArena().area.getMaximumPoint());

    }

    public void setRound(int roundNumber) {
        this.round = roundNumber;
        tickCounter = 0;
    }

    public void tick() {
        switch (round) {
            case 0:
                allowPVP = true;
                switch (tickCounter) {
                    case 0:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#1: Conociendo el terreno: "
                                + ChatColor.YELLOW + "Se el primero en subir a la torre primero a la torre.");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 1:
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#2: Dando caña: "
                                + ChatColor.YELLOW + "Utiliza tu espada para alejar a los enemigos.");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        //sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
                        g.giveToAllplayers(sword);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 2:
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#3: Más caña: "
                                + ChatColor.YELLOW + "¡Empuje a tu espada, pega y trepa rápido!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 3);
                        //sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
                        g.giveToAllplayers(sword);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 3:
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#4: Arco y espada: "
                                + ChatColor.YELLOW + "¡Empújalos y llega a la cima!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(16);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 4: // 5
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#5: Arco y espada: "
                                + ChatColor.YELLOW + "¡Empújalos y llega a la cima!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(31);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 3);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 5: //6ta
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#6: Salta y corre: "
                                + ChatColor.YELLOW + "¡Aprovecha tu salto y llega a la cima!");

                        PotionEffect effect = new PotionEffect(PotionEffectType.JUMP, 20, 3);

                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(35);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 3);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 6:// 7ma
                switch (tickCounter) {
                    case 0:
                        break;
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#7: Dales caña: "
                                + ChatColor.YELLOW + "¡Has todo lo posible por ganar, usa todo lo que tienes!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack witch = new ItemStack(Material.MONSTER_EGG, 1, (short) 66);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 3);
                        arrows.setAmount(32);
                        ItemStack bow = new ItemStack(Material.BOW);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 3);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(arrows);
                        g.giveToAllplayers(witch);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 7: //8va
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#8: Arco y espada: "
                                + ChatColor.YELLOW + "¡Empújalos y llega a la cima!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(32);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 4);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 5);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 8: //9na
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#9: Pega muuuy fuerte: "
                                + ChatColor.YELLOW + "¡Empújalos y llega a la cima!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack witch = new ItemStack(Material.MONSTER_EGG, 1, (short) 66);
                        ItemStack creeper = new ItemStack(Material.MONSTER_EGG, 1, (short) 50);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 8);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(witch);
                        g.giveToAllplayers(creeper);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 9: //10
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#10: Pega y corre: "
                                + ChatColor.YELLOW + "¡Corre y pega!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(32);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 4);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 6);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;
                }
                break;
            case 10: // 11
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#11: Cuida tus flechas: "
                                + ChatColor.YELLOW + "");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(6);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 4);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 7);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;

                }
                break;
            case 11: // 12
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#12: Dales caña! "
                                + ChatColor.YELLOW + "");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        Potion potion = new Potion(PotionType.SPEED, 2, true, false); // 7 segundos
                        Potion potion2 = new Potion(PotionType.SLOWNESS, 2, true, false); // 7 segundos

                        ItemStack potionspeed = potion.toItemStack(8);
                        ItemStack potionslownes = potion2.toItemStack(8);

                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(6);
                        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 5);
                        g.giveToAllplayers(potionspeed);
                        g.giveToAllplayers(potionslownes);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;

                }
                break;
            case 12: // 13
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#13: Triunfo de hierro: "
                                + ChatColor.YELLOW + "¡Escala a la cima para el triunfo!");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        PotionEffect effect = new PotionEffect(PotionEffectType.JUMP, 2, 3);
                        g.giveEffectToAllPlayers(effect);

                        ItemStack slowness = new ItemStack(373, 5, (short) 8234);

                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(30);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.IRON_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 6);
                        sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 7);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(slowness);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;

                }
                break;
            case 13: // 14
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#14: Triunfo de oro: "
                                + ChatColor.YELLOW + "");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack poison = new ItemStack(373, 3, (short) 16388);
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(30);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.GOLD_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 6);
                        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 6);
                        sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 6);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 7);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(poison);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;

                }
                break;
            case 14: // 15
                switch (tickCounter) {
                    case 1:
                        roundConditioner();
                        g.messageAll(ChatColor.GOLD + "#15: Triunfo de diamante: "
                                + ChatColor.YELLOW + "Ultima ronda");
                        g.messageAll(ChatColor.GREEN + "Recompensa: " + reward.get(round) + " pts.");
                        break;
                    case 10:
                        ItemStack creeper = new ItemStack(Material.MONSTER_EGG, 10, (short) 66);
                        ItemStack arrows = new ItemStack(Material.ARROW);
                        arrows.setAmount(30);
                        ItemStack bow = new ItemStack(Material.BOW);
                        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                        sword.addUnsafeEnchantment(Enchantment.KNOCKBACK, 6);
                        sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 3);
                        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 10);
                        g.giveToAllplayers(sword);
                        g.giveToAllplayers(bow);
                        g.giveToAllplayers(creeper);
                        g.giveToAllplayers(arrows);
                        break;
                    case 20:
                        g.openCages();
                        break;
                    case 30:
                        g.closeCages();
                        break;

                }
                break;
            default:
                if (round > 14) {
                    g.announseWinner();
                    g.endGame();
                }
        }
        tickCounter++;
        if (tickCounter > 90 && round >= 0) {
            g.messageAll(ChatColor.ITALIC + "Se agotó el tiempo de la ronda.");
            round++;
            tickCounter = 0;
            g.moveAllToSpawn();
        }

        //g.getPlugin().getLogger().info("Round: " + round + " tick: " + tickCounter);
    }

    private void roundConditioner() {
        g.closeCages();
        List<Entity> entList = g.getArena().capturePoint.getWorld().getEntities();
        for (Entity current : entList) {
            if ((current instanceof Item) || current.getType() == EntityType.CREEPER
                    || current.getType() == EntityType.WITCH) {
                current.remove();
                System.out.println("Eliminada entidad: " + current.getType());
            }

        }
    }

    private void damageControl(Player damager, Player player, boolean melee, boolean headshot, boolean dead, EntityDamageByEntityEvent e) {
        if (headshot) {
            dead = true;
        }
        if (dead) {
            List<Player> involved = new ArrayList<>();
            involved.add(player);
            involved.add(damager);
            if (headshot) {
                g.messageAll(damager.getDisplayName() + ChatColor.GRAY + ChatColor.ITALIC + " >--O--> " + player.getDisplayName(), involved);
            } else {
                if (melee) {
                    g.messageAll(damager.getDisplayName() + ChatColor.GRAY + ChatColor.ITALIC + " -I----- " + player.getDisplayName(), involved);
                } else {
                    g.messageAll(damager.getDisplayName() + ChatColor.GRAY + ChatColor.ITALIC + " >----> " + player.getDisplayName(), involved);
                }
            }
            int score = g.getPlugin().teamMan.getScore(player).getScore();
            g.getPlugin().teamMan.getScore(player).setScore(score - 1);
            score = g.getPlugin().teamMan.getScore(damager).getScore();
            g.getPlugin().teamMan.getScore(damager).setScore(score + 1);
            g.moveToSpawn(player);
            for (int i = 0; i < 5; i++) {
                damager.playSound(player.getLocation(), Sound.ORB_PICKUP, 100, i);
            }
        }
    }

}
