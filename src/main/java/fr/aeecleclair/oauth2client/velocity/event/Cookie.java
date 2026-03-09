package fr.aeecleclair.oauth2client.velocity.event;

import java.nio.charset.StandardCharsets;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import com.velocitypowered.api.proxy.Player;

import fr.aeecleclair.oauth2client.velocity.OAuth2Client;
import net.kyori.adventure.key.Key;

public class Cookie {
    private final Key key = Key.key("token");

    public void storeCookie(Player player, byte[] value) {
        try {
            OAuth2Client.logger().debug("Cookie à stocker pour " + player.getUsername() + " : " + new String(value, StandardCharsets.UTF_8));
            player.storeCookie(key, value);
            OAuth2Client.logger().debug("Cookie stocké !");
        } catch (IllegalArgumentException e) {} // if the player is from a version lower than 1.20.5
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public byte[] onCookieReceive(CookieReceiveEvent event) {
        OAuth2Client.logger().debug("Cookie à recevoir");
        OAuth2Client.logger()
            .debug("Cookie reçu : player " + event.getPlayer().getUsername() + ", clef "
                + event.getOriginalKey().asString() + ", valeur "
                + new String(event.getOriginalData(), StandardCharsets.UTF_8));
        return event.getOriginalData();
        //if (OAuth2Client.AuthManager().isAuthenticated(event.getPlayer().getUniqueId())) return null;
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onProxyConnect(PostLoginEvent event) {
        OAuth2Client.logger().debug("Log PostLoginEvent");
        OAuth2Client.logger().debug("Cookie à demander pour " + event.getPlayer().getUsername());
        try {
            event.getPlayer().requestCookie(key);
            OAuth2Client.logger().debug("Cookie demandé !");
        } catch (IllegalArgumentException e) {} // if the player is from a version lower than 1.20.5
    }

}