package fr.aeecleclair.oauth2client.velocity.player;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.velocitypowered.api.proxy.Player;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.SuffixNode;

public class LuckPerms {
    private static final UserManager userManager = LuckPermsProvider
            .get()
            .getUserManager();
    
    private static boolean checkPermission(UUID minecraftUUID, String permission) {
        return userManager
                .getUser(minecraftUUID)
            .getCachedData()
            .getPermissionData()
            .checkPermission(permission)
            .asBoolean();
    }

    public static boolean hasPermission(Player player, String subcommand) {
        UUID minecraftUUID = player.getUniqueId();
        return subcommand == null
                ? checkPermission(minecraftUUID, "oauth2client.player")
                : (checkPermission(minecraftUUID, "oauth2client.player.*")
                || checkPermission( minecraftUUID, String.format("oauth2client.player.%s", subcommand.toLowerCase())));
    }
    
    public static void addSuffix(UUID minecraftUUID, String suffix) throws InterruptedException, ExecutionException {
        User user = userManager.loadUser(minecraftUUID).get();
        user.data().add(SuffixNode.builder(suffix, 1).build());
        LuckPermsProvider.get().getUserManager().saveUser(user);
    }
}
