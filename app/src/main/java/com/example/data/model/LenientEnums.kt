package com.example.data.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Enums in the shared Firebase state are written by three different clients (Android app,
 * customer web, admin web). If one client stores a Thai label instead of the enum name, the
 * default Moshi enum adapter throws and the app rejects the whole state ("Read data error").
 *
 * These adapters accept the enum name, the Thai display name, or any unknown value and fall
 * back to a sensible default instead of failing the entire document.
 */
class LenientEnumAdapter<T : Enum<T>>(
    private val values: List<T>,
    private val fallback: T,
    private val displayName: (T) -> String = { it.name }
) : JsonAdapter<T>() {

    override fun fromJson(reader: JsonReader): T {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            return fallback
        }
        val raw = try {
            reader.nextString().trim()
        } catch (_: Exception) {
            reader.skipValue()
            return fallback
        }
        if (raw.isEmpty()) return fallback

        return values.firstOrNull { it.name.equals(raw, true) }
            ?: values.firstOrNull { displayName(it) == raw }
            // Multi-select labels arrive as "เสียบไม้, รับไม้เสียบแยก" — keep the first known one.
            ?: values.firstOrNull { raw.contains(displayName(it)) }
            ?: fallback
    }

    override fun toJson(writer: JsonWriter, value: T?) {
        if (value == null) writer.nullValue() else writer.value(value.name)
    }
}

object LenientEnums {

    val FACTORY = JsonAdapter.Factory { type, _, _ ->
        when (type) {
            SkewerOption::class.java -> LenientEnumAdapter(
                SkewerOption.values().toList(),
                SkewerOption.SKEWERED
            ) { it.displayName }

            SauceChoice::class.java -> LenientEnumAdapter(
                SauceChoice.values().toList(),
                SauceChoice.MIXED
            ) { it.displayName }

            OrderStatus::class.java -> LenientEnumAdapter(
                OrderStatus.values().toList(),
                OrderStatus.PENDING
            ) { it.title }

            FulfillmentType::class.java -> LenientEnumAdapter(
                FulfillmentType.values().toList(),
                FulfillmentType.DELIVERY
            ) { it.title }

            ItemStatus::class.java -> LenientEnumAdapter(
                ItemStatus.values().toList(),
                ItemStatus.ACTIVE
            )

            LogoType::class.java -> LenientEnumAdapter(
                LogoType.values().toList(),
                LogoType.CUSTOM_IMAGE
            )

            StampIconType::class.java -> LenientEnumAdapter(
                StampIconType.values().toList(),
                StampIconType.CUSTOM_IMAGE
            )

            else -> null
        }
    }
}
