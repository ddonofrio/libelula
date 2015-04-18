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

import java.util.TreeSet;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Class of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class LibelulaProtectionBlocks extends JavaPlugin {

    public Configuration config;
    public Internationalization i18n;
    public WorldGuardManager wgm;
    public ProtectionBlocks pbs;
    public ProtectionController pc;
    public SQLiteManager sql;
    public Economy eco;
    public TreeSet<String> bannedAdvicedPlayers;
    private Commands cs;

    @Override
    public void onEnable() {
        Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin == null) {
            ConsoleCommandSender cs = getServer().getConsoleSender();
            cs.sendMessage(ChatColor.RED + "CRITICAL: Plugin WorldGuard not found!");
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.isOp()) {
                    player.sendMessage(ChatColor.RED + "CRITICAL: Plugin WorldGuard not found! Disabling Libelula Protection Blocks");
                }
            }
            disablePlugin();
            return;
        }
        config = new Configuration(this);
        i18n = new Internationalization(config.getLanguage());
        wgm = new WorldGuardManager(this);
        sql = new SQLiteManager(this);
        if (!sql.isInitialized()) {
            getLogger().severe(i18n.getText("need_db_support"));
            disablePlugin();
            return;
        }
        pbs = new ProtectionBlocks(this);
        Plugin oldPS;
        oldPS = getServer().getPluginManager().getPlugin("ProtectionStones");
        boolean oldPluginConfigImported = false;
        if (oldPS != null) {
            TaskManager.disablePSAndLoadCommands(oldPS, this);
            if (!config.isOldPsImported()) {
                getLogger().info(i18n.getText("importing_oldps"));
                TaskManager.importFromPSWhenWGIsEnabled(this);
                oldPluginConfigImported = true;
            }
        } else {
            TaskManager.registerCommands(this);
        }
        getServer().getPluginManager().registerEvents(new Listener(this), this);        

        if (!oldPluginConfigImported) {
            pbs.load();
        }
        setupEconomy();
        pc = new ProtectionController(this);
        bannedAdvicedPlayers = new TreeSet<>();
        cs = new Commands(this);
        getCommand("ps").setExecutor(cs);
    }

    public void disablePlugin() {
        if (i18n != null) {
            getLogger().info(i18n.getText("disabling_plugin"));
        }
        getServer().getPluginManager().disablePlugin(this);
    }

    public String getPluginVersion() {
        return getDescription().getFullName() + " "
                + i18n.getText("created_by") + " "
                + getDescription().getAuthors().get(0);
    }

    private boolean setupEconomy() {
        eco = null;

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().info(i18n.getText("vault_not_found"));
            return false;
        }
        eco = rsp.getProvider();
        return eco != null;
    }
}