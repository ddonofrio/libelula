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
package me.libelula.capturethewool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 *
 */
public final class LangManager {

    private final Main plugin;
    private final YamlConfiguration lang;
    private final String messagePrefix;
    private final int minVersion = 5;

    public LangManager(Main plugin) {
        this.plugin = plugin;
        lang = new YamlConfiguration();
        File langFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("lang-file"));
        if (!langFile.exists()) {
            saveDefaultLangFiles();
        }
        if (langFile.exists()) {
            try {
                lang.load(langFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }
            int langVersion = lang.getInt("version", 0);
            if (langVersion < minVersion && (langFile.getName().equals("spanish.yml")
                    || langFile.getName().equals("english.yml"))
                    || langFile.getName().equals("italian.yml")) { // Texts must be updated.
                File backUpFile = new File(langFile.getParent(), langFile.getName() + "-" + langVersion + ".bak");
                langFile.renameTo(backUpFile);
                plugin.saveResource(langFile.getName(), true);
                try {
                    lang.load(langFile);
                } catch (IOException | InvalidConfigurationException ex) {
                    plugin.getLogger().severe(ex.toString());
                }
            }
        } else {
            plugin.getLogger().severe("Configured language file does not exists: ".concat(langFile.getAbsolutePath()));
        }
        messagePrefix = ChatColor.translateAlternateColorCodes('&', lang.getString("message-prefix"));
    }
    
    public void saveDefaultLangFiles() {
        File defaultLangFile;
        defaultLangFile = new File(plugin.getDataFolder(), "spanish.yml");
        if (!defaultLangFile.exists()) {
            plugin.saveResource(defaultLangFile.getName(), false);
        }
        defaultLangFile = new File(plugin.getDataFolder(), "english.yml");
        if (!defaultLangFile.exists()) {
            plugin.saveResource(defaultLangFile.getName(), false);
        }
        defaultLangFile = new File(plugin.getDataFolder(), "italian.yml");
        if (!defaultLangFile.exists()) {
            plugin.saveResource(defaultLangFile.getName(), false);
        }
    }

    public String getText(String label) {
        String text = lang.getString(label);
        if (text == null) {
            text = label;
        } else {
            text = ChatColor.translateAlternateColorCodes('&', text);
        }
        return text;
    }

    public String getMessage(String label) {
        return messagePrefix + " " + getText(label);
    }

    public void sendMessage(String label, Player player) {
        player.sendMessage(getMessage(label));
    }

    public void sendMessage(String label, CommandSender cs) {
        cs.sendMessage(getMessage(label));
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public void sendText(String baseLabel, Player player) {
        if (lang.getString(baseLabel) == null) {
            sendMessage(baseLabel, player);
            return;
        }
        for (String label : lang.getConfigurationSection(baseLabel).getKeys(false)) {
            sendMessage(baseLabel + "." + label, player);
        }
    }

    public ItemStack getHelpBook() {
        List<String> bookPages = new ArrayList<>();
        for (String page : lang.getConfigurationSection("help-book.pages").getKeys(false)) {
            try {
                Integer.parseInt(page);
            } catch (NumberFormatException ex) {
                continue;
            }
            String textPage = "";
            for (String line : lang.getConfigurationSection("help-book.pages." + page).getKeys(false)) {
                if (line != null) {
                    String text = ChatColor.translateAlternateColorCodes('&', lang.getString("help-book.pages." + page + "." + line));
                    textPage = textPage.concat(text).concat("\n");
                }
            }
            bookPages.add(textPage);
        }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta bm = (BookMeta) book.getItemMeta();
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', lang.getString("help-book.title")));
        bm.setAuthor(ChatColor.translateAlternateColorCodes('&', lang.getString("help-book.author")));
        bm.setTitle(ChatColor.translateAlternateColorCodes('&', lang.getString("help-book.title")));
        bm.setPages(bookPages);
        bm.addEnchant(Enchantment.LUCK, 1, true);
        book.setItemMeta(bm);
        return book;
    }

    public void sendVerbatimTextToWorld(String text, World world, Player filter) {
        for (Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(messagePrefix + " " + text);
        }
    }

    public void sendMessageToWorld(String label, World world, Player filter) {
        String text = getText(label);
        for (Player receiver : world.getPlayers()) {
            if (filter != null && receiver.getName().equals(filter.getName())) {
                continue;
            }
            receiver.sendMessage(messagePrefix + " " + text);
        }
    }

    public void sendMessageToTeam(String label, Player player) {
        sendVerbatimMessageToTeam(getText(label), player);
    }

    public void sendVerbatimMessageToTeam(String message, Player player) {
        TeamManager.TeamId playerTeam = plugin.pm.getTeamId(player);
        for (Player receiver : player.getWorld().getPlayers()) {
            if (playerTeam == plugin.pm.getTeamId(receiver)) {
                receiver.sendMessage(messagePrefix + " " + message);
            }
        }
    }

    public String getMurderText(Player player, Player killer, ItemStack is) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.by-player.message"));
        ret = ret.replace("%KILLER%", killer.getName());
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLER_COLOR%", plugin.pm.getChatColor(killer) + "");
        ret = ret.replace("%KILLED_COLOR%", plugin.pm.getChatColor(player) + "");
        String how;
        if (is != null) {
            how = lang.getString("death-events.by-player.melee.".concat(is.getType().name()));
            if (how == null) {
                how = lang.getString("death-events.by-player.melee._OTHER_");
            }
        } else {
            how = lang.getString("death-events.by-player.melee.PULL");
        }
        ret = ret.replace("%HOW%", how);
        return ret;
    }

    public String getRangeMurderText(Player player, Player killer, int distance, boolean headshoot) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.by-player.message"));
        ret = ret.replace("%KILLER%", killer.getName());
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLER_COLOR%", plugin.pm.getChatColor(killer) + "");
        ret = ret.replace("%KILLED_COLOR%", plugin.pm.getChatColor(player) + "");
        if (headshoot) {
            ret = ret.replace("%HOW%", lang.getString("death-events.by-player.range.HEADSHOT"));
        } else {
            ret = ret.replace("%HOW%", lang.getString("death-events.by-player.range.BODYSHOT"));
        }
        ret = ret.replace("%DISTANCE%", distance + "");
        return ret;
    }

    public String getNaturalDeathText(Player player, EntityDamageEvent.DamageCause cause) {
        String ret = ChatColor.translateAlternateColorCodes('&',
                lang.getString("death-events.natural.message"));
        ret = ret.replace("%KILLED%", player.getName());
        ret = ret.replace("%KILLED_COLOR%", plugin.pm.getChatColor(player) + "");
        String how = lang.getString("death-events.natural.cause.".concat(cause.name()));
        if (how == null) {
            how = lang.getString("death-events.natural.cause._OTHER_");
        }
        ret = ret.replace("%HOW%", how);
        return ret;
    }

}
