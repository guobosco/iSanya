// 项目设置文件
// 作用：配置 Gradle 插件管理和依赖解析策略

// 插件管理配置
// 作用：定义用于查找和下载 Gradle 插件的仓库
pluginManagement {
    repositories {
        // 优先使用腾讯云镜像源 - 提高插件下载速度
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        // 腾讯云 Gradle 插件镜像源
        maven {
            url = uri("https://mirrors.tencent.com/gradle-plugin/")
        }
        // 腾讯云 Maven Central 镜像源
        maven {
            url = uri("https://mirrors.tencent.com/maven/central/")
        }
        // 腾讯云 Google 镜像源
        maven {
            url = uri("https://mirrors.tencent.com/maven/google/")
        }
        // Gradle 插件官方仓库 - 作为备选
        gradlePluginPortal()
        // Google 官方仓库 - 作为备选
        google()
        // Maven Central 官方仓库 - 作为备选
        mavenCentral()
    }
}

// 依赖解析管理配置
// 作用：定义项目中所有模块使用的依赖仓库
// repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) - 表示子模块不能定义自己的仓库配置
// 必须使用此文件中定义的仓库配置，确保依赖解析的一致性
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 腾讯云 CloudBase 相关仓库（必须优先）
        // 主仓库 1
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }
        // 主仓库 2
        maven {
            url = uri("https://tencentcloudmaven-1251707799.cos.ap-guangzhou.myqcloud.com/repository/maven-public/")
        }
        // 腾讯云镜像源
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/")
        }
        // 腾讯云 Maven 仓库
        maven {
            url = uri("https://mirrors.tencent.com/maven/central/")
        }
        // Google 官方仓库 - 用于获取 Android 相关依赖
        google()
        // Maven Central 官方仓库 - 用于获取第三方库依赖
        mavenCentral()
        // JitPack 仓库 - 用于获取某些第三方库
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

// 根项目配置
// 设置根项目名称为 "Lulu"
rootProject.name = "Lulu"

// 包含子模块
// 将 ":app" 模块包含到项目中
// ":app" 是应用的主模块，包含实际的应用代码
include(":app")