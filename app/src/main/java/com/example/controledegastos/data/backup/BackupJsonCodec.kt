package com.example.controledegastos.data.backup

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.example.controledegastos.data.model.FlowType
import com.example.controledegastos.data.model.Items
import java.io.Reader
import java.io.FilterReader
import java.io.Writer
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class InvalidBackupException : Exception()

/** Versioned, UTF-8 format. Database IDs are informational, never imported as primary keys. */
class BackupJsonCodec @Inject constructor() {
    fun write(output: Writer, items: List<Items>, checkActive: () -> Unit) {
        JsonWriter(output).use { json ->
            json.setIndent("  ")
            json.beginObject()
            json.name("format").value(FORMAT)
            json.name("version").value(1)
            json.name("amountUnit").value("cents")
            json.name("transactions").beginArray()
            items.forEach { item ->
                checkActive()
                validate(item)
                json.beginObject()
                json.name("id").value(item.id)
                json.name("yearMonth").value(item.yearMonth)
                json.name("occurredAtMillis").value(item.occurredAtMillis)
                json.name("description").value(item.description)
                json.name("observation").value(item.observation)
                json.name("io").value(item.io)
                json.name("paymentMethod").value(item.paymentMethod)
                json.name("amountCents").value(item.amountCents)
                json.name("category").value(item.category)
                json.endObject()
            }
            json.endArray().endObject()
        }
    }

    fun read(input: Reader, checkActive: () -> Unit): List<Items> {
        try {
            return JsonReader(TokenBoundedReader(input, checkActive)).use { json ->
                var format: String? = null
                var version: Long? = null
                var unit: String? = null
                var items: List<Items>? = null
                val fields = mutableSetOf<String>()
                json.beginObject()
                while (json.hasNext()) {
                    checkActive()
                    val name = json.nextName()
                    require(fields.add(name))
                    when (name) {
                        "format" -> format = json.string()
                        "version" -> version = json.integer()
                        "amountUnit" -> unit = json.string()
                        "transactions" -> {
                            val rows = ArrayList<Items>()
                            json.beginArray()
                            while (json.hasNext()) {
                                checkActive()
                                require(rows.size < MAX_ITEMS)
                                rows.add(readItem(json))
                            }
                            json.endArray()
                            items = rows
                        }
                        else -> throw InvalidBackupException()
                    }
                }
                json.endObject()
                require(json.peek() == JsonToken.END_DOCUMENT)
                require(format == FORMAT && version == 1L && unit == "cents")
                requireNotNull(items)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoSuchElementException) {
            throw InvalidBackupException()
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException()
        } catch (e: IllegalStateException) {
            throw InvalidBackupException()
        } catch (e: android.util.MalformedJsonException) {
            throw InvalidBackupException()
        } catch (e: java.io.EOFException) {
            throw InvalidBackupException()
        } catch (e: java.nio.charset.CharacterCodingException) {
            throw InvalidBackupException()
        }
    }

    private fun readItem(json: JsonReader): Items {
        val strings = mutableMapOf<String, String>()
        val numbers = mutableMapOf<String, Long>()
        val fields = mutableSetOf<String>()
        json.beginObject()
        while (json.hasNext()) {
            val name = json.nextName()
            require(fields.add(name))
            when (name) {
                "id", "yearMonth", "occurredAtMillis", "amountCents" -> numbers[name] = json.integer()
                "description", "observation", "io", "paymentMethod", "category" -> strings[name] = json.string()
                else -> throw InvalidBackupException()
            }
        }
        json.endObject()
        val yearMonth = numbers.getValue("yearMonth")
        val amount = numbers.getValue("amountCents")
        val date = numbers.getValue("occurredAtMillis")
        require(yearMonth in 101..999912)
        val flow = strings.getValue("io")
        return Items(0, strings.getValue("description"), strings.getValue("observation"), flow,
            strings.getValue("paymentMethod"), amount, date, yearMonth.toInt(), strings.getValue("category")).also(::validate)
    }

    private fun validate(item: Items) {
        val valid = item.yearMonth / 100 in 1..9999 && item.yearMonth % 100 in 1..12 &&
            item.occurredAtMillis in -62135596800000L..253402300799999L &&
            item.amountCents != Long.MIN_VALUE &&
            ((item.io == FlowType.INFLOW.value && item.amountCents >= 0) ||
                (item.io == FlowType.OUTFLOW.value && item.amountCents <= 0)) &&
            listOf(item.description, item.observation, item.io, item.paymentMethod, item.category)
                .all { it.length <= MAX_TEXT_LENGTH }
        if (!valid) throw InvalidBackupException()
    }

    private fun JsonReader.string(): String {
        require(peek() == JsonToken.STRING)
        return nextString().also { require(it.length <= MAX_TEXT_LENGTH) }
    }

    private fun JsonReader.integer(): Long {
        require(peek() == JsonToken.NUMBER)
        // nextLong may round decimal tokens: parse the original token instead.
        return nextString().toLong()
    }

    companion object {
        const val FORMAT = "controle-de-gastos"
        const val MAX_ITEMS = 100_000
        const val MAX_TEXT_LENGTH = 100_000
    }
}

/** Prevent JsonReader from allocating an arbitrarily large string/number before validation. */
private class TokenBoundedReader(input: Reader, private val checkActive: () -> Unit) : FilterReader(input) {
    private var inString = false
    private var escaped = false
    private var tokenLength = 0

    private fun inspect(character: Char) {
        if (inString) {
            tokenLength++
            if (escaped) escaped = false
            else when (character) {
                '\\' -> escaped = true
                '"' -> { inString = false; tokenLength = 0 }
            }
        } else when {
            character == '"' -> { inString = true; tokenLength = 0 }
            character.isWhitespace() || character in "{}[],:" -> tokenLength = 0
            else -> tokenLength++
        }
        // A valid character can occupy six source characters (a JSON Unicode escape).
        if (tokenLength > BackupJsonCodec.MAX_TEXT_LENGTH * 6) throw InvalidBackupException()
    }

    override fun read(): Int {
        checkActive()
        return `in`.read().also { if (it >= 0) inspect(it.toChar()) }
    }

    override fun read(buffer: CharArray, offset: Int, length: Int): Int {
        checkActive()
        val count = `in`.read(buffer, offset, length)
        for (index in offset until offset + count) inspect(buffer[index])
        return count
    }
}
