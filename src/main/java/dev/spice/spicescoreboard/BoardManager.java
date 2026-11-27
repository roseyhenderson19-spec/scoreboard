package dev.spice.spicescoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BoardManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int MAX_LINES = 15;
    private static final Statistic PLAY_STAT = resolvePlayStat();

    private final SpiceScoreboard plugin;
    private final Hooks hooks;
    private final NamespacedKey hiddenKey;
    private final Map<UUID, PlayerBoard> boards = new HashMap<>();
    private BukkitTask task;

    BoardManager(SpiceScoreboard plugin, Hooks hooks) {
        this.plugin = plugin;
        this.hooks = hooks;
        this.hiddenKey = new NamespacedKey(plugin, "hidden");
    }

    private static final class PlayerBoard {
        final Scoreboard scoreboard;
        final Objective objective;
        Component title;
        List<Component> lines = new ArrayList<>();
        int registered = 0;

        PlayerBoard(Scoreboard scoreboard, Objective objective) {
            this.scoreboard = scoreboard;
            this.objective = objective;
        }
    }

    // ───────────── Lifecycle ─────────────

    void start() {
        stop();
        long interval = Math.max(1L, plugin.getConfig().getLong("update-interval-ticks", 20L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, interval);
    }

    private void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    void reload() {
        for (PlayerBoard pb : boards.values()) {
            pb.title = null;
            pb.lines = new ArrayList<>(); // force a full redraw
        }
        start();
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    void shutdown() {
        stop();
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerBoard pb = boards.get(p.getUniqueId());
            if (pb != null && sm != null && p.getScoreboard() == pb.scoreboard) {
                p.setScoreboard(sm.getMainScoreboard());
            }
        }
        boards.clear();
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    // ───────────── Toggle / visibility ─────────────

    /** @return true if the board is now hidden. */
    boolean toggle(Player p) {
        boolean hidden = isHidden(p);
        if (hidden) {
            p.getPersistentDataContainer().remove(hiddenKey);
            update(p);
            return false;
        }
        p.getPersistentDataContainer().set(hiddenKey, PersistentDataType.BYTE, (byte) 1);
        remove(p);
        return true;
    }

    private boolean isHidden(Player p) {
        return p.getPersistentDataContainer().has(hiddenKey, PersistentDataType.BYTE);
    }

    private boolean shouldShow(Player p) {
        if (!p.hasPermission("spicescoreboard.use")) return false;
        if (isHidden(p)) return false;
        return !plugin.getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName());
    }

    // ───────────── Board handling ─────────────

    void forget(UUID id) {
        boards.remove(id);
    }

    private void remove(Player p) {
        PlayerBoard pb = boards.remove(p.getUniqueId());
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (pb != null && sm != null && p.getScoreboard() == pb.scoreboard) {
            p.setScoreboard(sm.getMainScoreboard());
        }
    }

    private PlayerBoard create() {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return null;
        Scoreboard sb = sm.getNewScoreboard();
        Objective obj = sb.registerNewObjective("spicesb", Criteria.DUMMY, Component.empty());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        return new PlayerBoard(sb, obj);
    }

    void update(Player p) {
        if (!shouldShow(p)) {
            if (boards.containsKey(p.getUniqueId())) remove(p);
            return;
        }

        PlayerBoard pb = boards.get(p.getUniqueId());
        if (pb == null) {
            pb = create();
            if (pb == null) return;
            boards.put(p.getUniqueId(), pb);
        }
        if (p.getScoreboard() != pb.scoreboard) {
            p.setScoreboard(pb.scoreboard);
        }

        FileConfiguration cfg = plugin.getConfig();
        TagResolver resolver = resolverFor(p, cfg);

        // Title
        Component title = MM.deserialize(cfg.getString("title", "<gold>Scoreboard"), resolver);
        if (!title.equals(pb.title)) {
            pb.objective.displayName(title);
            pb.title = title;
        }

        // Lines
        List<String> templates = cfg.getStringList("lines");
        int count = Math.min(templates.size(), MAX_LINES);

        for (int i = 0; i < count; i++) {
            String template = templates.get(i);
            Component line = MM.deserialize(template.isBlank() ? " " : template, resolver);

            if (i < pb.lines.size() && line.equals(pb.lines.get(i))) continue;

            String entry = entryFor(i);
            Score score = pb.objective.getScore(entry);
            score.setScore(count - i);
            score.customName(line);
            score.numberFormat(NumberFormat.blank());

            if (i < pb.lines.size()) pb.lines.set(i, line);
            else pb.lines.add(line);
        }

        // Config shrank: clear leftover lines
        for (int i = count; i < pb.registered; i++) {
            pb.scoreboard.resetScores(entryFor(i));
        }
        while (pb.lines.size() > count) {
            pb.lines.remove(pb.lines.size() - 1);
        }
        pb.registered = count;
    }

    /** Unique invisible entry per line: "§0", "§1" ... "§e". */
    private static String entryFor(int index) {
        return "\u00a7" + Integer.toHexString(index);
    }

    private TagResolver resolverFor(Player p, FileConfiguration cfg) {
        String symbol = cfg.getString("currency-symbol", "$");
        boolean abbreviated = !"full".equalsIgnoreCase(cfg.getString("money-format", "abbreviated"));

        Double balance = hooks.balance(p);
        String money = Format.money(balance == null ? 0 : balance, abbreviated, symbol);

        long seconds = 0;
        if (PLAY_STAT != null) {
            seconds = p.getStatistic(PLAY_STAT) / 20L;
        }

        return TagResolver.resolver(
                Placeholder.unparsed("player", p.getName()),
                Placeholder.unparsed("money", money),
                Placeholder.unparsed("playtime", Format.duration(seconds)),
                Placeholder.unparsed("online", String.valueOf(Bukkit.getOnlinePlayers().size())),
                Placeholder.component("rank", hooks.rank(p))
        );
    }

    /** The playtime statistic has been renamed across versions; resolve it by name. */
    private static Statistic resolvePlayStat() {
        for (String name : new String[]{"PLAY_ONE_MINUTE", "PLAY_TIME"}) {
            try {
                return Statistic.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // try next
            }
        }
        return null;
    }
}
