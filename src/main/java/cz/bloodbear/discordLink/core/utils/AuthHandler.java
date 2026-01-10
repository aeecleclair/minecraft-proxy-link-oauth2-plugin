package cz.bloodbear.discordLink.core.utils;

import cz.bloodbear.discordLink.core.records.DiscordAccount;

public interface AuthHandler {
    DiscordAccount getDiscordAccount(String code);
}
