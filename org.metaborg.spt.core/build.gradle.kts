plugins {
    `java-library`
    `maven-publish`
    id("org.metaborg.convention.java")
    id("org.metaborg.convention.maven-publish")
}

dependencies {
    api(platform(libs.metaborg.platform)) { version { require("latest.integration") } }

    api(project(":org.metaborg.mbt.core"))
    api(libs.spoofax.core)
    api(libs.jsglr.shared)

    implementation(libs.jakarta.annotation)
    implementation(libs.jakarta.inject)
}
