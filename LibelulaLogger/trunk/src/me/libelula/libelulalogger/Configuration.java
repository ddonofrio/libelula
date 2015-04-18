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
package me.libelula.libelulalogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Class Configuration of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Configuration {

    public enum logType {

        INTERNAL, EXTERNAL, BOTH;
    }

    private static class ValidationMessage {

        protected enum MessageType {

            INFO, ERROR, WARNING
        }
        protected TreeMap<String, MessageType> messages;

        public ValidationMessage() {
            this.messages = new TreeMap<>();
        }
    }

    private static class Value {

        protected String dbDirectory;
        protected int maxDBsizeInDisk;
        protected int maxEventsInRam;
        protected List<String> ignoredMaterialsNames;
        protected TreeSet<String> ignoredPlayerNames;
        protected List<String> ignoredWorldsNames;
        protected boolean logOnlyModifiedBlocks;
        protected String wgRegionPolicy;
        protected boolean signsLogToFile;
        protected boolean signsIgnoreEmptyInLog;
        protected logType signTargetLog;
        protected String signsExternalLogFileName;
        protected boolean chestLogToFile;
        protected logType chestTargetLog;
        protected String chestExternalLogFileName;
    }

    private static class Cache {

        protected TreeSet<Material> ignoredMaterials;
        protected TreeSet<World> ignoredWorlds;
        protected boolean wgRegionPolicyIsDefault;

        public Cache() {
            ignoredMaterials = new TreeSet<>();
            ignoredWorlds = new TreeSet<>();
            wgRegionPolicyIsDefault = false;
        }
    }

    static class WorldComparator implements Comparator<World> {

        @Override
        public int compare(World w2, World w1) {
            return (w1.getUID().compareTo(w2.getUID()));
        }
    }
    private final LibelulaLogger plugin;
    private Value values;
    private Cache cache;

    public Configuration(LibelulaLogger plugin) {
        this.plugin = plugin;
        values = loadValues(plugin.getConfig().getDefaults());
        cache = new Cache();
        cache.ignoredMaterials = new TreeSet<>();
        cache.ignoredWorlds = new TreeSet<>(new WorldComparator());
    }

    public void reload() {
        plugin.reloadConfig();
        Value loadedValues = loadValues(plugin.getConfig());
        this.values = validatedConfigChanges(values, loadedValues);
        updateCache();
    }

    public void load() {
        Value oldValues = loadValues(plugin.getConfig().getDefaults());
        plugin.reloadConfig();
        Value loadedValues = loadValues(plugin.getConfig());
        this.values = validatedConfigChanges(oldValues, loadedValues);
        updateCache();
    }

    private void updateCache() {
        this.cache.ignoredMaterials.clear();
        for (String mat : values.ignoredMaterialsNames) {
            this.cache.ignoredMaterials.add(getValidMaterial(mat));
        }
        this.cache.ignoredWorlds.clear();
        for (String worldName : values.ignoredWorldsNames) {
            this.cache.ignoredWorlds.add(plugin.getServer().getWorld(worldName));
        }
        if (values.wgRegionPolicy.equals("EVER")) {
            cache.wgRegionPolicyIsDefault = true;
        } else {
            cache.wgRegionPolicyIsDefault = false;
        }

    }

    private void reportAndClear(ValidationMessage results, CommandSender sender) {
        for (Map.Entry<String, ValidationMessage.MessageType> entry : results.messages.entrySet()) {
            switch (entry.getValue()) {
                case INFO:
                    sender.sendMessage(ChatColor.GREEN + entry.getKey());
                    break;
                case WARNING:
                    sender.sendMessage(ChatColor.YELLOW + entry.getKey());
                    break;
                case ERROR:
                    sender.sendMessage(ChatColor.RED + entry.getKey());
                    break;
            }
        }
        results.messages.clear();
    }

    private void reportAndClear(ValidationMessage results) {
        for (Map.Entry<String, ValidationMessage.MessageType> entry : results.messages.entrySet()) {
            switch (entry.getValue()) {
                case INFO:
                    plugin.logInfo(entry.getKey());
                    break;
                case WARNING:
                    plugin.logWarning(entry.getKey());
                    break;
                case ERROR:
                    plugin.logSevere(entry.getKey());
                    break;
            }
        }
        results.messages.clear();
    }

    private Value validatedConfigChanges(Value oldValues, Value newValues) {
        ValidationMessage results = new ValidationMessage();

        if (!oldValues.dbDirectory.equals(newValues.dbDirectory)) {
            if (!validateDbDirectory(newValues, results)) {
                newValues.dbDirectory = oldValues.dbDirectory;
                reportAndClear(results);
            }
        }

        if (oldValues.ignoredMaterialsNames != newValues.ignoredMaterialsNames) {
            TreeSet<Material> ignoredMaterials = validateIgnoredMaterials(newValues, results);
            newValues.ignoredMaterialsNames.clear();
            for (Material mat : ignoredMaterials) {
                newValues.ignoredMaterialsNames.add(mat.name());
            }
            reportAndClear(results);
        }

        if (oldValues.ignoredPlayerNames != newValues.ignoredPlayerNames) {
            newValues.ignoredPlayerNames = validateIgnoredPlayers(newValues, results);
            reportAndClear(results);
        }

        if (oldValues.ignoredWorldsNames != newValues.ignoredWorldsNames) {
            TreeSet<World> ignoredWorlds = validateIgnoredWorlds(newValues, results);
            newValues.ignoredWorldsNames.clear();
            for (World world : ignoredWorlds) {
                newValues.ignoredWorldsNames.add(world.getName());
            }
            reportAndClear(results);
        }

        if (oldValues.maxDBsizeInDisk != newValues.maxDBsizeInDisk) {
            if (!validateMaxDiskDBsize(newValues, results)) {
                newValues.maxDBsizeInDisk = oldValues.maxDBsizeInDisk;
                reportAndClear(results);
            }
        }

        if (oldValues.maxEventsInRam != newValues.maxEventsInRam) {
            if (!validateMaxEventsInRam(newValues, results)) {
                newValues.maxEventsInRam = oldValues.maxEventsInRam;
                reportAndClear(results);
            }
        }

        if (!oldValues.wgRegionPolicy.equals(newValues.wgRegionPolicy)) {
            if (!validateWgRegionsPolicy(newValues, results)) {
                newValues.wgRegionPolicy = oldValues.wgRegionPolicy;
                reportAndClear(results);
            } else {
                newValues.wgRegionPolicy = newValues.wgRegionPolicy.toUpperCase();
            }
        }
        return newValues;
    }

    private static boolean validateDbDirectory(Value newValues, ValidationMessage results) {
        File dbDirectoryFile = new File(newValues.dbDirectory);
        if (!dbDirectoryFile.mkdirs() && !dbDirectoryFile.isDirectory()) {
            results.messages.put("Unable to create configured database-directory: "
                    .concat(newValues.dbDirectory), ValidationMessage.MessageType.WARNING);
            return false;
        }
        if (!dbDirectoryFile.canRead() || !dbDirectoryFile.canWrite()) {

            results.messages.put("Unable to perform IO operation into configured database-directory: "
                    .concat(newValues.dbDirectory), ValidationMessage.MessageType.WARNING);
            return false;
        }
        return true;
    }

    private static TreeSet<Material> validateIgnoredMaterials(Value newValues, ValidationMessage results) {
        TreeSet<Material> ignoredMaterials = new TreeSet<>();
        for (String matName : newValues.ignoredMaterialsNames) {
            Material mat = getValidMaterial(matName);
            if (mat == null) {
                results.messages.put("Invalid ignored-materials configured: ".concat(matName), ValidationMessage.MessageType.WARNING);
            } else {
                if (!ignoredMaterials.add(mat)) {
                    results.messages.put("Duplicated material in ignored-materials:  ".concat(matName), ValidationMessage.MessageType.WARNING);
                }
            }
        }
        return ignoredMaterials;
    }

    private TreeSet<String> validateIgnoredPlayers(Value newValues, ValidationMessage results) {
        TreeSet<String> names = new TreeSet<>();
        for (String playerName : newValues.ignoredPlayerNames) {
            if (!plugin.getServer().getOfflinePlayer(playerName).hasPlayedBefore()) {
                results.messages.put("ignored-players contains a player which never played: ".concat(playerName), ValidationMessage.MessageType.WARNING);
            }
            if (!names.add(playerName)) {
                results.messages.put("Ignoring duplicated name: ".concat(playerName), ValidationMessage.MessageType.WARNING);
            }
        }
        return names;
    }

    private TreeSet<World> validateIgnoredWorlds(Value newValues, ValidationMessage results) {
        TreeSet<World> ignoredWorlds = new TreeSet<>(new WorldComparator());
        for (String worldName : newValues.ignoredWorldsNames) {
            World ignoredWorld = plugin.getServer().getWorld(worldName);
            if (ignoredWorld == null) {
                results.messages.put("ignored-worlds has a configured world which not exists: ".concat(worldName),
                        ValidationMessage.MessageType.WARNING);
            } else {
                if (!ignoredWorlds.add(ignoredWorld)) {
                    results.messages.put("ignored-worlds has a duplicated world: ".concat(worldName),
                            ValidationMessage.MessageType.WARNING);
                }
            }
        }
        return ignoredWorlds;
    }

    private static boolean validateMaxDiskDBsize(Value newValues, ValidationMessage results) {
        if (newValues.maxDBsizeInDisk < 512) {
            results.messages.put("max-disk-db-size-mb cannot be less than 512MB, wrong configured value: " + newValues.maxDBsizeInDisk,
                    ValidationMessage.MessageType.WARNING);
            return false;
        }

        return true;
    }

    private static boolean validateMaxEventsInRam(Value newValues, ValidationMessage results) {
        if (newValues.maxEventsInRam < 512 || newValues.maxEventsInRam > 2048) {
            results.messages.put("Ignoring max-events-in-ram out of range value: " + newValues.maxEventsInRam,
                    ValidationMessage.MessageType.WARNING);
            results.messages.put("max-events-in-ram should be between 512 and 2048.",
                    ValidationMessage.MessageType.INFO);

            return false;
        }
        return true;
    }

    private static boolean validateWgRegionsPolicy(Value newValues, ValidationMessage results) {
        switch (newValues.wgRegionPolicy.toUpperCase()) {
            case "EVER":
            case "NEVER":
            case "DIFFERS":
                break;
            default:
                results.messages.put("Invalid value for worldguard-regions-policy: " + newValues.maxEventsInRam,
                        ValidationMessage.MessageType.WARNING);
                results.messages.put("Allowed values are: EVER, NEVER and DIFFERS",
                        ValidationMessage.MessageType.INFO);
                return false;
        }
        return true;
    }

    private Value loadValues(ConfigurationSection cs) {
        Value resultValues;
        resultValues = new Value();
        resultValues.dbDirectory = cs.getString("db-engine.database-directory");
        resultValues.maxDBsizeInDisk = cs.getInt("db-engine.max-disk-db-size-mb");
        resultValues.maxEventsInRam = cs.getInt("db-engine.max-events-in-ram");
        resultValues.ignoredMaterialsNames = cs.getStringList("log-policy.ignored-materials");

        resultValues.ignoredPlayerNames = new TreeSet<>();
        for (String playerName : cs.getStringList("log-policy.ignored-players")) {
            resultValues.ignoredPlayerNames.add(playerName);
        }

        resultValues.ignoredWorldsNames = cs.getStringList("log-policy.ignored-worlds");
        resultValues.logOnlyModifiedBlocks = cs.getBoolean("log-policy.only-modified-blocks");
        resultValues.wgRegionPolicy = cs.getString("log-policy.worldguard-regions-policy");

        resultValues.signsLogToFile = cs.getBoolean("log-to-file.signs.active");
        resultValues.signsIgnoreEmptyInLog = cs.getBoolean("log-to-file.signs.ignore-empty");
        resultValues.chestLogToFile = cs.getBoolean("log-to-file.chest.active");

        String auxString = cs.getString("log-to-file.signs.target-log");

        switch (auxString.toUpperCase()) {
            case "EXTERNAL":
                resultValues.signTargetLog = logType.EXTERNAL;
                break;
            case "INTERNAL":
                resultValues.signTargetLog = logType.INTERNAL;
                break;
            default:
                resultValues.signTargetLog = logType.BOTH;
                break;
        }

        auxString = cs.getString("log-to-file.chest.target-log");
        switch (auxString.toUpperCase()) {
            case "EXTERNAL":
                resultValues.chestTargetLog = logType.EXTERNAL;
                break;
            case "INTERNAL":
                resultValues.chestTargetLog = logType.INTERNAL;
                break;
            default:
                resultValues.chestTargetLog = logType.BOTH;
                break;
        }
        resultValues.signsExternalLogFileName = cs.getString("log-to-file.signs.external-log-filename");
        resultValues.chestExternalLogFileName = cs.getString("log-to-file.chest.external-log-filename");
        return resultValues;
    }

    public boolean logChestToFile() {
        return values.chestLogToFile;
    }

    public logType getLogTypeForChest() {
        return values.chestTargetLog;
    }

    public String getChestExternalLogFileName() {
        return values.chestExternalLogFileName;
    }

    public boolean logSignsToFile() {
        return values.signsLogToFile;
    }

    public boolean ignoreEmptySigns() {
        return values.signsIgnoreEmptyInLog;
    }

    public logType getLogTypeForSigns() {
        return values.signTargetLog;
    }

    public String getSignsExternalLogFileName() {
        return values.signsExternalLogFileName;
    }

    public static Material getValidMaterial(String stringMaterial) {
        Material m;

        if (stringMaterial.contains(":") && stringMaterial.split(":").length == 2) {
            try {
                int i = Integer.parseInt(stringMaterial.split(":")[1]);
                if (i > 255 || i < 0) {
                    return null;
                }

            } catch (Exception ex) {
                return null;
            }
            stringMaterial = stringMaterial.split(":")[0];
        }
        try {
            int id = Integer.parseInt(stringMaterial);
            m = Material.getMaterial(id);
        } catch (NumberFormatException e) {
            m = Material.matchMaterial(stringMaterial);
        }
        return m;
    }

    @Override
    public String toString() {
        return "database-directory: " + values.dbDirectory + "|"
                + "max-disk-db-size-mb: " + values.maxDBsizeInDisk + "|"
                + "max-events-in-ram: " + values.maxEventsInRam + "|"
                + "ignored-materials: " + values.ignoredMaterialsNames.toString() + "|"
                + "ignored-players: " + values.ignoredPlayerNames.toString() + "|"
                + "ignored-worlds: " + values.ignoredWorldsNames.toString() + "|"
                + "only-modified-blocks: " + (values.logOnlyModifiedBlocks ? "True" : "False") + "|"
                + "worldguard-regions-policy: " + values.wgRegionPolicy + "|"
                + "chest-log-to-file: " + (values.chestLogToFile ? "True" : "False") + "|"
                + "chest-target-log: " + values.chestTargetLog.toString() + "|"
                + "chest-external-log-filename: " + values.chestExternalLogFileName + "|"
                + "signs-log-to-file: " + (values.signsLogToFile ? "True" : "False") + "|"
                + "signs-ignore-empty-in-log: " + (values.signsIgnoreEmptyInLog ? "True" : "False") + "|"
                + "signs-target-log: " + values.signTargetLog.toString() + "|"
                + "signs-external-log-filename: " + values.signsExternalLogFileName;
    }

    public String getdbDirectory() {
        return values.dbDirectory;
    }

    public int getMaxDiskDBsizeMB() {
        return values.maxDBsizeInDisk;
    }

    public int getMaxEventsInRAM() {
        return values.maxEventsInRam;
    }

    public TreeSet<Material> getIgnoredMaterials() {
        return cache.ignoredMaterials;
    }

    public TreeSet<String> getIgnoredPlayerNames() {
        return values.ignoredPlayerNames;
    }

    public TreeSet<World> getIgnoredWorlds() {
        return cache.ignoredWorlds;
    }

    public boolean getFlagValue(String flagName) {
        boolean result;
        switch (flagName) {
            case "only-modified-blocks":
                result = values.logOnlyModifiedBlocks;
                break;
            default:
                result = false;
        }
        return result;
    }

    public boolean wgRegionPolicyIsDefault() {
        return cache.wgRegionPolicyIsDefault;
    }

    public String getWgRegionPolicy() {
        return values.wgRegionPolicy;
    }

    public void setValue(String key, String value, CommandSender sender) {
        Value newvalues = new Value();
        ValidationMessage results = new ValidationMessage();

        switch (key.toLowerCase()) {
            case "database-directory":
                newvalues.dbDirectory = value;
                if (validateDbDirectory(newvalues, results)) {
                    values.dbDirectory = value;
                }
                break;
            case "max-disk-db-size-mb":
                try {
                    newvalues.maxDBsizeInDisk = Integer.parseInt(value);
                    if (validateMaxDiskDBsize(newvalues, results)) {
                        values.maxDBsizeInDisk = newvalues.maxDBsizeInDisk;
                    }
                } catch (Exception ex) {
                    sender.sendMessage(ChatColor.RED + "This value must be a integer number.");
                }
                break;
            case "max-events-in-ram":
                try {
                    newvalues.maxEventsInRam = Integer.parseInt(value);
                    if (validateMaxEventsInRam(newvalues, results)) {
                        values.maxEventsInRam = newvalues.maxEventsInRam;
                    }
                } catch (Exception ex) {
                    sender.sendMessage(ChatColor.RED + "This value must be a integer number.");
                }

                break;
            case "ignored-materials":
                newvalues.ignoredMaterialsNames = new ArrayList<>();
                newvalues.ignoredMaterialsNames.addAll(Arrays.asList(value.split(",")));
                TreeSet<Material> ignoredMats = validateIgnoredMaterials(newvalues, results);
                values.ignoredMaterialsNames.clear();
                for (Material mat : ignoredMats) {
                    values.ignoredMaterialsNames.add(mat.name());
                }
                break;
            case "ignored-players":
                newvalues.ignoredPlayerNames = new TreeSet<>();
                newvalues.ignoredPlayerNames.addAll(Arrays.asList(value.split(",")));
                values.ignoredPlayerNames = validateIgnoredPlayers(newvalues, results);
                break;
            case "ignored-worlds":
                newvalues.ignoredWorldsNames = new ArrayList<>();
                newvalues.ignoredWorldsNames.addAll(Arrays.asList(value.split(",")));
                TreeSet<World> worlds = validateIgnoredWorlds(newvalues, results);
                values.ignoredWorldsNames.clear();
                for (Iterator<World> it = worlds.iterator(); it.hasNext();) {
                    World w = it.next();
                    values.ignoredWorldsNames.add(w.getName());
                }
                break;
            case "only-modified-blocks":
                switch (value.toLowerCase()) {
                    case "true":
                        values.logOnlyModifiedBlocks = true;
                        break;
                    case "false":
                        values.logOnlyModifiedBlocks = false;
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Only True or False are possible values for this key.");
                }
                break;
            case "worldguard-regions-policy":
                newvalues.wgRegionPolicy = value;
                if (validateWgRegionsPolicy(newvalues, results)) {
                    values.wgRegionPolicy = value.toUpperCase();
                }
                break;
            case "signs-log-to-file":
                if (validateBoolean(value)) {
                    values.signsLogToFile = toBoolean(value);
                } else {
                    sender.sendMessage(ChatColor.RED + "Only True or False are possible values for this key.");
                }
                break;
            case "chest-log-to-file":
                if (validateBoolean(value)) {
                    values.signsLogToFile = toBoolean(value);
                } else {
                    sender.sendMessage(ChatColor.RED + "Only True or False are possible values for this key.");
                }
                break;
            case "signs-ignore-empty-in-log":
                if (validateBoolean(value)) {
                    values.signsIgnoreEmptyInLog = toBoolean(value);
                } else {
                    sender.sendMessage(ChatColor.RED + "Only True or False are possible values for this key.");
                }
                break;
            case "chest-target-log":
                switch (value.toUpperCase()) {
                    case "INTERNAL":
                        values.chestTargetLog = logType.INTERNAL;
                        break;
                    case "EXTERNAL":
                        values.chestTargetLog = logType.EXTERNAL;
                        break;
                    case "BOTH":
                        values.chestTargetLog = logType.BOTH;
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Only INTERNAL, EXTERNAL or BOTH are possible values for this key.");
                }
                break;
            case "signs-target-log":
                switch (value.toUpperCase()) {
                    case "INTERNAL":
                        values.signTargetLog = logType.INTERNAL;
                        break;
                    case "EXTERNAL":
                        values.signTargetLog = logType.EXTERNAL;
                        break;
                    case "BOTH":
                        values.signTargetLog = logType.BOTH;
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Only INTERNAL, EXTERNAL or BOTH are possible values for this key.");
                }
                break;
            case "signs-external-log-filename":
                values.signsExternalLogFileName = value;
                break;
            case "chest-external-log-filename":
                values.signsExternalLogFileName = value;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "The configuration key \"" + key + "\" does not exists.");
        }
        reportAndClear(results, sender);
        updateCache();
    }

    private boolean toBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean validateBoolean(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return true;
        } else {
            return false;
        }
    }

    public void delValue(String key, CommandSender sender) {
        Value defaultValues = loadValues(plugin.getConfig().getDefaults());
        switch (key.toLowerCase()) {
            case "database-directory":
                values.dbDirectory = defaultValues.dbDirectory;
                break;
            case "max-disk-db-size-mb":
                values.maxDBsizeInDisk = defaultValues.maxDBsizeInDisk;
                break;
            case "max-events-in-ram":
                values.maxEventsInRam = defaultValues.maxEventsInRam;
                break;
            case "ignored-materials":
                values.ignoredMaterialsNames = defaultValues.ignoredMaterialsNames;
                break;
            case "ignored-players":
                values.ignoredPlayerNames = defaultValues.ignoredPlayerNames;
                break;
            case "ignored-worlds":
                values.ignoredWorldsNames = defaultValues.ignoredWorldsNames;
                break;
            case "only-modified-blocks":
                values.logOnlyModifiedBlocks = defaultValues.logOnlyModifiedBlocks;
                break;
            case "worldguard-regions-policy":
                values.wgRegionPolicy = defaultValues.wgRegionPolicy;
                break;
            case "chest-log-to-file":
                values.chestLogToFile = defaultValues.chestLogToFile;
                break;
            case "chest-target-log":
                values.chestTargetLog = defaultValues.chestTargetLog;
                break;
            case "chest-external-log-filename":
                values.chestExternalLogFileName = defaultValues.chestExternalLogFileName;
                break;
            case "signs-log-to-file":
                values.signsLogToFile = defaultValues.signsLogToFile;
                break;
            case "signs-ignore-empty-in-log":
                values.signsIgnoreEmptyInLog = defaultValues.signsIgnoreEmptyInLog;
                break;
            case "signs-target-log":
                values.signTargetLog = defaultValues.signTargetLog;
                break;
            case "signs-external-log-filename":
                values.signsExternalLogFileName = defaultValues.signsExternalLogFileName;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "The configuration key \"" + key + "\" does not exists.");
                return;
        }
        updateCache();
    }

    public void persistConfiguration() {
        List<String> ignoredPlayer = new ArrayList<>();
        ignoredPlayer.addAll(values.ignoredPlayerNames);
        plugin.getConfig().set("db-engine.database-directory", values.dbDirectory);
        plugin.getConfig().set("db-engine.max-disk-db-size-mb", values.maxDBsizeInDisk);
        plugin.getConfig().set("db-engine.max-events-in-ram", values.maxEventsInRam);
        plugin.getConfig().set("log-policy.ignored-materials", values.ignoredMaterialsNames);
        plugin.getConfig().set("log-policy.ignored-players", ignoredPlayer);
        plugin.getConfig().set("log-policy.ignored-worlds", values.ignoredWorldsNames);
        plugin.getConfig().set("log-policy.only-modified-blocks", values.logOnlyModifiedBlocks);
        plugin.getConfig().set("log-policy.worldguard-regions-policy", values.wgRegionPolicy);
        plugin.getConfig().set("log-to-file.chest.active", values.chestLogToFile);
        plugin.getConfig().set("log-to-file.chest.target-log", values.chestTargetLog.toString());
        plugin.getConfig().set("log-to-file.chest.external-log-filename", values.chestExternalLogFileName);
        plugin.getConfig().set("log-to-file.signs.active", values.signsLogToFile);
        plugin.getConfig().set("log-to-file.signs.ignore-empty", values.signsIgnoreEmptyInLog);
        plugin.getConfig().set("log-to-file.signs.target-log", values.signTargetLog.toString());
        plugin.getConfig().set("log-to-file.signs.external-log-filename", values.signsExternalLogFileName);
        plugin.saveConfig();
    }
}