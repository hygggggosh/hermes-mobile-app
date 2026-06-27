// Speed up Gradle dependency downloads inside CI by using Aliyun mirrors.
// Applied automatically because it lives under .gradle/init.d/ in the project root.

val mirrors = listOf(
    "https://maven.aliyun.com/repository/public",
    "https://maven.aliyun.com/repository/google",
    "https://maven.aliyun.com/repository/gradle-plugin"
)

settingsEvaluated {
    pluginManagement {
        repositories {
            mirrors.forEach { url -> maven(url) }
            gradlePluginPortal()
        }
    }

    // Override dependencyResolutionManagement so we don't fight FAIL_ON_PROJECT_REPOS.
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories {
            mirrors.forEach { url -> maven(url) }
            google()
            mavenCentral()
        }
    }
}