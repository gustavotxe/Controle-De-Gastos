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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

class InvalidBackupException : Exception()

class BackupJsonCodec @Inject constructor() {
    suspend fun writeRows(output: Writer, items: Flow<Items>, checkActive: () -> Unit): Int {
        var count = 0
        JsonWriter(output).use { json ->
            writeHeader(json)
            items.collect { item ->
                checkActive()
                if (count >= MAX_ITEMS) throw InvalidBackupException()
                writeItem(json, item)
                count++
            }
            json.endArray().endObject()
        }
        return count
    }

    fun write(output: Writer, items: List<Items>, checkActive: () -> Unit) {
        if (items.size > MAX_ITEMS) throw InvalidBackupException()
        JsonWriter(output).use { json ->
            writeHeader(json)
            items.forEach { item ->
                checkActive()
                writeItem(json, item)
            }
            json.endArray().endObject()
        }
    }

    private fun writeHeader(json: JsonWriter) {
        json.setIndent("  ")
        json.beginObject()
        json.name("format").value(FORMAT)
        json.name("version").value(1)
        json.name("amountUnit").value("cents")
        json.name("transactions").beginArray()
    }

    private fun writeItem(json: JsonWriter, item: Items) {
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

    fun read(input: Reader, checkActive: () -> Unit): List<Items> =
        input.use { readRows(it, checkActive).toList() }

    fun readRows(input: Reader, checkActive: () -> Unit): Sequence<Items> = sequence {
        try {
            JsonReader(TokenBoundedReader(input, checkActive)).use { json ->
                var format: String? = null
                var version: Long? = null
                var unit: String? = null
                var hasItems = false
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
                            var count = 0
                            json.beginArray()
                            while (json.hasNext()) {
                                checkActive()
                                require(count < MAX_ITEMS)
                                yield(readItem(json))
                                count++
                            }
                            json.endArray()
                            hasItems = true
                        }
                        else -> throw InvalidBackupException()
                    }
                }
                json.endObject()
                require(json.peek() == JsonToken.END_DOCUMENT)
                require(format == FORMAT && version == 1L && unit == "cents")
                require(hasItems)
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
        return nextString().toLong()
    }

    companion object {
        const val FORMAT = "controle-de-gastos"
        const val MAX_ITEMS = 100_000
        const val MAX_TEXT_LENGTH = 100_000
    }
}

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
