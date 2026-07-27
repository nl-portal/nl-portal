rootProject.name = "nl-portal-backend"

pluginManagement {
    val kotlinVersion: String =
        providers.gradleProperty("kotlinVersion").get()
    val springBootVersion: String =
        providers.gradleProperty("springBootVersion").get()
    val springDependencyManagementVersion: String =
        providers.gradleProperty("springDependencyManagementVersion").get()
    val benManesVersionsVersion: String =
        providers.gradleProperty("benManesVersionsVersion").get()
    val spotlessVersion: String =
        providers.gradleProperty("spotlessVersion").get()
    val gradleDockerComposeVersion: String =
        providers.gradleProperty("gradleDockerComposeVersion").get()
    val sonarqubeVersion: String =
        providers.gradleProperty("sonarqubeVersion").get()
    val dependencyLicenseVersion: String =
        providers.gradleProperty("dependencyLicenseVersion").get()
    val foojayResolverConventionVersion: String =
        providers.gradleProperty("foojayResolverConventionVersion").get()

    plugins {
        kotlin("jvm") version kotlinVersion apply false
        kotlin("plugin.spring") version kotlinVersion apply false
        kotlin("plugin.jpa") version kotlinVersion apply false
        kotlin("plugin.allopen") version kotlinVersion apply false

        id("org.springframework.boot") version springBootVersion apply false
        id("io.spring.dependency-management") version springDependencyManagementVersion apply false
        id("com.github.ben-manes.versions") version benManesVersionsVersion apply false
        id("com.diffplug.spotless") version spotlessVersion apply false
        id("com.avast.gradle.docker-compose") version gradleDockerComposeVersion apply false
        id("org.sonarqube") version sonarqubeVersion apply false
        id("com.github.jk1.dependency-license-report") version dependencyLicenseVersion apply false
        id("org.gradle.toolchains.foojay-resolver-convention") version foojayResolverConventionVersion apply false
    }
}
include(
    "app",
    "core",
    "gradle:license-report",
    "graphql",
    "portal-authentication",
    "zgw:idtoken-authentication",
    "zgw:common-ground-authentication",
    "zgw:common-ground-authentication-test",
    "zgw:objectenapi",
    "zgw:catalogi-api",
    "zgw:openklant",
    "case",
    "data",
    "messaging",
    "form",
    "haalcentraal-hr",
    "haalcentraal2",
    "zgw:taak",
    "zgw:documenten-api",
    "zgw:berichten",
    "zgw:besluiten",
    "payment",
    "payment-direct",
    "zgw:zaken-api",
    "zgw:openproduct",
    "zgw:verificatie",
    "product",
    "zgw:zaken",
)
