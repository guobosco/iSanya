// 项目级构建配置文件
// 作用：定义项目中使用的插件版本和全局任务

plugins {
    // Android Gradle Plugin (AGP) - Android 项目构建插件
    // 版本：8.13.2 - 用于构建和打包 Android 应用
    // apply false - 表示此插件不会直接应用到根项目，而是由子模块（如 app 模块）根据需要应用
    id("com.android.application") version "8.13.2" apply false
    
    // Kotlin Android 插件 - 支持在 Android 项目中使用 Kotlin 语言
    // 版本：1.9.20 - 与 Java 21 兼容的版本
    // apply false - 表示此插件不会直接应用到根项目，而是由子模块根据需要应用
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    

}

// 注册自定义任务
// 任务名称：clean - 用于清理项目构建产物
// 任务类型：org.gradle.api.tasks.Delete - 删除指定目录或文件
// 任务操作：delete(rootProject.buildDir) - 删除根项目的构建目录
// 执行方式：./gradlew clean
tasks.register("clean", org.gradle.api.tasks.Delete::class) {
    delete(rootProject.buildDir)
}
