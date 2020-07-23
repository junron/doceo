package com.example.attendance.util

import com.google.firebase.Timestamp
import kotlinx.serialization.*

@Serializer(forClass = Timestamp::class)
object TimestampSerializer : KSerializer<Timestamp> {
    override val descriptor = PrimitiveDescriptor("Timestamp", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeLong(value.seconds)
    }

    override fun deserialize(decoder: Decoder) = Timestamp(decoder.decodeLong(), 0)
}
