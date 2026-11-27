package dev.spice.spicescoreboard;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

final class VaultHook {

    private final Economy economy;

    private VaultHook(Economy economy) {
        this.economy = economy;
    }

    static VaultHook create(SpiceScoreboard plugin) {
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : new VaultHook(rsp.getProvider());
    }

    double balance(Player player) {
        return economy.getBalance(player);
    }
}
