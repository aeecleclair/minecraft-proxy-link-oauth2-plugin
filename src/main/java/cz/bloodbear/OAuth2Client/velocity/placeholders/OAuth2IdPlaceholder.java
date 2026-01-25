package cz.bloodbear.OAuth2Client.velocity.placeholders;

import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.OAuth2Client.velocity.OAuth2Client;
import cz.bloodbear.OAuth2Client.velocity.interfaces.Placeholder;
import cz.bloodbear.OAuth2Client.core.records.OAuth2Account;

public class OAuth2IdPlaceholder implements Placeholder {
    @Override
    public String getIdentifier() {
        return "[OAuth2Id]";
    }

    @Override
    public String replace(String input, Player player) {
        if (player != null) {
            OAuth2Account OAuth2Account = OAuth2Client.getInstance().getDatabaseManager().getOAuth2Account(player.getUniqueId().toString());
            String id = OAuth2Account != null ? OAuth2Account.id() : OAuth2Client.getInstance().getMessage("generic.none");
            return input.replace(getIdentifier(), id);
        }
        return input;
    }
}
