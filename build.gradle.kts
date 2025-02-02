import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "nl.fastnet.rebate-uploader"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "rebate-uploader"
            packageVersion = "1.0.2"
            description = "Upload charge card CSV files to the online api"
            copyright = "Copyright (c) 2025 Fastned"
            vendor = "Fastned"

            macOS {
                iconFile.set(project.file("src/main/resources/fastned.icns"))
            }
            windows {
                menu = false
                menuGroup = "Fastned Rebate"
                dirChooser = true
                perUserInstall = true
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "2F26204F-E07A-470E-A1D8-AE3D63A558AE"
                iconFile.set(project.file("src/main/resources/fastned.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/fastned.png"))
            }
        }
    }
}
