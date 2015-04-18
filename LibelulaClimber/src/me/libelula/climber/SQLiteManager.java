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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class SQLiteManager of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class SQLiteManager {

    private Connection sqlConn;
    private final Main plugin;

    public SQLiteManager(Main plugin) {
        this.plugin = plugin;
        File sqlFile = new File(plugin.getDataFolder(), "scores.db");
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
        sqlStatement.executeUpdate("CREATE TABLE IF NOT EXISTS score "
                + "(player TEXT, score INTEGER, "
                + "PRIMARY KEY(player));");
        sqlStatement.close();
    }

    public Map<String, Integer> getBestScores(int max) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        Map<String, Integer> scores = new TreeMap<>();
        ResultSet rs = sqlStatement.executeQuery("SELECT * FROM score ORDER BY score DESC;");
        while (rs.next()) {
            scores.put(rs.getString("player"), rs.getInt("score"));
//            plugin.getLogger().info("Debug: " + rs.getString("player") + rs.getInt("score"));
            if (max > 1) {
                if (scores.size() >= max) {
                    break;
                }
            }
        }
        sqlStatement.close();
        return scores;
    }

    public void incrementScores(Map<String, Integer> playerScores) throws SQLException {
        Statement sqlStatement = sqlConn.createStatement();
        for (String playerName : playerScores.keySet()) {
            int finalScore = playerScores.get(playerName);
            String query = "SELECT * FROM score WHERE player = '" + playerName + "';";

//            plugin.getLogger().info("Here1-->" + query);
            ResultSet rs = sqlStatement.executeQuery(query);
            if (rs.next()) {
                finalScore += rs.getInt("score");
                query = "UPDATE score SET score = " + finalScore + " WHERE player = '" + playerName + "';";
                sqlStatement.executeUpdate(query);
//                plugin.getLogger().info("Found! -->" + query);
            } else {
                sqlStatement.executeUpdate("INSERT INTO score "
                        + "VALUES (\"" + playerName + "\", " + finalScore + ");");
            }
        }
        sqlStatement.close();
    }

    public void closeConnection() {
        try {
            sqlConn.close();
        } catch (SQLException ex) {
            plugin.getLogger().severe(ex.toString());
        }
    }
}
