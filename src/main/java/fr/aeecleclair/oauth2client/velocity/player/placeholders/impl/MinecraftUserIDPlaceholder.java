package fr.aeecleclair.oauth2client.velocity.player.placeholders.impl;

import com.velocitypowered.api.proxy.Player;

import fr.aeecleclair.oauth2client.velocity.player.placeholders.Placeholder;

public class MinecraftUserIDPlaceholder implements Placeholder {

    @Override
    public String getIdentifier() {
        return "[MinecraftUserID]";
    }

    @Override
    public String replace(String input, Player player) {
        if (player != null)
            return input.replace(getIdentifier(), player.getUniqueId().toString());
        return input;
    }
}
