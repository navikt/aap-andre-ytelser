package no.nav.aap.kafka

import no.nav.aap.dto.kafka.AndreFolketrygdytelserKafkaDto
import no.nav.aap.kafka.streams.v2.Topic
import no.nav.aap.kafka.streams.v2.serde.JsonSerde

object Topics {
    val andreYtelser = Topic("aap.andre-folketrygdytelser.v1", JsonSerde.jackson<AndreFolketrygdytelserKafkaDto>())
}
