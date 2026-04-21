plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    jacoco
}

import java.util.Base64

android {
    namespace = "com.tap.apk"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tap.apk"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreBase64 = System.getenv("SIGNING_KEYSTORE_BASE64")
    val useReleaseSigning = !keystoreBase64.isNullOrBlank()
    val keystoreFile = layout.buildDirectory.file("signing/release.keystore").get().asFile

    if (useReleaseSigning) {
        keystoreFile.parentFile.mkdirs()
        keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
    }

    signingConfigs {
        create("release") {
            if (useReleaseSigning) {
                storeFile = keystoreFile
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (useReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")

    val buildDirPath = layout.buildDirectory.get().asFile.absolutePath

    val kotlinClasses = fileTree("$buildDirPath/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val javaClasses = fileTree("$buildDirPath/intermediates/javac/debug/classes") {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(files("$buildDirPath/jacoco/testDebugUnitTest.exec"))
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
