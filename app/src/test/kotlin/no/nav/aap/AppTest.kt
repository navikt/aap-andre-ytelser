package no.nav.aap

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import no.nav.aap.kafka.streams.v2.test.KStreamsMock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AppTest {
    @Test
    fun hello() {
        testApplication {
            environment { config = envVars }
            application { server(KStreamsMock()) }
            val response = client.get("actuator/live")
            Assertions.assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}

private val envVars = MapApplicationConfig(
    "KAFKA_BROKERS" to "mock://kafka",
    "KAFKA_STREAMS_APPLICATION_ID" to "ytelser",
    "KAFKA_TRUSTSTORE_PATH" to "",
    "KAFKA_KEYSTORE_PATH" to "",
    "KAFKA_CREDSTORE_PASSWORD" to "",
    "AZURE_OPENID_CONFIG_ISSUER" to "",
    "AZURE_APP_CLIENT_ID" to "",
    "AZURE_OPENID_CONFIG_JWKS_URI" to "",
    "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost",
    "AZURE_APP_CLIENT_SECRET" to "",
)
