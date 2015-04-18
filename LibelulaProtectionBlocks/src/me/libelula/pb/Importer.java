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

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Class Importer of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Importer {

    public static void importFromPS(LibelulaProtectionBlocks plugin) {
        File psConfigFile = new File("plugins/ProtectionStones/config.yml");
        if (psConfigFile.exists()) {
            FileConfiguration psConfig = YamlConfiguration.loadConfiguration(psConfigFile);
            for (String blockLine : psConfig.getStringList("Blocks")) {
                try {
                    Material material = Material.getMaterial(blockLine.split(" ")[0]);
                    int size = Integer.parseInt(blockLine.split(" ")[1]);
                    plugin.pbs.addOldPsBlock(material, size);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Error importing old PS Block list: {0}", ex.toString());
                }
            }

            HashMap<String, String> flags = new HashMap<>();
            for (String flagLine : psConfig.getStringList("Flags")) {
                try {
                    String flagname = flagLine.split(" ")[0];
                    String value = flagLine.substring(flagLine.indexOf(" ") + 1);
                    flags.put(flagname, value);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Error importing old PS Flag list: {0}", ex.toString());
                }
            }
            plugin.config.setFlags(flags);

            for (String ignoredWorld : psConfig.getString("Exclusion.WORLDS").split(" ")) {
                try {
                    World world = plugin.getServer().getWorld(ignoredWorld);
                    if (world != null) {
                        plugin.config.addIgnoredWorld(world);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Error importing old PS World list: {0}", ex.toString());
                }
            }
            plugin.config.setOldPsUseFullYaxis(psConfig.getBoolean("Region.SKYBEDROCK"));
            plugin.config.setOldPsAutoHide(psConfig.getBoolean("Region.AUTOHIDE"));
            plugin.config.setOldPsNoDrop(psConfig.getBoolean("Region.NODROP"));

        }

        String rex = ProtectionBlocks.regionIdRegexp;
        Pattern p = Pattern.compile("-?\\d+");
        int inc = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (Map.Entry<String, ProtectedRegion> regionSet : plugin.wgm.getRegions(world).entrySet()) {
                if (regionSet.getKey().matches(rex)) {
                    Matcher m = p.matcher(regionSet.getKey());
                    m.find();
                    int x = Integer.parseInt(m.group());
                    m.find();
                    int y = Integer.parseInt(m.group());
                    m.find();
                    int z = Integer.parseInt(m.group());

                    int regionSize = regionSet.getValue().getMaximumPoint().getBlockX()
                            - regionSet.getValue().getMinimumPoint().getBlockX();

                    Material material = Material.SPONGE;
                    Byte materialData = 0;
                    for (Map.Entry<Material, Integer> e : plugin.pbs.getoldPSs().entrySet()) {
                        if ((e.getValue() * 2) + 1 == regionSize) {
                            material = e.getKey();
                        }
                    }

                    Location location = new Location(world, x, y, z);
                    Boolean hidden = true;
                    if (plugin.pbs.oldPScontainsBlock(location.getBlock().getType())) {
                        hidden = false;
                        material = location.getBlock().getType();
                    }
                    regionSize++;
                    String name = plugin.i18n.getText("protection") + ": " + regionSize + " x " + regionSize + " x " + regionSize + " (" + plugin.i18n.getText("blocks") + ")";
                    List<String> lore = new ArrayList<>();
                    lore.add("Old protection stone");
                    lore.add("Imported from old plugin");
                    lore.add(ProtectionController.getFullHashFromValues(regionSize, regionSize, regionSize, material.getId(), inc));
                    int secondsFromEpoch = 0;
                    if (regionSet.getValue().getOwners().size() != 0) {
                        String playerName = regionSet.getValue().getOwners().getPlayers().iterator().next();
                        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
                        if (player != null) {
                            secondsFromEpoch = (int) (player.getLastPlayed() / 1000);
                        }
                    }
                    if (secondsFromEpoch == 0) {
                        secondsFromEpoch = (int) (new Date().getTime() / 1000);
                    }
                    plugin.pbs.addProtectionBlock(location, regionSet.getValue(), material, hidden, name, lore, materialData, secondsFromEpoch);
                    inc++;
                }
            }
        }
        plugin.getLogger().log(Level.INFO, "{0} Protection Stones has been imported.", plugin.pbs.size());
        if (plugin.pbs.size() != 0) {
            plugin.config.setOldPsImported(true);
            plugin.config.persist();
        }
    }
}
