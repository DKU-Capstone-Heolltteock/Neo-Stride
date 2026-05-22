plugins {
    id("com.android.application")
}

android {
    namespace = "com.neostride.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.neostride.wear"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
    // Wear OS 기본
    implementation("androidx.wear:wear:1.3.0")

    // GPS 위치
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // 폰 ↔ 워치 데이터 전송
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Retrofit (서버 직접 저장용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.cardview:cardview:1.0.0")

    implementation("androidx.fragment:fragment:1.6.2")
    implementation("androidx.activity:activity:1.8.0")
}