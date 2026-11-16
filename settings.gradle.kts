pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Atolye"

include(":atolye-design")
include(":toolbox-contract")
include(":toolbox-host")
include(":toolbox-plugin-base")
include(":plugin-speedmeter")
include(":plugin-keepawake")
include(":plugin-uykucu")
include(":plugin-ses-secici")
