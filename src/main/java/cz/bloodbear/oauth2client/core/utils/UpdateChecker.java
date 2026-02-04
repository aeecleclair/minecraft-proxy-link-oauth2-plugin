package cz.bloodbear.oauth2client.core.utils;

public class UpdateChecker {
    // private static final String API_URL = "https://api.modrinth.com/v2/project/oauth2client/version"; // if one day we somehow publish that
    private final String CURRENT_VERSION;
    private final String TARGET_LOADER;
    private String LATEST = null;

    // private final Gson gson = new Gson();

    // private final OkHttpClient client = new OkHttpClient();

    public UpdateChecker(String CURRENT, String LOADER) {
        this.CURRENT_VERSION = CURRENT;
        this.TARGET_LOADER = LOADER;
    }

    public String getLatestVersion() {
        // if(LATEST != null) return LATEST;
        // Request request = new Request.Builder()
        //         .url(API_URL)
        //         .get()
        //         .build();
        // try (Response response = client.newCall(request).execute()) {
        //     if(!response.isSuccessful() || response.body() == null) return null;

        //     String json = response.body().string();
        //     JsonArray versions = gson.fromJson(json, JsonArray.class);

        //     for (JsonElement element : versions) {
        //         JsonObject versionObj = element.getAsJsonObject();

        //         if(!versionObj.get("version_type").getAsString().equalsIgnoreCase("release")) continue;

        //         JsonArray loaders = versionObj.getAsJsonArray("loaders");
        //         for (JsonElement loader : loaders) {
        //             if(loader.getAsString().equalsIgnoreCase(TARGET_LOADER)) {
        //                 LATEST = versionObj.get("version_number").getAsString();
        //                 return LATEST;
        //             }
        //         }
        //     }

        //     return null;
        // } catch (Exception ignored) {
        //     return null;
        // }
        return "0.1.0";
    }

    public boolean isNewerVersionAvailable() {
        // String latest = getLatestVersion();
        // return latest != null && !latest.equals(CURRENT_VERSION);
        return false;
    }
}
