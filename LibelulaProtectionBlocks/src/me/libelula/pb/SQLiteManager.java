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

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Class SQLiteManager of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class SQLiteManager {

    private Connection sqlConn;
    private final LibelulaProtectionBlocks plugin;

    public SQLiteManager(LibelulaProtectionBlocks plugin) {
        this.plugin = plugin;
        File sqlFile = new File(plugin.getDataFolder(), "lps.db");
        boolean createTables = false;
        if (!sqlFile.exists()) {
            createTables = true;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            sqlConn = DriverManager.getConnection("jdbc:sqlite:".concat(sqlFile.getAbsolutePath()));
        } catch (SQLException | ClassNotFoundException ex) {
            plugin.getLogger().severe("Error connecting with DB: ".concat(ex.toString()));
            this.sqlConn = null;
            return;
        }

        if (createTables) {
            try {
                createTables();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Error creating DB: ".concat(ex.toString()));
                this.sqlConn = null;
                return;
            }
        }
    }

    public boolean isInitialized() {
        return (this.sqlConn != null);
    }

    private void createTables() throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        sqlStatement.executeUpdate("CREATE TABLE protection_block "
                + "(world_name TEXT, x INTEGER, y INTEGER, z INTEGER, "
                + "region_name TEXT, material_id INTEGER, hidden INTEGER, "
                + "name TEXT, lore TEXT, material_data INTEGER, "
                + "date INTEGER, "
                + "PRIMARY KEY(x, y, z, world_name));");
        sqlStatement.executeUpdate("CREATE TABLE protection_id "
                + "(hash TEXT, PRIMARY KEY(hash));");
        sqlStatement.close();
    }

    public void insertPSBlocks(ProtectionBlocks.PSBlocks psBlock) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        String lore = "";
        for (String l : psBlock.lore) {
            lore = lore.concat(l).concat("\n");
        }

        lore = lore.substring(0, lore.length() - 1);

        sqlStatement.executeUpdate("INSERT INTO protection_block "
                + "(world_name,x, y, z, "
                + "region_name, material_id, hidden, "
                + "name, lore, material_data, "
                + "date)"
                + " values ("
                + "\"" + psBlock.location.getWorld().getName() + "\", "
                + psBlock.location.getBlockX() + ","
                + psBlock.location.getBlockY() + ","
                + psBlock.location.getBlockZ() + ","
                + "\"" + psBlock.region.getId() + "\", "
                + psBlock.material.getId() + ", "
                + (psBlock.hidden ? 1 : 0) + ", "
                + "\"" + psBlock.name + "\", "
                + "\"" + lore + "\", "
                + psBlock.materialData.intValue() + ", "
                + psBlock.secondsFromEpoch
                + ");");
        sqlStatement.close();
    }

    public void removePSBlocks(Location location) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        sqlStatement.executeUpdate("DELETE FROM protection_block WHERE "
                + "x = " + location.getBlockX() + " and "
                + "y = " + location.getBlockY() + " and "
                + "z = " + location.getBlockZ() + " and "
                + "world_name = '" + location.getWorld().getName() + "'"
                + ";");
        sqlStatement.close();
    }

    public void updatePSBlockInfo(ProtectionBlocks.PSBlocks psb) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        String lore = "";
        for (String l : psb.lore) {
            lore = lore.concat(l).concat("\n");
        }
        sqlStatement.executeUpdate("UPDATE protection_block"
                + " SET "
                + "region_name = '" + psb.region.getId() + "',"
                + "material_id = " + psb.material.getId() + ","
                + "lore = '" + lore + "'"
                + " WHERE "
                + "x = " + psb.location.getBlockX() + " and "
                + "y = " + psb.location.getBlockY() + " and "
                + "z = " + psb.location.getBlockZ() + " and "
                + "world_name = '" + psb.location.getWorld().getName() + "'"
                + ";");
    }

    public void loadPSBlocks(ProtectionBlocks pb) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        ResultSet rs = sqlStatement.executeQuery("SELECT * FROM protection_block;");
        while (rs.next()) {
            World world = plugin.getServer().getWorld(rs.getString("world_name"));
            if (world == null) {
                plugin.getLogger().warning("Ignoring record from DB, invalid configured world: ".concat(rs.getString("world_name")));
                continue;
            }
            Location location = new Location(world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
            RegionManager rm = plugin.wgm.getRegionManager(world);
            ProtectedRegion region = rm.getRegion(rs.getString("region_name"));
            if (region == null) {
                plugin.getLogger().warning("Ignoring record from DB, invalid configured region: ".concat(rs.getString("region_name")));
                continue;
            }
            Material material;
            try {
                material = Material.getMaterial(rs.getInt("material_id"));
            } catch (Exception ex) {
                plugin.getLogger().warning("Ignoring record from DB, invalid configured material ID: ".concat(rs.getString("material_id")));
                continue;
            }
            boolean hidden = (rs.getInt("hidden") != 0);
            String name = rs.getString("name");
            List<String> lore = new ArrayList<>();
            lore.addAll(Arrays.asList(rs.getString("lore").split("\n")));

            byte materialData = (byte) rs.getInt("material_data");

            int secondsFromEpoch = rs.getInt("date");

            pb.addProtectionBlock(location, region, material, hidden, name, lore, materialData, secondsFromEpoch, false);
        }
        sqlStatement.close();
    }

    public void insertAvailableIDs(String hash) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        sqlStatement.executeUpdate("INSERT INTO protection_id "
                + "VALUES (\"" + hash + "\");");
        sqlStatement.close();
    }

    public void delAvailableIDs(String hash) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        sqlStatement.executeUpdate("DELETE FROM protection_id "
                + "WHERE hash = (\"" + hash + "\");");
        sqlStatement.close();
    }

    public void updatePSBlocks(ProtectionBlocks.PSBlocks psb) throws SQLException {
        removePSBlocks(psb.location);
        insertPSBlocks(psb);
    }

    public boolean isAvailableHashStored(String hash) throws SQLException {
        boolean result;
        Statement sqlStatement = sqlConn.createStatement();
        ResultSet rs = sqlStatement.executeQuery("SELECT * FROM protection_id WHERE hash = '" + hash + "'");
        result = rs.next();
        sqlStatement.close();
        return result;
    }
}
