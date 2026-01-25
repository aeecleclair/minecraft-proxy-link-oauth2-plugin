package cz.bloodbear.OAuth2Client.core.utils;

import cz.bloodbear.OAuth2Client.core.records.OAuth2Account;

public interface AuthHandler {
    OAuth2Account getDiscordAccount(String code);
}
