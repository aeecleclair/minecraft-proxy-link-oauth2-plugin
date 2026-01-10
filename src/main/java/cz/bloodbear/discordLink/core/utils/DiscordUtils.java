package cz.bloodbear.discordLink.core.utils;

public abstract class DiscordUtils {
    public static String getOAuthLink(String AuthUrl, String clientId, String redirectUri, String linkCode, String scope) {
        return AuthUrl+"/auth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + scope
                + "&state=" + linkCode;
    }
}
