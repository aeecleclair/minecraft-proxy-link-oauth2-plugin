package fr.aeecleclair.oauth2client.velocity.player.placeholders;

import com.velocitypowered.api.proxy.Player;

public interface Placeholder {
    String getIdentifier();
    String replace(String input, Player player);
}