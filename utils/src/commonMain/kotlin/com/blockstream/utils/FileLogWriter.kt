// A Kermit LogWriter that streams log entries to a file. The target file is
// truncated on construction, then each log() call enqueues a formatted line
// to a non-blocking channel that is drained by a background coroutine, so the
// calling thread never blocks on disk I/O. When the file reaches maxBytes it
// is truncated and writing starts over.

package com.blockstream.utils

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer

class FileLogWriter(
    private val file: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val minSeverity: Severity = Severity.Info,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) : LogWriter() {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channel = Channel<String>(Channel.UNLIMITED)

    init {
        scope.launch {
            var sink = fileSystem.sink(file).buffer()
            var written = 0L
            try {
                for (line in channel) {
                    val bytes = line.encodeToByteArray()
                    sink.write(bytes).writeByte(NEWLINE)
                    sink.flush()
                    written += bytes.size + 1
                    if (written >= maxBytes) {
                        sink.close()
                        sink = fileSystem.sink(file).buffer()
                        written = 0
                    }
                }
            } finally {
                sink.close()
            }
        }
    }

    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= minSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line = buildString {
            append("$tag[${severity.name}]:")
            append(message)
            if (throwable != null) {
                append('\n')
                append(throwable.stackTraceToString())
            }
        }
        channel.trySend(line)
    }

    fun close() {
        channel.close()
    }

    companion object {
        private const val NEWLINE = '\n'.code
        private const val DEFAULT_MAX_BYTES: Long = 10L * 1024 * 1024 // 10 MB

        fun create(
            file: Path,
            fileSystem: FileSystem = FileSystem.SYSTEM,
            minSeverity: Severity = Severity.Verbose,
            maxBytes: Long = DEFAULT_MAX_BYTES,
        ): FileLogWriter {
            file.parent?.let { parent ->
                if (!fileSystem.exists(parent)) fileSystem.createDirectories(parent)
            }
            return FileLogWriter(file, fileSystem, minSeverity, maxBytes)
        }
    }
}
