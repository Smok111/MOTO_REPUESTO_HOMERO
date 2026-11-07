import java.util.Properties

// Configuración de plugins para Android, Kotlin Compose y servicios de Google
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val firebaseConfig = file("google-services.json")
check(firebaseConfig.exists()) {
    "Falta app/google-services.json del proyecto Firebase tallerkhaory."
}
val firebaseJson = groovy.json.JsonSlurper().parse(firebaseConfig) as Map<*, *>
val firebaseProjectId = (firebaseJson["project_info"] as Map<*, *>)["project_id"] as? String
check(firebaseProjectId == "tallerkhaory") {
    "app/google-services.json pertenece a '$firebaseProjectId'; descarga el de tallerkhaory."
}
val firebasePackages = (firebaseJson["client"] as List<*>).map { client ->
    (((client as Map<*, *>)["client_info"] as Map<*, *>)["android_client_info"]
        as Map<*, *>)["package_name"] as? String
}
check("Com.Moto_Kaori" in firebasePackages) {
    "app/google-services.json no contiene la app Android Com.Moto_Kaori."
}

android {
    namespace = "com.khaory.taller"
    compileSdk = 36

    defaultConfig {
        applicationId = "Com.Moto_Kaori"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Carga de la API Key de Google Maps desde local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
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


    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Librerías de Firebase para base de datos y autenticación
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    // Componentes principales de Android y Compose
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Mapas y geolocalización
    implementation(libs.osmdroid)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Pruebas unitarias e instrumentales
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // Extensiones adicionales de Jetpack Compose y utilidades
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    
    // Retrofit para consumo de servicios REST
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    
    // Coil para carga de imágenes asíncronas
    implementation(libs.coil.compose)
}
