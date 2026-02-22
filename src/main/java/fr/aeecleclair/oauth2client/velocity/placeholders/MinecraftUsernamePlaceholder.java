package fr.aeecleclair.oauth2client.velocity.placeholders;

import com.velocitypowered.api.proxy.Player;

import fr.aeecleclair.oauth2client.velocity.interfaces.Placeholder;

public class MinecraftUsernamePlaceholder implements Placeholder {
    @Override
    public String getIdentifier() {
        return "[MinecraftUsername]";
    }

    @Override
    public String replace(String input, Player player) {
        if (player != null)
            return input.replace(getIdentifier(), player.getUsername());
        return input;
    }
}
