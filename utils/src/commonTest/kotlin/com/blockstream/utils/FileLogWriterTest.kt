package com.blockstream.utils

import co.touchlab.kermit.Severity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class FileLogWriterTest {

    private val fs: FileSystem = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var file: Path
    private var writer: FileLogWriter? = null

    @BeforeTest
    fun setup() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "FileLogWriterTest-${Random.nextLong()}"
        fs.createDirectories(dir)
        file = dir / "test.log"
    }

    @AfterTest
    fun teardown() {
        runBlocking {
            writer?.close()
            delay(50)
        }
        runCatching { fs.deleteRecursively(dir) }
    }

    private fun newWriter(
        minSeverity: Severity = Severity.Verbose,
        maxBytes: Long = 1024 * 1024,
    ): FileLogWriter = FileLogWriter(file, fs, minSeverity, maxBytes).also { writer = it }

    @Test
    fun isLoggableRespectsMinSeverity() {
        val w = newWriter(minSeverity = Severity.Warn)
        assertFalse(w.isLoggable("any", Severity.Verbose))
        assertFalse(w.isLoggable("any", Severity.Debug))
        assertFalse(w.isLoggable("any", Severity.Info))
        assertTrue(w.isLoggable("any", Severity.Warn))
        assertTrue(w.isLoggable("any", Severity.Error))
        assertTrue(w.isLoggable("any", Severity.Assert))
    }

    @Test
    fun writesLogLineWithTagAndSeverity() = runBlocking {
        val w = newWriter()
        w.log(Severity.Info, "hello", "TagX", null)
        awaitContent { it.contains("TagX[Info]:hello") }
    }

    @Test
    fun writesMultipleLinesInOrder() = runBlocking {
        val w = newWriter()
        w.log(Severity.Info, "first", "T", null)
        w.log(Severity.Info, "second", "T", null)
        w.log(Severity.Info, "third", "T", null)
        awaitContent { content ->
            val lines = content.lines().filter { it.isNotEmpty() }
            lines.size == 3 &&
                lines[0].endsWith(":first") &&
                lines[1].endsWith(":second") &&
                lines[2].endsWith(":third")
        }
    }

    @Test
    fun appendsStackTraceWhenThrowableProvided() = runBlocking {
        val w = newWriter()
        val ex = IllegalStateException("boom")
        w.log(Severity.Error, "failed", "T", ex)
        awaitContent { it.contains("failed") && it.contains("IllegalStateException") }
    }

    @Test
    fun truncatesFileWhenMaxBytesReached() = runBlocking {
        val w = newWriter(maxBytes = 200)
        repeat(50) { i ->
            w.log(Severity.Info, "padding-padding-padding-$i", "T", null)
        }
        await {
            val size = if (fs.exists(file)) fs.metadata(file).size ?: 0L else 0L
            size in 1..200
        }
    }

    @Test
    fun truncatesPreExistingFileOnConstruction() = runBlocking {
        fs.write(file) { writeUtf8("pre-existing content\n") }
        val w = newWriter()
        w.log(Severity.Info, "fresh", "T", null)
        awaitContent { !it.contains("pre-existing") && it.contains("fresh") }
    }

    private suspend fun awaitContent(timeoutMs: Long = 2000, check: (String) -> Boolean) {
        await(timeoutMs) {
            val content = if (fs.exists(file)) fs.read(file) { readUtf8() } else ""
            check(content)
        }
    }

    private suspend fun await(timeoutMs: Long = 2000, check: () -> Boolean) {
        val start = TimeSource.Monotonic.markNow()
        while (start.elapsedNow() < timeoutMs.milliseconds) {
            if (check()) return
            delay(10)
        }
        error("Timeout after ${timeoutMs}ms waiting for condition")
    }
}
