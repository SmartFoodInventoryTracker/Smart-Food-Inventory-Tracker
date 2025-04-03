plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.smartfoodinventorytracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartfoodinventorytracker"
        minSdk = 29
        targetSdk = 35
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ✅ Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    // ✅ Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // (Optional) Firestore for data storage (for future features)
    implementation("com.google.firebase:firebase-firestore")

    // ViewPager2 for swipeable onboarding screens
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Material Components (for TabLayout dots)
    implementation("com.google.android.material:material:1.9.0")

    implementation("androidx.work:work-runtime:2.9.0")

    //  Firebase Realtime Database
    implementation("com.google.firebase:firebase-database")

    //Speedview Gauges
    implementation("com.github.anastr:speedviewlib:1.6.1")

    implementation("androidx.cardview:cardview:1.0.0")

    // Android Charting Library
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")


    // Google ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // CameraX for Live Scanning
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // Guava dependency for ListenableFuture (Required by CameraX)
    implementation("com.google.guava:guava:31.0.1-android")

    // Volley for API calls (Open Food Facts)
    implementation("com.android.volley:volley:1.2.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
}