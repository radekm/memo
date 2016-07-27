buildscript {

    extra["kotlinVersion"] = "1.1-M01"
    extra["dokkaVersion"] = "0.9.9"
    extra["repo"] = "https://repo.gradle.org/gradle/repo"

    repositories {
        maven { setUrl(extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${extra["dokkaVersion"]}")
    }
}

apply {
    plugin("kotlin")
    plugin("org.jetbrains.dokka")
    plugin<ApplicationPlugin>()
}

configure<ApplicationPluginConvention> {
    mainClassName = "cz.radekm.memo.ProgramKt"
}

repositories {
    maven { setUrl(extra["repo"]) }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib:${extra["kotlinVersion"]}")
    compile("org.jetbrains.kotlin:kotlin-stdlib:${extra["kotlinVersion"]}:javadoc")
    compile("org.jetbrains.kotlin:kotlin-stdlib:${extra["kotlinVersion"]}:sources")

    testCompile("junit:junit:4.12")
    testCompile("junit:junit:4.12:javadoc")
    testCompile("junit:junit:4.12:sources")
}
