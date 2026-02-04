package cz.bloodbear.oauth2client.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import cz.bloodbear.oauth2client.core.utils.TabCompleterHelper;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;
import cz.bloodbear.oauth2client.core.utils.CodeGenerator;
import cz.bloodbear.oauth2client.velocity.utils.DatabaseManager;
import cz.bloodbear.oauth2client.core.utils.OAuth2Utils;
import cz.bloodbear.oauth2client.velocity.utils.PlaceholderRegistry;
import net.luckperms.api.LuckPermsProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OAuth2Command implements SimpleCommand {
    public OAuth2Command() {
    }

    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.generic.playeronly")));
            return;
        }

        if(!hasPermission(source)) {
            source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.noperms", (Player)invocation.source())));
            return;
        }

        if (args.length == 0) {
            source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.invite", (Player)invocation.source())));
            return;
        }

        if(!hasPermission(source, args[0].toLowerCase())) {
            source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.noperms", (Player)invocation.source())));
            return;
        }

        DatabaseManager databaseManager = OAuth2Client.getInstance().getDatabaseManager();
        Player player = (Player)invocation.source();

        if (args[0].equalsIgnoreCase("link")) {
            if (OAuth2Client.getInstance().getAuthManager().isAuthenticated(player.getUniqueId())) {
                source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.alreadyauthenticated", player)));
                return;
            }
            /*if (databaseManager.isLinked(player.getUniqueId().toString())) {
                source.sendMessage(oauth2client.getInstance().formatMessage(oauth2client.getInstance().getMessage("command.oauth2.alreadylinked", player)));
                return;
            }*/


            databaseManager.deleteLinkCodes(player.getUniqueId().toString());
            String code = CodeGenerator.generateCode();
            databaseManager.saveLinkRequest(player.getUniqueId().toString(), code);
            String url = OAuth2Utils.getOAuth2Client(OAuth2Client.getInstance().getAuthUrl(), OAuth2Client.getInstance().getClientId(), OAuth2Client.getInstance().getRedirectUri(), code, "profile");
            player.sendMessage(OAuth2Client.getInstance().formatMessage(PlaceholderRegistry.replacePlaceholders(OAuth2Client.getInstance().getMessage("command.oauth2.link", player).replace("[linkUrl]", url), player)));
            return;
        }

        if (args[0].equalsIgnoreCase("unlink")) {
            if (!databaseManager.isLinked(player.getUniqueId().toString())) {
                source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.notlinked", player)));
                return;
            }

            databaseManager.unlinkAccount(player.getUniqueId().toString());
            source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.unlinked", player)));
            return;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!databaseManager.isLinked(player.getUniqueId().toString())) {
                source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.notlinked", player)));
                return;
            }

            source.sendMessage(OAuth2Client.getInstance().formatMessage(OAuth2Client.getInstance().getMessage("command.oauth2.info", player)));
        }
    }

    public List<String> suggest(SimpleCommand.Invocation invocation) {
        if(!(invocation.source() instanceof Player)) return new ArrayList<>();

        if(invocation.arguments().length <= 1) {
            List<String> choices = Arrays.asList("link", "unlink", "info");
            List<String> finalChoices = new ArrayList<>();
            choices.forEach(choice -> {
                if(hasPermission(invocation.source(), choice)) finalChoices.add(choice);
            });
            if(invocation.arguments().length == 0) {
                return choices;
            }
            return TabCompleterHelper.getArguments(finalChoices, invocation.arguments()[0]);
        }

        return new ArrayList<>();
    }

    public static boolean hasPermission(CommandSource source) {
        if(source instanceof ConsoleCommandSource) return true;
        return LuckPermsProvider.get().getUserManager().getUser(((Player) source).getUniqueId()).getCachedData().getPermissionData().checkPermission("oauth2client.player").asBoolean();
    }

    public static boolean hasPermission(CommandSource source, String subcommand) {
        if(source instanceof ConsoleCommandSource) return true;
        return (LuckPermsProvider.get().getUserManager().getUser(((Player) source).getUniqueId()).getCachedData().getPermissionData().checkPermission(String.format("oauth2client.player.%s", subcommand.toLowerCase())).asBoolean()
        || LuckPermsProvider.get().getUserManager().getUser(((Player) source).getUniqueId()).getCachedData().getPermissionData().checkPermission("oauth2client.player.*").asBoolean());
    }
}
