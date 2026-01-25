package cz.bloodbear.OAuth2Client.velocity.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import cz.bloodbear.OAuth2Client.velocity.OAuth2Client;

public class PlayerConnection {

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if(OAuth2Client.getInstance().getDatabaseManager().isLinked(event.getPlayer().getUniqueId().toString())) {

        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        OAuth2Client.getInstance().getAuthManager().revoke(event.getPlayer().getUniqueId());
    }

}
