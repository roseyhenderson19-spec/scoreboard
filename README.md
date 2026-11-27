# SpiceScoreboard

Donut SMP style sidebar for Purpur / Paper 26.1.2.
Shows username, rank (LuckPerms prefix / LPC-MiniMessage), money (Vault) and playtime.

## Build
Requires JDK 25 and Gradle 9.1+ (or open the folder in IntelliJ and run the `jar` task).

    gradle jar

The plugin ends up in `build/libs/SpiceScoreboard-1.0.0.jar`. Drop it in `plugins/`.

## Requirements (all optional except the server)
- Vault + an economy (EssentialsX) -> money. EconomyShopGUI uses the same economy, so it stays in sync.
- LuckPerms -> rank

## Commands
- /spicescoreboard reload  (spicescoreboard.reload)
- /spicescoreboard toggle  (spicescoreboard.toggle)

## No local build setup?
Push this folder to a GitHub repo. The included workflow (.github/workflows/build.yml)
builds the jar automatically: Actions tab -> latest run -> Artifacts -> SpiceScoreboard.
