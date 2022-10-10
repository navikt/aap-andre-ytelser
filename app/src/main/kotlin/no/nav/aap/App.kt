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
import no.nav.aap.kafka.streams.KStreams
import no.nav.aap.kafka.streams.KStreamsConfig
import no.nav.aap.kafka.streams.KafkaStreams
import no.nav.aap.kafka.streams.extension.*
import no.nav.aap.ktor.client.AzureConfig
import no.nav.aap.ktor.config.loadConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory

private val secureLog = LoggerFactory.getLogger("secureLog")

data class Config(
    val kafka: KStreamsConfig,
    val azure: AzureConfig,
)

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

fun Application.server(kafka: KStreams = KafkaStreams) {
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
                val status = if (kafka.isLive()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "vedtak")
            }
            get("/ready") {
                val status = if (kafka.isReady()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "vedtak")
            }
        }
    }
}

private fun topology(): Topology {
    val streams = StreamsBuilder()

    streams.consume(Topics.andreYtelser)
        .filterNotNull("filter-andre-ytelser-tombstone")
        .filter("filter-andre-ytelser-request") { _, value -> value.response == null }
        .mapValues("lag-andre-ytelser-response") { _ -> mockResponse() }
        .produce(Topics.andreYtelser, "produced-andre-ytelser-med-response")

    return streams.build()
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
