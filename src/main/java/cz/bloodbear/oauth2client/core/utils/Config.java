package cz.bloodbear.oauth2client.core.utils;

import com.google.gson.JsonObject;
import cz.bloodbear.oauth2client.core.records.RoleEntry;

import java.util.List;

public interface Config {

    void save();
    String getString(String path, String defaultValue);;
    Integer getInt(String path, Integer defaultValue);
    Boolean getBoolean(String path, Boolean defaultValue);

}
