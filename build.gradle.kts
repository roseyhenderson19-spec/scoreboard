plugins {
    java
}

group = "dev.spice"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") {
        content { includeGroup("com.github.MilkBowl") }
    }
}

dependencies {
    // Purpur API (includes Paper, Spigot and Bukkit APIs)
    compileOnly("org.purpurmc.purpur:purpur-api:26.1.2.build.+")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") { isTransitive = false }
    compileOnly("net.luckperms:api:5.4")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
    jar {
        archiveFileName.set("SpiceScoreboard-${project.version}.jar")
    }
}
