plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.mobilellmchat"
    compileSdk = 36

    packaging {
        jniLibs {
            useLegacyPackaging = false
            // ✅ 新增：保留 libc++_shared.so
            pickFirsts += setOf("lib/*/libc++_shared.so")
        }
    }

    defaultConfig {
        applicationId = "com.example.mobilellmchat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ 新增：指定支持的 ABI（与 llama 库匹配）
        ndk {
            abiFilters.add("arm64-v8a")  // 只打包 arm64-v8a 架构
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
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

    kotlinOptions {
        jvmTarget = "11"
    }

    // ✅ 新增：配置打包选项，保留 native 库
    packaging {
        jniLibs {
            // 确保从依赖中提取 .so 文件
            useLegacyPackaging = false
        }
        // 保留所有 .so 文件
        resources {
            excludes += setOf("META-INF/*.kotlin_module")
        }
    }
}

dependencies {
    // ========== 核心依赖（原有） ==========
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ========== 网络请求 ==========
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ========== 协程支持 ==========
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ========== Lifecycle 组件 ==========
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ========== UI 组件 ==========
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // ========== 本地数据库 - Room ==========
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ========== JSON 解析 ==========
    implementation("com.google.code.gson:gson:2.10.1")

    // ========== 广播管理器 ==========
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // ========== llama.cpp Android 支持 ==========
    implementation("de.kherud:llama:3.3.0")
}

// ✅ 修改后的完整任务
tasks.register("extractNativeLibs") {
    doLast {
        // 1. 从 llama.jar 提取 .so 文件
        val llamaJar = project.configurations.getByName("debugRuntimeClasspath")
            .resolvedConfiguration.resolvedArtifacts
            .find { it.moduleVersion.id.toString().contains("de.kherud:llama:") }
            ?.file

        val jniLibsDir = file("src/main/jniLibs/arm64-v8a")
        jniLibsDir.mkdirs()

        if (llamaJar != null && llamaJar.exists()) {
            println("✅ 找到 llama.jar: ${llamaJar.absolutePath}")

            // 解压 JAR 中的 .so 文件
            copy {
                from(zipTree(llamaJar)) {
                    include("**/Linux-Android/aarch64/*.so")
                    eachFile {
                        path = name
                    }
                    includeEmptyDirs = false
                }
                into(jniLibsDir)
            }

            println("✅ .so 文件已提取到: ${jniLibsDir.absolutePath}")
        } else {
            println("❌ 未找到 llama.jar")
        }

        // ✅ 2. 从 Android NDK 复制必需的系统库
        val ndkPath = System.getenv("ANDROID_NDK_HOME")
            ?: "D:\\Users\\nkf\\AppData\\Local\\Android\\Sdk\\ndk\\26.3.11579264"  // ← 修改为你的路径

        val ndkDir = file(ndkPath)
        if (ndkDir.exists()) {
            println("✅ 找到 Android NDK: ${ndkDir.absolutePath}")

            val ndkLibDir = file("$ndkPath/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/aarch64-linux-android")

            // 复制 libc++_shared.so
            val libcppFile = file("$ndkLibDir/libc++_shared.so")
            if (libcppFile.exists()) {
                copy {
                    from(libcppFile)
                    into(jniLibsDir)
                }
                println("✅ 已复制 libc++_shared.so")
            }

            // ✅ 复制 libomp.so
            val libompFile = file("$ndkLibDir/libomp.so")
            if (libompFile.exists()) {
                copy {
                    from(libompFile)
                    into(jniLibsDir)
                }
                println("✅ 已复制 libomp.so")
            } else {
                println("⚠️ NDK 中未找到 libomp.so，请手动下载")
            }

        } else {
            println("⚠️ 未找到 Android NDK，请手动复制 libc++_shared.so 和 libomp.so")
        }
    }
}

// 确保在编译前提取 native 库
tasks.named("preBuild") {
    dependsOn("extractNativeLibs")
}