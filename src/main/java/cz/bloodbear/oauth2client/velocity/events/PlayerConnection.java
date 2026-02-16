package cz.bloodbear.oauth2client.velocity.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;

public class PlayerConnection {

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        // TODO: is there a use to it?
        if (OAuth2Client.getDatabaseManager().isLinked(event.getPlayer().getUniqueId().toString())) {
            // empty
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        OAuth2Client.AuthManager().revoke(event.getPlayer().getUniqueId());
    }

}
