package cz.bloodbear.OAuth2Client.core.utils.event;

public interface EventListener<T extends Event> {
    void onEvent(T event);
}
