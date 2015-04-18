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

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Class Internationalization of the plugin.
 *
 * @author Diego Lucio D'Onofrio <ddonofrio@member.fsf.org>
 * @version 1.0
 */
public class Internationalization {

    private FileConfiguration language;

    public Internationalization(FileConfiguration language) {
        this.language = language;
    }
    
    public void setLang(FileConfiguration language) {
        this.language = language;
    }

    public String getText(String label) {
        if (language == null) {
            return label;
        }
        String text = language.getString(label);
        if (text != null) {
            return text;
        } else {
            return label;
        }
    }
}
