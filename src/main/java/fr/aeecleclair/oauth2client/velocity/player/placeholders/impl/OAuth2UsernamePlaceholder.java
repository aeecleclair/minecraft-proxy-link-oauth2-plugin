package fr.aeecleclair.oauth2client.velocity.player.placeholders.impl;

import com.velocitypowered.api.proxy.Player;

import fr.aeecleclair.oauth2client.core.utils.OAuth2Account;
import fr.aeecleclair.oauth2client.velocity.OAuth2Client;
import fr.aeecleclair.oauth2client.velocity.player.placeholders.Placeholder;

public class OAuth2UsernamePlaceholder implements Placeholder {
    @Override
    public String getIdentifier() {
        return "[OAuth2Username]";
    }

    @Override
    public String replace(String input, Player player) {
        if (player != null) {
            OAuth2Account OAuth2Account = OAuth2Client.getDatabaseManager().getOAuth2Account(player.getUniqueId().toString());
            String username = OAuth2Account != null
                ? OAuth2Account.username()
                : OAuth2Client.getMessage("generic.none");
            return input.replace(getIdentifier(), username);
        }
        return input;
    }
}
