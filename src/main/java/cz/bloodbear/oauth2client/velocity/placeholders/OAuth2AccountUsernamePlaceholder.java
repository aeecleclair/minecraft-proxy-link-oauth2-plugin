package cz.bloodbear.oauth2client.velocity.placeholders;

import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.oauth2client.core.records.OAuth2Account;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;
import cz.bloodbear.oauth2client.velocity.interfaces.Placeholder;

public class OAuth2AccountUsernamePlaceholder implements Placeholder {
    @Override
    public String getIdentifier() {
        return "[OAuth2AccountUsername]";
    }

    @Override
    public String replace(String input, Player player) {
        if (player != null) {
            OAuth2Account OAuth2Account = OAuth2Client.getInstance().getDatabaseManager().getOAuth2Account(player.getUniqueId().toString());
            String username = OAuth2Account != null
                ? OAuth2Account.username()
                : OAuth2Client.getInstance().getMessage("generic.none");
            return input.replace(getIdentifier(), username);
        }
        return input;
    }
}
