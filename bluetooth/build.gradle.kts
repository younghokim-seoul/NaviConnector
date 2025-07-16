plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.cm.bluetooth"
}

dependencies {

    // coroutines
    implementation(libs.coroutines)
    testImplementation(libs.coroutines)
    testImplementation(libs.coroutines.test)

    implementation(libs.timber)
}