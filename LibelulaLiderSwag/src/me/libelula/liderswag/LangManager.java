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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class LangManager {

    private final Main plugin;
    private final YamlConfiguration lang;
    public final String messagePrefix;

    public LangManager(Main plugin) {
        this.plugin = plugin;
        lang = new YamlConfiguration();
        File langFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("lang-file"));

        ///DEBUG
        ///DEBUG
        ///DEBUG
        ///DEBUG
        ///DEBUG
        plugin.saveResource(langFile.getName(), true);
        ///DEBUG
        ///DEBUG
        ///DEBUG
        ///DEBUG

        if (!langFile.exists()) {
            plugin.saveResource(langFile.getName(), false);
        }
        if (langFile.exists()) {
            try {
                lang.load(langFile);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().severe(ex.toString());
            }
        } else {
            plugin.getLogger().severe("Configured language file does not exists: ".concat(langFile.getAbsolutePath()));
        }
        String prefix = plugin.getConfig().getString("plugin-text-prefix");
        if (prefix != null) {
            messagePrefix = ChatColor.translateAlternateColorCodes('&', prefix);
        } else {
            messagePrefix = "";
        }
    }

    public String getTranslatedText(String label) {
        String text = lang.getString(label);
        if (text == null) {
            text = label;
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getText(String label, boolean includePrefix) {
        String text = getTranslatedText(label);
        if (text == null) {
            return null;
        }
        if (includePrefix) {
            return messagePrefix + text;
        } else {
            return text;
        }
    }

    public String getText(String label) {
        return getText(label, true);
    }
    
    public List<String> getTexts(String label) {
        List<String> result = new ArrayList<>();
        ConfigurationSection cs = lang.getConfigurationSection(label);
        if (cs == null) {
            return null;
        }
        for (String key : cs.getKeys(false)) {
            result.add(messagePrefix + cs.getString(key));
        }
        return result;
    }

    public void sendTexts(CommandSender cs, String label) {
        for (String line : getTexts(label)) {
            cs.sendMessage(line);
        }
    }

    public void sendText(CommandSender cs, String label) {
        cs.sendMessage(getText(label));
    }

    public String getText(String label, TreeMap<String, String> replacements) {
        String text = getTranslatedText(label);
        if (text == null) {
            return null;
        }
        for (String find : replacements.keySet()) {
            text = text.replace(find, replacements.get(find));
        }

        return messagePrefix + text;
    }

}
