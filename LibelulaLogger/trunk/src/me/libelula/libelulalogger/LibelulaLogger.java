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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import me.libelula.meode.MEODE;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class LibelulaLogger extends JavaPlugin {

    private ConsoleCommandSender console;
    private static boolean debug = false;
    public Configuration config;
    public WorldGuardPlugin worldGuardPlugin;
    public MEODE meode;
    public WorldEditPlugin we;
    public ToolBox toolbox;
    private TreeMap<Player, List<ItemStack>> chestOpenedSet;
    private Lock _chestOpenedSet;

    public class PlayerComparator implements Comparator<Player> {

        @Override
        public int compare(Player o1, Player o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    @Override
    public void onEnable() {

        console = getServer().getConsoleSender();
        saveDefaultConfig();
        config = new Configuration(this);
        worldGuardPlugin = getWorldGuard();
        chestOpenedSet = new TreeMap<>(new PlayerComparator());
        _chestOpenedSet = new ReentrantLock(true);

        config.load();

        String meodeDBPath;
        if (!config.getdbDirectory().startsWith("/")) {
            meodeDBPath = getDataFolder().getAbsolutePath().concat("/").concat(config.getdbDirectory());
        } else {
            meodeDBPath = config.getdbDirectory();
        }

        Commands commmands = new Commands(this);

        getServer().getPluginManager().registerEvents(new EventLogger(this), this);
        commmands.registerCommands();
        try {
            meode = new MEODE(meodeDBPath,
                    config.getMaxDiskDBsizeMB(),
                    config.getMaxEventsInRAM(), this, debug);
        } catch (Exception ex) {
            getLogger().severe(ex.toString());
            this.disablePlugin();
        }

        toolbox = new ToolBox(this);

        we = getWorldEdit();
    }

    @Override
    public void onDisable() {
        if (meode != null) {
            meode.persistRamSynchronously();
        }
        if (toolbox != null) {
            toolbox.removeAllTools();
        }
        if (config != null) {
            config.persistConfiguration();
        }
    }

    public void disablePlugin() {
        logSevere("Disabling Libelula Login Plugin");
        getServer().getPluginManager().disablePlugin(this);
    }

    public void logSevere(String message) {
        console.sendMessage("[" + getName() + "] " + ChatColor.RED + message);
    }

    public void logWarning(String message) {
        console.sendMessage("[" + getName() + "] " + ChatColor.YELLOW + message);
    }

    public void logInfo(String message, ChatColor colour) {
        console.sendMessage("[" + getName() + "] " + colour + message);
    }

    public void logInfo(String message) {
        getLogger().info(message);
    }

    public WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            this.getLogger().warning("World Guard Plugin not found.");
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }

    public WorldEditPlugin getWorldEdit() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");

        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            this.getLogger().warning("World Edit Plugin not found.");
            return null;
        }

        if (debug) {
            logInfo("Linked with WorldEdit.");
        }
        return (WorldEditPlugin) plugin;
    }

    public String getPluginFullDescription() {
        return getDescription().getFullName()
                .concat(" by ".concat(getDescription().getAuthors().get(0)));
    }

    public void playerOpenChest(Player player, Inventory inventory) {
        if (chestOpenedSet.containsKey(player)) {
            _chestOpenedSet.lock();
            try {
                chestOpenedSet.remove(player);
            } finally {
                _chestOpenedSet.unlock();
            }
        }
        //ItemStack[] itemStacks = inventory.getContents().clone();
        List<ItemStack> itemStacks = new ArrayList<>();
        for (ItemStack is : inventory.getContents()) {
            if (is != null) {
                itemStacks.add(is.clone());
            }
        }
        //itemStacks.addAll(Arrays.asList(inventory.getContents()));

        _chestOpenedSet.lock();
        try {
            chestOpenedSet.put(player, itemStacks);
        } finally {
            _chestOpenedSet.unlock();
        }
    }

    public void playerCloseChest(Player player, Inventory inventory) {
        if (!chestOpenedSet.containsKey(player)) {
            return;
        }
        List<ItemStack> itemStacks = new ArrayList<>();
        _chestOpenedSet.lock();
        try {
            itemStacks.addAll(chestOpenedSet.remove(player));
        } finally {
            _chestOpenedSet.unlock();
        }

        String location = player.getLocation().getWorld().getName() + " "
                + player.getLocation().getBlockX() + " "
                + player.getLocation().getBlockY() + " "
                + player.getLocation().getBlockZ();

        String report = "Player " + player.getName() + " has interacted with a CHEST @(" + location + ") and removed:";
        boolean haveRemoved = false;
        for (ItemStack is : itemStacks) {
            if (is == null || inventory.contains(is.getType(), is.getAmount())) {
                continue;
            } else {
                haveRemoved = true;
                int ammount = is.getAmount();
                if (inventory.contains(is.getType())) {
                    for (ItemStack is2 : inventory.all(is.getType()).values()) {
                        ammount -= is2.getAmount();
                    }
                }
                report = report.concat(" (" + is.getType().name() + "x" + ammount + ")");
            }
        }
        if (haveRemoved) {
            getLogger().info(report);
        }
    }
}
