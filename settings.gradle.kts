pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS：忽略（含 init 脚本注入的）项目级仓库，由 settings 统一管理
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 阿里云镜像加速国内下载，官方源兜底
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
    }
}

rootProject.name = "mini-you"
include(":app")
