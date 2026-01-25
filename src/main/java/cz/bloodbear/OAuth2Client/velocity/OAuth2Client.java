package cz.bloodbear.OAuth2Client.velocity;

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
import cz.bloodbear.OAuth2Client.core.utils.event.EventBus;
import cz.bloodbear.OAuth2Client.velocity.commands.OAuth2Command;
import cz.bloodbear.OAuth2Client.velocity.events.Blockers;
import cz.bloodbear.OAuth2Client.velocity.events.PlayerConnection;
import cz.bloodbear.OAuth2Client.velocity.placeholders.OAuth2IdPlaceholder;
import cz.bloodbear.OAuth2Client.velocity.placeholders.OAuth2AccountUsernamePlaceholder;
import cz.bloodbear.OAuth2Client.velocity.placeholders.PlayerNamePlaceholder;
import cz.bloodbear.OAuth2Client.core.records.RoleEntry;
import cz.bloodbear.OAuth2Client.velocity.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Plugin(id = "OAuth2Client", name = "OAuth2Client", version = "25.7",
        authors = {"Mtn16", "warix8"}, url = "https://github.com/aeecleclair/minecraft-proxy-link-oauth2-plugin",
        description = "A Velocity plugin for OAuth2 integration.",
        dependencies = {
            @Dependency(id = "plan", optional = true)
        })
public class OAuth2Client implements cz.bloodbear.OAuth2Client.core.utils.Plugin {
    private static OAuth2Client instance;

    @Inject
    private final Logger logger;
    private final ProxyServer server;
    private final Path dataDirectory;
    private final PluginContainer container;

    private final JsonConfig config;
    private final JsonConfig messages;
    private final JsonConfig sync;
    private final MiniMessage miniMessage;

    private HtmlPage linkedPage;
    private HtmlPage failedPage;
    private HtmlPage missingCodePage;
    private HtmlPage missingStatePage;
    private HtmlPage invalidPage;
    private HtmlPage alreadyLinkedPage;
    private final String redirect;

    private final DatabaseManager databaseManager;
    private final WebServer webServer;
    private final OAuth2Handler oAuth2Handler;
    private final AuthManager authManager;

    private EventBus eventBus;

    private long startTime;

    @Inject
    public OAuth2Client(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, PluginContainer container, AuthManager authManager) {
        instance = this;

        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.container = container;

        this.config = new JsonConfig(dataDirectory, "config.json");
        this.messages = new JsonConfig(dataDirectory, "messages.json");
        this.sync = new JsonConfig(dataDirectory, "sync.json");
        this.miniMessage = MiniMessage.miniMessage();

        this.eventBus = new EventBus();

        startTime = System.currentTimeMillis();

        loadHTML();

        this.databaseManager = new DatabaseManager(
                config.getString("database.host", ""),
                config.getInt("database.port", 3306),
                config.getString("database.name", ""),
                config.getString("database.username", ""),
                config.getString("database.password", ""),
                config.getBoolean("database.useSSL", false)
        );

        this.webServer = new WebServer(
                config.getInt("webserver.port", 80),
                config.getBoolean("webserver.domain.use", false),
                config.getString("webserver.domain.domain", "")
        );

        if(config.getBoolean("webserver.domain.use", false)) {
            if(config.getBoolean("webserver.domain.https", false)) {
                redirect = "https://" + config.getString("webserver.domain.domain", "") + "/callback";
            } else {
                redirect = "http://" + config.getString("webserver.domain.domain", "") + "/callback";
            }
        } else {
            redirect = "http://" + config.getString("webserver.ip", "") + ":" + config.getString("webserver.port", "")  + "/callback";
        }
        this.oAuth2Handler = new OAuth2Handler(
                config.getString("oauth2.url", ""),
                config.getString("oauth2.client.id", ""),
                config.getString("oauth2.client.secret", ""),
                redirect
        );
        this.authManager = authManager;

        loadPlaceholders();
    }

