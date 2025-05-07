import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // use JUnit5 for tests
    id("de.mannodermaus.android-junit5") version "1.10.0.0"
    jacoco
    alias(libs.plugins.compose.compiler)
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.withType<Test> {
    extensions.configure<JacocoTaskExtension> {}
}

tasks.withType<JacocoReport> {
    executionData.setFrom(fileTree(mapOf("dir" to "${project.layout.buildDirectory.get().asFile}", "includes" to listOf("jacoco/test.exec"))))
    reports {
        html
        xml
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "Verification"
    description = "Generates Jacoco coverage reports after running tests."

    dependsOn("test")
    dependsOn("createDebugCoverageReport")

    val debugTree = fileTree(mapOf("dir" to "${project.layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug"))
    val mainSrc = "${project.projectDir}/src/main/java"

    classDirectories.setFrom(files(debugTree))
    sourceDirectories.setFrom(files(mainSrc))
    executionData.setFrom(fileTree(mapOf("dir" to "${project.layout.buildDirectory.get().asFile}", "includes" to listOf("jacoco/test.exec"))))

    doLast {
        println("View coverage report at file://${reports.html.outputLocation.get().asFile}/index.html")
    }
}

android {
    namespace = "com.aoeai.media"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aoeai.media"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        getByName("debug") {
            testCoverage {
                enableUnitTestCoverage = true
                enableAndroidTestCoverage = true
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
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildToolsVersion = "35.0.0"

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as ApkVariantOutputImpl
            output.outputFileName = "AoeMedia-${variant.versionName}-${variant.buildType.name}.apk"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    implementation(libs.accompanist.drawablepainter)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

//    implementation(libs.coil)
//    implementation(libs.coil.network.okhttp)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
//    implementation(libs.coil.compose)
    // Jetpack Paging 3库，它专为高效处理大量数据而设计
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jupiter.junit.jupiter)
    androidTestImplementation(libs.jupiter.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}