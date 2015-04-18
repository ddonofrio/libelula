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

package me.libelula.autoshop;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class Main extends JavaPlugin {

    protected class CretorParams {

        int quantity;
        float price;

        public CretorParams(int quantity, float price) {
            this.quantity = quantity;
            this.price = price;
        }

    }

    private final TreeMap<String, CretorParams> creatorList;
    private final ReentrantLock _creatorList_mutex;
    private final YamlConfiguration worth;
    private final CommandExecutor ce;
    private final File worthFile;
    private final String worthFileName;
    private final EventListener listener;
    private long worthFileLastMod;

    public Main() {
        worthFileName = getDataFolder().getAbsolutePath() + "/../Essentials/worth.yml";
        this.creatorList = new TreeMap<>();
        worth = new YamlConfiguration();
        _creatorList_mutex = new ReentrantLock();
        ce = new CommandExecutor(this);
        listener = new EventListener(this);
        worthFile = new File(worthFileName);
        worthFileLastMod = worthFile.lastModified();
    }

    @Override
    public void onEnable() {
        saveConfig();
        if (!worthFile.exists()) {
            getLogger().log(Level.SEVERE, "{0} file not found.", worthFileName);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            worth.load(worthFile);
        } catch (IOException | InvalidConfigurationException ex) {
            getLogger().log(Level.SEVERE,
                    "An error occurs reading Essentials/worth.yml file: {0}", ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ce.register();
        listener.register();
    }

    public boolean isPlayerCreatingShops(Player player) {
        boolean result;
        _creatorList_mutex.lock();
        try {
            result = creatorList.containsKey(player.getName());
        } finally {
            _creatorList_mutex.unlock();
        }
        return result;
    }

    public void sendMessage(CommandSender cs, String message) {
        cs.sendMessage(ChatColor.YELLOW + "[" + ChatColor.BLUE + getName() + ChatColor.YELLOW + "]"
                + ChatColor.GOLD + " " + ChatColor.translateAlternateColorCodes('&', message));
    }

    public float getWorth(ItemStack is) {
        float price = 0;
        String materialName = is.getType().name().replace("_", "").toLowerCase();

        if (worthFile.lastModified() != worthFileLastMod) {
            try {
                worth.load(worthFile);
            } catch (IOException | InvalidConfigurationException ex) {
                getLogger().log(Level.SEVERE,
                        "An error occurs reading Essentials/worth.yml file: {0}", ex.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return 0;
            }
        }
        
        price = (float) worth.getDouble("worth." + materialName + "."
                + is.getData().getData());
        if (price == 0) {
            price = (float) worth.getDouble("worth." + materialName);
        }
        return price;
    }

    public void removeShopCreator(Player player) {
        _creatorList_mutex.lock();
        try {
            creatorList.remove(player.getName());
        } finally {
            _creatorList_mutex.unlock();
        }
    }

    public void addShopCreator(Player player, int quantity, float price) {
        _creatorList_mutex.lock();
        try {
            CretorParams p = new CretorParams(quantity, price);
            creatorList.put(player.getName(), p);
        } finally {
            _creatorList_mutex.unlock();
        }
    }

    public void createShop(Sign sign, Player player) {
        CretorParams params;
        _creatorList_mutex.lock();
        try {
            params = creatorList.get(player.getName());
        } finally {
            _creatorList_mutex.unlock();
        }
        int quantity;
        if (params.quantity == 0) {
            quantity = player.getItemInHand().getAmount();
        } else {
            quantity = params.quantity;
        }
        float price;
        if (params.price == 0) {
            price = getWorth(player.getItemInHand());
            price = (float) (price * getConfig().getDouble("price-factor"));
        } else {
            price = params.price;
        }
        if (price == 0) {
            sendMessage(player, "This item has no price!");
        } else {
            sign.setLine(0, "Admin Shop");
            sign.setLine(1, "" + quantity);
            price = Math.round(price * quantity * 100);
            price = price / 100;
            String priceLine="B " + String.format( "%.2f", price);
            priceLine = priceLine.replace(",", ".");
            sign.setLine(2, priceLine);
            String materialLine = player.getItemInHand().getType().name();
            Byte data = player.getItemInHand().getData().getData();
            if (data != 0) {
                materialLine = materialLine + ":" + data;
            }
            sign.setLine(3, materialLine);
            sign.update();
            getLogger().log(Level.INFO, "Admin Shop Created by {3}: {0} for {1} of {2}", 
                    new Object[]{priceLine, quantity, materialLine, player.getName()});
        }
    }

}
