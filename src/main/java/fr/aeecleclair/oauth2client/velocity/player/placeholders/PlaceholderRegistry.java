package fr.aeecleclair.oauth2client.velocity.player.placeholders;

import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderRegistry {
    private static final Map<String, Placeholder> placeholders = new HashMap<>();

    public static void registerPlaceholder(Placeholder placeholder) {
        placeholders.put(placeholder.getIdentifier(), placeholder);
    }

    public static String replacePlaceholders(String text, Player player) {
        for (Placeholder placeholder : placeholders.values())
            text = placeholder.replace(text, player);
        return text;
    }
}
