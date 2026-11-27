package dev.spice.spicescoreboard;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

final class LuckPermsHook {

    private final LuckPerms api;

    private LuckPermsHook(LuckPerms api) {
        this.api = api;
    }

    static LuckPermsHook create() {
        try {
            return new LuckPermsHook(LuckPermsProvider.get());
        } catch (IllegalStateException | NoClassDefFoundError e) {
            return null;
        }
    }

    /** Raw prefix string (LPC-MiniMessage prefixes are stored as MiniMessage text). */
    String prefix(Player player) {
        return api.getPlayerAdapter(Player.class).getMetaData(player).getPrefix();
    }

    /** Primary group's display name, or its id if none is set. */
    String group(Player player) {
        User user = api.getPlayerAdapter(Player.class).getUser(player);
        String id = user.getPrimaryGroup();
        Group group = api.getGroupManager().getGroup(id);
        if (group != null) {
            String display = group.getDisplayName();
            if (display != null && !display.isBlank()) return display;
        }
        return id;
    }
}
