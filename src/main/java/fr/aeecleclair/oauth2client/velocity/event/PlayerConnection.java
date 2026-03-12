package fr.aeecleclair.oauth2client.velocity.event;

import java.util.Collection;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;

import fr.aeecleclair.oauth2client.velocity.OAuth2Client;

public class PlayerConnection {

    // @Subscribe
    // public void onPostLogin(PostLoginEvent event) {
    //     // TODO: is there a use to it?
    //     if (OAuth2Client.getDatabaseManager().isLinked(event.getPlayer().getUniqueId().toString())) {
    //         // empty
    //     }
    // }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onProxyConnect(PostLoginEvent event) {
        OAuth2Client.logger().debug("Log PostLoginEvent");
        OAuth2Client.logger().debug("Cookie à demander pour " + event.getPlayer().getUsername());
        Collection<ResourcePackInfo> a = event.getPlayer().getAppliedResourcePacks();
        OAuth2Client.logger().debug("Cookie reçu !");
        OAuth2Client.logger().debug("debut (" + a.size() + ") : " + a.toString() + " fin");
    }

    public void resourcePackEvent(PlayerResourcePackStatusEvent event) {
        OAuth2Client.logger().debug(event.getPackId().toString());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        OAuth2Client.AuthManager().revoke(event.getPlayer().getUniqueId());
    }

}
