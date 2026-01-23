package cz.bloodbear.discordLink.velocity.utils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private final Set<UUID> authenticated = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AuthManager() {

    }

    public boolean authenticate(UUID uuid) {
        if (uuid == null) return false;
        return authenticated.add(uuid);
    }

    public boolean revoke(UUID uuid) {
        if (uuid == null) return false;
        return authenticated.remove(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        if (uuid == null) return false;
        return authenticated.contains(uuid);
    }

    public Set<UUID> getAuthenticated() {
        return Collections.unmodifiableSet(authenticated);
    }

    public void clear() {
        authenticated.clear();
    }

}
