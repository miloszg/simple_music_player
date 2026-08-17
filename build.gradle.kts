buildscript {
    // AGP 9's built-in Kotlin resolves the Kotlin Gradle Plugin off the root
    // buildscript classpath and pins it to whatever AGP was built against
    // (2.2.10 for AGP 9.3.1). That compiler cannot read metadata produced by
    // newer Kotlin, and several current dependencies — Coil 3.5 among them —
    // ship 2.4 metadata. Raising the constraint here upgrades the built-in
    // compiler for the whole build. Must stay in step with `kotlin` in
    // libs.versions.toml, which also drives the Compose and serialization
    // plugin versions.
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
