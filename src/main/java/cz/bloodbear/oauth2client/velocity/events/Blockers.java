package cz.bloodbear.oauth2client.velocity.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;

public class Blockers {
    private final String commandName;
    public Blockers(String commandName) {
        this.commandName = commandName;
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;
        String commandName = event.getCommand().split(" ")[0];
        if (commandName.equals(this.commandName)) return;
        if (OAuth2Client.AuthManager().isAuthenticated(player.getUniqueId())) return;

        player.sendMessage(OAuth2Client.formatMessage(OAuth2Client.getMessage(
            OAuth2Client.getDatabaseManager().isLinked(player.getUniqueId().toString())
            ? "command.notloggedin"
            : "command.notlinked")));
        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onServerConnect(ServerPreConnectEvent event) {

        if (OAuth2Client.AuthManager().isAuthenticated(event.getPlayer().getUniqueId())) return;

        String allowed = OAuth2Client.limbo(); // servers allowed before linking
        if (!event.getOriginalServer().getServerInfo().getName().equals(allowed)) {
            event.getPlayer().sendMessage(OAuth2Client.formatMessage(OAuth2Client.getMessage(
                OAuth2Client.getDatabaseManager()
                    .isLinked(event.getPlayer().getUniqueId().toString())
                ? "command.notloggedin"
                : "command.notlinked")));
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            if (event.getPlayer().getCurrentServer().isEmpty()) 
                event.getPlayer().disconnect(OAuth2Client.formatMessage(OAuth2Client.getMessage("generic.limbodown")));
        }
    }


}
