package cz.bloodbear.oauth2client.velocity.utils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private final Set<UUID> authenticated = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean authenticate(UUID uuid) {
        if (uuid == null) return false;
        return authenticated.add(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        if (uuid == null) return false;
        return authenticated.contains(uuid);
    }

}
