plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cm.bluetooth"
}

dependencies {

    // coroutines
    implementation(libs.coroutines)
    testImplementation(libs.coroutines)
    testImplementation(libs.coroutines.test)
}