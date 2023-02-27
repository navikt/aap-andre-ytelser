package no.nav.aap

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.dto.kafka.AndreFolketrygdytelserKafkaDto
import no.nav.aap.kafka.Topics
import no.nav.aap.kafka.streams.v2.KStreams
import no.nav.aap.kafka.streams.v2.KafkaStreams
import no.nav.aap.kafka.streams.v2.Topology
import no.nav.aap.kafka.streams.v2.config.StreamsConfig
import no.nav.aap.ktor.client.AzureConfig
import no.nav.aap.ktor.config.loadConfig
import org.slf4j.LoggerFactory

private val secureLog = LoggerFactory.getLogger("secureLog")

data class Config(
    val kafka: StreamsConfig,
    val azure: AzureConfig,
)

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server(kafka: KStreams = KafkaStreams()) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = loadConfig<Config>()

    install(MicrometerMetrics) { registry = prometheus }

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> secureLog.error("Uhåndtert feil", e) }
    environment.monitor.subscribe(ApplicationStopping) { kafka.close() }

    kafka.connect(
        config = config.kafka,
        registry = prometheus,
        topology = topology()
    )

    routing {
        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }
            get("/live") {
                val status = if (kafka.live()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "vedtak")
            }
            get("/ready") {
                val status = if (kafka.ready()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "vedtak")
            }
        }
    }
}

private fun topology(): Topology = no.nav.aap.kafka.streams.v2.topology{

    consume(Topics.andreYtelser)
        .filter { value -> value.response == null }
        .map{ _ -> mockResponse() }
        .produce(Topics.andreYtelser)

}

private fun mockResponse() = AndreFolketrygdytelserKafkaDto(
    response = AndreFolketrygdytelserKafkaDto.Response(
        svangerskapspenger = AndreFolketrygdytelserKafkaDto.Response.Svangerskapspenger(
            fom = null,
            tom = null,
            grad = null,
            vedtaksdato = null
        )
    )
)
