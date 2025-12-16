plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mymusicplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mymusicplayer"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // --- Library AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // --- Library OkHttp (INI TARUH DI SINI)
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation ("androidx.room:room-runtime:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation("com.karumi:dexter:6.2.3")

    // --- Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
