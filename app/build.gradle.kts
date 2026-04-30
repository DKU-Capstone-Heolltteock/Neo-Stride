import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// ── local.properties에서 주소 읽어오기 ──
val properties = Properties()
val propertiesFile = project.rootProject.file("local.properties")
if (propertiesFile.exists()) {
    properties.load(propertiesFile.inputStream())
}
// 파일에 BASE_URL이 없으면 기본 포스트맨 주소를 쓰도록 설정
val baseUrl = properties.getProperty("BASE_URL") ?: "\"https://6b068f2e-91de-4c62-be70-f8302ba5e407.mock.pstmn.io/\""

android {
    namespace = "com.neostride.app"
    compileSdk = 36 // compileSdk 설정 방식 수정 (표준 방식)


    buildFeatures {
        buildConfig = true
    }


    defaultConfig {
        applicationId = "com.neostride.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "BASE_URL",
            "\"${project.findProperty("BASE_URL") ?: "http://10.0.2.2:8080/"}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── [중요] 변수를 자바 코드에서 쓸 수 있게 등록 ──
        buildConfigField("String", "BASE_URL", baseUrl)
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

    // ── BuildConfig 기능을 활성화 ──
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // ── 기존
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ── 여기부터 추가 내용 ──

    // Fragment
    implementation("androidx.fragment:fragment:1.6.2")

    // Navigation Component (하단 탭 + Fragment 전환)
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // Retrofit + OkHttp (백엔드 API 통신)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson (JSON 파싱)
    implementation("com.google.code.gson:gson:2.10.1")

    // Google Maps (러닝 측정 지도)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Glide (이미지 로딩)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}