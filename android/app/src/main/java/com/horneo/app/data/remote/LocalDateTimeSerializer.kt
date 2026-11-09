package com.horneo.app.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

// Convierte las fechas del JSON a LocalDateTime. El backend (Spring Boot) las envía como texto en
// formato ISO 8601, por ejemplo "2026-11-09T09:39:35.123456", que LocalDateTime sabe leer directamente.
// java.time necesita Android 8.0 (API 26) o superior: por eso minSdk es 26.
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
}
