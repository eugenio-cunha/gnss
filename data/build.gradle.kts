import com.android.build.api.variant.BuildConfigField
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.b256.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.b256.android.room)
    alias(libs.plugins.b256.hilt)
    alias(libs.plugins.b256.android.test)
}

android {
    namespace = "br.com.b256.data"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            // Necessário para o Robolectric (usado, por ex., em TelemetryDaoTest).
            isIncludeAndroidResources = true
            // Sem isso, qualquer chamada a um método Android não mockado no jar de unit test
            // (ex.: android.os.Trace, usado por androidx.tracing em NetworkImpl) lança exceção
            // em vez de simplesmente retornar um valor padrão/no-op.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(projects.domain)

    implementation(libs.geo.coords)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    // MockWebServer, para testar NetworkImpl sem bater numa API real.
    testImplementation(libs.okhttp.mockwebserver)
}

private val backendUrl =
    providers.fileContents(
        isolated.rootProject.projectDirectory.file("local.properties"),
    ).asText.map { text ->
        val properties = Properties()
        properties.load(StringReader(text))
        properties["NETWORK_BASE_URL"]
    }.orElse("http://example.com")

androidComponents {
    onVariants {
        it.buildConfigFields!!.put(
            "NETWORK_BASE_URL",
            backendUrl.map { value ->
                BuildConfigField(type = "String", value = """"$value"""", comment = null)
            },
        )
    }
}
