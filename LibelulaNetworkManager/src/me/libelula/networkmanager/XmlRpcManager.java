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

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.bukkit.Bukkit;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org> <ddonofrio@member.fsf.org>
 */
public class XmlRpcManager {

    private final Main plugin;
    private final ConfigurationManager cm;

    class XRInterface implements Callable<String> {

        private final String method;
        private final Object[] params;

        public XRInterface(String method, Object[] params) {
            this.method = method;
            this.params = params;
        }

        @Override
        public String call() throws Exception {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setConnectionTimeout(cm.getXmlrpcMsTimeOut());
            config.setGzipCompressing(true);
            config.setGzipRequesting(true);
            config.setServerURL(cm.getXmlrpcURI());
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            Object objResult = client.execute(method, params);
            String result = "";
            if (objResult instanceof String) {
                result = (String) objResult;
            } else {
                result = result + objResult;
            }
            return result;
        }
    }

    public XmlRpcManager(Main plugin) {
        this.plugin = plugin;
        cm = plugin.config;
    }

    private String getXmlRpcData(String method, Object[] params) {
        String resp = "-99";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future;
        future = executor.submit(new XRInterface(method, params));
        try {
            resp = future.get(cm.getXmlrpcMsTimeOut(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException ex) {
            plugin.getLogger().log(Level.SEVERE, "XMLRPC: {0}", ex.getMessage());
        }
        executor.shutdown();
        if (cm.isDebug()) {
            plugin.getLogger().log(Level.INFO, "XMLRPC desponce: {0}", resp);
        }
        return resp;
    }

    public void storePlayerInfo(final PlayerManager.PlayerInfo pi) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            String banReason = pi.getBanReason();
            if (banReason == null) {
                banReason = "";
            }
            Object[] params = {cm.getXmlrpcUser(), cm.getXmlrpcPassword(),
                pi.getName(), pi.getServer().getName(), pi.getTotalPlayedMs() / 1000,
                (pi.isBanned() ? "true" : "false"), banReason};

            plugin.getLogger().info(Arrays.toString(params));
            if (cm.isDebug()) {
                getXmlRpcData("libelula.storePlayerInfo", params);
            }

        });

    }

}
