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
package me.libelula.liderswag;

import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ScoreManager {

    private final Main plugin;

    private final TreeMap<Player, Objective> scoreBoards;
    private final TreeMap<String, Integer> scores;
    private final TreeSet<String> modified;

    public ScoreManager(Main plugin) {
        this.plugin = plugin;
        scoreBoards = new TreeMap<>(new Tools.PlayerComparator());
        scores = plugin.dm.getScores();
        modified = new TreeSet<>();
    }

    public void setScoreboard(Player player) {
        if (scoreBoards.containsKey(player)) {
            player.setScoreboard(scoreBoards.get(player).getScoreboard());
        } else {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective sideBar = board.registerNewObjective("player", "score");
            sideBar.setDisplaySlot(DisplaySlot.SIDEBAR);
            sideBar.setDisplayName(plugin.lm.getTranslatedText("scoreboard-title"));
            Team team = board.registerNewTeam("dummy");
            team.addPlayer(Bukkit.getOfflinePlayer(plugin.lm.getTranslatedText("scoreboard-score")));
            team.addPlayer(Bukkit.getOfflinePlayer(plugin.lm.getTranslatedText("scoreboard-total-score")));
            scoreBoards.put(player, sideBar);
            final Score playerScore = scoreBoards.get(player).getScore(Bukkit.getOfflinePlayer(plugin.lm.getTranslatedText("scoreboard-score")));
            final Score playerTotalScore = scoreBoards.get(player).getScore(Bukkit.getOfflinePlayer(plugin.lm.getTranslatedText("scoreboard-total-score")));
            playerScore.setScore(0);
            Integer total = scores.get(player.getName());
            if (total != null) {
                playerTotalScore.setScore(total);
            } else {
                playerTotalScore.setScore(0);
            }
            player.setScoreboard(board);
        }
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void playReminigHealthPointsEffect(final Player player) {
        final double playerHealth = player.getHealth();
        int ScheduleTime = 0;
        for (double i = playerHealth; i > 0.2; i = i - 0.2) {
            ScheduleTime++;
            final double newHealth = i;
            final Score playerScore = scoreBoards.get(player).getScore(Bukkit.getOfflinePlayer(plugin.lm.getTranslatedText("scoreboard-score")));
            final Score playerTotalScore = scoreBoards.get(player).getScore(Bukkit.getOfflinePlayer(plugin.lm.getTranslatedText("scoreboard-total-score")));

            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    player.setHealth(newHealth);
                    player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                    playerScore.setScore(playerScore.getScore() + 1);
                    playerTotalScore.setScore(playerTotalScore.getScore() + 1);
                }
            }, ScheduleTime);
        }
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                player.setHealth(player.getMaxHealth());
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1, 1);
                modified.add(player.getName());
                scores.put(player.getName(), scoreBoards.get(player).getScore(Bukkit.getOfflinePlayer(plugin.lm.getTranslatedText("scoreboard-total-score"))).getScore());
            }
        }, ScheduleTime + 20);
    }

    public void save() {
        for (String playerName : modified) {
            Integer score = scores.get(playerName);
            if (score != null) {
                plugin.dm.setScore(playerName, score);
            }
        }
    }
}
