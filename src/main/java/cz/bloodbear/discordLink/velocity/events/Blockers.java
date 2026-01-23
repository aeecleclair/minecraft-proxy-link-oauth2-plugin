package cz.bloodbear.discordLink.velocity.events;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.discordLink.velocity.DiscordLink;

import java.util.Set;

public class Blockers {

    @Subscribe(order = PostOrder.FIRST)
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;
        if (DiscordLink.getInstance().getAuthManager().isAuthenticated(player.getUniqueId()))
            return;

        var command = event.getCommand().split(" ")[0];

        if (command.equals("discord")) return;
        player.sendMessage(DiscordLink.getInstance().formatMessage("<red>Your account is not linked. Please link your account with /discord link to join the server.</red>"));
        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerConnect(ServerPreConnectEvent event) {

        if (DiscordLink.getInstance().getAuthManager().isAuthenticated(event.getPlayer().getUniqueId()))
            return;

        Set<String> allowed = Set.of("limbo", "limbo1", "limbo2"); // servers allowed before linking

        if (!allowed.contains(event.getOriginalServer().getServerInfo().getName())) {
            event.getPlayer().sendMessage(DiscordLink.getInstance().formatMessage("<red>Your account is not linked. Please link your account with /discord link to join the server.</red>"));
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            if (event.getPlayer().getCurrentServer().isEmpty()) {
                event.getPlayer().disconnect(DiscordLink.getInstance().formatMessage("<red>Limbo server is down</red>"));
            }
        }
    }


}
