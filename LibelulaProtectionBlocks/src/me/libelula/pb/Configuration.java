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
import com.sk89q.worldguard.protection.flags.StateFlag;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Class Configuration of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Configuration {

    private final LibelulaProtectionBlocks plugin;
    private FileConfiguration fc;
    private List<World> ignoredWorldList;
    private TreeSet<String> playerFlags;
    private final Lock _ignoredWorldList_mutex;

    public Configuration(LibelulaProtectionBlocks plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.ignoredWorldList = new ArrayList<>();
        playerFlags = new TreeSet<>();
        _ignoredWorldList_mutex = new ReentrantLock(true);
        reload();
    }

    public boolean isPlayerFlag(String flagName) {
        return playerFlags.contains(flagName);
    }

    public TreeSet<String> getPlayerConfigurableFlags() {
        return playerFlags;
    }

    public void persist() {
        List<String> worldNames = new ArrayList<>();
        _ignoredWorldList_mutex.lock();
        try {
            for (Iterator<World> it = this.ignoredWorldList.iterator(); it.hasNext();) {
                World world = it.next();
                worldNames.add(world.getName());
            }
        } finally {
            _ignoredWorldList_mutex.unlock();
        }
        fc.set("ignored.worlds", worldNames);
        plugin.saveConfig();
    }

    public boolean isOldPsImported() {
        return fc.getBoolean("ps-backward-compatibility.imported");
    }

    public void setOldPsImported(boolean state) {
        fc.set("ps-backward-compatibility.imported", state);
    }

    public int getOldPsMode() {
        switch (fc.getString("ps-backward-compatibility.mode")) {
            case "old":
                return 1;
            case "new":
                return 0;
            default:
                plugin.getLogger().log(Level.WARNING, "Invalid configuration for ps-backward-compatibility.mode: {0}",
                        fc.getString("ps-backward-compatibility.mode"));
                setOldPsMode(0);
                return 0;
        }
    }

    public void setOldPsMode(int mode) {
        switch (mode) {
            case 1:
                fc.set("ps-backward-compatibility.mode", "old");
                break;
            case 0:
                fc.set("ps-backward-compatibility.mode", "new");
                break;
        }
    }

    public void setFlags(HashMap<String, String> flags) {
        if (flags != null) {
            for (Map.Entry<String, String> flag : flags.entrySet()) {
                fc.set("ps-default.flags.".concat(flag.getKey()), flag.getValue());
            }
        }
    }

    public HashMap<String, String> getStringFlags() {
        HashMap<String, String> flags = new HashMap<>();
        for (String key : fc.getConfigurationSection("ps-default.flags").getKeys(false)) {
            flags.put(key, fc.getString("ps-default.flags.".concat(key)));
        }
        return flags;
    }

    public Map<Flag<?>, Object> getFlags(String player) {
        Map<Flag<?>, Object> flags = new TreeMap<>(new WorldGuardManager.FlagComparator());
        for (Flag<?> df : DefaultFlag.flagsList) {
            for (String key : fc.getConfigurationSection("ps-default.flags").getKeys(false)) {
                if (df.getName().toString().equalsIgnoreCase(key)) {
                    if (fc.getString("ps-default.flags.".concat(key)).equalsIgnoreCase("deny")) {
                        flags.put(df, StateFlag.State.DENY);
                    } else if (fc.getString("ps-default.flags.".concat(key)).equalsIgnoreCase("allow")) {
                        flags.put(df, StateFlag.State.ALLOW);
                    } else {
                        flags.put(df, (Object) fc.getString("ps-default.flags.".concat(key)).replace("%player%", player));
                    }
                }
            }
        }
        return flags;
    }

    public void setOldPsUseFullYaxis(boolean value) {
        fc.set("ps-backward-compatibility.full-y-axis", value);
    }

    public boolean getOldPsUseFullYaxis() {
        return fc.getBoolean("ps-backward-compatibility.full-y-axis");
    }

    public void setOldPsAutoHide(boolean value) {
        fc.set("ps-backward-compatibility.auto-hide", value);
    }

    public boolean getOldPsAutoHide() {
        return fc.getBoolean("ps-backward-compatibility.auto-hide");
    }

    public void setOldPsNoDrop(boolean value) {
        fc.set("ps-backward-compatibility.no-drops", value);
    }

    public boolean getOldPsNoDrop() {
        return fc.getBoolean("ps-backward-compatibility.no-drops");
    }

    public void addIgnoredWorld(World world) {
        _ignoredWorldList_mutex.lock();
        try {
            ignoredWorldList.add(world);
        } finally {
            _ignoredWorldList_mutex.unlock();
        }
    }

    public boolean ignoredWorldContains(World world) {
        return ignoredWorldList.contains(world);
    }

    public boolean setLenguage(String langName) {
        if (langName == null) {
            return false;
        }
        if (langName.length() == 2) {
            langName = langName.toLowerCase();
            switch (langName) {
                case "en":
                case "es":
                case "it":
                case "pt":
                    fc.set("language", langName);
                    return true;
                default:
                    return false;
            }
        } else if (langName.length() == 4) {
            if (langName.equalsIgnoreCase("enUS")) {
                fc.set("language", "enUS");
                return true;
            }
            if (langName.equalsIgnoreCase("esES")) {
                fc.set("language", "esES");
                return true;
            }
            if (langName.equalsIgnoreCase("esMX")) {
                fc.set("language", "esMX");
                return true;
            }
            if (langName.equalsIgnoreCase("itIT")) {
                fc.set("language", "itIT");
                return true;
            }
            if (langName.equalsIgnoreCase("ptBR")) {
                fc.set("language", "ptBR");
                return true;
            }
        }
        return false;
    }

    public FileConfiguration getLanguage() {
        File langFile;
        switch (fc.getString("language")) {
            case "en":
            case "enUS":
                langFile = new File(plugin.getDataFolder(), "enUS.yml");
                break;
            case "es":
            case "esES":
                langFile = new File(plugin.getDataFolder(), "esES.yml");
                break;
            case "esMX":
                langFile = new File(plugin.getDataFolder(), "esMX.yml");
                break;
            case "it":
            case "itIT":
                langFile = new File(plugin.getDataFolder(), "itIT.yml");
                break;
            case "pt":
            case "ptBR":
                langFile = new File(plugin.getDataFolder(), "ptBR.yml");
                break;
            default:
                langFile = null;
        }
        if (langFile == null) {
            return null;
        }
        if (langFile.exists()) {
            langFile.delete();
        }
        plugin.saveResource(langFile.getName(), false);
        return YamlConfiguration.loadConfiguration(langFile);
    }

    public final void reload() {
        plugin.reloadConfig();
        fc = plugin.getConfig();
        ignoredWorldList.clear();
        for (String worldName : fc.getStringList("ignored.worlds")) {
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                addIgnoredWorld(world);
            } else {
                plugin.getLogger().warning("Invalid configured world name in ignored worlds: ".concat(worldName));
            }
        }

        for (String flagName : fc.getStringList("player.configurable-flags")) {
            flagName = flagName.toLowerCase();
            if (!DefaultFlag.fuzzyMatchFlag(flagName).getName().equals(flagName)) {
                plugin.getLogger().warning("Invalid configured player configurable-flags name: ".concat(flagName));
            } else {
                playerFlags.add(flagName);
            }
        }
        if (plugin.i18n != null) {
            plugin.i18n.setLang(getLanguage());
        }
    }
}
