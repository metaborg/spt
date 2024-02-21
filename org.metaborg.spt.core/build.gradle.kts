plugins {
  id("org.metaborg.gradle.config.java-library")
}

fun compositeBuild(name: String) = "$group:$name:$version"
val spoofax2Version: String by ext
dependencies {
  api(platform("org.metaborg:parent:$spoofax2Version"))

  api(project(":org.metaborg.mbt.core"))
  api(compositeBuild("org.metaborg.spoofax.core"))
  api(compositeBuild("jsglr.shared"))

  implementation("jakarta.annotation:jakarta.annotation-api")
}
