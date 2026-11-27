package dev.spice.spicescoreboard;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class SpiceScoreboard extends JavaPlugin implements Listener, TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private BoardManager boards;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Hooks hooks = new Hooks(this);
        boards = new BoardManager(this, hooks);

        getServer().getPluginManager().registerEvents(this, this);

        var cmd = getCommand("spicescoreboard");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        boards.start();
        getLogger().info("SpiceScoreboard enabled.");
    }

    @Override
    public void onDisable() {
        if (boards != null) {
            boards.shutdown();
        }
    }

    // ───────────── Events ─────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        getServer().getScheduler().runTask(this, () -> {
            if (p.isOnline()) boards.update(p);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.forget(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        boards.update(e.getPlayer());
    }

    // ───────────── Command ─────────────

    private void send(CommandSender to, String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString("messages." + key, key);
        to.sendMessage(MM.deserialize(prefix + msg));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("spicescoreboard.reload")) {
                    sender.sendMessage(MM.deserialize(getConfig().getString("messages.no-permission", "")));
                    return true;
                }
                reloadConfig();
                boards.reload();
                send(sender, "reloaded");
            }
            case "toggle" -> {
                if (!(sender instanceof Player p)) {
                    send(sender, "players-only");
                    return true;
                }
                if (!p.hasPermission("spicescoreboard.toggle")) {
                    p.sendMessage(MM.deserialize(getConfig().getString("messages.no-permission", "")));
                    return true;
                }
                boolean nowHidden = boards.toggle(p);
                send(p, nowHidden ? "toggled-off" : "toggled-on");
            }
            default -> send(sender, "usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("spicescoreboard.toggle")) out.add("toggle");
            if (sender.hasPermission("spicescoreboard.reload")) out.add("reload");
            out.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
        }
        return out;
    }
}
