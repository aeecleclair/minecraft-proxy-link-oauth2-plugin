package fr.aeecleclair.oauth2client.velocity;

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

import fr.aeecleclair.oauth2client.core.adapters.ConsoleColor;
import fr.aeecleclair.oauth2client.core.adapters.DatabaseManager;
import fr.aeecleclair.oauth2client.core.adapters.HtmlPage;
import fr.aeecleclair.oauth2client.core.adapters.JsonConfig;
import fr.aeecleclair.oauth2client.core.adapters.OAuth2Handler;
import fr.aeecleclair.oauth2client.core.adapters.WebServer;
import fr.aeecleclair.oauth2client.core.utils.AuthManager;
import fr.aeecleclair.oauth2client.velocity.event.Blockers;
import fr.aeecleclair.oauth2client.velocity.event.PlayerConnection;
import fr.aeecleclair.oauth2client.velocity.player.placeholders.PlaceholderRegistry;
import fr.aeecleclair.oauth2client.velocity.player.placeholders.impl.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@Plugin(
    id = "oauth2client", 
    name = "OAuth2Client", 
    version = "25.7",
    authors = { "Mtn16", "warix8", "Marc-Andrieu" }, 
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

    private final HtmlPage missingStatePage;
    private final HtmlPage missingCodePage;

    private final HtmlPage invalidStatePage;
    private final HtmlPage invalidCodePage;

    private final HtmlPage alreadyLinkedMinecraftPage;
    private final HtmlPage alreadyLinkedOAuth2Page;

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
        Path HTMLDirectory = dataDirectory.resolve("html");
        linkedPage = new HtmlPage(HTMLDirectory, "linked.html");
        missingStatePage = new HtmlPage(HTMLDirectory, "missingState.html");
        missingCodePage = new HtmlPage(HTMLDirectory, "missingCode.html");
        invalidStatePage = new HtmlPage(HTMLDirectory, "invalidState.html");
        invalidCodePage = new HtmlPage(HTMLDirectory, "invalidCode.html");
        alreadyLinkedMinecraftPage = new HtmlPage(HTMLDirectory, "alreadyLinkedMinecraft.html");
        alreadyLinkedOAuth2Page = new HtmlPage(HTMLDirectory, "alreadyLinkedMinecraft.html");

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
            config.getInt("webserver.threads", 4),
            config.getString("webserver.callback", "/callback"),
            config.getString("oauth2.url", ""),
            OAuth2Client::moveToLobby
        );

        String redirectURI = (config.getBoolean("webserver.domain.use", false)
                && config.getBoolean("webserver.domain.https", false)
                ? "https://" : "http://")
            + (config.getBoolean("webserver.domain.use", false)
                ? config.getString("webserver.domain.domain", "")
                : config.getString("webserver.ip", "") + ":" + config.getString("webserver.port", ""))
            + config.getString("webserver.callback", "/callback");

        this.OAuth2Handler = new OAuth2Handler(
            config.getString("oauth2.url", ""),
            config.getString("oauth2.client.id", ""),
            config.getString("oauth2.client.secret", ""),
            redirectURI,
            config.getString("oauth2.endpoints.authorization", ""),
            config.getString("oauth2.endpoints.token", ""),
            config.getString("oauth2.endpoints.userinfo", ""),
            config.getString("oauth2.scope", ""),
            config.getString("oauth2.claim", "")
        );

        PlaceholderRegistry.registerPlaceholder(new MinecraftUserIDPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new MinecraftUsernamePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OAuth2UserIDPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OAuth2UsernamePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OAuth2ProviderNamePlaceholder(
            config.getString("oauth2.provider", "")));
        PlaceholderRegistry.registerPlaceholder(new MinecraftCommandNamePlaceholder(
            config.getString("server.command", "")));

        this.logger.info("Started OAuth2 plugin successfully!");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try { webServer.start(); } catch (IOException e) {
            logger.error(e.getMessage());
            server.shutdown();
        }
        String commandName = config.getString("server.command", "");

        server.getEventManager().register(this, new PlayerConnection());
        server.getEventManager().register(this, new Blockers(commandName));
        CommandManager commandManager = server.getCommandManager();
        CommandMeta OAuth2CommandMeta = commandManager.metaBuilder(commandName).plugin(container).build();

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
        } else if (name.equalsIgnoreCase("invalidState")) {
            return instance.invalidStatePage;
        } else if (name.equalsIgnoreCase("invalidCode")) {
            return instance.invalidCodePage;
        } else if (name.equalsIgnoreCase("alreadyLinkedMinecraft")) {
            return instance.alreadyLinkedMinecraftPage;
        } else if (name.equalsIgnoreCase("alreadyLinkedOAuth2")) {
            return instance.alreadyLinkedOAuth2Page;
        }
        return null;
    }

    public static DatabaseManager getDatabaseManager() { return instance.databaseManager; }
    public static OAuth2Handler OAuth2Handler() { return instance.OAuth2Handler; }
    public static AuthManager AuthManager() { return instance.authManager; }
    public static ConsoleColor logger() { return instance.logger; }

    public static Component formatMessage(String input) {
        return instance.miniMessage.deserialize(input);
    }

    private static void moveToServer(Player player, String serverName) {
        player.createConnectionRequest(instance.server.getServer(serverName).orElse(null)).fireAndForget();
    }

    public static void moveToLobby(UUID minecraftUUID, String message) {
        instance.server.getPlayer(minecraftUUID).ifPresent(p -> {
            p.sendMessage(OAuth2Client
                .formatMessage(OAuth2Client.getMessage(message, p)));
            moveToServer(p, instance.config.getString("server.lobby", "lobby"));
        });
    }

    public static void moveToLimbo(Player player) {
        moveToServer(player, instance.config.getString("server.limbo", "limbo"));
    }

    public static String limbo() {
        return instance.config.getString("server.limbo", "limbo");
    }

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
