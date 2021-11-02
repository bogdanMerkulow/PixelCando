buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("gradle.plugin.com.onesignal:onesignal-gradle-plugin:[0.12.10, 0.99.99]")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

tasks.register(
    "clean",
    Delete::class
) {
    delete(rootProject.buildDir)
}