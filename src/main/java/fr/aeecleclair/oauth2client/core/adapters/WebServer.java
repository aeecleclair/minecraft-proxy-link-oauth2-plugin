package fr.aeecleclair.oauth2client.core.adapters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

import fr.aeecleclair.oauth2client.core.utils.OAuth2Account;
import fr.aeecleclair.oauth2client.velocity.OAuth2Client;
import fr.aeecleclair.oauth2client.velocity.player.LuckPerms;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class WebServer {
    private HttpServer server;
    private final int port;
    private final boolean useDomain;
    private final int nThreads;
    private final String callbackEndpoint;
    private final String AuthUrl;
    private final BiConsumer<UUID, String> moveToLobbyCallback;


    public WebServer(
        int port,
        boolean useDomain,
        int nThreads,
        String callbackEndpoint,
        String AuthUrl,
        BiConsumer<UUID, String> moveToLobbyWithMessage
    ) {
        this.port = port;
        this.useDomain = useDomain;
        this.nThreads = nThreads;
        this.callbackEndpoint = callbackEndpoint;
        this.AuthUrl = AuthUrl;
        this.moveToLobbyCallback = moveToLobbyWithMessage;
    }

    public void start() throws IOException {
        Executor executor = Executors.newFixedThreadPool(nThreads, r -> {
            Thread t = new Thread(r);
            t.setDaemon(false);
            t.setName("HttpServer-Worker");
            return t;
        });
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(callbackEndpoint, new OAuthCallbackHandler());
        server.createContext("/", SimpleFileServer.createFileHandler(OAuth2Client.getZip().toAbsolutePath()));
        server.setExecutor(executor);
        server.start();
        OAuth2Client.logger().info("Webserver is running on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            OAuth2Client.logger().info("Webserver disabled.");
        }
    }

    private class OAuthCallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                if (useDomain) {
                    String referer = exchange.getRequestHeaders().getFirst("Referer");
                    if (!referer.equalsIgnoreCase(AuthUrl + "/")) {
                        sendResponse(exchange, 403, "Forbidden");
                        return;
                    }
                }

                String query = exchange.getRequestURI().getQuery();
                String state = getQueryParam(query, "state");
                String code = getQueryParam(query, "code");

                if (state == null) {
                    sendHtmlResponse(exchange, 400, OAuth2Client.getHtmlPage("stateMissing").getContent());
                    return;
                }

                if (code == null) {
                    sendHtmlResponse(exchange, 400, OAuth2Client.getHtmlPage("codeMissing").getContent());
                    return;
                }

                String minecraftUUID = OAuth2Client.getDatabaseManager().getPlayerByCode(state);
                if (minecraftUUID == null) {
                    sendHtmlResponse(exchange, 400, OAuth2Client.getHtmlPage("invalidState").getContent());
                    return;
                }

                OAuth2Account OAuth2Account = OAuth2Client.OAuth2Handler().getOAuth2Account(code);
                if (OAuth2Account == null) {
                    sendHtmlResponse(exchange, 400, OAuth2Client.getHtmlPage("invalidCode").getContent());
                    return;
                }

                // From this point, we have a valid UUID and an account from the OAuth2 provider.
                // We would like to check if the OAuth2 provider account is already linked,
                // and if the current Minecraft UUID corresponds to this account.
                OAuth2Client.logger().debug("Login attempt for " + OAuth2Account.username() + " as " + minecraftUUID + ".");

                if (OAuth2Client.getDatabaseManager().isLinked(minecraftUUID)) {

                    // If the UUID is already linked, we need to check if it's linked to the same account on the OAuth2 provider
                    OAuth2Account existingAccount = OAuth2Client.getDatabaseManager().getOAuth2Account(minecraftUUID);
                    if (!existingAccount.id().equals(OAuth2Account.id())) {
                        sendHtmlResponse(exchange, 400, OAuth2Client.getHtmlPage("alreadyLinkedMinecraft").getContent());
                        return;
                    }
                    moveToLobbyCallback.accept(UUID.fromString(minecraftUUID), "command.loggedin");
                } else {
                    // If the UUID is not linked, we need to check if the OAuth2 provider account is already linked to another UUID
                    if (OAuth2Client.getDatabaseManager().isOAuth2AccountLinked(OAuth2Account.id())) {
                        sendHtmlResponse(exchange, 400, OAuth2Client.getHtmlPage("alreadyLinkedOAuth2").getContent());
                        return;
                    }
                    // Then we can proceed to link the account
                    OAuth2Client.getDatabaseManager().linkAccount(minecraftUUID, OAuth2Account.id(), OAuth2Account.username());
                    // And register the nickname as a LuckPerms suffix
                    LuckPerms.addSuffix(UUID.fromString(minecraftUUID), OAuth2Account.username());
                    moveToLobbyCallback.accept(UUID.fromString(minecraftUUID), "command.linked");
                }

                OAuth2Client.AuthManager().authenticate(UUID.fromString(minecraftUUID));
                sendHtmlResponse(exchange, 200, OAuth2Client.getHtmlPage("linked").getContent());

            } catch (Exception e) { OAuth2Client.logger().error(e.getMessage()); }
        }

    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param))
                return keyValue[1];
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
