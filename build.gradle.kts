import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.hilt) apply false
}

private typealias AndroidExtension = com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>

private val Project.androidExtension: AndroidExtension get() = extensions.getByType(com.android.build.api.dsl.CommonExtension::class.java)

private fun Project.android(block: AndroidExtension.() -> Unit) {
    plugins.withType<com.android.build.gradle.BasePlugin>().configureEach {
        androidExtension.block()
    }
}

private val targetSdkVersion = libs.versions.targetSdk.get().toInt()
private val bytecodeVersion = JavaVersion.toVersion(libs.versions.jvmBytecode.get())

subprojects {
    android {
        defaultConfig {
            vectorDrawables.useSupportLibrary = true
        }

        compileOptions {
            sourceCompatibility = bytecodeVersion
            targetCompatibility = bytecodeVersion
        }

        lint {
            abortOnError = false
        }
    }

    plugins.withType<com.android.build.gradle.AppPlugin>().configureEach {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            defaultConfig {
                targetSdk = targetSdkVersion
            }
        }
    }

    // Configurations for `com.android.test` plugin
    plugins.withType<com.android.build.gradle.TestPlugin>().configureEach {
        extensions.configure<com.android.build.api.dsl.TestExtension> {
            defaultConfig {
                targetSdk = targetSdkVersion
            }
        }
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(bytecodeVersion.toString()))
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlin.time.ExperimentalTime"
            )
        }
    }

}