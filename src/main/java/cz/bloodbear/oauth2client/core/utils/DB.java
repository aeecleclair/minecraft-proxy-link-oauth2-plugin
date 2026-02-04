package cz.bloodbear.oauth2client.core.utils;

import cz.bloodbear.oauth2client.core.records.OAuth2Account;

import java.util.Map;

public interface DB {

    void linkAccount(String minecraftUUID, String OAuth2AccountId, String OAuth2AccountUsername);

    OAuth2Account getOAuth2Account(String uuid);

    boolean isOAuth2AccountLinked(String OAuth2AccountId);

    void unlinkAccount(String uuid);

    boolean isLinked(String uuid);

    void saveLinkRequest(String uuid, String code);

    String getPlayerByCode(String code);

    void deleteLinkCodes(String uuid);

    void deleteLinkCodes();

    Map<String, String> getAllLinkedAccounts();

    void close();
}