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
package me.libelula.lobby;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachment;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class PlayerManager {

    public enum playerState {

        ERROR, PROCESSING, BANNED, UNREGISTERED,
        REGISTERED, LOGGED_IN, PREMIUM,
        BOGGED_DOWN, UNACTIVATED
    }

    public enum nonPremiumChecks {

        TARPPITING, CHECKING_FOR_REGISTERED, CHECKING_FOR_PREMIUM,
        LOGGING_IN, SETTING_PREMIUM, SENDING_TO_LOBBY
    }

    protected class PlayerInstance {

        private Player player;
        private playerState state;
        private nonPremiumChecks checkStatus;
        private long stateDateTime;
        private InetAddress lastInetAddress;
        private String typedPassword;
        private final String lowerCasedName;
        private long lastTalked;
        private String resourcePackName;
        private ArrayList<ItemStack> armour;

        public PlayerInstance(Player player) {
            this.player = player;
            this.lowerCasedName = player.getName().toLowerCase();
            this.lastInetAddress = player.getAddress().getAddress();
            lastTalked = -1;
            ItemStack resourcePackIs = getResourcePackItemFromInv(player);
            if (resourcePackIs != null) {
                setResourcePack(
                        ChatColor.stripColor(resourcePackIs.getItemMeta().getDisplayName()));
            }
            
        }

        public void setArmour(ArrayList<ItemStack> armour) {
            this.armour = armour;
        }

        public ArrayList<ItemStack> getArmour() {
            return armour;
        }

        public final void setResourcePack(String resourcePackName) {
            this.resourcePackName = resourcePackName;
            String packLink = null;
            switch (resourcePackName) {
                case "R3D Craft":
                    packLink = "http://libelula.me/resourcepacks/R3D.CRAFT_SR-64x_v0.2.0.zip";
                    break;
                case "Traditional Beauty":
                    packLink = "http://libelula.me/resourcepacks/Traditional_Beauty_18v2.zip";
                    break;
                case "Pure BD Craft":
                    packLink = "http://libelula.me/resourcepacks/PureBDcraft_64x_MC18.zip";
                    break;
                case "Soartex Fanver (Light)":
                    packLink = "http://libelula.me/resourcepacks/Soartex_Fanver_Lite.zip";
                    break;
                case "Modern HD":
                    packLink = "http://libelula.me/resourcepacks/ModernHD_1.8.1.zip";
                    break;
                case "Soartex Invictus":
                    packLink = "http://libelula.me/resourcepacks/Soartex_Invictus-2.1.zip";
                    break;
            }

            if (packLink != null) {
                if (cm.isDebugMode()) {
                    plugin.logInfo("Sending resource pack: '" + packLink + "' to " + player.getName());
                }
                player.setResourcePack(packLink);
                plugin.sendMessage(player, "Te hemos enviado las texturas, si no se han cargado es porque tienes desactivadas las texturas de servidor en tu minecraft.", 40);
                plugin.sendMessage(player, "Puedes descargar este resource pack desde &1&n" + packLink, 42);
            }
        }

        public String getResourcePackName() {
            return resourcePackName;
        }
        
        public final ItemStack getResourcePackItemFromInv(Player player) {
            ItemStack is = null;
            if (player.getInventory().getItem(8) != null) {
                String playerItemName = ChatColor.stripColor(player.getInventory().getItem(8).getItemMeta().getDisplayName());
                for (ItemStack rsIs : resourcePacks) {
                    if (ChatColor.stripColor(rsIs.getItemMeta().getDisplayName())
                            .equals(playerItemName)) {
                        is = rsIs;
                        break;
                    }
                }
            }
            return is;
        }

        public final ItemStack getSelectedResourceItem() {
            ItemStack is;
            is = new ItemStack(Material.SPONGE);
            ItemMeta im = is.getItemMeta();
            im.setDisplayName(ChatColor.YELLOW + "Selector de Resource packs");
            is.setItemMeta(im);
            if (player.getInventory().getItem(8) != null) {
                is = getResourcePackItemFromInv(player);
            } else {
                if (resourcePackName != null) {
                    for (ItemStack rsIs : resourcePacks) {
                        if (ChatColor.stripColor(rsIs.getItemMeta().getDisplayName()).equals(resourcePackName)) {
                            is = rsIs;
                            break;
                        }
                    }
                }
            }
            return is;
        }

        public void sendMessage(final String text) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.sendMessage(player, text);
            });
        }

        public void setLastInetAddress(InetAddress lastInetAddress) {
            this.lastInetAddress = lastInetAddress;
        }

        public void kickPlayer(final String text) {
            plugin.pm.kickPlayer(player, text);
        }

        public Player getPlayer() {
            return player;
        }

        public playerState getState() {
            return state;
        }

        public long getStateDateTime() {
            return stateDateTime;
        }

        public void setState(playerState state) {
            this.state = state;
        }

        public void setStateDateTime(long stateDateTime) {
            this.stateDateTime = stateDateTime;
        }

        public InetAddress getLastInetAddress() {
            return lastInetAddress;
        }

        public String getTypedPassword() {
            return typedPassword;
        }

        public void setCheckStatus(nonPremiumChecks checkStatus) {
            this.state = playerState.PROCESSING;
            this.checkStatus = checkStatus;
            _players_mutex.lock();
            try {
                processQueue.remove(lowerCasedName);
                processQueue.add(lowerCasedName);
                stateDateTime = new Date().getTime();

            } finally {
                _players_mutex.unlock();
            }
        }
    }

    private final Main plugin;
    private final TreeMap<String, PlayerInstance> players;
    private final ArrayList<String> processQueue;
    private final ReentrantLock _players_mutex;
    private final ConfigurationManager cm;
    private boolean processing;
    private final ArrayList<Location> spawns;
    private int lastSpawnPoint;
    private final ItemStack itemStackAir;
    private final ItemStack beginnersGuide;
    private final ItemStack rulesBook;
    private final ItemStack helpBook;
    private final ItemStack compass;
    private final ArrayList<ItemStack> resourcePacks;

    public PlayerManager(Main plugin) {
        this.plugin = plugin;
        players = new TreeMap<>();
        processQueue = new ArrayList<>();
        _players_mutex = new ReentrantLock();
        this.cm = plugin.cm;
        processing = false;
        spawns = plugin.cm.getSpawnPoints();
        lastSpawnPoint = 0;
        this.itemStackAir = new ItemStack(Material.AIR);
        this.beginnersGuide = getBook("beginners");
        this.rulesBook = getBook("rules");
        this.helpBook = getBook("help");
        this.compass = getCompass();
        resourcePacks = new ArrayList<>();

        ItemStack resourcePackItem;
        ItemMeta im;
        ArrayList<String> lore = new ArrayList<>();
        lore.add("Click para activar");
        resourcePackItem = new ItemStack(Material.SPONGE);
        im = resourcePackItem.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "R3D Craft");
        im.setLore(lore);
        resourcePackItem.setItemMeta(im);
        resourcePacks.add(resourcePackItem);
        resourcePackItem = new ItemStack(Material.SPONGE);
        im = resourcePackItem.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Traditional Beauty");
        im.setLore(lore);
        resourcePackItem.setItemMeta(im);
        resourcePacks.add(resourcePackItem);
        resourcePackItem = new ItemStack(Material.SPONGE);
        im = resourcePackItem.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Pure BD Craft");
        im.setLore(lore);
        resourcePackItem.setItemMeta(im);
        resourcePacks.add(resourcePackItem);
        resourcePackItem = new ItemStack(Material.SPONGE);
        im = resourcePackItem.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Soartex Fanver (Light)");
        im.setLore(lore);
        resourcePackItem.setItemMeta(im);
        resourcePacks.add(resourcePackItem);
        resourcePackItem = new ItemStack(Material.SPONGE);
        im = resourcePackItem.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Modern HD");
        im.setLore(lore);
        resourcePackItem.setItemMeta(im);
        resourcePacks.add(resourcePackItem);
        resourcePackItem = new ItemStack(Material.SPONGE);
        im = resourcePackItem.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Soartex Invictus");
        im.setLore(lore);
        resourcePackItem.setItemMeta(im);
        resourcePacks.add(resourcePackItem);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            processQueue();
        }, 10, 10);
        if (plugin.cm.isFrontServer()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                checkNonPremiumPlayerList();
            }, 15, 15);
        }
    }

    public void kickPlayer(final Player player, final String text) {
        kickPlayer(player, text, 1);
    }

    public void kickPlayer(Player givenPlayer, final String text, final int tics) {
        final Player player = plugin.getServer().getPlayer(givenPlayer.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (tics != 0) {
                if (player == null || !player.isOnline()) {
                    return;
                }
            }
            if (cm.isDebugMode()) {
                plugin.logInfo("Debug: Kick " + player.getName() + " \"" + text + "\"");
            }
            player.kickPlayer(text);
        }, tics);
    }

    public void registerUser(Player player) {
        PlayerInstance pi;
        _players_mutex.lock();
        try {
            pi = players.get(player.getName().toLowerCase());
        } finally {
            _players_mutex.unlock();
        }

        if (pi != null) {
            pi.player = player;
        }

        if (cm.isPremium()) {
            if (pi == null) {
                pi = new PlayerInstance(player);
                pi.setCheckStatus(nonPremiumChecks.SETTING_PREMIUM);
                addPlayerInstance(pi);
            }
            return;
        }

        if (pi != null) {
            switch (pi.getState()) {
                case BOGGED_DOWN:
                    long now = new Date().getTime();
                    if (now - pi.getStateDateTime() < cm.getTarpittingPenaltySeconds() * 1000) {
                        plugin.pm.kickPlayer(player, cm.getTarpittingPenaltyMessage(), 30);
                    } else {
                        pi.setCheckStatus(nonPremiumChecks.TARPPITING);
                    }
                    break;
                case BANNED:
                    pi.kickPlayer("Tu cuenta está desactivada, si crees que es un error escribe a info@libelula.me");
                    plugin.logInfo(pi.getPlayer().getName()
                            + " echado por estar baneado en el foro.");
                    break;
                case PREMIUM:
                    if (!cm.isAllowPremiumOnNonpremium() && !cm.isPremium()) {
                        pi.kickPlayer(cm.getPremiumKickMessage());
                        plugin.logInfo(pi.getPlayer().getName()
                                + " echado por ser premium.");
                    }
                    break;
                case LOGGED_IN:
                    if (pi.getLastInetAddress().equals(player.getAddress().getAddress())) {
                        pi.sendMessage("&1[&6Autologin&1]&6 ¡Bienvenido de regreso "
                                + player.getName() + "!");
                        plugin.logInfo(pi.getPlayer().getName()
                                + " Auto logged in.");
                    } else { // Different IP
                        pi.state = playerState.REGISTERED;
                        pi.sendMessage("Hola " + player.getName()
                                + ", escribe /login y tu contraseña para empezar.");
                    }
                    break;
                case PROCESSING:
                    plugin.pm.kickPlayer(player, "Ahora no puedes entrar, aguarda unos momentos y vuelve a intentarlo.", 30);
                    plugin.logWarn("Echado el jugador " + pi.getPlayer().getName() + " por entrar mientras se procesan sus datos.");
                    break;
                case REGISTERED:
                    if (cm.isFrontServer()) {
                        pi.setCheckStatus(nonPremiumChecks.TARPPITING);
                    }
                    break;
                default:
                    plugin.logErr("Estado de jugador no válido: " + pi.getState()
                            + " para " + player.getName());
                    removePlayerFromList(player);
                    pi = null;
            }
        }

        if (pi == null) {
            pi = new PlayerInstance(player);
            if (cm.isFrontServer()) {
                pi.setCheckStatus(nonPremiumChecks.TARPPITING);
            } else {
                pi.state = playerState.REGISTERED;
            }
            addPlayerInstance(pi);
        }

        if (!cm.isFrontServer()) {
            switch (pi.getState()) {
                case REGISTERED:
                    giveUnloggedStuff(player);
                    break;
                case LOGGED_IN:
                case PREMIUM:
                    giveLoggedStuff(player);
                    break;
            }
        }
    }

    private void addPlayerInstance(PlayerInstance pi) {
        _players_mutex.lock();
        try {
            players.put(pi.lowerCasedName, pi);
        } finally {
            _players_mutex.unlock();
        }
    }

    public void removePlayerFromList(Player player) {
        _players_mutex.lock();
        try {
            PlayerInstance pi = players.remove(player.getName().toLowerCase());
            if (pi != null && pi.getState() != null
                    && pi.getState() == playerState.PROCESSING) {
                processQueue.remove(pi.lowerCasedName);
            }
        } finally {
            _players_mutex.unlock();
        }
    }

    private void processQueue() {
        boolean exit = false;
        _players_mutex.lock();
        try {
            if (!processing) {
                processing = true;
            } else {
                exit = true;
            }
        } finally {
            _players_mutex.unlock();
        }

        if (exit) {
            return;
        }

        PlayerInstance pi;
        int loops = 0;
        while (!processQueue.isEmpty()) {
            _players_mutex.lock();
            try {
                pi = players.get(processQueue.remove(0));
            } finally {
                _players_mutex.unlock();
            }

            if (pi != null && pi.state == playerState.PROCESSING
                    && pi.checkStatus != null) {
                switch (pi.checkStatus) {
                    case TARPPITING:
                        long time = (new Date().getTime()) - pi.stateDateTime;
                        if (time < cm.getTarpittingMs()) {
                            _players_mutex.lock();
                            try {
                                processQueue.add(pi.lowerCasedName);
                            } finally {
                                _players_mutex.unlock();
                            }
                        } else {
                            if (pi.player.isOnline()) {
                                pi.setCheckStatus(nonPremiumChecks.CHECKING_FOR_REGISTERED);
                            } else {
                                if (cm.isDebugMode()) {
                                    plugin.logInfo("player " + pi.getPlayer().getName() + " has been bogged down.");
                                }
                                pi.setCheckStatus(nonPremiumChecks.TARPPITING);
                                pi.setState(playerState.BOGGED_DOWN);
                            }
                        }
                        break;
                    case CHECKING_FOR_REGISTERED:
                        plugin.xr.checkForRegistered(pi);
                        break;
                    case CHECKING_FOR_PREMIUM:
                        plugin.xr.checkForPremium(pi);
                        break;
                    case LOGGING_IN:
                        plugin.xr.checkPassword(pi);
                        break;
                    case SETTING_PREMIUM:
                        plugin.xr.setPremium(pi);
                        pi.setState(playerState.PREMIUM);
                        giveLoggedStuff(pi.getPlayer());
                        break;
                    case SENDING_TO_LOBBY:
                        plugin.teleportToServer(pi.getPlayer(), cm.getLobbyServerName());
                        removePlayerFromList(pi.player);
                        break;
                    default:
                        plugin.logErr("Estado no válido en check status: " + pi.checkStatus);
                        break;
                }
            }
            if (processQueue.size() < loops) {
                break;
            }
            loops++;
        }
        _players_mutex.lock();
        try {
            processing = false;
        } finally {
            _players_mutex.unlock();
        }
    }

    public void spawn(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(spawns.get(lastSpawnPoint));
        });
        lastSpawnPoint++;
        if (lastSpawnPoint >= spawns.size()) {
            lastSpawnPoint = 0;
        }
    }

    private void checkNonPremiumPlayerList() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerInstance pi = players.get(player.getName().toLowerCase());
            if (pi != null) {
                switch (pi.state) {
                    case PREMIUM:
                        pi.kickPlayer(cm.getPremiumKickMessage());
                        break;
                    case BANNED:
                        pi.kickPlayer("Tu cuenta está desactivada, si crees que es un error escribe a info@libelula.me");
                        break;
                    case BOGGED_DOWN:
                        long now = new Date().getTime();
                        if (now - pi.getStateDateTime() < cm.getTarpittingPenaltySeconds() * 1000) {
                            plugin.pm.kickPlayer(player, cm.getTarpittingPenaltyMessage(), 30);
                        } else {
                            pi.setCheckStatus(nonPremiumChecks.TARPPITING);
                        }
                        break;
//                    case REGISTERED:
//                        pi.kickPlayer("Error yendo al servidor esperado.");
                }
            }
        }
    }

    public boolean isPremium(Player player) {
        boolean result = false;
        PlayerInstance pi = players.get(player.getName().toLowerCase());
        if (pi != null) {
            if (pi.state == playerState.PREMIUM) {
                result = true;
            }
        }
        return result;
    }

    public boolean isBoggedDown(Player player) {
        boolean result = false;
        PlayerInstance pi = players.get(player.getName().toLowerCase());
        if (pi != null) {
            if (pi.state == playerState.BOGGED_DOWN) {
                long now = new Date().getTime();
                if (now - pi.getStateDateTime() < cm.getTarpittingPenaltySeconds() * 1000) {
                    result = true;
                }
            }
        }
        return result;
    }

    public boolean isLogged(Player player) {
        boolean result = false;
        PlayerInstance pi = players.get(player.getName().toLowerCase());
        if (pi != null) {
            result = (pi.state == playerState.LOGGED_IN
                    || pi.state == playerState.PREMIUM);
        }
        return result;
    }

    public void logginSucces(PlayerInstance pi) {
        pi.setState(playerState.LOGGED_IN);
        plugin.sendMessage(pi.getPlayer(), "* Bienvenido a Libélula Minecraft Edition *");
        giveLoggedStuff(pi.getPlayer());
    }

    public void giveUnloggedStuff(Player player) {
        clearStuff(player);
        player.getInventory().addItem(beginnersGuide);
        spawn(player);
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission("lobby.unlogged", true);
        attachment.setPermission("lobby.logged", false);
    }

    public void giveLoggedStuff(Player player) {
        boolean canFly = player.getLocation().distance(plugin.cm.getZeroPoint())
                < plugin.cm.getPlayerFlyBlocksFromZero();
        if (canFly) {
            player.teleport(player.getLocation().add(0, 1, 0));
        }
        clearStuff(player, canFly);
        player.getInventory().addItem(compass);
        if (!cm.isAltmenu()) {
            player.getInventory().setItem(6, helpBook);
            player.getInventory().setItem(7, rulesBook);
            PlayerInstance pi = players.get(player.getName().toLowerCase());
            if (pi != null) {
                player.getInventory().setItem(8, pi.getSelectedResourceItem());
                int inverntorySlot = 27;
                for (ItemStack rsIs : resourcePacks) {
                    if (rsIs.getItemMeta().getDisplayName()
                            .equals(player.getInventory().getItem(8).getItemMeta().getDisplayName())) {
                        continue;
                    }
                    player.getInventory().setItem(inverntorySlot++, rsIs);
                }
            }
        }
        player.getInventory().setHeldItemSlot(0);
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission("lobby.unlogged", false);
        attachment.setPermission("lobby.logged", true);
    }

    public void clearStuff(Player player) {
        clearStuff(player, false);
    }

    public void clearStuff(Player player, boolean allowFlight) {
        if (!cm.isOpAllowed(player)) {
            player.setOp(false);
        }
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight);
        player.setCompassTarget(cm.getZeroPoint());
        player.setFoodLevel(20);
        player.setHealth(20);
        player.setGameMode(GameMode.ADVENTURE);
        player.setLevel(0);
        player.setExp(0);
        player.getInventory().clear();
        /*
         player.getInventory().setBoots(itemStackAir);
         player.getInventory().setChestplate(itemStackAir);
         player.getInventory().setHelmet(itemStackAir);
         player.getInventory().setLeggings(itemStackAir);
         */
    }

    public final ItemStack getBook(String bookName) {
        List<String> bookPages = cm.getBookPages(bookName);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta bm = (BookMeta) book.getItemMeta();
        bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(bookName + "-book.title")));
        bm.setAuthor(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(bookName + "-book.author")));
        bm.setTitle(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(bookName + "-book.title")));
        bm.setPages(bookPages);
        bm.addEnchant(Enchantment.LUCK, 1, true);
        book.setItemMeta(bm);
        return book;
    }

    public final ItemStack getCompass() {
        ItemStack is = new ItemStack(Material.COMPASS);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Navegador" + ChatColor.GOLD + " Libelula");
        is.setItemMeta(im);
        return is;
    }

    public boolean requestLogIn(Player player, String typedPassword) {
        boolean result = false;
        PlayerInstance pi = players.get(player.getName().toLowerCase());
        if (pi != null) {
            switch (pi.state) {
                case PROCESSING:
                    pi.sendMessage("Espera unos momentos y vuelve a intentarlo...");
                    break;
                case BANNED:
                    player.kickPlayer("Tu cuenta ha sido desactivada por un administrador.");
                    break;
                case UNACTIVATED:
                    player.kickPlayer("Regresa al menos 24 horas después de haberte registrado.");
                    break;
                default:
                    result = true;
                    pi.typedPassword = typedPassword;
                    pi.setCheckStatus(nonPremiumChecks.LOGGING_IN);
            }

        }
        return result;
    }

    public int getMsBetweenLastTlkCmd(Player player) {
        int result = Integer.MAX_VALUE;
        PlayerInstance pi = players.get(player.getName().toLowerCase());
        if (pi != null) {
            long now = new Date().getTime();
            if (pi.lastTalked != -1) {
                result = (int) (now - pi.lastTalked);
            }
            pi.lastTalked = now;
        }
        return result;
    }

    public void showServerMenu(Player player) {

    }

    public void showTextureMenu(Player player) {

    }

    public void manegeSpecialClick(Player player, ItemStack is) {
        PlayerInstance pi = players.get(player.getName().toLowerCase());
        String playerItemName = ChatColor.stripColor(is.getItemMeta().getDisplayName());
        boolean found = false;
        for (ItemStack rsIs : resourcePacks) {
            if (ChatColor.stripColor(rsIs.getItemMeta().getDisplayName())
                    .equals(playerItemName)) {
                pi.setResourcePack(playerItemName);
                found = true;
                break;
            }
        }
        if (found) {
            player.getInventory().setItem(8, is);
        }
    }
}
