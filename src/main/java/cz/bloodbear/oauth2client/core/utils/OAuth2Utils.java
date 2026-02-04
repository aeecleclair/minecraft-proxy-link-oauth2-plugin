package cz.bloodbear.oauth2client.core.utils;

public abstract class OAuth2Utils {
    public static String getOAuth2Client(String AuthUrl, String clientId, String redirectUri, String linkCode, String scope) {
        return AuthUrl+"/auth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + scope
                + "&state=" + linkCode;
    }
}
