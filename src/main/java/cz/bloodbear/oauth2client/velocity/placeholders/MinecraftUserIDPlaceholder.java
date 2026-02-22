package cz.bloodbear.oauth2client.velocity.placeholders;

import com.velocitypowered.api.proxy.Player;

import cz.bloodbear.oauth2client.velocity.interfaces.Placeholder;

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
