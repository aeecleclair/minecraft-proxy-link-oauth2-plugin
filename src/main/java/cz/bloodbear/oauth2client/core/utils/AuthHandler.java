package cz.bloodbear.oauth2client.core.utils;

import cz.bloodbear.oauth2client.core.records.OAuth2Account;

public interface AuthHandler {
    OAuth2Account getOAuth2Account(String code);
}
