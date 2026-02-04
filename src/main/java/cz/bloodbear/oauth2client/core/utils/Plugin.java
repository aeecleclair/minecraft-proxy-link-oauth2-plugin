package cz.bloodbear.oauth2client.core.utils;

import cz.bloodbear.oauth2client.core.records.RoleEntry;
import cz.bloodbear.oauth2client.core.utils.event.EventBus;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.List;

public interface Plugin {

    String getMessage(String key);

    Page getHtmlPage(String name);

    DB getDatabaseManager();

    AuthHandler getOAuth2Handler();

    Component formatMessage(String input);

    String getClientId();

    String getRedirectUri();

    List<RoleEntry> getRoles();

    boolean isPlaceholderAPIEnabled();

    Duration getUptime();

    EventBus getEventBus();
}