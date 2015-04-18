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
package me.libelula.networkmanager;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ChatManager {

    private final Main plugin;
    private final TreeMap<String, String> playerLastChatLine;
    private final TreeMap<String, Integer> playerWarnings;
    private String lastTalkPlayer;
    private long lastChatLineTime;
    private final ReentrantLock _player_mutex;
    private final int secondsBetweenSameLine;
    private final Pattern badWordsPattern;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.secondsBetweenSameLine = 10;
        _player_mutex = new ReentrantLock(true);
        playerLastChatLine = new TreeMap<>();
        playerWarnings = new TreeMap<>();
        lastTalkPlayer = "";
        lastChatLineTime = 0;

        File enBadWords = new File(plugin.getDataFolder(), "badwords.txt");
        File spBadWords = new File(plugin.getDataFolder(), "malaspalabras.txt");

        if (!enBadWords.exists()) {
            plugin.saveResource(enBadWords.getName(), true);
        }

        if (!spBadWords.exists()) {
            plugin.saveResource(spBadWords.getName(), true);
        }

        String badWordsList;
        badWordsList = loadBadWords(enBadWords);
        badWordsList = badWordsList.concat("|" + loadBadWords(spBadWords));
        badWordsPattern = Pattern.compile("\\b(" + badWordsList + ")\\b");
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().log(Level.INFO, "Debug: bad world list: {0}", badWordsList);
        }
    }

    private String loadBadWords(File file) {
        String badWordsList = "";
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(file);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                //Read File Line By Line
                while ((strLine = br.readLine()) != null) {
                    badWordsList = badWordsList.concat(strLine + "|");
                }
                badWordsList = badWordsList.substring(0, badWordsList.length() - 1);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe(ex.toString());
        }
        return badWordsList.replaceAll(" ", "");
    }

    public void processChat(AsyncPlayerChatEvent e) {
        if (!e.getPlayer().hasPermission("lnm.freechat")) {
            String playerName = e.getPlayer().getName();

            e.setMessage(e.getMessage().replaceAll("^ +| +$|( )+", " "));

            _player_mutex.lock();
            try {
                String lastLine = playerLastChatLine.get(playerName);
                long now = new Date().getTime();
                if (lastLine != null && lastTalkPlayer.equals(playerName)) {
                    if (lastLine.equals(e.getMessage())) {
                        if (now - lastChatLineTime < (secondsBetweenSameLine * 1000)) {
                            e.setCancelled(true);
                            plugin.pm.sendSyncMessage(e.getPlayer(), ChatColor.RED
                                    + "No es necesario que repitas, ya te hemos leído.", true);
                        }
                    }
                }

                if (!e.isCancelled() && e.getMessage().length() > 3) {

                    int capitalLetters = 0;
                    for (char c : e.getMessage().toCharArray()) {
                        if (c <= 'Z' && c >= 'A') {
                            capitalLetters++;
                        }
                    }

                    if (capitalLetters > e.getMessage().length() / 2) {
                        e.setMessage(capitalize(e.getMessage()));
                    }
                }

                String lowerCaseMessage = e.getMessage().toLowerCase();
                boolean badWordFound = false;
                int badWorldCounter = 0;
                do {
                    Matcher matcher = badWordsPattern.matcher(lowerCaseMessage);
                    if (matcher.find()) {
                        e.setMessage(lowerCaseMessage.replace(matcher.group(0), "@#$&#!"));
                        lowerCaseMessage = e.getMessage();
                        badWordFound = true;
                        badWorldCounter++;
                    } else {
                        badWordFound = false;
                    }
                } while (badWordFound);

                if (badWorldCounter > 0) {
                    Integer warnings = playerWarnings.get(playerName);
                    if (warnings == null) {
                        plugin.pm.sendSyncMessage(e.getPlayer(), "La red libélula es un lugar familiar apto para los más pequeños, por favor modera tu lenguaje.", true);
                        playerWarnings.put(playerName, 1);
                    } else {
                        switch (warnings) {
                            case 1:
                                plugin.pm.sendSyncMessage(e.getPlayer(), "Estás rompiendo la 6ta regla de la red Libélula, es posible que recibas una sanción por tu vocabulario.", true);
                                break;
                            case 2:
                                plugin.pm.sendSyncMessage(e.getPlayer(), "Por favor, intenta adaptar tu vocabulario al lugar dónde estás jugando.", true);
                                break;
                            case 3:
                                plugin.pm.sendSyncMessage(e.getPlayer(), "Has el esfuerzo de omitir las palabras inadecuadas.", true);
                                break;
                            default:
                                break;

                        }
                        playerWarnings.put(playerName, warnings + 1);
                    }
                }

                if (!e.isCancelled()) {
                    playerLastChatLine.put(playerName, e.getMessage());
                    lastTalkPlayer = playerName;
                    lastChatLineTime = now;
                }

            } finally {
                _player_mutex.unlock();
            }

        }
    }

    private String capitalize(String string) {
        String result;

        result = string.substring(0, 1).toUpperCase().concat(
                string.toLowerCase().substring(1));

        return result;
    }
}
