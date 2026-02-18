package cz.bloodbear.oauth2client.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import cz.bloodbear.oauth2client.core.utils.ConsoleColor;
import cz.bloodbear.oauth2client.velocity.commands.OAuth2Command;
import cz.bloodbear.oauth2client.velocity.events.Blockers;
import cz.bloodbear.oauth2client.velocity.events.PlayerConnection;
import cz.bloodbear.oauth2client.velocity.placeholders.OAuth2IdPlaceholder;
import cz.bloodbear.oauth2client.velocity.placeholders.OAuth2AccountUsernamePlaceholder;
import cz.bloodbear.oauth2client.velocity.placeholders.PlayerNamePlaceholder;
import cz.bloodbear.oauth2client.velocity.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

@Plugin(
    id = "oauth2client", 
    name = "OAuth2Client", 
    version = "25.7",
    authors = { "Mtn16", "warix8" }, 
    url = "https://github.com/aeecleclair/minecraft-proxy-link-oauth2-plugin",
    description = "A Velocity plugin for OAuth2 integration.",
    dependencies = {
        @Dependency(id = "luckperms", optional = false),
        @Dependency(id = "plan", optional = true)
    }
)
public class OAuth2Client {
    private static OAuth2Client instance;

    public final ConsoleColor logger;
    private final ProxyServer server;
    private final PluginContainer container;

    private final JsonConfig config;
    private final JsonConfig messages;
    private final MiniMessage miniMessage;

    private final HtmlPage linkedPage;
    private final HtmlPage failedPage;
    private final HtmlPage missingCodePage;
    private final HtmlPage missingStatePage;
    private final HtmlPage invalidPage;
    private final HtmlPage alreadyLinkedPage;
    private final String redirect;

    private final DatabaseManager databaseManager;
    private final WebServer webServer;
    private final OAuth2Handler OAuth2Handler;
    private final AuthManager authManager;

    private final long startTime;

    @Inject
    public OAuth2Client(
        ProxyServer server,
        Logger logger,
        @DataDirectory Path dataDirectory,
        PluginContainer container
    ) {
        instance = this;

        this.server = server;
        this.logger = new ConsoleColor(logger);
        this.container = container;

        this.logger.debug("Starting OAuth2 plugin...");

        config = new JsonConfig(dataDirectory, "config.json");
        messages = new JsonConfig(dataDirectory, "messages.json");
        miniMessage = MiniMessage.miniMessage();
        authManager = new AuthManager();

        startTime = System.currentTimeMillis();
        linkedPage = new HtmlPage(dataDirectory, "linked.html");
        failedPage = new HtmlPage(dataDirectory, "failed.html");
        missingCodePage = new HtmlPage(dataDirectory, "missingCode.html");
        missingStatePage = new HtmlPage(dataDirectory, "missingState.html");
        invalidPage = new HtmlPage(dataDirectory, "invalid.html");
        alreadyLinkedPage = new HtmlPage(dataDirectory, "alreadyLinked.html");

        databaseManager = new DatabaseManager(
            config.getString("database.host", ""),
            config.getInt("database.port", 3306),
            config.getString("database.name", ""),
            config.getString("database.username", ""),
            config.getString("database.password", ""),
            config.getBoolean("database.useSSL", false)
        );

        webServer = new WebServer(
            config.getInt("webserver.port", 80),
            config.getBoolean("webserver.domain.use", false),
            config.getString("webserver.domain.domain", "")
        );

        if (config.getBoolean("webserver.domain.use", false)) {
            if (config.getBoolean("webserver.domain.https", false)) {
                redirect = "https://" + config.getString("webserver.domain.domain", "") + "/callback";
            } else {
                redirect = "http://" + config.getString("webserver.domain.domain", "") + "/callback";
            }
        } else {
            redirect = "http://" + config.getString("webserver.ip", "") + ":" + config.getString("webserver.port", "")  + "/callback";
        }
        this.OAuth2Handler = new OAuth2Handler(
            config.getString("oauth2.url", ""),
            config.getString("oauth2.client.id", ""),
            config.getString("oauth2.client.secret", ""),
            redirect
        );

        PlaceholderRegistry.registerPlaceholder(new PlayerNamePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OAuth2IdPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OAuth2AccountUsernamePlaceholder());

        this.logger.info("Started OAuth2 plugin successfully!");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try { webServer.start(); } catch (IOException e) {
            logger.error(e.getMessage());
            server.shutdown();
        }

        server.getEventManager().register(this, new PlayerConnection());
        server.getEventManager().register(this, new Blockers());
        CommandManager commandManager = server.getCommandManager();
        CommandMeta OAuth2CommandMeta = commandManager.metaBuilder("myecl").plugin(container).build();

        commandManager.register(OAuth2CommandMeta, new OAuth2Command());

        databaseManager.deleteLinkCodes();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        databaseManager.close();
        webServer.stop();
    }

    public static @NotNull String getMessage(String key) {
        return instance.messages.getString(key, "<red>Unknown message: " + key + "</red>");
    }

    public static @NotNull String getMessage(String key, Player player) {
        return PlaceholderRegistry.replacePlaceholders(
            instance.messages.getString(key, "<red>Unknown message: " + key + "</red>"),
            player
        );
    }

    public static HtmlPage getHtmlPage(String name) {
        if (name.equalsIgnoreCase("linked")) {
            return instance.linkedPage;
        } else if (name.equalsIgnoreCase("stateMissing")) {
            return instance.missingStatePage;
        } else if (name.equalsIgnoreCase("codeMissing")) {
            return instance.missingCodePage;
        } else if (name.equalsIgnoreCase("invalid")) {
            return instance.invalidPage;
        } else if (name.equalsIgnoreCase("failed")) {
            return instance.failedPage;
        } else if (name.equalsIgnoreCase("alreadyLinked")) {
            return instance.alreadyLinkedPage;
        }
        return null;
    }

    public static DatabaseManager getDatabaseManager() { return instance.databaseManager; }

    public static OAuth2Handler OAuth2Handler() { return instance.OAuth2Handler; }

    public static AuthManager AuthManager() { return instance.authManager; }

    public static ConsoleColor logger() { return instance.logger; }

    public static ProxyServer getServer() { return instance.server; }

    public static Component formatMessage(String input) { return instance.miniMessage.deserialize(input); }

    public static String getClientId() { return instance.config.getString("oauth2.client.id", ""); }
    public static String getRedirectUri() { return instance.redirect; }
    public static String getAuthUrl() { return instance.config.getString("oauth2.url", ""); }

    // TODO: use that in /myecl info
    public static Duration getUptime() { return Duration.ofMillis(System.currentTimeMillis() - instance.startTime); }

    // TODO: use that in /myecl info
    static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();

        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
