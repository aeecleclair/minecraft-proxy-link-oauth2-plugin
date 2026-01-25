package cz.bloodbear.OAuth2Client.velocity.interfaces;

import com.velocitypowered.api.proxy.Player;

public interface Placeholder {
    String getIdentifier();
    String replace(String input, Player player);
}
