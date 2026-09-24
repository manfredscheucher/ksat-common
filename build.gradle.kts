plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

// The shared base of the ksat SAT-solver ports: the `SatSolver` interface, `SatResult`,
// the `Traceable` trace hook, and the DIMACS parser. Every solver port implements this,
// so it lives in its own repo and is consumed as a git submodule by each solver repo and
// by the main sat-solvers-kotlin repo. See the README.
kotlin {
    androidTarget()
    jvm()

    js { browser(); nodejs() }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser(); nodejs() }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosArm64() // for the Kotlin/Native benchmark in the minisat port
    linuxX64()
    mingwX64()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "org.bytefred.ksat.common"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
