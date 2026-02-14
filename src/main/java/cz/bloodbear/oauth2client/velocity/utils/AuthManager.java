package cz.bloodbear.oauth2client.velocity.utils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private final Set<UUID> authenticated = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void authenticate(UUID uuid) {
        if (uuid == null) return;
        authenticated.add(uuid);
    }

    public void revoke(UUID uuid) {
        if (uuid == null) return;
        authenticated.remove(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        if (uuid == null) return false;
        return authenticated.contains(uuid);
    }
}
