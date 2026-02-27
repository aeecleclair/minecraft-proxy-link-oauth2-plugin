package fr.aeecleclair.oauth2client.velocity.player.placeholders.impl;

import com.velocitypowered.api.proxy.Player;

import fr.aeecleclair.oauth2client.velocity.player.placeholders.Placeholder;

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
