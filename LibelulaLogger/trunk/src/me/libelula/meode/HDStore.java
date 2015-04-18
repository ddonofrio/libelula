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
package me.libelula.meode;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import static me.libelula.meode.Store.getRowName;
import static me.libelula.meode.Store.getTableName;
import static me.libelula.meode.Store.rowSize;
import orestes.bloomfilter.BloomFilter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

/**
 * Class HDStore of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class HDStore extends Store {

    private final File database;
    private final Plugin plugin;
    public boolean debug;
    private File supertable;
    private File playersTable;
    private TreeMap<String, Integer> playerIDs;
    private FilenameFilter supertableNameFilter;
    private BloomFilter<String> dbFilter;
    private String lastQueryCacheString;
    RandomAccessFile lastQueryCacheAccessFile;

    /**
     *
     * @param dbPath Path to DB directory.
     * @param maxDbSizeMB Maximum amount of MB to store in disk before rotating
     * storage information.
     * @param plugin main plugin.
     */
    public HDStore(String dbPath, int maxDbSizeMB, Plugin plugin) throws Exception {
        database = new File(dbPath);
        this.plugin = plugin;
        playerIDs = null;
        debug = false;
        supertable = null;
        playersTable = null;
        lastQueryCacheAccessFile = null;
        lastQueryCacheString = null;
        if (!database.mkdirs() && !database.isDirectory()
                || !database.canWrite() || !database.canRead()) {
            throw new Exception("Unable to access, create or write database directory: "
                    .concat(dbPath));
        }

        supertableNameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("\\d{4}-\\d{2}-\\d{2}");
            }
        };

        Object objects[] = {this};
        new AsyncTask(AsyncTask.TaskType.LOAD_FILTER, objects, plugin).runTaskAsynchronously(plugin);
    }

    private void persistPlayersTable() throws IOException {
        if (playersTable != null && playerIDs != null) {
            if (debug) {
                plugin.getLogger().info("DEBUG: Saving players to ".concat(playersTable.getAbsolutePath()));
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(playersTable.getCanonicalPath(), false))) {
                oos.writeObject(playerIDs);
                oos.close();
            }
        } else {
            throw new SyncFailedException("Unable to persist player database.");
        }
    }

    @SuppressWarnings("unchecked")
    public void setSupertable(String supertableName) throws IOException, ClassNotFoundException, Exception {
        if (debug) {
            plugin.getLogger().info("DEBUG: setting table to ".concat(supertableName));
        }
        supertable = new File(database.getCanonicalPath()
                .concat("/").concat(supertableName));

        if (!supertable.mkdirs() && !supertable.isDirectory()
                || !supertable.canWrite() || !supertable.canRead()) {
            throw new Exception("Unable to access, create or write table directory: "
                    .concat(supertableName));
        }

        playersTable = new File(supertable.getCanonicalPath().concat("/players.dat"));

        if (debug) {
            plugin.getLogger().info("DEBUG: Looking for players table: ".concat(playersTable.getCanonicalPath()));
        }
        if (playersTable.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(playersTable.getCanonicalPath()))) {
                playerIDs = (TreeMap<String, Integer>) ois.readObject();
                if (debug && playerIDs != null) {
                    plugin.getLogger().log(Level.INFO, "DEBUG: {0} players loaded from table.", playerIDs.size());
                }
                ois.close();
            }

            if (playerIDs == null) {
                plugin.getLogger().severe("Players table is corrupted. Saved user information were lost.");
                File corruptedFile = new File(supertable.getCanonicalPath().concat("/")
                        .concat(new SimpleDateFormat("mmss").format(new Date())
                        .concat("corrupted-players.dat")));
                playersTable.renameTo(corruptedFile);
                playersTable.deleteOnExit();
            }

        } else {
            if (debug) {
                plugin.getLogger().info("DEBUG: Players table not found, creating a new one on memory.");
            }
            playerIDs = new TreeMap<>();
        }

    }

    private int getPlayerID(String playerName) {
        if (playerIDs != null) {
            if (!playerIDs.containsKey(playerName)) {
                playerIDs.put(playerName, playerIDs.size());
            }
            return playerIDs.get(playerName);
        }
        return -1;
    }

    public void loadBloomFilter() {
        try {
            this.dbFilter = getBloomFilter();
        } catch (Exception ex) {
            plugin.getLogger().severe(ex.toString());
            plugin.getLogger().severe("Database will work with low performance.");
            dbFilter = null;
        }
    }

    private BloomFilter<String> getBloomFilter() throws FileNotFoundException, IOException {
        dbFilter = new BloomFilter<>(10_000_000, 0.01);
        dbFilter.add("MEODE 1.0");
        File bloomFilterFile = new File(database.getAbsolutePath()
                .concat("/filter.dat"));
        if (bloomFilterFile.exists()) {
            if (debug) {
                plugin.getLogger().log(Level.INFO, "DEBUG: {0} file found.", bloomFilterFile);
            }
            FileInputStream fis = new FileInputStream(bloomFilterFile);
            try (GZIPInputStream gzipIn = new GZIPInputStream(fis)) {
                try (ObjectInputStream objectIn = new ObjectInputStream(gzipIn)) {
                    dbFilter.setBitSet((BitSet) objectIn.readObject());
                }
            } catch (ClassNotFoundException ex) {
                plugin.getLogger().severe("filter.dat corrupted. Reindexing requiered.");
                dbFilter = null;
                if (debug) {
                    plugin.getLogger().info("DEBUG: ".concat(ex.toString()));
                }
            }
            fis.close();
        }

        if (dbFilter != null) {
            if (!dbFilter.contains("MEODE 1.0")) {
                plugin.getLogger().severe("filter.dat incompatible with this engine version.");
                dbFilter = null;
            }
        }

        return dbFilter;
    }

    public void peristRamAsynchronously(RAMStore rams) {
        Object objects[] = {rams, this};
        new AsyncTask(AsyncTask.TaskType.SAVE_EVENTS, objects, plugin).runTaskAsynchronously(plugin);
    }

    /**
     *
     * @param rams RAMStore Object
     */
    public void peristRamSynchronously(RAMStore rams) throws IOException, ClassNotFoundException, Exception {
        TreeSet<BlockEvent> blockEvents = rams.getBlockEventsAndRotate();
        File table = null;
        File row = null;
        boolean pathChanged = false;
        DataOutputStream rowStream = null;
        for (Iterator<BlockEvent> it = blockEvents.iterator(); it.hasNext();) {
            BlockEvent be = it.next();

            if (supertable == null || !supertable.getPath().endsWith(getSuperTableName(be.eventTime))) {
                if (supertable != null) {
                    persistPlayersTable();
                }
                this.setSupertable(getSuperTableName(be.eventTime));
                pathChanged = true;
            }

            if (table == null || !table.getPath().endsWith(getTableName(be.location.getWorld().getName(),
                    be.location.getBlockZ()))) {
                table = new File(supertable.getAbsolutePath().concat("/")
                        .concat(getTableName(be.location.getWorld().getName(),
                        be.location.getBlockZ())));
                table.mkdir();
                pathChanged = true;
            }

            if (pathChanged || row == null || !row.getPath().endsWith(getRowName(be.location.getBlockY(), be.placed))) {
                pathChanged = false;
                if (rowStream != null) {
                    if (debug && row != null) {
                        plugin.getLogger().info("DEBUG: Closing file ".concat(row.getAbsolutePath()));
                    }
                    rowStream.close();
                }

                row = new File(table.getAbsolutePath().concat("/")
                        .concat(getRowName(be.location.getBlockY(), be.placed)));

                if (debug) {
                    plugin.getLogger().info("DEBUG: Opening file ".concat(row.getAbsolutePath()));
                }

                rowStream = new DataOutputStream(new FileOutputStream(row, true));
            }

            if (rowStream != null) {
                rowStream.writeInt(be.location.getBlockX());
                rowStream.writeInt(be.location.getBlockZ());
                rowStream.writeShort(Auxiliary.minutesFromMidnight(be.eventTime));
                rowStream.writeInt(be.blockTypeID);
                rowStream.writeByte(be.blockData);
                rowStream.writeShort(getPlayerID(be.playerName));
                addModifiedBlockToFilter(be.location);

                if (be.blockTypeID == 63 || be.blockTypeID == 68) {
                    saveSigns(supertable, be.location, be.aditionalData);
                }
            }

        } // for (Iterator...

        if (rowStream != null) {
            if (debug && row != null) {
                plugin.getLogger().info("DEBUG: Closing file ".concat(row.getAbsolutePath()));
            }
            rowStream.close();
        }
        if (supertable != null) {
            persistPlayersTable();
        }

        saveDBFilterToDisk();
    }

    private static void saveSigns(File supertable, Location loc, String aditionalData) throws FileNotFoundException, IOException {
        if (aditionalData == null) {
            return;
        }
        File signDB = new File(supertable, loc.getWorld().getName().concat("-signs.db"));
        DataOutputStream signStream = new DataOutputStream(new FileOutputStream(signDB, true));
        signStream.writeInt(loc.getBlockX());
        signStream.writeInt(loc.getBlockY());
        signStream.writeInt(loc.getBlockZ());
        signStream.writeBytes(aditionalData.concat("\n"));
        signStream.close();
    }

    private String getSignsText(String supertableName, Location loc, BlockEvent be) throws FileNotFoundException, IOException {
        File signDB = new File(database.getAbsolutePath().concat("/").concat(supertableName), loc.getWorld().getName().concat("-signs.db"));
        if (!signDB.exists()) {
            return null;
        }
        RandomAccessFile signAccess = new RandomAccessFile(signDB, "r");
        String result = null;
        int registerSize = 61 + (3 * 4);
        int cursor = 0;
        while (true) {
            signAccess.seek(cursor);

            int X = signAccess.readInt();
            int Y = signAccess.readInt();
            int Z = signAccess.readInt();
            if (loc.getBlockX() == X && loc.getBlockY() == Y && loc.getBlockZ() == Z) {
                result = signAccess.readLine();
                break;
            }
            cursor = cursor + registerSize;
            if (cursor > signAccess.length() - registerSize) {
                break;
            }
        }
        signAccess.close();
        return result;
    }

    void saveDBFilterToDisk() throws FileNotFoundException, IOException {
        if (debug) {
            plugin.getLogger().info("DEBUG: Saving DB Filter to disk...");
        }
        File bloomFilterFile;
        if (dbFilter != null) {
            bloomFilterFile = new File(database.getAbsolutePath(), "/filter.dat.tmp");
            bloomFilterFile.delete();
            FileOutputStream fos = new FileOutputStream(bloomFilterFile);
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(fos)) {
                try (ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut)) {
                    objectOut.writeObject(dbFilter.getBitSet());
                }
            }
            fos.close();
            if (debug) {
                plugin.getLogger().info("DB Filter saved.");
            }
            File oldBloomFilterFile = new File(database.getAbsolutePath(), "/filter.dat");
            oldBloomFilterFile.delete();
            bloomFilterFile.renameTo(oldBloomFilterFile);
        } else {
            if (debug) {
                plugin.getLogger().info("DB Filter not actived, nothing to save.");
            }
        }
    }

    public String query(Location loc, boolean placed) throws IOException, ClassNotFoundException, ParseException {
        BlockEvent be = getLastBlockEvent(loc, placed);
        String result = null;
        if (be != null) {
            result = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(be.eventTime)
                    .concat(" ").concat(be.playerName)
                    .concat(be.placed ? " placed " : " removed ")
                    .concat(Material.getMaterial(be.blockTypeID).toString());
        }
        return result;
    }

    public BlockEvent getLastBlockEvent(Location loc, boolean placed) throws IOException, ClassNotFoundException, ParseException {
        BlockEvent result = null;

        TreeSet<String> tables = new TreeSet<>(Collections.reverseOrder());
        tables.addAll(Arrays.asList(database.list(supertableNameFilter)));


        for (Iterator<String> it = tables.iterator(); it.hasNext();) {
            String superTableName = it.next();
            File table = new File(database.getCanonicalPath()
                    .concat("/")
                    .concat(superTableName)
                    .concat("/")
                    .concat(getTableName(loc.getWorld().getUID().getLeastSignificantBits(), loc.getBlockZ())));

            if (!table.exists()) {
                table = new File(database.getCanonicalPath()
                        .concat("/")
                        .concat(superTableName)
                        .concat("/")
                        .concat(getTableName(loc.getWorld().getName(), loc.getBlockZ())));
                if (!table.exists())
                    continue;
            }

            File row = new File(table.getAbsolutePath().concat("/").concat(getRowName(loc.getBlockY(), placed)));

            if (!row.exists()) {
                continue;
            }

            RandomAccessFile rowAccess;

            if (lastQueryCacheString == null) {
                lastQueryCacheString = row.getAbsolutePath();
                rowAccess = new RandomAccessFile(row, "r");
                lastQueryCacheAccessFile = rowAccess;
            } else {
                if (lastQueryCacheString.equals(row.getAbsolutePath())) {
                    try {
                        lastQueryCacheAccessFile.read();
                        rowAccess = lastQueryCacheAccessFile;

                    } catch (IOException ex) {
                        rowAccess = new RandomAccessFile(row, "r");
                    }
                } else {
                    lastQueryCacheAccessFile.close();
                    rowAccess = new RandomAccessFile(row, "r");
                    lastQueryCacheAccessFile = rowAccess;
                }
            }

            for (long i = rowAccess.length() - rowSize; i >= 0; i -= rowSize) {
                rowAccess.seek(i);
                int blockPosX = rowAccess.readInt();
                int blockPosZ = rowAccess.readInt();
                if (blockPosX == loc.getBlockX() && blockPosZ == loc.getBlockZ()) {
                    result = new BlockEvent();
                    short minutesFromMidNight = rowAccess.readShort();
                    result.blockTypeID = rowAccess.readInt();
                    result.blockData = rowAccess.readByte();
                    short playerId = rowAccess.readShort();
                    result.playerName = getPlayerFromSupertable(superTableName, playerId);
                    SimpleDateFormat superTableFormat = new SimpleDateFormat("yyyy-MM-dd");
                    result.eventTime = new Date((60 + minutesFromMidNight) * 60 * 1000).getTime() + superTableFormat.parse(superTableName).getTime();
                    // Time in Disk has not defintion, the comparator founds blocks equals so...
                    result.eventTime = result.eventTime + i;
                    result.placed = placed;
                    result.location = loc;
                    if (result.blockTypeID == 63 || result.blockTypeID == 68) {
                        result.aditionalData = getSignsText(superTableName, loc, result);
                    }
                    return result;
                }
            }
            rowAccess.close();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String getPlayerFromSupertable(String superTableName, short playerId)
            throws IOException, ClassNotFoundException {
        TreeMap<String, Integer> playerIDsLocal;
        File playerTable = new File(database.getCanonicalPath()
                .concat("/").concat(superTableName)
                .concat("/players.dat"));
        if (!playerTable.exists()) {
            return "(Unknown:" + playerId + ")";
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(playerTable.getCanonicalPath()))) {
            playerIDsLocal = (TreeMap<String, Integer>) ois.readObject();
            ois.close();
        }

        Set<String> playerName = Auxiliary.getKeysByValue(playerIDsLocal, (int) playerId);
        if (playerName != null && !playerName.isEmpty()) {
            return playerName.iterator().next();
        }
        return "(Unknown:" + playerId + ")";
    }

    public TreeSet<BlockEvent> getBlockEvents(Location minLoc, Location maxLoc,
            TreeSet<BlockEvent> ignore, String playerName)
            throws IOException, ClassNotFoundException, ParseException {
        TreeSet<BlockEvent> resultSet = new TreeSet<>(new BlockEventsTimeComparator());
        TreeSet<BlockEvent> querySet = new TreeSet<>();

        if (minLoc.getBlockY() < 0) {
            minLoc.setY(0);
        }

        if (maxLoc.getY() > 255) {
            maxLoc.setY(255);
        }

        World world = minLoc.getWorld();

        int id = 0;
        for (int x = minLoc.getBlockX(); x <= maxLoc.getBlockX(); x++) {
            for (int y = minLoc.getBlockY(); y <= maxLoc.getBlockY(); y++) {
                for (int z = minLoc.getBlockZ(); z <= maxLoc.getBlockZ(); z++) {
                    if (!filterContains(world.getName(), x, y, z)) {
                        continue;
                    }
                    Location loc = new Location(world, x, y, z);
                    BlockEvent be = new BlockEvent();
                    be.location = loc;
                    be.eventTime = id;
                    querySet.add(be);
                    id++;
                }
            }
        }

        BlockEvent result;
        for (BlockEvent be : querySet) {
            for (int placed = 0; placed <= 1; placed++) {

                if (placed == 0) {
                    result = getLastBlockEvent(be.location, true);
                } else {
                    result = getLastBlockEvent(be.location, false);
                }

                if (result == null) {
                    continue;
                }

                if (playerName != null) {
                    if (!playerName.equalsIgnoreCase(result.playerName)) {
                        continue;
                    }
                }

                resultSet.add(result);

            }
        }

        return resultSet;
    }

    protected void addModifiedBlockToFilter(Block block) {
        if (dbFilter != null) {
            addModifiedBlockToFilter(block.getLocation());
        }
    }

    protected void addModifiedBlockToFilter(Location loc) {
        if (dbFilter != null) {
            dbFilter.add(loc.getWorld().getName()
                    + loc.getBlockX() + "X"
                    + loc.getBlockY() + "Y"
                    + loc.getBlockZ() + "Z");
        }
    }

    public boolean filterContains(Block block) {
        return filterContains(block.getLocation());
    }

    public boolean filterContains(Location loc) {
        return filterContains(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean filterContains(String worldName, int X, int Y, int Z) {
        if (dbFilter != null) {
            return dbFilter.contains(worldName
                    + X + "X"
                    + Y + "Y"
                    + Z + "Z");
        } else {
            return true;
        }
    }
}
