package cz.bloodbear.oauth2client.core.utils.event;

public interface EventListener<T extends Event> {
    void onEvent(T event);
}
