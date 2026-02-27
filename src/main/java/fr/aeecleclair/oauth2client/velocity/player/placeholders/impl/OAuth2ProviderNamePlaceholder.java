package fr.aeecleclair.oauth2client.velocity.player.placeholders.impl;

import com.velocitypowered.api.proxy.Player;

import fr.aeecleclair.oauth2client.velocity.player.placeholders.Placeholder;

public class OAuth2ProviderNamePlaceholder implements Placeholder {
    private final String OAuth2ProviderName;

    public OAuth2ProviderNamePlaceholder(String OAuth2ProviderName) {
        this.OAuth2ProviderName = OAuth2ProviderName;
    }

    @Override
    public String getIdentifier() {
        return "[OAuth2ProviderName]";
    }

    @Override
    public String replace(String input, Player player) {
        return input.replace(getIdentifier(), OAuth2ProviderName);
    }
}
