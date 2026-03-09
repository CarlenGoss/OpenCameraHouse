plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.carlengosez.open_camera_server"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.carlengosez.open_camera_server"
        minSdk = 24
        targetSdk = 34
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
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Versión de CameraX
    val cameraXVersion = "1.3.2"

    // Librerías base de CameraX
    implementation("androidx.camera:camera-core:${cameraXVersion}")
    implementation("androidx.camera:camera-camera2:${cameraXVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraXVersion}")

    // Librería para grabar video (.mp4)
    implementation("androidx.camera:camera-video:${cameraXVersion}")

    // Librería para la vista previa en pantalla
    implementation("androidx.camera:camera-view:${cameraXVersion}")

    // --- KTOR SERVER (Fase 2) ---
    val ktorVersion = "2.3.12"

    // Núcleo del servidor y motor Netty
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    // Para poder enviar listas de videos en formato JSON
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")

    // Para permitir conexiones desde otras apps (CORS)
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
}
