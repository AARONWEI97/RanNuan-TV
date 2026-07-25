import java.time.LocalDate
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rannuan.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rannuan.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "2.1.4"

        // API 基址
        // 模拟器默认：http://10.0.2.2:3000（10.0.2.2 = 宿主机 localhost）
        // 真机打包：gradlew :app:installDebug -PserverUrl="http://你的IP:3000"
        buildConfigField("String", "SERVER_URL",
            "\"${project.findProperty("serverUrl") ?: "http://47.108.80.234:3000"}\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity + Navigation
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil 图片加载
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}

// ============================================================
// 将已经签名并验证过的 APK 暂存到服务端更新目录，同时生成 latest.json。
// 普通 assembleRelease 不发布版本，避免 APK 尚未上传时客户端先看到更新。
// 用法: gradle :app:stageUpdate -PupdateApk="D:/path/RanNuan-TV.apk"
// ============================================================
tasks.register("stageUpdate") {
    group = "rannuan"
    description = "复制已签名 APK 到服务端更新目录并生成带大小和 SHA-256 的 latest.json"

    doLast {
        val vc = android.defaultConfig.versionCode
        val vn = android.defaultConfig.versionName
            ?: throw GradleException("versionName 未配置")
        val apkArgument = providers.gradleProperty("updateApk").orNull
            ?.takeIf { it.isNotBlank() }
            ?: throw GradleException("请通过 -PupdateApk=... 指定已经签名的 APK")
        val sourceApk = file(apkArgument)
        if (!sourceApk.isFile || !sourceApk.name.endsWith(".apk", ignoreCase = true)) {
            throw GradleException("updateApk 不是有效的 APK 文件: ${sourceApk.absolutePath}")
        }

        val targetDir = rootDir.resolve("../../server/public/dataupdate")
        val apkDir = targetDir.resolve("apk")
        val targetFile = targetDir.resolve("latest.json")
        val publishedApk = apkDir.resolve("RanNuan-TV-v$vn.apk")

        if (!apkDir.exists() && !apkDir.mkdirs()) {
            throw GradleException("无法创建更新目录: ${apkDir.absolutePath}")
        }
        sourceApk.copyTo(publishedApk, overwrite = true)

        val digest = MessageDigest.getInstance("SHA-256")
        publishedApk.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

        // 如果已有 latest.json，保留更新说明和强制更新策略。
        val existing: Map<String, Any?> = if (targetFile.exists()) {
            try {
                val json = groovy.json.JsonSlurper().parseText(targetFile.readText()) as Map<String, Any?>
                json
            } catch (_: Exception) { emptyMap() }
        } else { emptyMap() }

        val changelog = (existing["changelog"] as? List<*>) ?: emptyList<String>()
        val forceUpdate = (existing["forceUpdate"] as? Boolean) ?: false
        val minSupported = (existing["minSupportedVersionCode"] as? Number)?.toInt() ?: 1

        val json = """
{
  "enabled": true,
  "versionCode": $vc,
  "versionName": "$vn",
  "downloadUrl": "/dataupdate/apk/RanNuan-TV-v$vn.apk",
  "sizeBytes": ${publishedApk.length()},
  "sha256": "$sha256",
  "forceUpdate": $forceUpdate,
  "minSupportedVersionCode": $minSupported,
  "publishedAt": "${LocalDate.now()}",
  "changelog": ${groovy.json.JsonOutput.toJson(changelog)}
}
        """.trimIndent()

        targetFile.writeText(json + "\n")
        println("更新文件已暂存: versionCode=$vc versionName=$vn")
        println("APK: ${publishedApk.absolutePath}")
        println("latest.json: ${targetFile.absolutePath}")
    }
}
