// Holds one FileLogWriter per LogBucket, all rooted at logsDir. Built eagerly
// so each bucket file is opened exactly once for the lifetime of the registry,
// avoiding any cross-thread synchronisation when callers ask for a writer.

package com.blockstream.utils

import okio.FileSystem
import okio.Path
import okio.SYSTEM

class FileLogWriterRegistry(
    private val logsDir: Path,
    fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val writers: Map<LogBucket, FileLogWriter> =
        LogBucket.entries.associateWith { bucket ->
            FileLogWriter.create(fileFor(bucket), fileSystem, bucket.minSeverity)
        }

    fun forBucket(bucket: LogBucket): FileLogWriter = writers.getValue(bucket)

    fun fileFor(bucket: LogBucket): Path = logsDir / bucket.fileName
}
