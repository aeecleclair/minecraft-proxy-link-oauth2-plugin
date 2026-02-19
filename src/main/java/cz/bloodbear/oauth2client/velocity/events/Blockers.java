package cz.bloodbear.oauth2client.velocity.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;

public class Blockers {

    @Subscribe(priority = Short.MAX_VALUE)
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;
        if (OAuth2Client.AuthManager().isAuthenticated(player.getUniqueId())) return;

        var command = event.getCommand().split(" ")[0];

        if (command.equals("myecl")) return;
        player.sendMessage(OAuth2Client.formatMessage("<red>Your account is not linked. Please link your account with /myecl login to join the server.</red>"));
        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onServerConnect(ServerPreConnectEvent event) {

        if (OAuth2Client.AuthManager().isAuthenticated(event.getPlayer().getUniqueId())) return;

        String allowed = OAuth2Client.limbo(); // servers allowed before linking
        if (!event.getOriginalServer().getServerInfo().getName().equals(allowed)) {
            event.getPlayer().sendMessage(OAuth2Client.formatMessage("<red>Your account is not linked. Please link your account with /myecl login to join the server.</red>"));
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            if (event.getPlayer().getCurrentServer().isEmpty()) 
                event.getPlayer().disconnect(OAuth2Client.formatMessage("<red>Limbo server is down</red>"));
        }
    }


}
