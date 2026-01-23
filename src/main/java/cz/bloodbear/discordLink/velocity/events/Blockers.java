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
    public void onChat(PlayerChatEvent event) {
        if (true)
            event.setResult(PlayerChatEvent.ChatResult.denied());
    }


    @Subscribe(order = PostOrder.FIRST)
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;
        if (DiscordLink.getInstance().getDatabaseManager().isLinked(player.getUniqueId().toString()))
            return;

        var command = event.getCommand().split(" ")[0];

        if (command.equals("discord")) return;

        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerConnect(ServerPreConnectEvent event) {

        Set<String> allowed = Set.of("lobby", "lobby2");

        if (!allowed.contains(event.getOriginalServer().getServerInfo().getName())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }


}
