plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.io.File

fun debugLog(
    runId: String,
    hypothesisId: String,
    location: String,
    message: String,
    data: String
) {
    val escapedMessage = message.replace("\"", "\\\"")
    val payload = """{"sessionId":"efe250","runId":"$runId","hypothesisId":"$hypothesisId","location":"$location","message":"$escapedMessage","data":$data,"timestamp":${System.currentTimeMillis()}}"""
    File(rootProject.rootDir, "debug-efe250.log").appendText(payload + "\n")
}

android {
    namespace = "com.example.blegps"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.blegps"
        minSdk = 29
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

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

afterEvaluate {
    // #region agent log
    val compileOptionsExt = extensions.findByName("android")?.let { ext ->
        ext.javaClass.methods.find { method -> method.name == "getCompileOptions" }?.invoke(ext)
    }
    val sourceCompatibility = compileOptionsExt?.javaClass?.methods?.find { method -> method.name == "getSourceCompatibility" }?.invoke(compileOptionsExt)?.toString()
    val targetCompatibility = compileOptionsExt?.javaClass?.methods?.find { method -> method.name == "getTargetCompatibility" }?.invoke(compileOptionsExt)?.toString()
    debugLog(
        runId = "pre-fix",
        hypothesisId = "H1",
        location = "app/build.gradle.kts:afterEvaluate",
        message = "Android compileOptions snapshot",
        data = """{"sourceCompatibility":"${sourceCompatibility ?: "null"}","targetCompatibility":"${targetCompatibility ?: "null"}"}"""
    )
    // #endregion

    tasks.matching { it.name == "compileDebugJavaWithJavac" }.configureEach {
        doFirst {
            // #region agent log
            val options = this.javaClass.methods.find { method -> method.name == "getOptions" }?.invoke(this)
            val release = options?.javaClass?.methods?.find { method -> method.name == "getRelease" }?.invoke(options)?.toString()
            val source = this.javaClass.methods.find { method -> method.name == "getSourceCompatibility" }?.invoke(this)?.toString()
            val target = this.javaClass.methods.find { method -> method.name == "getTargetCompatibility" }?.invoke(this)?.toString()
            debugLog(
                runId = "pre-fix",
                hypothesisId = "H2",
                location = "app/build.gradle.kts:compileDebugJavaWithJavac",
                message = "Java compile task targets",
                data = """{"sourceCompatibility":"${source ?: "null"}","targetCompatibility":"${target ?: "null"}","release":"${release ?: "null"}"}"""
            )
            // #endregion
        }
    }

    tasks.matching { it.name == "compileDebugKotlin" }.configureEach {
        doFirst {
            // #region agent log
            val kotlinOptions = this.javaClass.methods.find { method -> method.name == "getKotlinOptions" }?.invoke(this)
            val jvmTarget = kotlinOptions?.javaClass?.methods?.find { method -> method.name == "getJvmTarget" }?.invoke(kotlinOptions)?.toString()
            debugLog(
                runId = "pre-fix",
                hypothesisId = "H3",
                location = "app/build.gradle.kts:compileDebugKotlin",
                message = "Kotlin compile task target",
                data = """{"jvmTarget":"${jvmTarget ?: "null"}"}"""
            )
            // #endregion
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
}
