package com.example.controledegastos.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FilterInputStream
import java.io.InputStream
import java.io.IOException
import java.io.File
import java.io.FilterOutputStream
import java.io.Writer
import javax.inject.Inject

class BackupDocumentStore @Inject constructor(@ApplicationContext private val context: Context) {
    fun openInput(uri: Uri): InputStream {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException()
        return object : FilterInputStream(stream) {
            private var count = 0L
            private fun countBytes(size: Int) {
                if (size > 0) count += size
                if (count > MAX_BYTES) throw InvalidBackupException()
            }
            override fun read(): Int = `in`.read().also { if (it >= 0) countBytes(1) }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                `in`.read(buffer, offset, length).also(::countBytes)
        }
    }

    suspend fun <T> withLocalInput(uri: Uri, checkActive: () -> Unit, block: suspend (File) -> T): T {
        val temporary = File.createTempFile("import-", ".json", context.cacheDir)
        try {
            openInput(uri).use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        checkActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
            checkActive()
            return block(temporary)
        } finally {
            temporary.delete()
        }
    }

    suspend fun write(uri: Uri, checkActive: () -> Unit, encode: suspend (Writer) -> Unit) {
        val temporary = File.createTempFile("transactions-", ".json", context.cacheDir)
        try {
            temporary.outputStream().use { fileStream ->
                val bounded = object : FilterOutputStream(fileStream) {
                    private var count = 0L
                    private fun reserve(size: Int) {
                        checkActive()
                        count += size
                        if (count > MAX_BYTES) throw InvalidBackupException()
                    }
                    override fun write(value: Int) { reserve(1); out.write(value) }
                    override fun write(buffer: ByteArray, offset: Int, length: Int) {
                        reserve(length)
                        out.write(buffer, offset, length)
                    }
                }
                bounded.bufferedWriter(Charsets.UTF_8).use { encode(it) }
            }
            checkActive()
            val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
            output.use { destination ->
                temporary.inputStream().use { source ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        checkActive()
                        val read = source.read(buffer)
                        if (read < 0) break
                        destination.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            temporary.delete()
        }
    }

    companion object { const val MAX_BYTES = 50L * 1024 * 1024 }
}
