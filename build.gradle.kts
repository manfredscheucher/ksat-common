plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
}

// The shared base of the ksat SAT-solver ports: the `SatSolver` interface, `SatResult`,
// the `Traceable` trace hook, and the DIMACS parser. Every solver port implements this,
// so it lives in its own repo and is consumed as a git submodule by each solver repo and
// by the main ksat repo. See the README.
kotlin {
    androidLibrary {
        namespace = "org.bytefred.ksat.common"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}
    }
    jvm()

    js { browser(); nodejs() }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser(); nodejs() }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosArm64() // for the Kotlin/Native benchmark in the minisat port
    macosX64()
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
