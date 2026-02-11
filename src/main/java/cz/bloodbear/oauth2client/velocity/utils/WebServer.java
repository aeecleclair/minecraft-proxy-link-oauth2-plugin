package cz.bloodbear.oauth2client.velocity.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.oauth2client.core.records.OAuth2Account;
import cz.bloodbear.oauth2client.core.utils.ConsoleColor;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WebServer {
    private HttpServer server;
    private final int port;
    private final boolean useDomain;
    private final String domain;

    public WebServer(int port, boolean useDomain, String domain) {
        this.port = port;
        this.useDomain = useDomain;
        this.domain = domain;
    }

    private class OAuthCallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if(!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                if(useDomain) {
                    String origin = exchange.getRequestHeaders().getFirst("Origin");
                    if(domain.equalsIgnoreCase(origin)) {
                        sendResponse(exchange, 403, "Forbidden");
                        return;
                    }
                }

                String query = exchange.getRequestURI().getQuery();
                String state = getQueryParam(query, "state");
                String code = getQueryParam(query, "code");

                if(state == null) {
                    // sendResponse(exchange, 400, "State parameter missing");
                    sendHtmlResponse(exchange, 400, OAuth2Client.getInstance().getHtmlPage("stateMissing").getContent());
                    return;
                }

                if(code == null) {
                    //sendResponse(exchange, 400, "Code parameter missing");
                    sendHtmlResponse(exchange, 400, OAuth2Client.getInstance().getHtmlPage("codeMissing").getContent());
                    return;
                }

                String uuid = OAuth2Client.getInstance().getDatabaseManager().getPlayerByCode(state);
                if(uuid == null) {
                    //sendResponse(exchange, 400, "Invalid or expired code");
                    sendHtmlResponse(exchange, 400, OAuth2Client.getInstance().getHtmlPage("invalid").getContent());
                    return;
                }

                OAuth2Account OAuth2Account = OAuth2Client.getInstance().getOAuth2Handler().getOAuth2Account(code);
                if(OAuth2Account == null) {
                    //sendResponse(exchange, 400, "Failed to verify account.");
                    sendHtmlResponse(exchange, 400, OAuth2Client.getInstance().getHtmlPage("failed").getContent());
                    return;
                }

                // From this point, we have a valid UUID and an account from the OAuth2 provider

                // We would like to check if the OAuth2 provider account is already linked
                // And if the current Minecraft UUID corresponds to this account
                /*if(oauth2client.getInstance().getDatabaseManager().isOAuth2AccountLinked(OAuth2Account.id()) &&
                !oauth2client.getInstance().getDatabaseManager().getOAuth2Account(uuid).id().equals(OAuth2Account.id())) {
                    sendHtmlResponse(exchange, 400, oauth2client.getInstance().getHtmlPage("alreadylinked").getContent());
                    return;
                }*/


                if(OAuth2Client.getInstance().getDatabaseManager().isLinked(uuid)) {

                    // If the UUID is already linked, we need to check if it's linked to the same account on the OAuth2 provider
                    OAuth2Account existingAccount = OAuth2Client.getInstance().getDatabaseManager().getOAuth2Account(uuid);
                    if(!existingAccount.id().equals(OAuth2Account.id())) {
                        sendHtmlResponse(exchange, 400, OAuth2Client.getInstance().getHtmlPage("alreadylinked").getContent());
                        return;
                    }
                } else {
                    // If the UUID is not linked, we need to check if the OAuth2 provider account is already linked to another UUID
                    if(OAuth2Client.getInstance().getDatabaseManager().isOAuth2AccountLinked(OAuth2Account.id())) {
                        sendHtmlResponse(exchange, 400, OAuth2Client.getInstance().getHtmlPage("alreadylinked").getContent());
                        return;
                    }
                    // Then we can proceed to link the account
                    OAuth2Client.getInstance().getDatabaseManager().linkAccount(uuid, OAuth2Account.id(), OAuth2Account.username());
                }

                OAuth2Client.getInstance().getAuthManager().authenticate(UUID.fromString(uuid));
                sendHtmlResponse(exchange, 200, OAuth2Client.getInstance().getHtmlPage("linked").getContent());

                Optional<Player> player = OAuth2Client.getInstance().getServer().getPlayer(UUID.fromString(uuid));
                // Move the player to the lobby/host server after linking
                player.ifPresent(p -> p.createConnectionRequest(OAuth2Client.getInstance().getServer().getServer("lobby").orElse(null)).fireAndForget());


            } catch (Exception e) {
                OAuth2Client.getInstance().getLogger().error(e.getMessage());
            }
        }
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if(keyValue.length == 2 && keyValue[0].equals(param)) {
                return keyValue[1];
            }
        }
        return null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void sendHtmlResponse(HttpExchange exchange, int statusCode, String htmlContent) throws IOException {
        byte[] responseBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
