package dev.spice.spicescoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

/**
 * Soft-dependency bridge. Vault and LuckPerms classes are only touched
 * once we know the plugin is actually present, so the plugin still loads
 * fine if either is missing.
 */
final class Hooks {

    private final SpiceScoreboard plugin;
    private VaultHook vault;
    private LuckPermsHook luckPerms;

    Hooks(SpiceScoreboard plugin) {
        this.plugin = plugin;
    }

    /** Vault balance (Essentials / EconomyShopGUI economy), or null if unavailable. */
    Double balance(Player player) {
        PluginManager pm = plugin.getServer().getPluginManager();
        if (vault == null && pm.isPluginEnabled("Vault")) {
            vault = VaultHook.create(plugin); // null until an economy provider registers
        }
        return vault == null ? null : vault.balance(player);
    }

    /** Rank as a styled component. */
    Component rank(Player player) {
        PluginManager pm = plugin.getServer().getPluginManager();
        if (luckPerms == null && pm.isPluginEnabled("LuckPerms")) {
            luckPerms = LuckPermsHook.create();
        }

        String fallback = plugin.getConfig().getString("rank-fallback", "<gray>Member");
        if (luckPerms == null) {
            return Format.parseText(fallback);
        }

        String raw = null;
        if ("prefix".equalsIgnoreCase(plugin.getConfig().getString("rank-source", "prefix"))) {
            raw = luckPerms.prefix(player);
        }
        if (raw == null || raw.isBlank()) {
            String group = luckPerms.group(player);
            if (group != null && !group.isBlank()) {
                raw = Format.capitalize(group);
            }
        }
        if (raw == null || raw.isBlank()) {
            raw = fallback;
        }
        return Format.parseText(raw.strip());
    }
}