    // private void checkVersion() {
    //     String pluginVersion = getServer().getPluginManager().getPlugin("OAuth2Client").get().getDescription().getVersion().get();
    //     UpdateChecker updateChecker = new UpdateChecker(pluginVersion, "velocity");
    //     if(updateChecker.isNewerVersionAvailable()) {
    //         getServer().getConsoleCommandSource().sendMessage(miniMessage.deserialize("<yellow><b>Update notification:</b></yellow> <green>A OAuth2Client update is available!</green> <newline><newline><yellow>Your version:</yellow> <green>"+ pluginVersion +"</green><newline><yellow>Latest version:</yellow> <green>" + updateChecker.getLatestVersion() + "</green>"));
    //     }
    // }

    private void loadPlaceholders() {
        PlaceholderRegistry.registerPlaceholder(new PlayerNamePlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OAuth2IdPlaceholder());
        PlaceholderRegistry.registerPlaceholder(new OAuth2AccountUsernamePlaceholder());
    }

    private void loadHTML() {
        this.linkedPage = new HtmlPage(dataDirectory, "linked.html");
        this.failedPage = new HtmlPage(dataDirectory, "failed.html");
        this.missingCodePage = new HtmlPage(dataDirectory, "missingCode.html");
        this.missingStatePage = new HtmlPage(dataDirectory, "missingState.html");
        this.invalidPage = new HtmlPage(dataDirectory, "invalid.html");
        this.alreadyLinkedPage = new HtmlPage(dataDirectory, "alreadylinked.html");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            webServer.start();
        } catch (IOException e) {
            logger.error(e.getMessage());
            server.shutdown();
        }

        server.getEventManager().register(this, new PlayerConnection());
        server.getEventManager().register(this, new Blockers());
        CommandManager commandManager = server.getCommandManager();
        CommandMeta OAuth2CommandMeta = commandManager.metaBuilder("oauth2")
                .plugin(container)
                .build();

        CommandMeta adminCommandMeta = commandManager.metaBuilder("oauth2admin")
                .plugin(container)
                .build();

        commandManager.register(OAuth2CommandMeta, new OAuth2Command());

        databaseManager.deleteLinkCodes();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        databaseManager.close();
        webServer.stop();
    }

    public static OAuth2Client getInstance() { return instance; }

    public @NotNull String getMessage(String key) {
        return messages.getString(key, "<red>Unknown message: " + key + "</red>");
    }

    public @NotNull String getMessage(String key, Player player) {
        return PlaceholderRegistry.replacePlaceholders(messages.getString(key, "<red>Unknown message: " + key + "</red>"), player);
    }

    public HtmlPage getHtmlPage(String name) {
        if (name.equalsIgnoreCase("linked")) {
            return linkedPage;
        } else if (name.equalsIgnoreCase("stateMissing")) {
            return missingStatePage;
        } else if (name.equalsIgnoreCase("codeMissing")) {
            return missingCodePage;
        } else if (name.equalsIgnoreCase("invalid")) {
            return invalidPage;
        } else if (name.equalsIgnoreCase("failed")) {
            return failedPage;
        } else if (name.equalsIgnoreCase("alreadylinked")) {
            return alreadyLinkedPage;
        }
        return null;
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }

    public OAuth2Handler getOAuth2Handler() { return oAuth2Handler; }

    public AuthManager getAuthManager() { return authManager; }

    public Logger getLogger() { return logger; }
    public ProxyServer getServer() { return server; }

    public Component formatMessage(String input) {
        return miniMessage.deserialize(input);
    }

    public String getClientId() { return config.getString("oauth2.client.id", ""); }
    public String getRedirectUri() { return redirect; }
    public String getAuthUrl() { return config.getString("oauth2.url", ""); }

    public List<RoleEntry> getRoles() { return sync.getRoles("roles"); }

    public boolean isPlaceholderAPIEnabled() {
        return false;
    }

    public void reloadConfig() {
        config.reload();
        messages.reload();
        sync.reload();

        loadHTML();
    }

    // public boolean getJoinGuild() {
    //     return config.getBoolean("oauth2.link.join_guild", false);
    // }

    // public String getGuildId() {
    //     return config.getString("oauth2.guildId", "");
    // }

    public Duration getUptime() { return Duration.ofMillis(System.currentTimeMillis() - startTime); }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
