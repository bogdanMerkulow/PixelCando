plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
    id("com.onesignal.androidsdk.onesignal-gradle-plugin")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.pixelcando.fityouroutfit"
        minSdk = 21
        targetSdk = 31
        versionCode = 29
        versionName = "1.3.0"
        resourceConfigurations.add("en")
        resourceConfigurations.add("it")
        resourceConfigurations.add("es")
        resourceConfigurations.add("de")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            keyAlias = "release"
            keyPassword = "znR5TgYldG"
            storeFile = file("../keystore.jks")
            storePassword = "H6Vb5ziBTO"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("qa") {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    flavorDimensions.add("default")
    productFlavors {
        create("develop") {
            dimension = "default"
            applicationIdSuffix = ".develop"
            versionNameSuffix = "-dev"
        }
        create("production") {
            dimension = "default"
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    val coroutinesVersion = "1.5.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0-rc01")

    val mobiusVersion = "1.5.3"
    implementation("com.spotify.mobius:mobius-core:$mobiusVersion")
    implementation("com.spotify.mobius:mobius-android:$mobiusVersion")
    implementation("com.spotify.mobius:mobius-extras:$mobiusVersion")
    debugImplementation("org.slf4j:slf4j-api:1.7.30")
    debugImplementation("uk.uuid.slf4j:slf4j-android:1.7.30-0")

    implementation("com.hannesdorfmann:adapterdelegates4-kotlin-dsl-viewbinding:4.3.0")

    implementation("com.github.terrakok:cicerone:7.1")

    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    val moshiVersion = "1.12.0"
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")

    val chuckerVersion = "3.5.2"
    debugImplementation("com.github.chuckerteam.chucker:library:$chuckerVersion")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:$chuckerVersion")
    add("qaImplementation", "com.github.chuckerteam.chucker:library:$chuckerVersion")

    val cameraVersion = "1.0.2"
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:1.0.0-alpha30")
    implementation("androidx.exifinterface:exifinterface:1.3.3")

    implementation("com.elvishew:xlog:1.10.1")

    implementation("io.coil-kt:coil:1.4.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation("org.ocpsoft.prettytime:prettytime:5.0.2.Final")

    implementation(platform("com.google.firebase:firebase-bom:29.3.1"))

    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta2")

    implementation("com.onesignal:OneSignal:[4.0.0, 4.99.99]")

    val appCenterSdkVersion = "4.3.1"
    implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

    testImplementation("junit:junit:4.+")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
