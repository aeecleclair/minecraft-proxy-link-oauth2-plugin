package cz.bloodbear.oauth2client.velocity.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.bloodbear.oauth2client.core.records.OAuth2Account;
import cz.bloodbear.oauth2client.core.utils.AuthHandler;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;
import okhttp3.*;

import java.io.IOException;

public class OAuth2Handler implements AuthHandler {
    private final String AUTH_URL;
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String REDIRECT_URI;

    private final OkHttpClient httpClient;

    public OAuth2Handler(String AUTH_URL, String CLIENT_ID, String CLIENT_SECRET, String REDIRECT_URI) {
        this.AUTH_URL = AUTH_URL;
        this.CLIENT_ID = CLIENT_ID;
        this.CLIENT_SECRET = CLIENT_SECRET;
        this.REDIRECT_URI = REDIRECT_URI;

        this.httpClient = new OkHttpClient();
    }

    private String getAccessToken(String code) throws IOException {
        RequestBody formBody = (new FormBody.Builder())
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .build();
        Request request = (new Request.Builder()).url(AUTH_URL+"/auth/token").post(formBody).build();

        try (Response response = this.httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                return jsonObject.get("access_token").getAsString();
            }
        }

        return null;
    }

    public OAuth2Account getOAuth2Account(String code) {
        try {
            String accessToken = getAccessToken(code);

            OAuth2Account accountDetails = getAccountDetails(accessToken);
            return new OAuth2Account(accountDetails.id(), accountDetails.username());
        } catch (IOException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
        return null;
    }

    private OAuth2Account getAccountDetails(String accessToken) {
        try {
            Request request = (new Request.Builder())
                    .url(AUTH_URL+"/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = this.httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    String userId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
                    userId = userId.substring(0, userId.indexOf("\""));
                    String nickname = responseBody.substring(responseBody.indexOf("\"nickname\":\"") + 12);
                    nickname = nickname.substring(0, nickname.indexOf("\""));
                    return new OAuth2Account(userId, nickname);
                }
            }
        } catch (IOException e) {
            OAuth2Client.getInstance().getLogger().error(e.getMessage());
        }
        return null;
    }
}
