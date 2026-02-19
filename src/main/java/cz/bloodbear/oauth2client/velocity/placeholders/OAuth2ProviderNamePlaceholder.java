package cz.bloodbear.oauth2client.velocity.placeholders;

import com.velocitypowered.api.proxy.Player;

import cz.bloodbear.oauth2client.velocity.interfaces.Placeholder;

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
