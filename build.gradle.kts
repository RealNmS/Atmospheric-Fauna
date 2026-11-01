plugins {
    // This plugin applies the correct loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"   
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

// This can be used for publishing on Modrinth and Curseforge
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

// Formats as "1.21.6 - 1.21.8" for ranges, or "1.21.4" if single version
val mcVersionRange = when {
    compatibleVersions.size > 1 -> "${compatibleVersions.first()} - ${compatibleVersions.last()}"
    compatibleVersions.isNotEmpty() -> compatibleVersions.first()
    else -> sc.current.version
}

val releaseDisplayName = "v${property("mod.version")} [$mcVersionRange]"

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")

    maven("https://maven.terraformersmc.com/") { name = "Terraformers" }
    maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions
    loomx.applyMojangMappings()

    // Fabric Loader
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    // Fabric API
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    // Mod Menu (Optional)
    modCompileOnly("maven.modrinth:modmenu:${property("deps.modmenu")}")
    modLocalRuntime("maven.modrinth:modmenu:${property("deps.modmenu")}")

    // Cloth Config (Optional)
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}")
    modLocalRuntime("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}")

    // Dev Environment Testing Mods
    if (sc.current.version == "26.1") {
        modLocalRuntime("maven.modrinth:spark:1.10.173-fabric")
        modLocalRuntime("maven.modrinth:ok-zoomer:17.0.0-beta.5")
        modLocalRuntime("maven.modrinth:sodium:mc26.1.1-0.8.9-fabric")
        modLocalRuntime("maven.modrinth:sodium-extra:mc26.1.1-0.8.7+fabric")
        modLocalRuntime("maven.modrinth:iris:1.10.9+26.1-fabric")
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run") // Shares the run directory between versions
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        exclude("assets/docs/**")
        exclude("assets/sources/**")
        
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            register("loader", Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        // loomx.mod(Sources)Jar returns the jar task for the applied loom variant
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}

publishMods {
    file.set(loomx.modJar.flatMap { it.archiveFile })

    // Optional: Also publish the sources jar as an additional file
    // additionalFiles.from(loomx.modSourcesJar.flatMap { it.archiveFile })

    val changelogFile = rootProject.file("CHANGELOG.md")
    changelog.set(if (changelogFile.exists()) changelogFile.readText() else "No changelog provided.")

    type.set(STABLE)
    modLoaders.add("fabric")

    displayName.set(releaseDisplayName)
    version.set("${project.version}")

    /*
    modrinth {
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        projectId.set("yd14I2wN")
        minecraftVersions.addAll(compatibleVersions)
    }

    curseforge {
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        projectId.set("1427513")
        minecraftVersions.addAll(compatibleVersions)
    }
    */
}