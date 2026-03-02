package fr.aeecleclair.oauth2client.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import fr.aeecleclair.oauth2client.core.adapters.DatabaseManager;
import fr.aeecleclair.oauth2client.velocity.player.LuckPerms;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class OAuth2Command implements SimpleCommand {
    public OAuth2Command() {
    }

    static String generateCode() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static List<String> getArguments(List<String> choices, String argument) {
        List<String> arguments = new ArrayList<>();
        if (argument.isEmpty()) return choices;
        choices.forEach(choice -> {
            if (choice.toLowerCase().startsWith(argument.toLowerCase()))
                arguments.add(choice);
        });

        return arguments;
    }

    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(
                OAuth2Client.formatMessage(
                    OAuth2Client.getMessage("command.playeronly", null)
                )
            );
            return;
        }
        Player player = (Player) invocation.source();

        if (!LuckPerms.hasPermission(player, null)) {
            player.sendMessage(
                OAuth2Client.formatMessage(
                    OAuth2Client.getMessage("command.noperms", player)
                )
            );
            return;
        }
        if (args.length != 1) {
            player.sendMessage(
                OAuth2Client.formatMessage(
                    OAuth2Client.getMessage("command.usage", player)
                )
            );
            return;
        }
        if (!LuckPerms.hasPermission(player, args[0].toLowerCase())) {
            player.sendMessage(
                OAuth2Client.formatMessage(
                    OAuth2Client.getMessage("command.noperms", player)
                )
            );
            return;
        }

        DatabaseManager databaseManager = OAuth2Client.getDatabaseManager();

        if (args[0].equalsIgnoreCase("login")) {
            if (OAuth2Client.AuthManager().isAuthenticated(player.getUniqueId())) {
                player.sendMessage(
                    OAuth2Client.formatMessage(
                        OAuth2Client.getMessage("command.alreadyloggedin", player)
                    )
                );
                return;
            }

            databaseManager.deleteLinkCodes(player.getUniqueId().toString());
            String code = generateCode();
            databaseManager.saveLinkRequest(player.getUniqueId().toString(), code);
            String url = OAuth2Client.OAuth2Handler().makeAuthorizationURL(code);
            player.sendMessage(
                OAuth2Client.formatMessage(
                    OAuth2Client.getMessage("command.link", player).replace("[linkUrl]", url)
                )
            );
        }

        if (args[0].equalsIgnoreCase("logout")) {
            if (!databaseManager.isLinked(player.getUniqueId().toString())) {
                player.sendMessage(
                    OAuth2Client.formatMessage(
                        OAuth2Client.getMessage("command.notlinked", player)));
                return;
            }

            if (!OAuth2Client.AuthManager().isAuthenticated(player.getUniqueId())) {
                player.sendMessage(
                    OAuth2Client.formatMessage(
                        OAuth2Client.getMessage("command.notloggedin", player)
                    )
                );
                return;
            }
            OAuth2Client.AuthManager().revoke(player.getUniqueId());
            OAuth2Client.moveToLimbo(player);
            player.sendMessage(
                OAuth2Client.formatMessage(
                    OAuth2Client.getMessage("command.loggedout", player)
                )
            );
        }

        if (args[0].equalsIgnoreCase("unlink")) {
            if (!databaseManager.isLinked(player.getUniqueId().toString())) {
                player.sendMessage(
                    OAuth2Client.formatMessage(
                        OAuth2Client.getMessage("command.notlinked", player)));
                return;
            }

            if (!OAuth2Client.AuthManager().isAuthenticated(player.getUniqueId())) {
                player.sendMessage(
                    OAuth2Client.formatMessage(
                        OAuth2Client.getMessage("command.notloggedin", player)
                    )
                );
                return;
            }


            OAuth2Client.AuthManager().revoke(player.getUniqueId());
            LuckPerms.removeSuffix(player.getUniqueId());
            databaseManager.unlinkAccount(player.getUniqueId().toString());
            OAuth2Client.moveToLimbo(player);
            player.sendMessage(
                OAuth2Client.formatMessage(
                    OAuth2Client.getMessage("command.unlinked", player)
                )
            );
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!databaseManager.isLinked(player.getUniqueId().toString())) {
                player.sendMessage(
                    OAuth2Client.formatMessage(
                        OAuth2Client.getMessage("command.notlinked", player)
                    )
                );
                return;
            }

            player.sendMessage(
                    OAuth2Client.formatMessage(
                            OAuth2Client.getMessage("command.info", player)));
        }
    }

    public List<String> suggest(SimpleCommand.Invocation invocation) {
        if (!(invocation.source() instanceof Player))
            return new ArrayList<>();

        if (invocation.arguments().length <= 1) {
            List<String> choices = Arrays.asList("login", "logout", "unlink", "info");
            List<String> finalChoices = new ArrayList<>();
            choices.forEach(choice -> {
                if (LuckPerms.hasPermission((Player) invocation.source(), choice))
                    finalChoices.add(choice);
            });
            if (invocation.arguments().length == 0)
                return choices;
            return getArguments(finalChoices, invocation.arguments()[0]);
        }

        return new ArrayList<>();
    }
}
