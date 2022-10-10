package no.nav.aap.kafka

import no.nav.aap.dto.kafka.AndreFolketrygdytelserKafkaDto
import no.nav.aap.kafka.serde.json.JsonSerde
import no.nav.aap.kafka.streams.Topic

object Topics {
    val andreYtelser = Topic("aap.andre-folketrygdytelser.v1", JsonSerde.jackson<AndreFolketrygdytelserKafkaDto>())
}
