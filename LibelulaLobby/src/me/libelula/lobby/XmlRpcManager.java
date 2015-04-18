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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

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
        cm = plugin.cm;
    }

    public void setPremium(PlayerManager.PlayerInstance pi) {
        Object[] params = new Object[]{cm.getXmlrpcUser(),
            cm.getXmlrpcPassword(), pi.getPlayer().getName()};
        String result = getXmlRpcData("libelula.setPremium", params);
        switch (result) {
            case "-1":
                plugin.logErr("XMLRPC Server Invalid credentials for user: " + cm.getXmlrpcUser());
                break;
            case "-2":
                plugin.logErr("XMLRPC Server the user " + cm.getXmlrpcUser() + " is not an admin.");
                break;
            case "-3":
                if (cm.isDebugMode()) {
                    plugin.logInfo("XMLRPC Server player " + pi.getPlayer().getName() + " is not registered.");
                    break;
                }
            case "0":
                if (cm.isDebugMode()) {
                    plugin.logInfo("XMLRPC Server player " + pi.getPlayer().getName() + " was premium (nothing to do).");
                }
                break;
            case "1":
                if (cm.isDebugMode()) {
                    plugin.logInfo("XMLRPC Server player " + pi.getPlayer().getName() + " is now marked as premium.");
                }
                break;
        }
    }

    public void checkForPremium(PlayerManager.PlayerInstance pi) {
        Object[] params = new Object[]{cm.getXmlrpcUser(),
            cm.getXmlrpcPassword(), pi.getPlayer().getName()};
        String result = getXmlRpcData("libelula.isPremium", params);

        switch (result) {
            case "-1":
                plugin.logErr("XMLRPC Server Invalid credentials for user: " + cm.getXmlrpcUser());
                break;
            case "-2":
                plugin.logErr("XMLRPC Server the user " + cm.getXmlrpcUser() + " is not an admin.");
                break;
            case "-3":
                plugin.logWarn("XMLRPC Server player " + pi.getPlayer().getName() + " was not registered (checking for premium).");
                plugin.pm.removePlayerFromList(pi.getPlayer());
                pi.kickPlayer(cm.getNonPremiumUnregisteredMsg());
                break;
            case "1":
                if (cm.isDebugMode()) {
                    plugin.logInfo("XMLRPC Server player " + pi.getPlayer().getName() + " was marked as premium.");
                }
                pi.setState(PlayerManager.playerState.PREMIUM);
                pi.kickPlayer(cm.getPremiumKickMessage());
                break;
            default:
                if (cm.isDebugMode()) {
                    if ("0".equals(result)) {
                        plugin.logInfo("XMLRPC Server player " + pi.getPlayer().getName() + " OK = is not premium.");
                    } else {
                        plugin.logInfo("XMLRPC Server player " + pi.getPlayer().getName() + " error '" + result + "' occurs while checking for premium.");
                    }
                }
                pi.setCheckStatus(PlayerManager.nonPremiumChecks.SENDING_TO_LOBBY);
                break;
        }

    }

    public void checkForRegistered(PlayerManager.PlayerInstance pi) {
        Object[] params = new Object[]{pi.getPlayer().getName()};
        String result = getXmlRpcData("libelula.userExists", params);

        switch (result) {
            case "-1":
                plugin.pm.removePlayerFromList(pi.getPlayer());
                pi.kickPlayer(cm.getNonPremiumUnregisteredMsg());
                break;
            default:
                pi.setState(PlayerManager.playerState.PROCESSING);
                pi.setCheckStatus(PlayerManager.nonPremiumChecks.CHECKING_FOR_PREMIUM);
        }

    }

    public void checkPassword(PlayerManager.PlayerInstance pi) {
        Object[] params = new Object[]{pi.getPlayer().getName(),
            pi.getTypedPassword()};
        String result = getXmlRpcData("libelula.login", params);
        
        plugin.logInfo("Login " + pi.getPlayer().getName() +  ":" 
                + pi.getTypedPassword() + " = " + result);
        
        switch (result) {
            case "0":
                plugin.pm.logginSucces(pi);
                break;
            case "1":
                plugin.sendMessage(pi.getPlayer(), "&4¡Contraseña incorrecta!");
                plugin.sendMessage(pi.getPlayer(), "Debes usar la misma que te hemos enviado por correo al registrarte.");
                pi.setState(PlayerManager.playerState.REGISTERED);
                break;
            case "2":
                plugin.sendMessage(pi.getPlayer(), "&4¡Cuenta no activada aún!");
                plugin.sendMessage(pi.getPlayer(), "Debes esperar a que un administrador active tu cuenta, esto suele tardar 1 día como mínimo.");
                pi.setState(PlayerManager.playerState.UNACTIVATED);
                break;
            case "3":
                plugin.sendMessage(pi.getPlayer(), "&4¡Cuenta denegada por un administrador!");
                pi.setState(PlayerManager.playerState.BANNED);
                break;
            default:
                plugin.sendMessage(pi.getPlayer(), "&4¡Ha ocurrido un error interno del servidor!");
                plugin.sendMessage(pi.getPlayer(), "Espera un rato e inténtalo de nuevo.");
                pi.setState(PlayerManager.playerState.REGISTERED);
        }

    }

    private String getXmlRpcData(String method, Object[] params) {
        String resp = "-99";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future;
        future = executor.submit(new XRInterface(method, params));
        try {
            resp = future.get(cm.getXmlrpcMsTimeOut(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException ex) {
            plugin.logErr("XMLRPC: " + ex);
        }
        executor.shutdown();
        return resp;
    }
}
