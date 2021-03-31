plugins {
    java
    // add
    application
    id("org.openjfx.javafxplugin") version "0.0.9"
    id("com.github.ben-manes.versions") version "0.38.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

application {
	mainClassName = "com.suushiemaniac.cubing.bld.santascramble.Main"
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("com.suushiemaniac:cubing.bld.suite:3.0")
    implementation("com.suushiemaniac:lang.json:2.0")

    implementation("org.worldcubeassociation.tnoodle:lib-scrambles:0.18.0")
    implementation("org.worldcubeassociation.tnoodle:scrambler-min2phase:0.18.0")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://dl.bintray.com/thewca/tnoodle-lib")
}

javafx {
    version = "11"
    modules = listOf("javafx.controls", "javafx.fxml")
}