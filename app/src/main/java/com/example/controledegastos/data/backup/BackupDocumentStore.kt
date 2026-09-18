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

/** Only holds the application context; streams are always owned and closed by the caller. */
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

    /** Stage and bound the JSON before touching the chosen destination. */
    fun write(uri: Uri, checkActive: () -> Unit, encode: (Writer) -> Unit) {
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
                bounded.bufferedWriter(Charsets.UTF_8).use(encode)
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
