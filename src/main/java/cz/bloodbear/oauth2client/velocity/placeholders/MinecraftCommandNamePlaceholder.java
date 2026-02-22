package cz.bloodbear.oauth2client.velocity.placeholders;

import com.velocitypowered.api.proxy.Player;

import cz.bloodbear.oauth2client.velocity.interfaces.Placeholder;

public class MinecraftCommandNamePlaceholder implements Placeholder {
    private final String MinecraftCommandName;

    public MinecraftCommandNamePlaceholder(String MinecraftCommandName) {
        this.MinecraftCommandName = MinecraftCommandName;
    }

    @Override
    public String getIdentifier() {
        return "[MinecraftCommandName]";
    }

    @Override
    public String replace(String input, Player player) {
        return input.replace(getIdentifier(), MinecraftCommandName);
    }
}
