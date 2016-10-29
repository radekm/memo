buildscript {
    extra["dokkaVersion"] = "0.9.9"

    repositories {
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
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
    gradleScriptKotlin()
}

dependencies {
    compile(kotlinModule("stdlib"))
    compile(kotlinModule("stdlib", "$embeddedKotlinVersion:javadoc"))
    compile(kotlinModule("stdlib", "$embeddedKotlinVersion:sources"))

    testCompile("junit:junit:4.12")
    testCompile("junit:junit:4.12:javadoc")
    testCompile("junit:junit:4.12:sources")
}
